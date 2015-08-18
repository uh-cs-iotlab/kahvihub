/*
 * fi.helsinki.cs.iot.hub.api.ApplicationGetRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class ApplicationGetRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "ApplicationGetRequestHandler";
	//private Path dir;
	private List<Method> methods;

	public ApplicationGetRequestHandler(Path dir) {
		//this.dir = dir;
		this.methods = new ArrayList<>();
		this.methods.add(Method.GET);	
	}

	@Override
	public Response handleRequest(IotHubRequest uri) {
		Log.d(TAG, "Got a request");
		// Case where we just want to list the libraires
		if (uri.getIdentifiers().size() == 0) {
			//Quick try for executing javascript code
			HttpClient httpclient = HttpClientBuilder.create().build();
			// Prepare a request object
			HttpGet httpget = new HttpGet("http://localhost:8080/libraries/test_jsoo.js"); 
			// Execute the request
			HttpResponse response;
			try {
				response = httpclient.execute(httpget);
				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				// If the response does not enclose an entity, there is no need
				// to worry about connection release
				if (entity != null) {
					// A Simple JSON Response Read
					InputStream instream = entity.getContent();
					String result= convertStreamToString(instream);
					DuktapeJavascriptEngineWrapper wrapper = new DuktapeJavascriptEngineWrapper();
					wrapper.runScript(result);
					instream.close();
				}
			} catch (Exception e) {
				getResponseKo("TODO", e.getMessage());
			}
			return getResponseOk("");
		}
		else if (uri.getIdentifiers().size() == 1) {
			//Quick try for executing javascript code
			HttpClient httpclient = HttpClientBuilder.create().build();
			// Prepare a request object
			HttpGet httpget = new HttpGet("http://localhost:8080/libraries/" + uri.getIdentifiers().get(0)); 
			// Execute the request
			HttpResponse response;
			try {
				response = httpclient.execute(httpget);
				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				// If the response does not enclose an entity, there is no need
				// to worry about connection release
				if (entity != null) {
					// A Simple JSON Response Read
					InputStream instream = entity.getContent();
					String result= convertStreamToString(instream);
					DuktapeJavascriptEngineWrapper wrapper = new DuktapeJavascriptEngineWrapper();
					wrapper.runScript(result);
					instream.close();
				}
			} catch (Exception e) {
				getResponseKo("TODO", e.getMessage());
			}
			return getResponseOk("Run is finished");
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	//http://www.vogella.com/code/com.vogella.android.webservice.rest/src/com/vogella/android/webservice/rest/RestClient.html
	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
