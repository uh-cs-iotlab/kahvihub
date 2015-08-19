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
package fi.helsinki.cs.iot.hub.api.handlers.plugins;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginGetRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "PluginGetRequestHandler";
	private List<Method> methods;

	public PluginGetRequestHandler() {
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

	private Response getResponseWithStringId(String stringId, String type) {
		if (stringId == null || type == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the plugin id and the type");
		}
		else {
			try {
				long id = Long.parseLong(stringId);
				if (PluginRequestHandler.ENABLER.equals(type)) {
					PluginInfo pluginInfo = IotHubDataAccess.getInstance().getPluginInfo(id);
					if (pluginInfo == null) {
						return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Could not get the plugin with id " + id);
					}
					else {
						return getResponseOk(pluginInfo.toJSON().toString());
					}
				}
				else if (PluginRequestHandler.SERVICE.equals(type)) {
					ServiceInfo serviceInfo = IotHubDataAccess.getInstance().getServiceInfo(id);
					if (serviceInfo == null) {
						return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Could not get the plugin with id " + id);
					}
					else {
						return getResponseOk(serviceInfo.toJSON().toString());
					}
				}
				else {
					return getResponseKo(STATUS_BAD_REQUEST, "Type of the plugin unknown");
				}
			} catch (JSONException | NumberFormatException e) {
				return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the plugin id, and plugin id should be a long");
			}
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubRequest uri) {
		Log.d(TAG, "I got a get request for plugin");
		// Case where we just want to list the plugins
		if (uri.getIdentifiers().size() == 0) {
			String stringId = uri.getOptions().get("id");
			String type = uri.getOptions().get("type");
			if (stringId == null) {
				JSONArray jArray = new JSONArray();
				if (type == null || PluginRequestHandler.ENABLER.equals(type)) {
					List<PluginInfo> plugins = IotHubDataAccess.getInstance().getPlugins();
					for (PluginInfo plugin : plugins) {
						try {
							jArray.put(plugin.toJSON());
						} catch (JSONException e) {
							System.err.println(e);
							return getResponseKo(STATUS_IO_ERROR, e.getMessage());
						}
					}
				}
				if (type == null || PluginRequestHandler.SERVICE.equals(type)) {
					List<ServiceInfo> services = IotHubDataAccess.getInstance().getServiceInfos();
					for (ServiceInfo serviceInfo : services) {
						try {
							jArray.put(serviceInfo.toJSON());
						} catch (JSONException e) {
							System.err.println(e);
							return getResponseKo(STATUS_IO_ERROR, e.getMessage());
						}
					}
				}
				if (type != null && (!PluginRequestHandler.ENABLER.equals(type) || !PluginRequestHandler.SERVICE.equals(type))) {
					return getResponseKo(STATUS_BAD_REQUEST, "The type of plugin is unknown");
				}
				return getResponseOk(jArray.toString());
			}
			else {
				return getResponseWithStringId(stringId, type);
			}
		}
		else if (uri.getIdentifiers().size() == 1) {
			return getResponseWithStringId(uri.getIdentifiers().get(0), uri.getOptions().get("type"));
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

}
