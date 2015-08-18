/*
 * fi.helsinki.cs.iot.hub.service.JavascriptRunnableServiceHelper
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class JavascriptRunnableServiceHelper implements RunnableServiceHelper {

	private static final String TAG = "JavascriptRunnableServiceHelper";
	//private static final String TAG = "JavascriptRunnableServiceHelper";
	private List<String> initFiles;
	private String serviceFolder;

	public JavascriptRunnableServiceHelper(String serviceFolder, List<String> initFiles) {
		this.initFiles = new ArrayList<String>();
		for (String f : initFiles) {
			this.initFiles.add(serviceFolder + f); 
		}
		this.serviceFolder = serviceFolder;
	}

	public RunnableService createService(Service service) {
		try {
			InputStream inputStream = new FileInputStream(serviceFolder + service.getServiceInfo().getFilename());
			String script = ScriptUtils.convertStreamToString(inputStream);
			inputStream.close();
			//TODO I have to implement the modes for the services as well
			JavascriptRunnableService javascriptRunnableService = new JavascriptRunnableService(0, service, script);
			return javascriptRunnableService;
		}
		catch (IOException e) {
			e.printStackTrace();
		} 
		return null;
	}

	@Override
	public void checkService(String serviceName, File file) throws ServiceException {
		DuktapeJavascriptEngineWrapper jsEngineWrapper = 
				new DuktapeJavascriptEngineWrapper();
		try {
			InputStream inputStream = new FileInputStream(file);
			String script = ScriptUtils.convertStreamToString(inputStream);
			inputStream.close();
			if (jsEngineWrapper.checkService(serviceName, script)) {
				Log.i(TAG, "I have checked this script");
			}
			else {
				throw new ServiceException("The script has not pass the checks and should have raised an exception");
			}
		}
		catch (IOException e) {
			throw new ServiceException(e.getMessage());
		} catch (JavascriptEngineException e) {
			throw new ServiceException(e.getTag() + ": " + e.getMessage());
		}
	}

}
