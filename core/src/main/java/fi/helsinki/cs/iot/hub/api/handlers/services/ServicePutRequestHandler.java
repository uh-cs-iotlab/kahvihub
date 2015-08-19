/*
 * fi.helsinki.cs.iot.hub.api.handlers.services.ServicePutRequestHandler
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
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.service.RunnableService;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceException;
import fi.helsinki.cs.iot.hub.model.service.ServiceManager;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class ServicePutRequestHandler extends IotHubApiRequestHandler {

	private static final String TAG = "ServicePutRequestHandler";
	private List<Method> methods;

	public ServicePutRequestHandler() {
		this.methods = new ArrayList<>();
		this.methods.add(Method.PUT);	
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	private Response getResponseForServiceUpdate(String serviceName, String bodyData) {
		if (serviceName == null || bodyData == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method put is not available without the service name");
		}
		else {
			try {
				Service service = IotHubDataAccess.getInstance().getService(serviceName);
				if (service == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Cannot update the non-exisiting service " + serviceName);
				}

				JSONObject jdata = new JSONObject(bodyData);

				if (!jdata.has("configuration") && !jdata.has("name") && !jdata.has("metadata") && !jdata.has("bootAtStartup")) {
					Log.w(TAG, "No need to update if nothing is new");
					System.err.println("No need to update if nothing is new");
					return getResponseOk(service.toJSON().toString());
				}

				String newName = jdata.has("name") ? jdata.getString("name") : service.getName();
				String newMetadata = jdata.has("metadata") ? jdata.getString("metadata") : service.getMetadata();
				String newConfiguration = jdata.has("configuration") ? jdata.getString("configuration") : service.getConfig();
				boolean bootAtStartup = jdata.has("bootAtStartup") ? jdata.getBoolean("bootAtStartup") : service.bootAtStartup();

				//Need to check the new configuration if there is one
				if (jdata.has("configuration")) {
					try {
						RunnableService runnableService = ServiceManager.getInstance().getConfiguredRunnableService(service);
						if (runnableService == null) {
							return getResponseKo(STATUS_BAD_REQUEST, "Could not get the corresponding runnableService");
						}
						if (!runnableService.configure(newConfiguration)) {
							return getResponseKo(STATUS_BAD_REQUEST, "The runnableService could not be configured");
						}
					} catch (ServiceException e ) {
						e.printStackTrace();
						return getResponseKo(STATUS_BAD_REQUEST, "The runnableService could not be retrieve");
					}
				}

				Service updatedService = IotHubDataAccess.getInstance().updateService(service, newName, newMetadata, newConfiguration, bootAtStartup);
				if (updatedService == null) {
					return getResponseKo(STATUS_BAD_REQUEST, "Could not update the service with name " + serviceName);
				}
				else {
					return getResponseOk(updatedService.toJSON().toString());
				}
			} catch (JSONException e) {
				return getResponseKo(STATUS_BAD_REQUEST, "Could not update the service with name " + serviceName);
			}
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.request.IotHubRequest)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		if (request == null) {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, "Cannot work with no request");
		}
		else if (request.getMethod() != Method.PUT) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Method " + request.getMethod() + " not supported");
		}
		else if (request.getIdentifiers().size() == 0) {
			String enablerName = request.getOptions().get("name");
			if (enablerName == null) {
				return getResponseKo(STATUS_BAD_REQUEST, "Cannot update the enabler without name");
			}
			else {
				return getResponseForServiceUpdate(enablerName, request.getBodyData());	
			}
		}
		else if (request.getIdentifiers().size() == 1) {
			//TODO In the future, I would like to be able to update many features at once
			return getResponseForServiceUpdate(request.getIdentifiers().get(0), request.getBodyData());	
		}
		else {
			//FIXME fix that
			return getResponseKo(STATUS_BAD_REQUEST, STATUS_BAD_REQUEST);
		}
	}

}
