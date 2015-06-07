/*
 * fi.helsinki.cs.iot.kahvihub.admin.EnablerHttpRequestHandler
 * v0.1
 * 2015
 *
 * Copyright 2015 University of Helsinki
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied.
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 */
package fi.helsinki.cs.iot.kahvihub.admin;

import java.util.List;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class EnablerHttpRequestHandler extends HttpRequestHandler {
	
	private static final String TAG = "EnablerHttpRequestHandler";
	
	private static final String ENABLER_NAME = "enablerName";
	private static final String ENABLER_METADATA = "enablerMetadata";
	private static final String ENABLER_PLUGIN_ID = "enablerPluginId";
	
	private final String uriFilter;
	/**
	 * @param pluginFolder
	 * @param uriFilter
	 */
	public EnablerHttpRequestHandler(String uriFilter) {
		this.uriFilter = uriFilter;
	}
	
	private class EnablerFormDetails {

		private String name;
		private String metadata;
		private PluginInfo pluginInfo;

		private EnablerFormDetails(String name, String metadata,
				PluginInfo pluginInfo) {
			this.name = name;
			this.metadata = metadata;
			this.pluginInfo = pluginInfo;
		}

		private boolean hasName() {
			return name != null && name.length() > 0;
		}

		private boolean hasPlugin() {
			return pluginInfo != null;
		}

		private boolean isEmpty () {
			return name == null && metadata == null && pluginInfo == null;
		}

		@Override
		public String toString() {
			return "EnablerFormDetails [name=" + name + ", metadata="
					+ metadata + ", pluginInfo=" + pluginInfo + "]";
		}	
	}
	
	private EnablerFormDetails getEnablerFormDetails(Map<String, String> parameters, Map<String, String> files) {
		String name = parameters.get(ENABLER_NAME);
		String metadata = parameters.get(ENABLER_METADATA);
		if (parameters.containsKey(ENABLER_PLUGIN_ID)) {
			long pluginId = Long.parseLong(parameters.get(ENABLER_PLUGIN_ID));
			return new EnablerFormDetails(name, metadata, IotHubDataAccess.getInstance().getPluginInfo(pluginId));
		}
		else {
			return new EnablerFormDetails(name, metadata, null);
		}
	}

	private boolean checkEnablerFormDetails(EnablerFormDetails efd) {
		if (efd.isEmpty()) {
			return false;
		}
		else {
			return efd.hasName() && efd.hasPlugin();
		}
	}
	
	private Response getInstallEnabler(long pluginId) {
		String html = "<html>";
		html += "<head><title>Install a new enabler for your Kahvihub</title></head>";
		html += "<body>";
		html += "<form method=\"POST\" enctype=\"multipart/form-data\">";
		html += "Fields with a (*) are mandatory<br/>";
		html += "<label for=\"" + ENABLER_NAME + "\">Enabler name (*):</label>";
		html += "<input type='text' name='" + ENABLER_NAME +"' size='50' placeholder='Enabler name' />";
		html += "<label for=\"" + ENABLER_METADATA + "\">Enabler metadata:</label>";
		html += "<input type='text' name='" + ENABLER_METADATA + "' size='50' placeholder='metadata' />";
		html += "<label for=\"" + ENABLER_PLUGIN_ID + "\">Plugin (*):</label>";
		html += "<select name='" + ENABLER_PLUGIN_ID+ "'>";
		html += "<option value='0'>Select a plugin</option>";
		for (PluginInfo pluginInfo : IotHubDataAccess.getInstance().getPlugins()) {
			html += "<option value='" + pluginInfo.getId() + "'" + 
					(pluginId == pluginInfo.getId() ? " selected" : "")+  ">" + pluginInfo.toString() + "</option>";
		}
		html += "</select>";
		html += "<input type=\"submit\" value=\"Submit\">";
		html += "</form>";
		html += "<div><h1>List of already installed enablers</h1>";
		html += getHtmlListOfEnablers();
		html += "</div>";
		html += "</body></html>";
		return getHtmlResponse(html);
	}

	private String getHtmlListOfEnablers() {
		List<Enabler> enablers = IotHubDataAccess.getInstance().getEnablers();
		if (enablers == null || enablers.isEmpty()) {
			return "<p>No enabler has been found</p>";
		}
		String html = "<ul>";
		for (Enabler enabler : enablers) {
			String enablerHtml = "<b>" + enabler.getName() + "</b>: ";
			enablerHtml += enabler.getPluginInfo().getPackageName() + " - " + enabler.getPluginInfo().getServiceName();
			enablerHtml += "<a href='"+ uriFilter + "/" + enabler.getId() +"'>Configure this enabler</a>";
			html += "<li>" + enablerHtml + "</li>";
		}
		html += "</ul>";
		return html;
	}

	private String getConfigurationHtmlForm (PluginInfo pluginInfo) {
		Plugin plugin = PluginManager.getInstance().getPlugin(pluginInfo);
		if (!plugin.needConfiguration()) {
			return "<p>The plugin does not need configuration</p>";
		}
		else {
			String html = "<p>This enabler does need conf</p>";
			html += "<form method=\"POST\" enctype=\"multipart/form-data\">";
			html += plugin.getConfigurationHtmlForm();
			html += "<input type=\"submit\" value=\"Submit\">";
			html += "</form>";
			return html;
		}
	}

	private String getConfigurationFromForm(Enabler enabler, Map<String, String> parameters, Map<String, String> files) {
		Plugin plugin = PluginManager.getInstance().getPlugin(enabler.getPluginInfo());
		if (plugin == null) {
			Log.e(TAG, "The plugin should not be null");
			return null;
		}
		if (!plugin.needConfiguration()) {
			Log.d(TAG, "The plugin does not need configuration");
			return null;
		}
		else {
			Log.d(TAG, "Now checking if I can get the configuration from the native plugin");
			return plugin.getConfigurationFromHtmlForm(parameters, files);
		}
	}

	private void addFeaturesToEnabler(Enabler enabler) {
		if (enabler.getFeatures() != null && !enabler.getFeatures().isEmpty()) {
			Log.d(TAG, "The enabler has a non-empty list of features, I should do something about it");
			return;
		}
		Plugin plugin = null;
		try {
			plugin = PluginManager.getInstance().getConfiguredPlugin(enabler.getPluginInfo(), enabler.getPluginConfig());
			for(int i = 0; i < plugin.getNumberOfFeatures(); i++) {
				FeatureDescription fd = plugin.getFeatureDescription(i);
				Feature feature = IotHubDataAccess.getInstance().addFeature(enabler, fd.getName(), fd.getType());
				if (feature == null) {
					Log.e(TAG, "The feature should not be null");
					break;
				}
				
			}
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Response handleSingleEnablerRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		String enablerUrlFilterWithSlash = uriFilter + "/";
		String enablerIdentier = uri.substring(enablerUrlFilterWithSlash.length(), uri.length());
		long enablerId = Long.parseLong(enablerIdentier);
		Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerId);
		if (updateFeatureToFeedStatus(enabler, parameters)) {
			//Reload
			enabler = IotHubDataAccess.getInstance().getEnabler(enablerId);
		}
		else {
			if (enabler == null) {
				Log.e(TAG, "The enabler with id " + enablerId + " should not be null");
			}
			String enablerConfig = getConfigurationFromForm(enabler, parameters, files);
			if (enablerConfig != null) {
				Log.d(TAG, "Trying to update the configuration of the enabler");
				try {
					Plugin plugin = PluginManager.getInstance().getConfiguredPlugin(enabler.getPluginInfo(), enablerConfig);
					if (plugin != null) {
						Enabler enablerWithConfig = IotHubDataAccess.getInstance().updateEnabler(enabler, 
								enabler.getName(), enabler.getMetadata(), enablerConfig);
						if (enablerWithConfig != null) {
							Log.i(TAG, "The enabler " + enabler.getName() + " is now configured");
							enabler = enablerWithConfig;
							addFeaturesToEnabler(enabler);
							enabler = IotHubDataAccess.getInstance().getEnabler(enabler.getId());
							if (enabler != null) {
								Log.d(TAG, "The enabler " + enablerId + " should have its features installed");
							}
						}
					}
				} catch (PluginException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String html = "<html>";
		html += "<head><title>Configuration of the enabler " + enabler.getName() + "</title></head>";
		html += "<body>";
		html += "<h1>Configuration of the enabler " + enabler.getName() + "</h1>";
		html += getConfigurationHtmlForm(enabler.getPluginInfo());
		html += getHtmlFormForFeaturesToFeed(enabler);
		html += "</div></body></html>";
		return getHtmlResponse(html);
	}

	private boolean updateFeatureToFeedStatus(Enabler enabler, Map<String, String> parameters) {
		if (enabler == null){
			Log.e(TAG, "Cannot update features if the enabler is null");
			return false;
		}
		else if (!parameters.containsKey("_type") || !parameters.get("_type").equals("toFeed")) {
			Log.d(TAG, "This is not a feature to feed request");
			return false;
		}
		Log.i(TAG, "It is time to update the features of the enabler");
		boolean hasChanged = false;
		//The form results only forward the things that are checked
		for (Feature feature : enabler.getFeatures()) {
			boolean isChecked = parameters.containsKey("feature_" + feature.getId());
			if (isChecked != feature.isAtomicFeed()) {
				Log.i(TAG, "The feature has a different state");
				IotHubDataAccess.getInstance().updateFeature(feature, isChecked);
				hasChanged = true;
			}
		}
		return hasChanged;
	}

	private String getHtmlFormForFeaturesToFeed(Enabler enabler) {
		if (enabler.getFeatures() != null && !enabler.getFeatures().isEmpty()) {
			String html = "<form method=\"POST\" enctype=\"multipart/form-data\">";
			html += "<input type='hidden' name='_type' value='toFeed'>";
			html += "The enabler has " + enabler.getFeatures().size() + " features";
			for (Feature feature : enabler.getFeatures()) {
				String value = "feature_" + feature.getId();
				html += "<input type='checkbox' name='" + value + 
						"' value='" + feature.getName() + "'" +
						(feature.isAtomicFeed() ? " checked" : "") + "/> " + 
						feature.getName() + ": " + feature.getType() + "<br/>";
			}
			html += "<input type=\"submit\" value=\"Submit\">";
			return html + "</form>";
		}
		return "<p>No features for this enabler</p>";
	}

	@Override
	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {

		String enablerUrlFilterWithSlash = uriFilter + "/";
		if (uri.startsWith(enablerUrlFilterWithSlash) && uri.length() > enablerUrlFilterWithSlash.length()) {
			return handleSingleEnablerRequest(method, uri, parameters, mimeType, files);
		}
		else {
			EnablerFormDetails efd = getEnablerFormDetails(parameters, files);
			if (!efd.isEmpty()) {
				boolean isValid = checkEnablerFormDetails(efd);
				if (isValid) {
					Enabler enabler = IotHubDataAccess.getInstance().addEnabler(efd.name, efd.metadata, efd.pluginInfo, null);
					if (enabler == null) {
						return new NanoHTTPD.Response(Status.BAD_REQUEST, "text/plain; charset=utf-8", efd.toString());
					}
				}
				else {
					return new NanoHTTPD.Response(Status.BAD_REQUEST, "text/plain; charset=utf-8", efd.toString());
				}
				
			}
			if (parameters.containsKey("plugin")) {
				long pluginId = Long.parseLong(parameters.get("plugin"));
				return getInstallEnabler(pluginId);
			}
			else {
				return getInstallEnabler(0);
			}
		}
	}
	
	@Override
	public boolean acceptRequest(Method method, String uri) {
		return uri != null && uri.startsWith(uriFilter);
	}
}
