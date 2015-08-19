/*
 * fi.helsinki.cs.iot.hub.model.enabler.Enabler
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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class Enabler {

    private long id;
    private String name;
    private String metadata;
    private PluginInfo plugin;
    private String pluginInfoConfig;
    private List<Feature> features;

    public Enabler(long id, String name, String metadata, PluginInfo plugin, String pluginInfoConfig) {
        this.id = id;
        this.name = name;
        this.metadata = metadata;
        this.plugin = plugin;
        this.pluginInfoConfig = pluginInfoConfig;
        this.features = new ArrayList<Feature>();
    }

//    public Enabler(long id, String name, String metadata, PluginInfo plugin, String pluginInfoConfig, List<Feature> features) {
//        this.id = id;
//        this.name = name;
//        this.metadata = metadata;
//        this.plugin = plugin;
//        this.pluginInfoConfig = pluginInfoConfig;
//        this.features = features;
//    }

    public List<Feature> getFeatures() {
        return features;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMetadata() {
        return metadata;
    }

    public PluginInfo getPluginInfo() {
        return plugin;
    }

    public String getPluginConfig() {
        return pluginInfoConfig;
    }

    public boolean addFeature(Feature feature) {
        return features.add(feature);
    }

    public boolean removeFeature(Feature feature) {
        return features.remove(feature);
    }

    public boolean hasFeature(FeatureDescription featureDescription) {
        if (features != null) {
            for (Feature feature : features) {
                if(feature.equals(featureDescription)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Feature getFeature(FeatureDescription fd) {
        if (features != null) {
            for (Feature feature : features) {
                if(feature.equals(fd)) {
                    return feature;
                }
            }
        }
        return null;
    }

	public Object toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", name);
		if (metadata != null) {
			json.put("metadata", metadata);
		}
		if (pluginInfoConfig != null) {
			json.put("config", pluginInfoConfig);
		}
		json.put("plugin", plugin.toJSON());
		JSONArray array = new JSONArray();
		for(Feature feature : features) {
			array.put(feature.toJSON());
		}
		json.put("features", array);
		return json;
	}
}
