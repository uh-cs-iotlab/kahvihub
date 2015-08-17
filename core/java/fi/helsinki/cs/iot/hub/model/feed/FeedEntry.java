/*
 * fi.helsinki.cs.iot.hub.model.feed.FeedEntry
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

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class FeedEntry {
	
	private static final String TAG = "FeedEntry";
	private long id;
	private Feed feed;
	private Date timestamp;
	private JSONObject data;
	
	/**
	 * @param id
	 * @param feed
	 * @param timestamp
	 * @param data
	 */
	public FeedEntry(long id, Feed feed, Date timestamp, JSONObject data) {
		this.id = id;
		this.feed = feed;
		this.timestamp = timestamp;
		this.data = data;
	}

	public long getId() {
		return id;
	}

	public Feed getFeed() {
		return feed;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public JSONObject getData() {
		return data;
	}
	
	public JSONObject toJSON() {
		try {
			JSONObject jEntry = new JSONObject();
			jEntry.put("feed", feed.getName());
			jEntry.put("timestamp", timestamp.getTime());
			jEntry.put("data", data);
			return jEntry;
		} catch (JSONException e) {
			Log.e(TAG, "Could not generate the JSON object for the feed entry");
			return null;
		}
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}
	
	
}
