/*
 * fi.helsinki.cs.iot.hub.api.FeedPostRequestHandler
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
package fi.helsinki.cs.iot.hub.api.handlers.feeds;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.handlers.basic.IotHubApiRequestHandler;
import fi.helsinki.cs.iot.hub.api.request.IotHubRequest;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.feed.AtomicFeed;
import fi.helsinki.cs.iot.hub.model.feed.ComposedFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeedDescription;
import fi.helsinki.cs.iot.hub.model.feed.Feed;
import fi.helsinki.cs.iot.hub.model.feed.FeedEntry;
import fi.helsinki.cs.iot.hub.model.feed.FieldDescription;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class FeedPostRequestHandler extends IotHubApiRequestHandler {

	private static final String WRONG_URI = "Wrong URI";
	private static final String ERROR = "Error";

	private static final String TAG = "FeedPostRequestHandler";
	private List<Method> methods;
	private Path libdir;
	/**
	 * 
	 */
	public FeedPostRequestHandler(Path libdir) {
		this.methods = new ArrayList<>();
		this.methods.add(Method.POST);	
		this.libdir = libdir;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	private List<String> getKeywords(JSONArray jarrKeywords) {
		List<String> keywords = null;
		if (jarrKeywords != null) {
			keywords = new ArrayList<>();
			for (int i = 0; i < jarrKeywords.length(); i++) {
				try {
					keywords.add(jarrKeywords.getString(i));
				} catch (JSONException e) {
					Log.e(TAG, "The keyword is not a string");
					return null;
				}
			}
		}
		return keywords;
	}

	private List<FieldDescription> getFields(JSONArray jarrFields) {
		List<FieldDescription> fields = null;
		if (jarrFields != null) {
			fields = new ArrayList<>();
			for (int i = 0; i < jarrFields.length(); i++) {
				try {
					JSONObject jField = jarrFields.getJSONObject(i);
					String name = jField.getString("name");
					String type = jField.getString("type");
					String metadata = jField.optString("metadata");
					boolean optional = jField.getBoolean("optional");
					List<String> keywords = getKeywords(jField.optJSONArray("keywords"));
					FieldDescription fd = new FieldDescription(name, type, metadata, optional, keywords);
					fields.add(fd);
				} catch (JSONException e) {
					Log.e(TAG, "Could not build the list of fields");
				}
			}
		}
		return fields;
	}

	private AtomicFeed handleAtomicFeedDescriptionPost(JSONObject jFeedDescription) {
		//TODO
		return null;
	}

	private ComposedFeed handleComposedFeedDescriptionPost(JSONObject jFeedDescription) {
		ComposedFeed feed = null;
		try {
			String name = jFeedDescription.getString("name");
			String metadata = jFeedDescription.getString("metadata");
			boolean storage = jFeedDescription.getBoolean("storage");
			boolean readable = jFeedDescription.getBoolean("readable");
			boolean writable = jFeedDescription.getBoolean("writable");
			List<String> keywords = getKeywords(jFeedDescription.optJSONArray("keywords"));
			List<FieldDescription> fields = getFields(jFeedDescription.getJSONArray(ComposedFeed.KEY_COMPOSED_FEED));
			if (fields == null || fields.size() == 0) {
				Log.e(TAG, "A composed feed must have at least one field");
				return null;
			}
			feed = IotHubDataAccess.getInstance().addComposedFeed(name, metadata, 
					storage, readable, writable, keywords, fields);
		} catch (JSONException e) {
			Log.e(TAG, "Could not generate the composed feed");
		}
		return feed;
	}

	private ExecutableFeedDescription getExecutableFeedDescription(JSONObject jFeedDescription) {
		return new ExecutableFeedDescription(jFeedDescription);
	}

	private ExecutableFeed handleExecutableFeedDescriptionPost(JSONObject jFeedDescription) {
		try {
			String name = jFeedDescription.getString("name");
			String metadata = jFeedDescription.getString("metadata");
			boolean readable = jFeedDescription.getBoolean("readable");
			boolean writable = jFeedDescription.getBoolean("writable");
			List<String> keywords = getKeywords(jFeedDescription.optJSONArray("keywords"));
			ExecutableFeedDescription executableFeedDescription = 
					getExecutableFeedDescription(jFeedDescription.getJSONObject(ExecutableFeed.KEY_EXECUTABLE_FEED));
			ExecutableFeed feed = IotHubDataAccess.getInstance().addExecutableFeed(name, metadata, 
					readable, writable, keywords, executableFeedDescription);
			Log.e(TAG, "Executable script descr: " + feed.getDescription());
			return feed;
		} catch (JSONException e) {
			Log.e(TAG, "Could not generate the executable feed");
			return null;
		}
	}

	private Feed handleFeedDescriptionPost(JSONObject jFeedDescription) {
		if (jFeedDescription.has(AtomicFeed.KEY_ATOMIC_FEED)) {
			return handleAtomicFeedDescriptionPost(jFeedDescription);
		} 
		else if (jFeedDescription.has(ComposedFeed.KEY_COMPOSED_FEED)) {
			return handleComposedFeedDescriptionPost(jFeedDescription);
		}
		else if (jFeedDescription.has(ExecutableFeed.KEY_EXECUTABLE_FEED)) {
			return handleExecutableFeedDescriptionPost(jFeedDescription);
		}
		else {
			Log.e(TAG, "The feed type is unknown");
			return null;
		}
	}

	private boolean checkFeedData(Feed feed, String data) {
		//TODO Make the javascript library check if the data is correct and corresponds to the feed
		//This means that all non optional fields should be present, that all field name are present in
		//the feed declaration and their types matches
		return true;
	}

	private boolean handleFeedUpdateData(Feed feed, String data) {
		switch (feed.getFeedType()) {
		case ATOMIC:
			AtomicFeed atomicFeed = (AtomicFeed)feed;
			if (atomicFeed.getFeature().isWritable()) {
				return atomicFeed.postValue(data);
			}
			return false;
		case COMPOSED:
			ComposedFeed composedFeed = (ComposedFeed)feed;
			return composedFeed.isWritable();
		case EXECUTABLE:
			// Currently not used for executable feeds, only composed feed uses this
			ExecutableFeed executableFeed = (ExecutableFeed)feed;
			return executableFeed.executeScript(libdir, data) != null;
		default:
			return false;
		}
	}

	private boolean handleFeedStoreData(Feed feed, String data) {
		switch (feed.getFeedType()) {
		case ATOMIC:
			return true;
		case COMPOSED:
			ComposedFeed composedFeed = (ComposedFeed)feed;
			if (!composedFeed.hasStorage()) {
				Log.e(TAG, "Cannot store data to a feed with no storage");
				return false;
			}
			try {
				JSONObject jdata = new JSONObject(data);
				FeedEntry entry = IotHubDataAccess.getInstance().addFeedEntry(feed, jdata);
				return (entry != null);
			} catch (JSONException e) {
				Log.e(TAG, "The data is not a JSON Object");
				return false;
			}
		default:
			return false;
		}
	}

	private boolean doUpdate(IotHubRequest uri) {
		return uri.getOptions().containsKey("update");
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubRequest uri) {
		Log.d(TAG, "Received a request for feeds, uri: "+ uri.getFullUri());
		switch (uri.getIdentifiers().size()) {
		case 0:
			// Case where we want to add a new feed to the list
			// we will return the newly created feed
			if (uri.getBodyData() == null) {
				return getResponseKo(ERROR, "There is no data associated with this uri");
			}
			JSONObject jFeedDescription = null;
			try {
				jFeedDescription = new JSONObject(uri.getBodyData());
			} catch (JSONException e) {
				Log.e(TAG, "The feed description is not a JSON object");
				return getResponseKo(ERROR, "The feed description is not a JSON object");
			}
			Feed feed = handleFeedDescriptionPost(jFeedDescription);
			if (feed == null) {
				return getResponseKo(ERROR, "The feed could not be created");
			}
			else {
				String jsonDescription = feed.getDescription();
				if (jsonDescription != null) {
					Log.d(TAG, "Returns the description of the created field");
					return getResponseOk(jsonDescription.toString());
				}
				else {
					return getResponseKo(ERROR, "The feed description could not be retrieved");
				}
			}
		case 1:
			// Case where we want to write/send data to feed. By default, we do not care about sending
			// new information to the sensor (we would rather use a PUT request for that). However, if the
			// flag "update" is set we will also write the data to the feed
			String data = uri.getBodyData();
			feed = IotHubDataAccess.getInstance().getFeed(uri.getIdentifiers().get(0));
			if (feed == null) {
				Log.e(TAG, "No feed could be found with that name");
				return getResponseKo(ERROR, "No feed could be found with that name");
			}
			else {
				switch (feed.getFeedType()) {
				case COMPOSED:
					return handlePostComposedFeed((ComposedFeed)feed, data, doUpdate(uri));
				case ATOMIC:
					return handlePostAtomicFeed((AtomicFeed)feed, data);
				case EXECUTABLE:
					return handlePostExecutableFeed((ExecutableFeed)feed, data);
				}
			}
		default:
			return getResponseKo(WRONG_URI, "The uri is inappropriate");
		}
	}

	private Response handlePostExecutableFeed(ExecutableFeed feed, String data) {
		if (data != null) {
			String result = feed.executeScript(libdir, data);
			if (result != null) {
				return getResponseOk(result);
			} else {
				return getResponseKo(ERROR, "The script could not be executed correctly");
			}
		}
		else {
			Log.e(TAG, "The script was not successfully evaluated by " + feed.getName());
			return getResponseKo(ERROR, "The script was not successfully evaluated by " + feed.getName());
		}
	}

	private Response handlePostAtomicFeed(AtomicFeed feed, String data) {
		if (data == null) {
			Log.e(TAG, "Very unlikely the data should be null");
		}
		if (feed.getFeature().isAvailable() && feed.getFeature().isWritable()) {
			boolean worked = feed.postValue(data);
			if (worked) {
				return getResponseOk(data);
			}
			else {
				return getResponseKo(ERROR, "The data could not be posted to feed " + feed.getName());
			}
		}
		else {
			return getResponseKo(ERROR, "The feature is either not available nor writable " + feed.getName());
		}
	}

	private Response handlePostComposedFeed(ComposedFeed feed, String data, boolean doUpdate) {
		if (!checkFeedData(feed, data)) {
			Log.e(TAG, "The data posted does not match the type of feed " + feed.getName());
			return getResponseKo(ERROR, "The data posted does not match the type of feed " + feed.getName());
		}
		else if (!handleFeedStoreData(feed, data)) {
			Log.e(TAG, "The data could not be stored for feed " + feed.getName());
			return getResponseKo(ERROR, "The data could not be stored for feed " + feed.getName());
		}
		else if (doUpdate && !handleFeedUpdateData(feed, data)) {
			Log.e(TAG, "The data could not be updated for feed " + feed.getName());
			return getResponseKo(ERROR, "The data could not be updated for feed " + feed.getName());
		}
		else {
			Log.i(TAG, "The data was stored" + 
					(doUpdate ? " and updated" : "") + " for feed " + feed.getName());
			return getResponseOk("");
		}
	}

}
