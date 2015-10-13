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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * @author mineraud
 *
 */
public class PluginRequestHandler extends IotHubApiRequestHandler {
	
	private Map<Method, IotHubApiRequestHandler> subHandlers;
	private List<Method> supportedMethods;
	protected static final String SERVICE = "service";
	protected static final String ENABLER = "enabler";

	public PluginRequestHandler(Path libdir) {
		this.subHandlers = new HashMap<>();
		this.subHandlers.put(Method.GET, new PluginGetRequestHandler());
		this.subHandlers.put(Method.POST, new PluginPostRequestHandler(libdir));
		this.subHandlers.put(Method.DELETE, new PluginDeleteRequestHandler());
		this.supportedMethods = new ArrayList<>(subHandlers.keySet());
	}

	@Override
	public List<Method> getSupportedMethods() {
		return supportedMethods;
	}

	@Override
	public Response handleRequest(IotHubRequest request) {
		if (request == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "The request is null");
		}
		IotHubApiRequestHandler handler = this.subHandlers.get(request.getMethod());
		if (handler == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Method " + request.getMethod() + " not supported");
		}
		else {
			return handler.handleRequest(request);
		}
	}

}
