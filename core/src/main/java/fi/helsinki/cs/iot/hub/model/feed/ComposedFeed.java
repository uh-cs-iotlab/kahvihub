/*
 * fi.helsinki.cs.iot.hub.model.feed.ComposedFeed
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
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class ComposedFeed extends Feed {

	public static final String KEY_COMPOSED_FEED = "composed";
	private boolean storage;
    private boolean writable;
    private boolean readable;
    private Map<String, Field> fields;

    public ComposedFeed(long id, String name, String metadata, List<String> keywords,
                        boolean storage, boolean readable, boolean writable,
                        Map<String, Field> fields) {
        super(id, name, metadata, keywords);
        this.storage = storage;
        this.writable = writable;
        this.readable = readable;
        this.fields = fields;
        if (this.fields != null) {
        	for (Field field : this.fields.values()) {
        		field.setComposedFeed(this);
        	}
        }
    }

    public boolean hasStorage() {
        return storage;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean isReadable() {
        return readable;
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    @Override
    public FeedType getFeedType() {
        return FeedType.COMPOSED;
    }

    @Override
    public String getDescription() {
    	try {
    	JSONObject jDescription = new JSONObject();
    	//jDescription.put("id", this.getId());
    	jDescription.put("name", this.getName());
    	jDescription.put("metadata", this.getMetadata());
    	if (this.getKeywords() != null) {
    		JSONArray jKeywords = new JSONArray();
    		for (String keyword : getKeywords()) {
    			jKeywords.put(keyword);
    		}
    		jDescription.put("keywords", jKeywords);
    	}
    	else {
    		jDescription.put("keywords", JSONObject.NULL);
    	}
    	jDescription.put("storage", storage);
    	jDescription.put("readable", readable);
    	jDescription.put("writable", writable);
    	JSONArray jFields = new JSONArray();
    	for (Field field : fields.values()) {
    		JSONObject jField = new JSONObject();
    		jField.put("name", field.getName());
    		jField.put("metadata", field.getMetadata());
    		jField.put("optional", field.isOptional());
    		jField.put("type", field.getType());
    		if (field.getKeywords() != null) {
    			JSONArray jKeywords = new JSONArray();
        		for (String keyword : field.getKeywords()) {
        			jKeywords.put(keyword);
        		}
        		jField.put("keywords", jKeywords);
        	}
        	else {
        		jField.put("keywords", JSONObject.NULL);
        	}
    		jFields.put(jField);
    	}
    	jDescription.put("composed", jFields);
    	return jDescription.toString();
    	} catch (JSONException e) {
    		return null;
    	}
        
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public boolean postValue(String json) {
        return false;
    }
}
