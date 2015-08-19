/*
 * fi.helsinki.cs.iot.hub.api.handlers.plugins.PluginPostRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.plugins;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.BasicPluginInfo.Type;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.service.ServiceException;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginPostRequestHandler extends IotHubApiRequestHandler {

	private List<Method> methods;
	private Path libdir;

	public PluginPostRequestHandler(Path libdir) {
		this.libdir = libdir;
		this.methods = new ArrayList<>();
		this.methods.add(Method.POST);	
	}

	/**
	 * @return the libdir
	 */
	public Path getLibdir() {
		return libdir;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}


	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.request.IotHubRequest)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		if (request == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Cannot work with no request");
		}
		else if (request.getMethod() != Method.POST) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Method " + request.getMethod() + " not supported");
		}
		else if (request.getIdentifiers().size() != 0){
			return getResponseKo(STATUS_BAD_REQUEST, "Cannot work with this request if the number of identifier is greater than 0");
		}
		else {
			String bodyData = request.getBodyData();
			if (bodyData == null) {
				return getResponseKo(STATUS_BAD_REQUEST, "The data seems to be missing");
			}
			try {
				JSONObject jdata = new JSONObject(bodyData);
				String pluginName = jdata.getString("plugin");
				String packageName = jdata.has("package") ? jdata.getString("package") : null;
				boolean isService = jdata.optBoolean("isService") || PluginRequestHandler.SERVICE.equals(request.getOptions().get("type"));
				
				Type type = BasicPluginInfo.Type.valueOf(jdata.getString("type"));
				
				PluginInfo pluginInfo = null;
				ServiceInfo serviceInfo = null;
				
				if (type == Type.NATIVE) {
					File file = ScriptUtils.decodeBase64ToFile(jdata.getString("file"));
					if (!isService) {
						PluginManager.getInstance().checkNativePlugin(pluginName, packageName, file);
					}
					else {
						return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "The native service feature is not yet implemented");
					}
					int id = (!isService) ? IotHubDataAccess.getInstance().getPlugins().size() : IotHubDataAccess.getInstance().getServiceInfos().size();
					Path pathInLibFolder = Paths.get(this.libdir.toString(), String.format("native-%s-%s-%d.jar", (!isService) ? "plugin" : "service", pluginName, id));
					Path newPath = Files.copy(Paths.get(file.getAbsolutePath()), pathInLibFolder);
					if (newPath != null) {
						if (!isService) {
							pluginInfo = IotHubDataAccess.getInstance().addNativePlugin(pluginName, packageName, newPath.toFile());
						}
						else {
							return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "The native service feature is not yet implemented");
							//serviceInfo = IotHubDataAccess.getInstance().addServiceInfo(name, file)
						}
					}
				}
				else {
					String script = ScriptUtils.decodeBase64ToString(jdata.getString("file"));
					if (!isService) {
						PluginManager.getInstance().checkJavacriptPlugin(pluginName, script);
					}
					else {
						ServiceManager.getInstance().checkService(pluginName, script);
					}
					int id = (!isService) ? IotHubDataAccess.getInstance().getPlugins().size() : IotHubDataAccess.getInstance().getServiceInfos().size();
					File ld = this.libdir.toFile();
					if (!(ld.exists() && ld.isDirectory())) {
						System.err.println(ld.getAbsolutePath());
					}
					Path pathInLibFolder = Paths.get(this.libdir.toString(), String.format("javscript-%s-%s-%d.js", (!isService) ? "plugin" : "service", pluginName, id));
					InputStream stream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
					long written = Files.copy(stream, pathInLibFolder);
					if (written > 0) {
						if (!isService) {
							pluginInfo = IotHubDataAccess.getInstance().addJavascriptPlugin(pluginName, packageName, pathInLibFolder.toFile());
						}
						else {
							serviceInfo = IotHubDataAccess.getInstance().addServiceInfo(pluginName, pathInLibFolder.toFile());
						}
					}
				}
				
				if (pluginInfo == null && serviceInfo == null) {
					return getResponseKo(STATUS_BAD_REQUEST, String.format("I could not add the %s to the db", (!isService) ? "plugin" : "service"));
				}
				else {
					return getResponseOk((!isService) ? pluginInfo.toJSON().toString() : serviceInfo.toJSON().toString());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getResponseKo(STATUS_BAD_REQUEST, "The data is not JSON data or is incorrect");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getResponseKo(STATUS_BAD_REQUEST, "Could not read properly the file in the data");
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return getResponseKo(STATUS_BAD_REQUEST, "The plugin is incorrect " + e.getMessage());
			} catch (ServiceException e) {
				// TODO Auto-generated catch block
				return getResponseKo(STATUS_BAD_REQUEST, "The service is incorrect " + e.getMessage());
			}
		}

	}

}
