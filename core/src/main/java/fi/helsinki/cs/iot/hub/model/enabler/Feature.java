/*
 * fi.helsinki.cs.iot.hub.model.enabler.Feature
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

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.feed.FeatureDescription;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class Feature extends FeatureDescription implements Comparable<Feature> {

    //private static final String TAG = "Feature";
	private Enabler enabler;
    private long id;
    private boolean isAtomicFeed;

    public Feature(long id, Enabler enabler, String name, String type) {
        super(name, type);
        this.id = id;
        this.enabler = enabler;
    }

    public Feature(long id, Enabler enabler, String name, String type, boolean isAtomicFeed) {
        super(name, type);
        this.id = id;
        this.enabler = enabler;
        this.isAtomicFeed = isAtomicFeed;
    }

    public long getId() {
        return id;
    }

    public Enabler getEnabler() {
        return enabler;
    }

    public boolean isReadable() {
		try {
			Plugin plugin = 
					PluginManager.getInstance().getConfiguredPlugin(enabler);
			return plugin.isReadable(this);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }

    public boolean isWritable() {
    	try {
    		Plugin plugin = 
					PluginManager.getInstance().getConfiguredPlugin(enabler);
			return plugin.isWritable(this);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }

    public boolean isSupported() {
    	try {
			Plugin plugin = 
					PluginManager.getInstance().getConfiguredPlugin(enabler);
			return plugin.isSupported(this);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }

    public boolean isAtomicFeed() {
        return isAtomicFeed;
    }

    public void setAtomicFeed(boolean isAtomicFeed) {
        this.isAtomicFeed = isAtomicFeed;
    }

    public boolean isAvailable() {
    	try {
			Plugin plugin = 
					PluginManager.getInstance().getConfiguredPlugin(enabler);
			return plugin.isAvailable(this);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return false;
    }

    @Override
    public int compareTo(Feature another) {
        //I want to have first the supported features, then order them by name
        if (isSupported() == another.isSupported()) {
            return getName().compareTo(another.getName());
        } else if (isSupported()) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "Feature{" +
                "enabler=" + enabler +
                ", id=" + id +
                ", isReadable=" + isReadable() +
                ", isWritable=" + isWritable() +
                ", isSupported=" + isSupported() +
                ", isAvailable=" + isAvailable() +
                ", isAtomicFeed=" + isAtomicFeed +
                '}';
    }

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("name", getName());
		json.put("type", getType());
		json.put("isReadable", isReadable());
		json.put("isWritable", isWritable());
		json.put("isSupported", isSupported());
		json.put("isAvailable", isAvailable());
		json.put("isAtomicFeed", isAtomicFeed);
		return json;
	}
}
