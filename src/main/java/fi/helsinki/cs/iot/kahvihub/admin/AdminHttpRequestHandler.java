/*
 * fi.helsinki.cs.iot.kahvihub.admin.AdminHttpRequestHandler
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.helsinki.cs.iot.hub.api.HttpRequestHandler;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class AdminHttpRequestHandler extends HttpRequestHandler {

	//private static final String TAG = "AdminHttpRequestHandler";

	private static final String filter = "/admin/";
	private static final String pluginUrlFilter = filter + "plugins";
	private static final String enablerUrlFilter = filter + "enablers";
	private static final String serviceUrlFilter = filter + "services";

	private List<HttpRequestHandler> requestHandlers;

	public AdminHttpRequestHandler(String pluginFolder) {
		this.requestHandlers = new ArrayList<HttpRequestHandler>();
		this.requestHandlers.add(new PluginHttpRequestHandler(pluginFolder, pluginUrlFilter));
		this.requestHandlers.add(new EnablerHttpRequestHandler(enablerUrlFilter));
		this.requestHandlers.add(new ServiceHttpRequestHandler(pluginFolder, serviceUrlFilter));
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.HttpRequestHandler#acceptRequest(fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method, java.lang.String)
	 */
	@Override
	public boolean acceptRequest(Method method, String uri) {
		return uri != null && uri.startsWith(filter);
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
		for (HttpRequestHandler requestHandler : requestHandlers) {
			if (requestHandler.acceptRequest(method, uri)) {
				return requestHandler.handleRequest(method, uri, parameters, mimeType, files);
			}
		}
		return new NanoHTTPD.Response(Status.NOT_FOUND, "text/plain; charset=utf-8", "404 - Page not found");
	}

}
