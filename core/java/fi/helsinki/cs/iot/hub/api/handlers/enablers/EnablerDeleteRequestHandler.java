/*
 * fi.helsinki.cs.iot.hub.api.handlers.enablers.EnablerDeleteRequestHandler
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

import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class EnablerDeleteRequestHandler extends IotHubApiRequestHandler {

	private List<Method> methods;

	public EnablerDeleteRequestHandler() {
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

	private Response getResponseWithEnablerId(String enablerName) {
		if (enablerName == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the enabler name");
		}
		else {
			try {
				Enabler enabler = IotHubDataAccess.getInstance().getEnabler(enablerName);
				if (enabler == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the enabler with name " + enablerName);
				}
				enabler = IotHubDataAccess.getInstance().deleteEnabler(enabler);
				if (enabler == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the enabler with id " + enablerName);
				}
				else {
					PluginManager.getInstance().removePlugin(enabler);
					return getResponseOk(enabler.toJSON().toString());
				}
			} catch (JSONException | NumberFormatException e) {
				return getResponseKo(STATUS_BAD_REQUEST, "The method delete failed for the enabler " + enablerName);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.request.IotHubRequest)
	 */
	@Override
	public Response handleRequest(IotHubRequest uri) {
		// To delete a enabler, there should be just the name
		
		if (uri.getIdentifiers().size() > 1) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the enabler name");
		}
		else if (uri.getIdentifiers().size() == 0) {
			return getResponseWithEnablerId(uri.getOptions().get("name"));
		}
		else {
			return getResponseWithEnablerId(uri.getIdentifiers().get(0));
		}
	}

}
