/*
 * fi.helsinki.cs.iot.hub.api.handlers.enablers.EnablerGetRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.enablers;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class EnablerGetRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "EnablerGetRequestHandler";
	private List<Method> methods;

	public EnablerGetRequestHandler() {
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

	private Response getResponseWithEnablerName(String enablerName) {
		if (enablerName == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the enabler name");
		}
		else {
			try {
				Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerName);
				if (enabler == null) {
					return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Could not get the enable with name " + enablerName);
				}
				else {
					return getResponseOk(enabler.toJSON().toString());
				}
			} catch (JSONException e) {
				return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the plugin id, and plugin id should be a long");
			}
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		Log.d(TAG, "I got a get request for enabler");
		// Case where we just want to list the enablers
		if (request.getIdentifiers().size() == 0) {
			String enablerName = request.getOptions().get("name");
			if (enablerName == null) {
				List<Enabler> enablers = IotHubDataAccess.getInstance().getEnablers();
				JSONArray jArray = new JSONArray();
				for (Enabler enabler : enablers) {
					try {
						jArray.put(enabler.toJSON());
					} catch (JSONException e) {
						System.err.println(e);
						return getResponseKo(STATUS_IO_ERROR, e.getMessage());
					}
				}
				return getResponseOk(jArray.toString());
			}
			else {
				return getResponseWithEnablerName(enablerName);
			}
		}
		else if (request.getIdentifiers().size() == 1) {
			return getResponseWithEnablerName(request.getIdentifiers().get(0));
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

}
