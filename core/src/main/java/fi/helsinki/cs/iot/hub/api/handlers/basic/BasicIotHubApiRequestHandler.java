/*
 * fi.helsinki.cs.iot.hub.api.handlers.basic.BasicIotHubApiRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.basic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.enablers.EnablerRequestHandler;
import fi.helsinki.cs.iot.hub.api.handlers.feeds.FeedRequestHandler;
import fi.helsinki.cs.iot.hub.api.handlers.plugins.PluginRequestHandler;
import fi.helsinki.cs.iot.hub.api.handlers.services.ServiceRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest.Type;
import fi.helsinki.cs.iot.hub.model.enabler.JavascriptPluginHelperImpl;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.model.service.JavascriptRunnableServiceHelper;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class BasicIotHubApiRequestHandler extends HttpRequestHandler {

	private static final String STATUS_WRONG_URI = "WRONG_URI";
	private static final String TAG = "BasicIotHubApiRequestHandler";

	private List<Method> supportedMethods;
	private Map<Type, IotHubApiRequestHandler> subHandlers;

	public BasicIotHubApiRequestHandler(Path libdir) {

		this.supportedMethods = new ArrayList<>();
		this.subHandlers = new HashMap<>();

		// First the plugins
		if (libdir != null) {
			PluginManager.getInstance().setJavascriptPluginHelper(
					new JavascriptPluginHelperImpl(libdir));
			this.subHandlers.put(Type.PLUGIN, 
					new PluginRequestHandler(libdir));
			
			ServiceManager.getInstance().setServiceHelper(new JavascriptRunnableServiceHelper(libdir));
			//Then the services
			this.subHandlers.put(Type.SERVICE, 
					new ServiceRequestHandler());
			
			//Then the feeds
			this.subHandlers.put(Type.FEED, 
					new FeedRequestHandler(libdir));
		}
		else {
			Log.w(TAG, "The libdir path is null, this is a bad idea");
		}

		//Then the enablers
		this.subHandlers.put(Type.ENABLER, 
				new EnablerRequestHandler());

		

		//Add the list of supported methods
		for(IotHubApiRequestHandler handler : subHandlers.values()) {
			List<Method> methods = handler.getSupportedMethods();
			for (Method method : methods) {
				if (!supportedMethods.contains(method)) {
					supportedMethods.add(method);
				}
			}
		}
	}

	public int getVersion() {
		return 0;
	}

	private Response getResponseKo(String status, String errMessage) {
		JSONObject answer = new JSONObject();
		try {
			answer.put("status", status);
			answer.put("message", errMessage);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Response response = new NanoHTTPD.Response(
				Status.BAD_REQUEST, 
				"application/json", 
				answer.toString());
		return response;
	}

	/*private String getAllowMethodsHeader() {
		List<Method> methods = new ArrayList<NanoHTTPD.Method>();
		methods.add(Method.GET);
		methods.add(Method.POST);
		methods.add(Method.DELETE);
		methods.add(Method.PUT);
		if (methods != null && methods.size() > 0) {
			String header = methods.get(0).name();
			for (int i = 1; i < methods.size(); i++) {
				header += ", " + methods.get(i);
			}
			return header;
		} else {
			return null;
		}
	}

	private Response getDashboard() {
		String html = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\"\n" + 
				"\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
				"<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>" +
				//"<script src=\"libraries/dashboard.js\"></script>" +
				"</head><body></body></html>";

		Response response = new NanoHTTPD.Response(Status.OK, "text/html; charset=utf-8", html);
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}*/


	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		IotHubRequest request = new IotHubRequest(method, uri, parameters, mimeType, getJsonData(method, files));
		IotHubApiRequestHandler handler = this.subHandlers.get(request.getType());
		if (handler == null) {
			//TODO put proper ko response
			return getResponseKo(STATUS_WRONG_URI, STATUS_WRONG_URI);
		}
		else {
			return handler.handleRequest(request);
		}
	}

	@Override
	public boolean acceptRequest(Method method, String uri) {
		//TODO ATM, we accept all requests, but later it would be better the filter the bad ones out
		return true;
	}
}
