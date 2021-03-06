/*
 * fi.helsinki.cs.iot.hub.service.ServiceManager
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
package fi.helsinki.cs.iot.hub.model.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class ServiceManager {

	private static final String TAG = "ServiceManager";
	private static ServiceManager instance = null;
	private Map<String, RunnableService> services;
	private RunnableServiceHelper serviceHelper;


	private ServiceManager() {
		this.services = new HashMap<String, RunnableService>();
	}

	public static ServiceManager getInstance() {
		if (instance == null) {
			instance = new ServiceManager();
		}
		return instance;
	}

	public void setServiceHelper(RunnableServiceHelper serviceHelper) {
		if (serviceHelper == null) {
			Log.w(TAG, "Be careful, you are unsetting the service helper");
		}
		this.serviceHelper = serviceHelper;
	}

	public RunnableService getRunnableService(Service service) {
		if (service == null) {
			return null;
		}
		RunnableService runnableService = services.get(service.getName());
		// If the service does not exist, I need to create one
		if (runnableService == null) {
			if (serviceHelper == null) {
				System.out.println(TAG);
				Log.e(TAG, "Trying to instanciate a runnable service when no helper has been set");
				return null;
			}
			runnableService = serviceHelper.createRunnableService(service);
			if (runnableService == null) {
				Log.e(TAG, "The runnable service could not be created");
			}
			else {
				services.put(service.getName(), runnableService);
				Log.i(TAG, "Service " + service.getName() + " registered");
			}
			return runnableService;
		}
		return runnableService;
	}

	public RunnableService updateRunnableService(Service oldService, Service newService) {
		RunnableService runnableService = services.get(oldService.getName());
		if (runnableService == null) {
			Log.e(TAG, "Trying to update a service that does not exist");
			return null;
		}
		runnableService.setService(newService);
		services.remove(oldService.getName());
		services.put(newService.getName(), runnableService);
		return runnableService;
	}
	
	public boolean startStopRunnableService(Service service, boolean doStart) {
		RunnableService runnableService = services.get(service.getName());
		if (runnableService == null) {
			Log.e(TAG, "Trying to update a service that does not exist");
			return false;
		}
		if (doStart && !runnableService.isStarted()) {
			runnableService.start();
			services.put(service.getName(), runnableService);
			return true;
		}
		else if (!doStart && runnableService.isStarted()) {
			runnableService.stop();
			services.put(service.getName(), runnableService);
			return true;
		}
		return false;
	}

	public RunnableService getConfiguredRunnableService(Service service) throws ServiceException {
		RunnableService runnableService = getRunnableService(service);
		if (runnableService == null) {
			return null;
		}
		if (runnableService.needConfiguration()) {
			Log.d(TAG, "Service " + service.getName() + " needs configuration");
			if (runnableService.isConfigured()) {
				if (!runnableService.compareConfiguration(service.getConfig())) {
					runnableService.configure(service.getConfig());
				}
				Log.d(TAG, "No need to reconfigure the service with the same configuration");
			}
			else {
				runnableService.configure(service.getConfig());
			}
			return runnableService;
		}
		else {
			//No need to do anything if the service does not need to be configured
			Log.d(TAG, "Service " + service.getName() + " does not need configuration");
			return runnableService;
		}
	}

	public void checkService(String serviceName, String script) throws ServiceException {
		if (serviceHelper != null) {
			Log.i(TAG, "Checking service " + serviceName);
			serviceHelper.checkService(serviceName, script);
		}
		else {
			Log.e(TAG, "Cannot check the native plugin if the helper is null");
		}
	}

	public void removeAllServices() {
		for(Iterator<Map.Entry<String, RunnableService>> it = services.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, RunnableService> entry = it.next();
			entry.getValue().stop();
			it.remove();
		}
	}

	public RunnableService removeService(Service service) {
		RunnableService runnableService = services.get(service.getName());
		if (runnableService != null) {
			runnableService.stop();
		}
		return services.remove(service.getName());

	}

	public void removeAllServicesForServiceInfo(ServiceInfo serviceInfo) {
		for(Iterator<Map.Entry<String, RunnableService>> it = services.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, RunnableService> entry = it.next();
			Service service = IotHubDataAccess.getInstance().getService(entry.getKey());
			if (service != null && service.getServiceInfo().equals(serviceInfo)) {
				entry.getValue().stop();
				it.remove();
			}
		}
	}

}
