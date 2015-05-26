/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub.admin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.utils.IotHubDataModel;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;
import fi.helsinki.cs.iot.kahvihub.plugin.IPlugin;

/**
 * @author mineraud
 *
 */
public class AdminHttpRequestHandler extends HttpRequestHandler {

	private static final String TAG = "AdminHttpRequestHandler";

	private static final String filter = "/admin/";
	private static final String pluginUrlFilter = "/admin/plugins";
	private static final String enablerUrlFilter = "/admin/enablers";

	private static final String PLUGIN_SERVICE_NAME = "serviceName";
	private static final String PLUGIN_PACKAGE_NAME = "packageName";
	private static final String PLUGIN_TYPE = "type";
	private static final String PLUGIN_FILE= "file";

	private static final String ENABLER_NAME = "enablerName";
	private static final String ENABLER_METADATA = "enablerMetadata";
	private static final String ENABLER_PLUGIN_ID = "enablerPluginId";

	private String pluginFolder;


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


	public AdminHttpRequestHandler(String pluginFolder) {
		this.pluginFolder = pluginFolder;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.HttpRequestHandler#acceptRequest(fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method, java.lang.String)
	 */
	@Override
	public boolean acceptRequest(Method method, String uri) {
		return uri != null && uri.startsWith(filter);
	}

	private Response getHtmlResponse(String html) {
		return new NanoHTTPD.Response(Status.OK, "text/html; charset=utf-8", html);
	}

	private PluginFormDetails getPluginFormDetails(Map<String, String> parameters, Map<String, String> files) {
		String serviceName = parameters.get(PLUGIN_SERVICE_NAME);
		String packageName = parameters.get(PLUGIN_PACKAGE_NAME);
		String type = parameters.get(PLUGIN_TYPE);
		String filename = parameters.get(PLUGIN_FILE);
		File file = filename == null ? null : new File(files.get(PLUGIN_FILE));
		return new PluginFormDetails(serviceName, packageName, type, file);
	}

	private boolean checkPluginFormDetails(PluginFormDetails pfd) {
		if (!pfd.hasType()) {
			return false;
		}
		else if (IotHubDataHandler.NATIVE_PLUGIN.equals(pfd.type)) {
			return pfd.hasServiceName() && pfd.hasPackageName() && pfd.hasFile() && checkNativePlugin(pfd);
		}
		else if (IotHubDataHandler.JAVASCRIPT_PLUGIN.equals(pfd.type)) {
			return pfd.hasServiceName() && pfd.hasPackageName() && pfd.hasFile();
		}
		else {
			return false;
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

	private boolean checkNativePlugin(PluginFormDetails pfd) {

		try { 
			String classname = pfd.packageName + "." + pfd.serviceName;
			URL[] urls = {pfd.file.toURI().toURL()};
			ClassLoader classLoader = new URLClassLoader(urls);
			Class<?> pluginClass = Class.forName(classname, true, classLoader); 

			if(IPlugin.class.isAssignableFrom(pluginClass)){ 
				@SuppressWarnings("unchecked")
				Class<IPlugin> castedClass = (Class<IPlugin>)pluginClass; 
				IPlugin plugin = castedClass.newInstance();
				List<String> dataTypes = plugin.getDataTypes();
				for (String dataType : dataTypes) {
					if (!IotHubDataModel.getInstance().checkType(dataType)) {
						Log.e(TAG, "The data type " + dataType + " for plugin " + plugin.toString() + " is invalid");
						return false;
					}
				}
				return true;
			}
			else {
				System.err.println("The provided class is not a IPlugin");
				return false;
			}
		} catch (ClassNotFoundException e1) { 
			e1.printStackTrace(); 
		} catch (InstantiationException e) { 
			e.printStackTrace(); 
		} catch (IllegalAccessException e) { 
			e.printStackTrace(); 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (AbstractMethodError e) {
			// This is needed when the plugin is not uptodate
			e.printStackTrace();
		}
		return false;
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
			pluginHtml += "<a href='"+ enablerUrlFilter +"?plugin=" + pluginInfo.getId() + "'>Install an enabler for this plugin</a>";
			html += "<li>" + pluginHtml + "</li>";
		}
		html += "</ul>";
		return html;
	}

	private Response handlePluginRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		PluginFormDetails pfd = getPluginFormDetails(parameters, files);
		if (pfd.isEmpty()) {
			//TODO I would need to do more later
			return getInstallPlugin();
		}
		else {
			boolean isValid = checkPluginFormDetails(pfd);
			if (isValid) {
				File file = copyPluginFile(pfd);
				if (file != null) {
					if (pfd.type.equals("native")) {
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
			enablerHtml += enabler.getPlugin().getPackageName() + " - " + enabler.getPlugin().getServiceName();
			enablerHtml += "<a href='"+ enablerUrlFilter + "/" + enabler.getId() +"'>Configure this enabler</a>";
			html += "<li>" + enablerHtml + "</li>";
		}
		html += "</ul>";
		return html;
	}

	private IPlugin getPlugin(PluginInfo pluginInfo) {
		if (pluginInfo == null || !pluginInfo.isNative()) {
			Log.e(TAG, "The plugin info does not permit to find the corresponding native plugin");
			return null;
		}
		try { 
			String classname = pluginInfo.getPackageName() + "." + pluginInfo.getServiceName();
			File pluginFile = new File(pluginFolder + pluginInfo.getFilename());
			URL[] urls = {pluginFile.toURI().toURL()};
			ClassLoader classLoader = new URLClassLoader(urls);
			Class<?> pluginClass = Class.forName(classname, true, classLoader); 

			if(IPlugin.class.isAssignableFrom(pluginClass)){ 
				@SuppressWarnings("unchecked")
				Class<IPlugin> castedClass = (Class<IPlugin>)pluginClass; 
				IPlugin plugin = castedClass.newInstance();
				return plugin;
			}
			else {
				System.err.println("The provided class is not a IPlugin");
				return null;
			}
		} catch (ClassNotFoundException e1) { 
			e1.printStackTrace(); 
		} catch (InstantiationException e) { 
			e.printStackTrace(); 
		} catch (IllegalAccessException e) { 
			e.printStackTrace(); 
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (AbstractMethodError e) {
			// This is needed when the plugin is not uptodate
			e.printStackTrace();
		}
		return null;
	}

	private String getHtmlPluginForm (PluginInfo pluginInfo) {
		if (pluginInfo.isNative()) {
			IPlugin plugin = getPlugin(pluginInfo);
			if (plugin != null) {
				return plugin.getConfigurationHtmlForm();
			}
			else {
				return null;
			}
		}
		else {
			return null;
		}
	}

	private Response handleSingleEnablerRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		String enablerUrlFilterWithSlash = enablerUrlFilter + "/";
		String enablerIdentier = uri.substring(enablerUrlFilterWithSlash.length(), uri.length());
		long enablerId = Long.parseLong(enablerIdentier);
		Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerId);
		String html = "<html>";
		html += "<head><title>Configuration of the enabler " + enabler.getName() + "</title></head>";
		html += "<body>";
		html += "<h1>Configuration of the enabler " + enabler.getName() + "</h1>";
		html += getHtmlListOfFeatures(enabler);
		html += "</div></body></html>";
		return getHtmlResponse(html);
	}

	private String getHtmlListOfFeatures(Enabler enabler) {
		if (enabler.getFeatures() != null && !enabler.getFeatures().isEmpty()) {
			String html = "<ul>";
			for (Feature feature : enabler.getFeatures()) {
				html += "<li>" + feature.getName() + "</li>";
			}
			return html + "</ul>";
		}
		else if (enabler.getPluginConfig() == null) {
			//First we need to get the plugin from the PluginInfo
			PluginInfo pluginInfo = enabler.getPlugin();
			if (pluginInfo != null) {
				if (pluginInfo.isNative()) {
					IPlugin plugin = getPlugin(pluginInfo);
					if (plugin.configure(null)) {
						//if this return true, the plugin does not need configuration
						return "<p>This enabler does not need conf</p>";
					}
					else {
						//I need to provide the configuration template for this plugin
						return "<p>This enabler does need conf</p>";
					}
				}
			}
		}
		return null;
		/*List<Enabler> enablers = IotHubDataAccess.getInstance().getEnablers();
		if (enablers == null || enablers.isEmpty()) {
			return "<p>No enabler has been found</p>";
		}
		String html = "<ul>";
		for (Enabler enabler : enablers) {
			String enablerHtml = "<b>" + enabler.getName() + "</b>: ";
			enablerHtml += enabler.getPlugin().getPackageName() + " - " + enabler.getPlugin().getServiceName();
			enablerHtml += "<a href='"+ enablerUrlFilter + "/" + enabler.getId() +"'>Configure this enabler</a>";
			html += "<li>" + enablerHtml + "</li>";
		}
		html += "</ul>";
		return html;*/
	}

	private Response handleEnablerRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {

		String enablerUrlFilterWithSlash = enablerUrlFilter + "/";
		if (uri.startsWith(enablerUrlFilterWithSlash) && uri.length() > enablerUrlFilterWithSlash.length()) {
			return handleSingleEnablerRequest(method, uri, parameters, mimeType, files);
		}
		else {
			EnablerFormDetails efd = getEnablerFormDetails(parameters, files);
			if (efd.isEmpty()) {
				if (parameters.containsKey("plugin")) {
					long pluginId = Long.parseLong(parameters.get("plugin"));
					return getInstallEnabler(pluginId);
				}
				else {
					return getInstallEnabler(0);
				}
			}
			else {
				boolean isValid = checkEnablerFormDetails(efd);
				if (isValid) {
					Enabler enabler = IotHubDataAccess.getInstance().addEnabler(efd.name, efd.metadata, efd.pluginInfo, null);
					if (enabler != null) {
						return new NanoHTTPD.Response(Status.OK, "text/plain; charset=utf-8", efd.toString());
					}
				}
				return new NanoHTTPD.Response(Status.BAD_REQUEST, "text/plain; charset=utf-8", efd.toString());
			}
		}


		/*if (parameters.containsKey("plugin")) {
			long pluginId = Long.parseLong(parameters.get("plugin"));
			PluginInfo plugin = IotHubDataAccess.getInstance().getPluginInfo(pluginId);
			String html = "<html>";
			html += "<head><title>Configure an enabler for plugin " + plugin.toString() + "</title></head>";
			html += "<body>";
			html += html += "<form method=\"POST\" enctype=\"multipart/form-data\">";
			html += getHtmlPluginForm(plugin);
			html += "<input type=\"submit\" value=\"Submit\">";
			html += "</form>";
			html += "<div><h1>List of already installed enablers</h1>";
			html += getHtmlListOfEnablers();
			html += "</div></body></html>";
			return getHtmlResponse(html);
		}
		else {
			String html = "<html>";
			html += "<head><title>List of currently installed enablers</title></head>";
			html += "<body>";
			html += "<div><h1>List of already installed enablers</h1>";
			html += getHtmlListOfEnablers();
			html += "</div></body></html>";
			return getHtmlResponse(html);
		}*/
	}


	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.HttpRequestHandler#handleRequest(
	 * fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method, java.lang.String, java.util.Map, java.lang.String, java.lang.String)
	 */
	@Override
	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		if (!acceptRequest(method, uri)) {
			return null;
		}
		if (uri.startsWith(pluginUrlFilter)) {
			return handlePluginRequest(method, uri, parameters, mimeType, files);
		}
		else if (uri.startsWith(enablerUrlFilter)) {
			return handleEnablerRequest(method, uri, parameters, mimeType, files);
		}
		else {
			return new NanoHTTPD.Response(Status.NOT_FOUND, "text/plain; charset=utf-8", "404 - Page not found");
		}
	}

}
