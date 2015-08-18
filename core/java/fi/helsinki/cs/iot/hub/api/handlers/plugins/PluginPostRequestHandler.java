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
				Type type = BasicPluginInfo.Type.valueOf(jdata.getString("type"));
				
				PluginInfo pluginInfo = null;
				
				if (type == Type.NATIVE) {
					File file = ScriptUtils.decodeBase64ToFile(jdata.getString("file"));
					PluginManager.getInstance().checkNativePlugin(pluginName, packageName, file);
					int id = IotHubDataAccess.getInstance().getPlugins().size();
					Path pathInLibFolder = Paths.get(this.libdir.toString(), String.format("native-plugin-%s-%d.jar", pluginName, id));
					Path newPath = Files.copy(Paths.get(file.getAbsolutePath()), pathInLibFolder);
					if (newPath != null) {
						pluginInfo = IotHubDataAccess.getInstance().addNativePlugin(pluginName, packageName, newPath.toFile());
					}
				}
				else {
					String script = ScriptUtils.decodeBase64ToString(jdata.getString("file"));
					PluginManager.getInstance().checkJavacriptPlugin(pluginName, script);
					int id = IotHubDataAccess.getInstance().getPlugins().size();
					File ld = this.libdir.toFile();
					if (!(ld.exists() && ld.isDirectory())) {
						System.err.println(ld.getAbsolutePath());
					}
					Path pathInLibFolder = Paths.get(this.libdir.toString(), String.format("native-plugin-%s-%d.js", pluginName, id));
					InputStream stream = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
					long written = Files.copy(stream, pathInLibFolder);
					if (written > 0) {
						pluginInfo = IotHubDataAccess.getInstance().addJavascriptPlugin(pluginName, packageName, pathInLibFolder.toFile());
					}
				}
				
				if (pluginInfo == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "I could not add the plugin to the db");
				}
				else {
					return getResponseOk(pluginInfo.toJSON().toString());
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
			}
		}

	}

}
