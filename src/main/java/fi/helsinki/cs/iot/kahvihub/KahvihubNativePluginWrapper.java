/*
 * fi.helsinki.cs.iot.kahvihub.KahvihubNativePluginWrapper
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
package fi.helsinki.cs.iot.kahvihub;

import java.util.Map;

import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;
import fi.helsinki.cs.iot.kahvihub.plugin.KahvihubNativeFeatureDescription;
import fi.helsinki.cs.iot.kahvihub.plugin.KahvihubNativePlugin;

/**
 * This class makes the bridge between the native plugin interface (unknown from the core library)
 * and the plugin interface from the core library.
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class KahvihubNativePluginWrapper implements Plugin {

	private final KahvihubNativePlugin plugin;
	
	/**
	 * @param plugin
	 */
	public KahvihubNativePluginWrapper(KahvihubNativePlugin plugin) {
		this.plugin = plugin;
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isConfigured()
	 */
	@Override
	public boolean isConfigured() {
		return plugin.isConfigured();
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#needConfiguration()
	 */
	@Override
	public boolean needConfiguration() {
		return plugin.needConfiguration();
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#reconfigure(java.lang.String)
	 */
	@Override
	public boolean configure(String configuration) {
		return plugin.configure(configuration);
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#compareConfiguration(java.lang.String)
	 */
	@Override
	public boolean compareConfiguration(String configuration) {
		return plugin.compareConfiguration(configuration);
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getConfigurationFromHtmlForm(java.util.Map, java.util.Map)
	 */
	@Override
	public String getConfigurationFromHtmlForm(Map<String, String> parameters,
			Map<String, String> files) {
		return plugin.getConfigurationFromHtmlForm(parameters, files);
	}
	
	@Override
	public String getConfigurationHtmlForm() {
		return plugin.getConfigurationHtmlForm();
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getFeatureDescription(int)
	 */
	@Override
	public FeatureDescription getFeatureDescription(int index) {
		KahvihubNativeFeatureDescription nfd = plugin.getFeatureDescription(index);
		if (nfd == null) {
			return null;
		}
		else {
			return new FeatureDescription(nfd.getName(), nfd.getType());
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#getNumberOfFeatures()
	 */
	@Override
	public int getNumberOfFeatures() {
		return plugin.getNumberOfFeatures();
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isAvailable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isAvailable(FeatureDescription featureDescription) {
		if (featureDescription != null) {
			return plugin.isFeatureAvailable(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()));
		}
		else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isReadable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isReadable(FeatureDescription featureDescription) {
		if (featureDescription != null) {
			return plugin.isFeatureReadable(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()));
		}
		else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isSupported(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isSupported(FeatureDescription featureDescription) {
		if (featureDescription != null) {
			return plugin.isFeatureSupported(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()));
		}
		else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.model.enabler.Plugin#isWritable(fi.helsinki.cs.iot.hub.model.feed.FeatureDescription)
	 */
	@Override
	public boolean isWritable(FeatureDescription featureDescription) {
		if (featureDescription != null) {
			return plugin.isFeatureWritable(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()));
		}
		else {
			return false;
		}
	}

	@Override
	public String getJsonData(FeatureDescription featureDescription) {
		if (featureDescription != null) {
			return plugin.getData(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()));
		}
		else {
			return null;
		}
	}

	@Override
	public boolean postJsonData(FeatureDescription featureDescription, String data) {
		if (featureDescription != null) {
			return plugin.postData(
					new KahvihubNativeFeatureDescription(featureDescription.getName(), featureDescription.getType()), data);
		}
		else {
			return false;
		}
	}

	

}
