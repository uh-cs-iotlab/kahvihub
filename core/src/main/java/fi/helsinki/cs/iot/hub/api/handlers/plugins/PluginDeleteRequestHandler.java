/*
 * fi.helsinki.cs.iot.hub.api.PluginDeleteRequestHandler
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class PluginDeleteRequestHandler extends IotHubApiRequestHandler {

	private List<Method> methods;

	public PluginDeleteRequestHandler() {
		this.methods = new ArrayList<>();
		this.methods.add(Method.DELETE);	
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
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the plugin id");
		}
		else {
			try {
				long id = Long.parseLong(stringId);
				if (PluginRequestHandler.ENABLER.equals(type)) {
					PluginInfo pluginInfo = IotHubDataAccess.getInstance().deletePlugin(id);
					if (pluginInfo == null) {
						return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the plugin with id " + id);
					}
					else {
						File file = new File(pluginInfo.getFilename());
						if (file.exists()) {
							file.delete();
						}
						PluginManager.getInstance().removeAllPluginsForPluginInfo(pluginInfo);
						return getResponseOk(pluginInfo.toJSON().toString());
					}
				}
				else if (PluginRequestHandler.SERVICE.equals(type)) {
					ServiceInfo serviceInfo = IotHubDataAccess.getInstance().deleteServiceInfo(id);
					if (serviceInfo == null) {
						return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the plugin with id " + id);
					}
					else {
						File file = new File(serviceInfo.getFilename());
						if (file.exists()) {
							file.delete();
						}
						ServiceManager.getInstance().removeAllServicesForServiceInfo(serviceInfo);
						return getResponseOk(serviceInfo.toJSON().toString());
					}
				}
				else {
					return getResponseKo(STATUS_BAD_REQUEST, "Unknow plugin type");
				}
			} catch (JSONException | NumberFormatException e) {
				return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the plugin id, and plugin id should be a long");
			}
		}
	}
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubRequest uri) {
		// To delete a plugin, there should be just the name

		if (uri.getIdentifiers().size() > 1) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the plugin id");
		}
		else if (uri.getIdentifiers().size() == 0) {
			return getResponseWithStringId(uri.getOptions().get("id"), uri.getOptions().get("type"));
		}
		else {
			return getResponseWithStringId(uri.getIdentifiers().get(0), uri.getOptions().get("type"));
		}
	}

}
