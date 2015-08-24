/*
 * fi.helsinki.cs.iot.hub.model.feed.AtomicFeed
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
package fi.helsinki.cs.iot.hub.model.feed;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.Plugin;
import fi.helsinki.cs.iot.hub.model.enabler.PluginException;
import fi.helsinki.cs.iot.hub.model.enabler.PluginManager;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * This class represents all feeds which are directly linked to an enabler feature
 */
public class AtomicFeed extends Feed {

    private static final String TAG = "AtomicFeed";
	public static final String KEY_ATOMIC_FEED = "atomic";
    private Feature feature;

    public AtomicFeed(long id, String name, String metadata, List<String> keywords, Feature feature) {
        super(id, name, metadata, keywords);
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }

    @Override
    public FeedType getFeedType() {
        return FeedType.ATOMIC;
    }

    @Override
    public String getDescription() {
        JSONObject mainObject = new JSONObject();
        try {
            mainObject.put("name", getName());
            mainObject.put("atomic", feature.getType());
            mainObject.put("readable", feature.isReadable());
            mainObject.put("writable", feature.isWritable());
            if (getMetadata() != null) {
                mainObject.put("metadata", getMetadata());
            }
            if (getKeywords() != null || !getKeywords().isEmpty()) {
                JSONArray keywordArray = new JSONArray();
                for (String keyword : getKeywords()) {
                    keywordArray.put(keyword);
                }
                mainObject.put("keywords", keywordArray);
            }

        } catch (Exception e) {
        	//TODO auto-generated stuff
        	e.printStackTrace();
            return null;
        }
        return mainObject.toString();
    }

    @Override
    public String getValue() {
    	if (feature == null) {
            Log.w(TAG, "The feature should not be null, EVER");
        } else if (!feature.isAvailable()) {
        	Log.w(TAG, "The feature " + feature.getId() +
                    " is currently not available and thus cannot be read");
        } else if (!feature.isReadable()) {
        	Log.w(TAG, "The feature " + feature.getId() +
                    " is not available readable");
        }
        else {
        	Enabler enabler = feature.getEnabler();
        	try {
				Plugin plugin = PluginManager.getInstance().getConfiguredPlugin(enabler);
				if (plugin != null) {
					return plugin.getValue(feature);
				}
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return null;
    }

    @Override
    public boolean postValue(String value) {
    	if (feature == null) {
            Log.w(TAG, "The feature should not be null, EVER");
        } else if (!feature.isAvailable()) {
        	Log.w(TAG, "The feature " + feature.getId() +
                    " is currently not available and thus cannot be read");
        } else if (!feature.isWritable()) {
        	Log.w(TAG, "The feature " + feature.getId() +
                    " is not available writable");
        }
        else {
        	Enabler enabler = feature.getEnabler();
        	try {
				Plugin plugin = PluginManager.getInstance().getConfiguredPlugin(enabler);
				if (plugin != null) {
					boolean result = plugin.postValue(feature, value);
					return result;
					
				}
			} catch (PluginException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return false;
    }


}
