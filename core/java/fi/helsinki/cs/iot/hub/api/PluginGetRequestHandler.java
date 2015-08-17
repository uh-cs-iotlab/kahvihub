/*
 * fi.helsinki.cs.iot.hub.api.PluginGetRequestHandler
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
package fi.helsinki.cs.iot.hub.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.uri.IotHubUri;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginGetRequestHandler extends IoTHubApiRequestHandler {

	private static final String TAG = "PluginGetRequestHandler";
	private List<Method> methods;
	private Path libdir;

	public PluginGetRequestHandler(Path libdir) {
		this.libdir = libdir;
		this.methods = new ArrayList<>();
		this.methods.add(Method.GET);	
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	/**
	 * @return the libdir
	 */
	public Path getLibdir() {
		return libdir;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubUri uri) {
		Log.d(TAG, "I got a get request for plugin");
		// Case where we just want to list the plugins
		if (uri.getIdentifiers().size() == 0) {
			List<PluginInfo> plugins = IotHubDataAccess.getInstance().getPlugins();
			JSONArray jArray = new JSONArray();
			for (PluginInfo plugin : plugins) {
				try {
					jArray.put(plugin.toJSON());
				} catch (JSONException e) {
					System.err.println(e);
					return getResponseKo(STATUS_IO_ERROR, e.getMessage());
				}
			}
			return getResponseOk(jArray.toString());
		}
		else if (uri.getIdentifiers().size() == 1) {
			if ("native".equalsIgnoreCase(uri.getIdentifiers().get(0))) {
				List<PluginInfo> plugins = IotHubDataAccess.getInstance().getNativePlugins();
				JSONArray jArray = new JSONArray();
				for (PluginInfo plugin : plugins) {
					try {
						jArray.put(plugin.toJSON());
					} catch (JSONException e) {
						System.err.println(e);
						return getResponseKo(STATUS_IO_ERROR, e.getMessage());
					}
				}
				return getResponseOk(jArray.toString());
			}
			else if ("javascript".equalsIgnoreCase(uri.getIdentifiers().get(0))) {
				List<PluginInfo> plugins = IotHubDataAccess.getInstance().getJavascriptPlugins();
				JSONArray jArray = new JSONArray();
				for (PluginInfo plugin : plugins) {
					try {
						jArray.put(plugin.toJSON());
					} catch (JSONException e) {
						System.err.println(e);
						return getResponseKo(STATUS_IO_ERROR, e.getMessage());
					}
				}
				return getResponseOk(jArray.toString());
			} 
			else {
				//TODO 
				return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
			}
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

}
