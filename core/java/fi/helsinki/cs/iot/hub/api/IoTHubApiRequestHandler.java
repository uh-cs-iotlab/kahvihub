/*
 * fi.helsinki.cs.iot.hub.api.IoTHubApiRequestHandler
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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
public abstract class IoTHubApiRequestHandler {

	protected static final String STATUS_METHOD_NOT_SUPPORTED = "METHOD_NOT_SUPPORTED";
	protected static final String STATUS_NOT_YET_IMPLEMENTED = "NOT_YET_IMPLEMENTED";
	protected static final String STATUS_IO_ERROR = "IO_ERROR";
	
	public abstract List<Method> getSupportedMethods();
	public abstract Response handleRequest(IotHubUri uri);

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

	/**
	 * From MarkdownWebServerPlugin (NanoHttpd)
	 * @author Paul S. Hawke (paul.hawke@gmail.com)
	 * On: 9/13/13 at 4:03 AM
	 */
	private String readSource(File file) {
		FileReader fileReader = null;
		BufferedReader reader = null;
		try {
			fileReader = new FileReader(file);
			reader = new BufferedReader(fileReader);
			String line = null;
			StringBuilder sb = new StringBuilder();
			do {
				line = reader.readLine();
				if (line != null) {
					sb.append(line).append("\n");
				}
			} while (line != null);
			reader.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (fileReader != null) {
					fileReader.close();
				}
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ignored) {}
		}
	}

	protected Response getResponseOk(File file, String mimeType) {
		String source = readSource(file);
		if (source == null) {
			return getResponseKo(STATUS_IO_ERROR, STATUS_IO_ERROR);
		}
		Response response = new NanoHTTPD.Response(Status.OK, mimeType, readSource(file));
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
