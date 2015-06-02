/*
 * fi.helsinki.cs.iot.kahvihub.admin.PluginHttpRequestHandler
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class PluginHttpRequestHandler extends HttpRequestHandler {
	
	private static final String TAG = "PluginHttpRequestHandler";
	
	private static final String PLUGIN_SERVICE_NAME = "serviceName";
	private static final String PLUGIN_PACKAGE_NAME = "packageName";
	private static final String PLUGIN_TYPE = "type";
	private static final String PLUGIN_FILE= "file";
	
	private final String pluginFolder;
	private final String uriFilter;
	
	/**
	 * @param pluginFolder
	 * @param uriFilter
	 */
	public PluginHttpRequestHandler(String pluginFolder, String uriFilter) {
		this.pluginFolder = pluginFolder;
		this.uriFilter = uriFilter;
	}
	
	private class PluginFormDetails {

		private String serviceName;
		private String packageName;
		private String type;
		private File file;

		private PluginFormDetails(String serviceName, String packageName,
				String type, File file) {
			this.serviceName = serviceName;
			this.packageName = packageName;
			this.type = type;
			this.file = file;
		}

		private boolean hasServiceName() {
			return serviceName != null && serviceName.length() > 0;
		}

		private boolean hasPackageName() {
			return packageName != null && packageName.length() > 0;
		}

		private boolean hasType() {
			return type != null && type.length() > 0;
		}

		private boolean hasFile() {
			return file != null && file.exists() && !file.isDirectory();
		}

		private boolean isEmpty () {
			return !hasServiceName() && !hasPackageName() && !hasType() && !hasFile();
		}

		@Override
		public String toString() {
			return "PluginFormDetails [serviceName=" + serviceName
					+ ", packageName=" + packageName + ", type=" + type
					+ ", file=" + file + "]";
		}
	}
	
	private PluginFormDetails getPluginFormDetails(Map<String, String> parameters, Map<String, String> files) {
		String serviceName = parameters.get(PLUGIN_SERVICE_NAME);
		String packageName = parameters.get(PLUGIN_PACKAGE_NAME);
		String type = parameters.get(PLUGIN_TYPE);
		String filename = parameters.get(PLUGIN_FILE);
		File file = filename == null ? null : new File(files.get(PLUGIN_FILE));
		return new PluginFormDetails(serviceName, packageName, type, file);
	}

	private boolean checkPluginFormDetails(PluginFormDetails pfd) throws PluginException {
		if (!pfd.hasType()) {
			return false;
		}
		else if (IotHubDataHandler.NATIVE_PLUGIN.equals(pfd.type)) {
			return pfd.hasServiceName() && pfd.hasPackageName() && pfd.hasFile() && checkNativePlugin(pfd);
		}
		else if (IotHubDataHandler.JAVASCRIPT_PLUGIN.equals(pfd.type)) {
			return pfd.hasServiceName() && pfd.hasPackageName() && pfd.hasFile() && checkJavascriptPlugin(pfd);
		}
		else {
			return false;
		}
	}
	
	private boolean checkNativePlugin(PluginFormDetails pfd) throws PluginException {
		PluginManager.getInstance().checkNativePlugin(pfd.serviceName, pfd.packageName, pfd.file);
		return true;
	}
	
	private boolean checkJavascriptPlugin(PluginFormDetails pfd) throws PluginException  {
		PluginManager.getInstance().checkJavacriptPlugin(pfd.serviceName, pfd.packageName, pfd.file);
		return true;
	}

	private File copyPluginFile(PluginFormDetails pfd) {
		/* 
		 * TODO A number of features would need to be added, first it would be nice to have
		 * separate folders for native and javascript plugins
		 * then we need to make sure to manage different plugins version
		 * Update them if necessary (similar to apps and services)
		 */
		if (pfd.hasFile()) {
			File pf = new File(pluginFolder);
			if (!pf.exists() || !pf.isDirectory()) {
				Log.e(TAG, "Plugin folder does not exists or is not a directory");
				return null;
			}
			try {
				String filename = pfd.file.getName();
				File file = new File(pluginFolder + filename);
				Files.copy(pfd.file.toPath(), 
						file.toPath(), 
						StandardCopyOption.REPLACE_EXISTING);
				return file;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	private Response getInstallPlugin() {
		String html = "<html>";
		html += "<head><title>Install a new plugin for your Kahvihub</title></head>";
		html += "<body>";
		html += "<form method=\"POST\" enctype=\"multipart/form-data\">";
		html += "Fields with a (*) are mandatory<br/>";
		html += "<label for=\"" + PLUGIN_SERVICE_NAME + "\">Service name (*):</label>";
		html += "<input type='text' name='" + PLUGIN_SERVICE_NAME +"' size='50' placeholder='Service name' />";
		html += "<label for=\"" + PLUGIN_PACKAGE_NAME + "\">Package name (*):</label>";
		html += "<input type='text' name='" + PLUGIN_PACKAGE_NAME + "' size='50' placeholder='key' />";
		html += "<label for=\"" + PLUGIN_TYPE + "\">Package type (*):</label>";
		html += "<select name='" + PLUGIN_TYPE+ "'>";
		html += "<option value='" + IotHubDataHandler.NATIVE_PLUGIN + "'>Native</option>";
		html += "<option value='" + IotHubDataHandler.JAVASCRIPT_PLUGIN + "'>Javascript</option>";	
		html += "</select>";
		html += "Please specify a file:<br/>";
		html += "<input type='file' name='" + PLUGIN_FILE +"' size='40' />";
		html += "<input type=\"submit\" value=\"Submit\">";
		html += "</form>";
		html += "<div><h1>List of already installed plugins</h1>";
		html += getHtmlListOfPlugins();
		html += "</div>";
		html += "</body></html>";
		return getHtmlResponse(html);
	}

	private String getHtmlListOfPlugins() {
		List<PluginInfo> plugins = IotHubDataAccess.getInstance().getPlugins();
		if (plugins == null || plugins.isEmpty()) {
			return "<p>No plugin has been found</p>";
		}
		String html = "<ul>";
		for (PluginInfo pluginInfo : plugins) {
			String pluginHtml = "<b>" + pluginInfo.getType().name() + "</b>: ";
			pluginHtml += pluginInfo.getPackageName() + " - " + pluginInfo.getServiceName();
			pluginHtml += "<a href='"+ uriFilter +"?plugin=" + pluginInfo.getId() + "'>Install an enabler for this plugin</a>";
			html += "<li>" + pluginHtml + "</li>";
		}
		html += "</ul>";
		return html;
	}

	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		PluginFormDetails pfd = getPluginFormDetails(parameters, files);
		if (pfd.isEmpty()) {
			//TODO I would need to do more later
			return getInstallPlugin();
		}
		else {
			boolean isValid = false;
			try {
				isValid = checkPluginFormDetails(pfd);
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "There was something wrong with the plugin");
				return new NanoHTTPD.Response(Status.BAD_REQUEST, "text/plain; charset=utf-8", pfd.toString());
			}
			if (isValid) {
				File file = copyPluginFile(pfd);
				if (file != null) {
					if (pfd.type.equals("native")) {
						Log.d(TAG, "Adding the native plugin to db");
						IotHubDataAccess.getInstance().addNativePlugin(pfd.serviceName, pfd.packageName, file);
					}
					else {
						IotHubDataAccess.getInstance().addJavascriptPlugin(pfd.serviceName, pfd.packageName, file);
					}
					return new NanoHTTPD.Response(Status.OK, "text/plain; charset=utf-8", pfd.toString());
				}
			}

			return new NanoHTTPD.Response(Status.BAD_REQUEST, "text/plain; charset=utf-8", pfd.toString());
		}
	}

	@Override
	public boolean acceptRequest(Method method, String uri) {
		return uri != null && uri.startsWith(uriFilter);
	}

}
