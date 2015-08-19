/*
 * fi.helsinki.cs.iot.hub.api.handlers.services.ServiceDeleteRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.services;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class ServiceDeleteRequestHandler extends IotHubApiRequestHandler {

	private List<Method> methods;

	public ServiceDeleteRequestHandler() {
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

	private Response getResponseWithServiceName(String serviceName) {
		if (serviceName == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method delete is not available without the service name");
		}
		else {
			try {
				Service service = IotHubDataAccess.getInstance().getService(serviceName);
				if (service == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the service with name " + serviceName);
				}
				service = IotHubDataAccess.getInstance().deleteService(service);
				if (service == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not delete the service  " + serviceName);
				}
				else {
					ServiceManager.getInstance().removeService(service);
					return getResponseOk(service.toJSON().toString());
				}
			} catch (JSONException | NumberFormatException e) {
				return getResponseKo(STATUS_BAD_REQUEST, "The method delete failed for the enabler " + serviceName);
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
			return getResponseWithServiceName(uri.getOptions().get("name"));
		}
		else {
			return getResponseWithServiceName(uri.getIdentifiers().get(0));
		}
	}

}
