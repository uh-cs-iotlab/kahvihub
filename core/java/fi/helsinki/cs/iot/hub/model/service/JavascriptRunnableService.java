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

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.utils.Log;


/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class JavascriptRunnableService implements RunnableService {

	private static final String TAG = "JavascriptRunnableService";
	private Service service;
	private String configuration;
	private String script;
	private int mode;
	private Thread thread;
	private DuktapeJavascriptEngineWrapper runningJsEngine;

	public JavascriptRunnableService(int mode, Service service, String script) {
		//TODO I need to have a better look at this
		this.mode = DuktapeJavascriptEngineWrapper.HTTP_REQUEST |
				DuktapeJavascriptEngineWrapper.EVENT_LOOP |
				DuktapeJavascriptEngineWrapper.TCP_SOCKET;
		this.service = service;
		this.configuration = null;
		this.script = script;
		this.runningJsEngine = null;
		this.thread = null;
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
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#configure(java.lang.String)
	 */
	@Override
	public boolean configure(String configuration) throws ServiceException {
		this.configuration = configuration;
		return true;
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
	 * @see fi.helsinki.cs.iot.hub.model.service.Service#needConfiguration()
	 */
	@Override
	public boolean needConfiguration() throws ServiceException {
		DuktapeJavascriptEngineWrapper jsEngineWrapper = 
				new DuktapeJavascriptEngineWrapper(this, mode);
		try {
			return jsEngineWrapper.serviceNeedConfiguration(service.getServiceInfo().getName(), script);
		}
		catch (JavascriptEngineException e) {
			throw new ServiceException(e.getTag() + ": " + e.getMessage());
		}
	}

	@Override
	public Service getService() {
		return service;
	}


	private void cancelAllEvents() {
		if (runningJsEngine != null) {
			runningJsEngine.stopAllEvents(true);
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
		final JavascriptRunnableService jsrservice = this;
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				runningJsEngine = new DuktapeJavascriptEngineWrapper(jsrservice, mode);
				try {
					runningJsEngine.run(
							service.getServiceInfo().getName(), script, service.getConfig());
				} catch (JavascriptEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				runningJsEngine = null;
			}
		});
		thread.start();
	}

	@Override
	public void stop() {
		if (isStarted()) {
			cancelAllEvents();
		}
		else {
			Log.e(TAG, "The thread is not running");
		}

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
