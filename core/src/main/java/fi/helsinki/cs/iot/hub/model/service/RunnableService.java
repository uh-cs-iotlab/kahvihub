/*
 * fi.helsinki.cs.iot.hub.service.RunnableService
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

import fi.helsinki.cs.iot.hub.jsengine.JavascriptedIotHubCode;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public interface RunnableService extends JavascriptedIotHubCode{
	
	public Service getService();
	public void setService(Service service);
	public boolean needConfiguration() throws ServiceException;
	public boolean isConfigured() throws ServiceException; 
	public boolean compareConfiguration(String configuration);
	public boolean configure(String configuration) throws ServiceException;
	public void start();
	public void stop();
	public boolean isStarted();
}
