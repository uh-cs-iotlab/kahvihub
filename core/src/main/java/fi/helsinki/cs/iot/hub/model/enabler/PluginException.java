/*
 * fi.helsinki.cs.iot.hub.model.PluginException
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
package fi.helsinki.cs.iot.hub.model.enabler;

import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;

/**
 * This exception is raised when the configuration of the plugin was wrong
 * or the plugin file was not of the proper kind
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class PluginException extends JavascriptEngineException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1732515090408872296L;
	private static final String TAG = "PluginException";
	
	public enum Type { NATIVE, JAVASCRIPT };
	
	private Type type;
	
	private PluginException(Type type, String message) {
		super(TAG, message);
		this.type = type;
	}
	
	public static PluginException newNativeException(String message) {
		return new PluginException(Type.NATIVE, message);
	}
	
	public static PluginException newJavascriptException(String message) {
		return new PluginException(Type.JAVASCRIPT, message);
	}

	public boolean isNative() {
		return this.type == Type.NATIVE;
	}
	
	public boolean isJavascript() {
		return this.type == Type.JAVASCRIPT;
	}
	
}
