/*
 * fi.helsinki.cs.iot.hub.api.handlers.services.ServiceGetRequestHandler
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

import org.json.JSONArray;
import org.json.JSONException;

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
public class ServiceGetRequestHandler extends IotHubApiRequestHandler {
	
	private static final String TAG = "ServiceGetRequestHandler";
	private static final String START = "start";
	private static final String STOP = "stop";
	
	private List<Method> methods;
	
	

	public ServiceGetRequestHandler() {
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

	private Response getResponseWithServiceName(String serviceName) {
		if (serviceName == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the service name");
		}
		else {
			try {
				Service service = IotHubDataAccess.getInstance().getService(serviceName);
				if (service == null) {
					return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Could not get the service with name " + serviceName);
				}
				else {
					return getResponseOk(service.toJSON().toString());
				}
			} catch (JSONException e) {
				return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the service name");
			}
		}
	}

	private Response getResponseWithSendingSignalToService(String serviceName, boolean doStart) {
		if (serviceName == null) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the service name");
		}
		try {
			Service service = IotHubDataAccess.getInstance().getService(serviceName);
			if (service == null) {
				return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Could not get the service with name " + serviceName);
			}
			else {
				RunnableService runnableService = ServiceManager.getInstance().getConfiguredRunnableService(service);
				if (doStart) {
					if (runnableService.isStarted()) {
						Log.w(TAG, "No need to start the service, it has been already started");
					}
					else {
						runnableService.start();
					}
				}
				else {
					if (!runnableService.isStarted()) {
						Log.w(TAG, "No need to stop the service, it is already stopped");
					}
					else {
						runnableService.stop();
					}
				}
				return getResponseOk(service.toJSON().toString());
			}
		} catch (JSONException e) {
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "The method get is not available without the service name");
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return getResponseKo(STATUS_METHOD_NOT_SUPPORTED, "Problem with the plugin");
		}
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubRequest request) {
		Log.d(TAG, "I got a get request for services");
		// Case where we just want to list the enablers
		if (request.getIdentifiers().size() == 0) {
			String serviceName = request.getOptions().get("name");
			if (serviceName == null) {
				List<Service> services = IotHubDataAccess.getInstance().getServices();
				JSONArray jArray = new JSONArray();
				for (Service service : services) {
					try {
						jArray.put(service.toJSON());
					} catch (JSONException e) {
						System.err.println(e);
						return getResponseKo(STATUS_IO_ERROR, e.getMessage());
					}
				}
				return getResponseOk(jArray.toString());
			}
			else {
				return getResponseWithServiceName(serviceName);
			}
		}
		else if (request.getIdentifiers().size() == 1) {
			return getResponseWithServiceName(request.getIdentifiers().get(0));
		}
		else if (request.getIdentifiers().size() == 2) {
			if (START.equals(request.getIdentifiers().get(1))) {
				return getResponseWithSendingSignalToService(request.getIdentifiers().get(0), true);
			}
			else if (STOP.equals(request.getIdentifiers().get(1))) {
				return getResponseWithSendingSignalToService(request.getIdentifiers().get(0), false);
			}
			else {
				return getResponseKo(STATUS_BAD_REQUEST, "Signal unknown");
			}
		}
		else {
			return getResponseKo(STATUS_NOT_YET_IMPLEMENTED, STATUS_NOT_YET_IMPLEMENTED);
		}
	}

}
