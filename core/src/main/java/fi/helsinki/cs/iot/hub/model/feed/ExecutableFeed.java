/*
 * fi.helsinki.cs.iot.hub.model.feed.ExecutableFeed
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

import java.nio.file.Path;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.jsengine.JavascriptEngineException;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 * //TODO This class is yet to be defined
 */
public class ExecutableFeed extends Feed {

	private static final String TAG = "ExecutableFeed";
	public static final String KEY_EXECUTABLE_FEED = "executable";
    private boolean readable;
    private boolean writable;
    private ExecutableFeedDescription description;
    
	public ExecutableFeed(long id, String name, String metadata, List<String> keywords, 
			boolean readable, boolean writable, ExecutableFeedDescription description) {
        super(id, name, metadata, keywords);
        this.readable = readable;
        this.writable = writable;
        this.description = description;
    }

    @Override
    public FeedType getFeedType() {
        return FeedType.EXECUTABLE;
    }

    @Override
    public String getDescription() {
    	try {
			JSONObject jDescription = new JSONObject();
			jDescription.put("name", getName());
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
			jDescription.put("readable", readable);
			jDescription.put("writable", writable);
			jDescription.put(KEY_EXECUTABLE_FEED, description.toJSON());
			return jDescription.toString();
		} catch (JSONException e) {
			Log.e(TAG, "Could not generate the JSON description of feed " + super.getName());
			return null;
		}
    }

    @Override
    public String getValue() {
    	//TODO auto generated stuff
        return null;
    }

    @Override
    public boolean postValue(String json) {
        return false;
    }

	public boolean isReadable() {
		return readable;
	}

	public boolean isWritable() {
		return writable;
	}

	/*public ExecutableFeedDescription getDescription() {
		return description;
	}*/

	public boolean executeScript(Path libdir, String script) {
		DuktapeJavascriptEngineWrapper djew = new DuktapeJavascriptEngineWrapper(libdir);
		try {
			return djew.runScript(script) != null;
		} catch (JavascriptEngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
