/*
 * fi.helsinki.cs.iot.hub.jsengine.SimpleJavascriptedIotHubCode
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
package fi.helsinki.cs.iot.hub.jsengine;

/**
 * @author mineraud
 *
 */
public class SimpleJavascriptedIotHubCode implements JavascriptedIotHubCode {

	private int mode;
	private String script;
	
	
	public SimpleJavascriptedIotHubCode(int mode, String script) {
		this.mode = mode;
		this.script = script;
	}


	@Override
	public int getJsEngineModes() {
		return mode;
	}
	
	public String getScript() {
		return script;
	}

	
	@Override
	public boolean configure(String configuration) throws JavascriptEngineException {
		return false;
	}


	@Override
	public boolean configurePersistant(String configuration) {
		// TODO Auto-generated method stub
		return false;
	}

}
