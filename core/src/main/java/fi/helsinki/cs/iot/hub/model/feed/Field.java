/*
 * fi.helsinki.cs.iot.hub.model.feed.Field
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

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class Field extends FieldDescription {

    private long id;
    private ComposedFeed feed;

    public Field(long id, String name, String type, String metadata, boolean optional, List<String> keywords) {
        super(name, type, metadata, optional, keywords);
    	this.id = id;
        this.feed = null;
    }

    public Field(long id, String name, String type, String metadata, boolean optional) {
        this(id, name, type, metadata, optional, new ArrayList<String>());
    }

    public long getId() {
        return id;
    }

    public ComposedFeed getComposedFeed() {
        return feed;
    }
    
    public void setComposedFeed(ComposedFeed feed) {
        this.feed = feed;
    }

    /*

    public Field(JSONObject json) throws Exception {
        this.name = json.getString("name");
        this.type = FeatureUtils.stringToFeatureType(json.getString("type"));
        this.metadata = json.has("metadata") ? json.getString("metadata") : null;
        this.keywords = new ArrayList<String>();
        if (!json.has("keywords")) return;
        JSONArray kw = json.getJSONArray("keywords");
        for (int i = 0; i < kw.length(); ++i) {
            this.keywords.add(kw.getString(0));
        }
    }

    public String toString() {
        try {
            JSONObject json = toJson();
            return json.toString();
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), Log.getStackTraceString(e));
            return null;
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", this.name);
        obj.put("datatype", this.type);
        if (metadata != null) {
            obj.put("metadata", this.metadata);
        }
        return obj;
    }
    */
}
