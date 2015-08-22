/*
 * fi.helsinki.cs.iot.hub.model.enabler.JavascriptPlugin
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

import java.util.Map;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptedIotHubCode;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class JavascriptPlugin implements Plugin, JavascriptedIotHubCode {

	private Thread thread;
	private boolean needToStop;
	private final String jname;
	private final String jscript;
	private String configuration;
	private DuktapeJavascriptEngineWrapper wrapper;
	private Enabler enabler;

	public JavascriptPlugin(String jname, String jscript, int jsEngineModes) {
		this(null, jname, jscript, jsEngineModes);
	}
	
	public JavascriptPlugin(Enabler enabler, String jname, String jscript, int jsEngineModes) {
		this.needToStop = false;
		this.jname = jname;
		this.jscript = jscript;
		this.thread = null;
		this.wrapper = new DuktapeJavascriptEngineWrapper(this, jsEngineModes);
		this.enabler = enabler;
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#needConfiguration()
	 */
	@Override
	public boolean needConfiguration() throws PluginException {
		try {
			return wrapper.pluginNeedConfiguration(jname, jscript);
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#compareConfiguration(java.lang.String)
	 */
	@Override
	public boolean compareConfiguration(String pluginConfig) {
		if (!isConfigured()) {
			return pluginConfig == null;
		}
		else {
			return configuration.equals(pluginConfig);
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#configure(java.lang.String)
	 */
	@Override
	public boolean configure(String pluginConfig) throws PluginException {
		if (compareConfiguration(pluginConfig)) {
			return true;
		}
		close();
		try {
			boolean res = wrapper.pluginCheckConfiguration(jname, jscript, pluginConfig);
			if (res) {
				return configurePersistant(pluginConfig);
			}
			return res;
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}
	
	@Override
	public boolean configurePersistant(String configuration) {
		this.configuration = configuration;
		// For testing I need to check if I have a instance of db
		if (this.enabler != null && IotHubDataAccess.getInstance() != null) {
			Enabler e = IotHubDataAccess.getInstance().updateEnabler(enabler, configuration);
			if (e == null) {
				System.err.println("I could not save the persistant configuration to the db");
			}
			else {
				enabler = e;
			}
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isConfigured()
	 */
	@Override
	public boolean isConfigured() {
		return configuration != null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isSupported(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isSupported(FeatureDescription featureDescription) throws PluginException {
		try {
			return wrapper.isPluginFeatureSupported(jname, jscript, configuration, featureDescription.getName());
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isAvailable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isAvailable(FeatureDescription featureDescription) throws PluginException {
		try {
			return wrapper.isPluginFeatureAvailable(jname, jscript, configuration, featureDescription.getName());
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isReadable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isReadable(FeatureDescription featureDescription) throws PluginException {
		try {
			return wrapper.isPluginFeatureReadable(jname, jscript, configuration, featureDescription.getName());
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isWritable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isWritable(FeatureDescription featureDescription) throws PluginException {
		try {
			return wrapper.isPluginFeatureWritable(jname, jscript, configuration, featureDescription.getName());
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getConfigurationFromHtmlForm(java.util.Map, java.util.Map)
	 */
	@Override
	public String getConfigurationFromHtmlForm(Map<String, String> parameters, Map<String, String> files) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getConfigurationHtmlForm()
	 */
	@Override
	public String getConfigurationHtmlForm() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getNumberOfFeatures()
	 */
	@Override
	public int getNumberOfFeatures() throws PluginException {
		try {
			return wrapper.getPluginNumberOfFeatures(jname, jscript, configuration);
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getFeatureDescription(int)
	 */
	@Override
	public FeatureDescription getFeatureDescription(int index) throws PluginException {
		try {
			return wrapper.getPluginFeatureDescription(jname, jscript, configuration, index);
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getValue(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public String getValue(FeatureDescription featureDescription) throws PluginException {
		try {
			return wrapper.getPluginFeatureValue(jname, jscript, configuration, featureDescription.getName());
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#postValue(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription, java.lang.String)
	 */
	@Override
	public boolean postValue(FeatureDescription featureDescription, String data) throws PluginException {
		try {
			//TODO maybe the post plugin feature should send some result back
			//TODO I need to escape the double quotes here and make sure that the type is the same than the fd
			return wrapper.postPluginFeatureValue(jname, jscript, configuration, featureDescription.getName(), data);
		} catch (JavascriptEngineException e) {
			throw PluginException.newJavascriptException(e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#close()
	 */
	@Override
	public void close() throws PluginException {
		// TODO Auto-generated method stub
		if (thread != null && thread.isAlive()) {
			this.needToStop = true;
		}
		while(this.needToStop) {
			;
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#run()
	 */
	@Override
	public void run() throws PluginException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		return "JavascriptPlugin [jname=" + jname + ", jscript=" + jscript + ", configuration=" + configuration + "]";
	}

	

}
