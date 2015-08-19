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
import java.nio.file.Path;
import java.nio.file.Paths;

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.utils.ScriptUtils;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class JavascriptRunnableServiceHelper implements RunnableServiceHelper {

	//private static final String TAG = "JavascriptRunnableServiceHelper";
	private Path libdir;

	public JavascriptRunnableServiceHelper(Path libdir) {
		this.libdir = libdir;
	}

	public RunnableService createRunnableService(Service service) {
		String pluginName = service.getServiceInfo().getName();
		File file = Paths.get(libdir.toAbsolutePath().toString(), service.getServiceInfo().getFilename()).toFile();
		String script = ScriptUtils.convertFileToString(file);
		//TODO fix that
		int mode = DuktapeJavascriptEngineWrapper.EVENT_LOOP |
				DuktapeJavascriptEngineWrapper.HTTP_REQUEST |
				DuktapeJavascriptEngineWrapper.TCP_SOCKET;
		JavascriptRunnableService javascriptRunnableService = new JavascriptRunnableService(service, pluginName, script, mode);
		return javascriptRunnableService;
	}

	@Override
	public void checkService(String serviceName, File file) throws ServiceException {
		String script = ScriptUtils.convertFileToString(file);
		checkService(serviceName, script);
	}

	@Override
	public void checkService(String serviceName, String script) throws ServiceException {
		DuktapeJavascriptEngineWrapper wrapper = 
				new DuktapeJavascriptEngineWrapper();
		try {
			boolean checked = wrapper.checkService(serviceName, script);
			if (!checked) {
				throw PluginException.newJavascriptException("Javascript plugin has not passed the checkup");
			}
		} catch (JavascriptEngineException e) {
			throw new ServiceException(e.getMessage());
		}

	}

}
