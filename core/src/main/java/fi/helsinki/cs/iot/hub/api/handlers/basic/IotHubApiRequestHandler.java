/*
 * fi.helsinki.cs.iot.hub.api.IotHubApiRequestHandler
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

import java.io.File;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response.Status;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public abstract class IotHubApiRequestHandler {

	protected static final String STATUS_METHOD_NOT_SUPPORTED = "METHOD_NOT_SUPPORTED";
	protected static final String STATUS_NOT_YET_IMPLEMENTED = "NOT_YET_IMPLEMENTED";
	protected static final String STATUS_BAD_REQUEST = "BAD_REQUEST";
	protected static final String STATUS_IO_ERROR = "IO_ERROR";
	
	public abstract List<Method> getSupportedMethods();
	public abstract Response handleRequest(IotHubRequest request);

	public boolean isMethodSupported(Method method) {
		return getSupportedMethods().contains(method);
	}
	
	private String getAllowMethodsHeader() {
		List<Method> methods = getSupportedMethods();
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

	protected Response getResponseOk(File file, String mimeType) {
		String source = ScriptUtils.convertFileToString(file);
		if (source == null) {
			return getResponseKo(STATUS_IO_ERROR, STATUS_IO_ERROR);
		}
		Response response = new NanoHTTPD.Response(Status.OK, mimeType, ScriptUtils.convertFileToString(file));
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}

	protected Response getResponseOk(String message) {
		Response response = new NanoHTTPD.Response(Status.OK, "application/json", message);
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}

	protected Response getResponseKo(String status, String errMessage) {
		JSONObject answer = new JSONObject();
		try {
			answer.put("status", status);
			answer.put("message", errMessage);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Response response = new NanoHTTPD.Response(Status.BAD_REQUEST, "application/json", answer.toString());
		String allowMethodsHeader = getAllowMethodsHeader();
		if (allowMethodsHeader != null) {
			response.addHeader("Access-Control-Allow-Methods", allowMethodsHeader);
		}
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "X-Requested-With");
		return response;
	}


}
