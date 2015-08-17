/*
 * fi.helsinki.cs.iot.hub.api.BasicIotHubApiRequestHandler
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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.uri.IotHubUri;
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
	private static final String STATUS_NOT_YET_IMPLEMENTED = "NOT_YET_IMPLEMENTED";
	private static final String STATUS_METHOD_NOT_SUPPORTED = "METHOD_NOT_SUPPORTED";

	private List<Method> supportedMethods;
	private File rootDir;
	private IoTHubApiRequestHandler libGetReqHandler;
	private IoTHubApiRequestHandler appGetReqHandler;
	private IoTHubApiRequestHandler feedGetReqHandler;
	private IoTHubApiRequestHandler feedPostReqHandler;
	private IoTHubApiRequestHandler pluginGetReqHandler;
	private IoTHubApiRequestHandler pluginPostReqHandler;

	public BasicIotHubApiRequestHandler(File rootDir) {
		this.supportedMethods = new ArrayList<>();
		this.supportedMethods.add(Method.GET);
		this.supportedMethods.add(Method.POST);
		this.supportedMethods.add(Method.PUT);
		this.supportedMethods.add(Method.DELETE);
		this.rootDir = rootDir;
		this.libGetReqHandler = new LibraryGetRequestHandler(
				Paths.get(this.rootDir.getAbsolutePath()));
		this.appGetReqHandler = new ApplicationGetRequestHandler(
				Paths.get(this.rootDir.getAbsolutePath(), "applications"));
		this.feedGetReqHandler = new FeedGetRequestHandler();
		this.feedPostReqHandler = new FeedPostRequestHandler();
		this.pluginGetReqHandler = new PluginGetRequestHandler(
				Paths.get(this.rootDir.getAbsolutePath(), "libraries"));
		this.pluginPostReqHandler = new PluginPostRequestHandler(
				Paths.get(this.rootDir.getAbsolutePath(), "libraries"));
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

	private String getAllowMethodsHeader() {
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
				"<script src=\"libraries/dashboard.js\"></script>" +
				"</head><body></body></html>";

		Response response = new NanoHTTPD.Response(Status.OK, "text/html; charset=utf-8", html);
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}

	private Response handleGetRequest(IotHubUri uri) {
		switch (uri.getType()) {
		case PLUGIN:
			return pluginGetReqHandler.handleRequest(uri);
		case LIBRARY:
			return libGetReqHandler.handleRequest(uri);
		case APPLICATION:
			return appGetReqHandler.handleRequest(uri);
		case FEED:
			return feedGetReqHandler.handleRequest(uri);
		case UNKNOWN:
			if (uri.getIdentifiers() == null || uri.getIdentifiers().isEmpty()) {
				return getDashboard();
			}
		default:
			return getResponseKo(STATUS_WRONG_URI, STATUS_WRONG_URI);
		}
	}

	private Response handlePostRequest(IotHubUri uri) {
		switch (uri.getType()) {
		case PLUGIN:
			return pluginPostReqHandler.handleRequest(uri);
		case FEED:
			return feedPostReqHandler.handleRequest(uri);
		default:
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "POST requests are not yet implemented for others than feeds");
		}
	}

	private Response handlePutRequest(IotHubUri uri) {
		return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "PUT requests are not yet implemented"); 
	}

	private Response handleDeleteRequest(IotHubUri uri) {
		return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "DELETE requests are not yet implemented");
	}

	public Response handleRequest(Method method, String uri,
			Map<String, String> parameters, String mimeType, Map<String, String> files) {
		IotHubUri hubUri = new IotHubUri(uri, parameters, mimeType, getJsonData(files));
		switch (method) {
		case GET:
			return handleGetRequest(hubUri);
		case POST:
			return handlePostRequest(hubUri);
		case PUT:
			return handlePutRequest(hubUri);
		case DELETE:
			return handleDeleteRequest(hubUri);
		default:
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Method " + method.toString() + " is not supported");
		}
	}

	@Override
	public boolean acceptRequest(Method method, String uri) {
		//TODO ATM, we accept all requests, but later it would be better the filter the bad ones out
		return true;
	}
}
