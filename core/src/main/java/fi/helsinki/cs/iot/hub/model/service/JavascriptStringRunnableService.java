/*
 * fi.helsinki.cs.iot.hub.service.JavascriptRunnableService
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

import java.nio.file.Path;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.utils.Log;


/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class JavascriptStringRunnableService implements RunnableService {

	private static final String TAG = "JavascriptStringRunnableService";
	
	private Thread thread;
	private final String jname;
	private final String jscript;
	private String configuration;
	private DuktapeJavascriptEngineWrapper wrapper;
	private Service service;
	
	public JavascriptStringRunnableService(Path libdir, String jname, String jscript, int jsEngineModes) {
		this(libdir, null, jname, jscript, jsEngineModes);
	}
	
	public JavascriptStringRunnableService(Path libdir, Service service, String jname, String jscript, int jsEngineModes) {
		this.jname = jname;
		this.jscript = jscript;
		this.thread = null;
		this.wrapper = new DuktapeJavascriptEngineWrapper(libdir, this, jsEngineModes);
		this.service = service;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#compareConfiguration(java.lang.String)
	 */
	@Override
	public boolean compareConfiguration(String configuration) {
		if (this.configuration == null) {
			return configuration == null;
		}
		return this.configuration.equals(configuration);
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#configure(java.lang.String)
	 */
	@Override
	public boolean configure(String pluginConfig) throws ServiceException {
		if (compareConfiguration(pluginConfig)) {
			return true;
		}
		stop();
		try {
			boolean res = wrapper.pluginCheckConfiguration(jname, jscript, pluginConfig);
			if (res) {
				this.configuration = pluginConfig;
				updateConfiguration();
			}
			return res;
		} catch (JavascriptEngineException e) {
			throw new ServiceException(e.getMessage());
		}
	}

	private void updateConfiguration() {
		if (this.service != null) {
			Service s = IotHubDataAccess.getInstance().updateService(
					service, service.getName(), service.getMetadata(), this.configuration, service.bootAtStartup());
			if (s == null) {
				System.err.println("I could not save the configuration to the db");
			}
			else {
				service = s;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#isConfigured()
	 */
	@Override
	public boolean isConfigured() throws ServiceException {
		return !(needConfiguration() && configuration == null) ;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#isStarted()
	 */
	@Override
	public boolean isStarted() {
		return thread != null && thread.isAlive();
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#needConfiguration()
	 */
	@Override
	public boolean needConfiguration() throws ServiceException {
		try {
			return wrapper.pluginNeedConfiguration(jname, jscript);
		} catch (JavascriptEngineException e) {
			throw new ServiceException(e.getMessage());
		}
	}

	@Override
	public Service getService() {
		return service;
	}


	private void cancelAllEvents() {
		if (wrapper != null) {
			wrapper.stopAllEvents(true);
		}
		else {
			Log.w(TAG, "The javascript engine does not look like it is running");
		}
	}

	public void start() {
		if (isStarted()) {
			Log.d(TAG, "The thread is already started");
			return;
		}
		wrapper.stopAllEvents(false);
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					wrapper.run(service.getServiceInfo().getName(), jscript, configuration);
				} catch (JavascriptEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	@Override
	public void stop() {
		if (isStarted()) {
			cancelAllEvents();
		}
		thread = null;
	}

	@Override
	public void setService(Service service) {
		this.service = service;
	}

	@Override
	public boolean configurePersistant(String configuration) {
		// TODO Auto-generated method stub
		return false;
	}

}
