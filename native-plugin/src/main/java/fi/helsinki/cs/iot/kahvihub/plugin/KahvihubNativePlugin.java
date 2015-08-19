/*
 * fi.helsinki.cs.iot.kahvihub.plugin.KahvihubNativePlugin
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
package fi.helsinki.cs.iot.kahvihub.plugin;

import java.util.List;
import java.util.Map;

/**
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public interface KahvihubNativePlugin {

	public void plug();
	public void unplug();
	public String getName();
	public boolean isConfigured();
	public boolean needConfiguration();
	public boolean configure(String configuration);
	public boolean compareConfiguration(String configuration);
	public List<String> getDataTypes();
	public String getConfigurationFromHtmlForm(Map<String, String> parameters, Map<String, String> files);
	public String getConfigurationHtmlForm();
	public int getNumberOfFeatures();
    public KahvihubNativeFeatureDescription getFeatureDescription(int index);
    public boolean isFeatureSupported(KahvihubNativeFeatureDescription description);
    public boolean isFeatureAvailable(KahvihubNativeFeatureDescription description);
    public boolean isFeatureWritable(KahvihubNativeFeatureDescription description);
    public boolean isFeatureReadable(KahvihubNativeFeatureDescription description);
    public String getValue(KahvihubNativeFeatureDescription description);
    public boolean postValue(KahvihubNativeFeatureDescription description, String value);

}
