/*
 * fi.helsinki.cs.iot.hub.api.FeedGetRequestHandler
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
package fi.helsinki.cs.iot.hub.api;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.api.uri.IotHubUri;
import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.model.feed.ComposedFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeed;
import fi.helsinki.cs.iot.hub.model.feed.Feed;
import fi.helsinki.cs.iot.hub.model.feed.FeedEntry;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Method;
import fi.helsinki.cs.iot.hub.webserver.NanoHTTPD.Response;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class FeedGetRequestHandler extends IoTHubApiRequestHandler {

	private static final String WRONG_URI = "Wrong URI";
	private static final String ERROR = "Error";

	private static final String TAG = "FeedGetRequestHandler";
	private List<Method> methods;

	public FeedGetRequestHandler() {
		this.methods = new ArrayList<>();
		this.methods.add(Method.GET);	
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#getSupportedMethods()
	 */
	@Override
	public List<Method> getSupportedMethods() {
		return methods;
	}

	private JSONArray getFeedList() {
		List<Feed> feedList = IotHubDataAccess.getInstance().getFeeds();
		if (feedList == null) {
			Log.e(TAG, "The feed list is null, so it cannot be converted to JSON");
			return null;
		}
		else {
			JSONArray jarray = new JSONArray();
			for (Feed feed : feedList) {
				JSONObject jsonDescription = null;
				try {
					jsonDescription = new JSONObject(feed.getDescription());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (jsonDescription != null) {
					jarray.put(jsonDescription);
				}
				else {
					Log.e(TAG, "The feed description of feed " + feed.getName() + " is corrupted");
				}
			}
			return jarray;
		}
	}
	
	private JSONArray getComposedFeedEntries(ComposedFeed feed) {
		List<FeedEntry> entries = IotHubDataAccess.getInstance().getFeedEntries(feed);
		if (entries == null || !feed.hasStorage()) {
			Log.e(TAG, "Cannot get feed entries for feed " + feed.getName());
			return null;
		}
		JSONArray jEntries = new JSONArray();
		for (FeedEntry entry : entries) {
			jEntries.put(entry.toJSON());
		}
		return jEntries;
	}
	
	private Response handleAtomicFeed(Feed feed) {
		Log.d(TAG, "Now trying to handle a basic call for the atomic feed " + feed.getName());
		String response = feed.getValue();
		if (response != null) {
			Log.d(TAG, "Returns the feed's data");
			return getResponseOk(response);
		}
		else {
			Log.e(TAG, "The feed's data could not be retrieved");
			return getResponseKo(ERROR, "The feed's data could not be retrieved");
		}
	}
	
	private Response handleComposedFeed(Feed feed, boolean doStorage) {
		ComposedFeed composedFeed = (ComposedFeed)feed;
		String response = null;
		if (doStorage) {
			JSONArray jEntries = getComposedFeedEntries(composedFeed);
			if (jEntries != null) {
				response = jEntries.toString();
			}
		}
		else {
			response = composedFeed.getValue();
		}
		if (response != null) {
			Log.d(TAG, "Returns the feed's data");
			return getResponseOk(response);
		}
		else {
			Log.e(TAG, "The feed's data could not be retrieved");
			return getResponseKo(ERROR, "The feed's data could not be retrieved");
		}
	}
	
	private Response handleExecutableFeed(Feed feed) {
		ExecutableFeed executableFeed = (ExecutableFeed)feed;
		if (executableFeed.isReadable()) {
			Log.e(TAG, "The feed " + feed.getName() + " is not readable");
			return getResponseKo(ERROR, "The feed " + feed.getName() + " is not readable");
		}
		else {
			String jValue = executableFeed.getValue();
			if (jValue != null) {
				Log.d(TAG, "Returns the feed's value");
				return getResponseOk(jValue.toString());
			}
			else {
				Log.e(TAG, "The feed " + feed.getName() + " has not value");
				return getResponseKo(ERROR, "The feed " + feed.getName() + " has not value");
			}
		}
	}

	/* (non-Javadoc)
	 * This methods handle the get request for feeds
	 * At the moment, I will just implement the base /feeds/ and /feeds/feedname 
	 * and an attribute ?meta (default for the base) can be added to get the specs
	 * @see fi.helsinki.cs.iot.hub.api.RequestHandler#handleRequest(fi.helsinki.cs.iot.hub.api.uri.IotHubUri)
	 */
	@Override
	public Response handleRequest(IotHubUri uri) {
		Log.d(TAG, "Received a request for feeds, uri: "+ uri.getFullUri());
		switch (uri.getIdentifiers().size()) {
		case 0:
			// Case where we just want to list the meta of all the feeds
			// TODO make the possibility of filtering the feeds
			JSONArray jFeedList = getFeedList();
			if (jFeedList != null) {
				Log.d(TAG, "Returns a list of feeds");
				return getResponseOk(jFeedList.toString());
			}
			else {
				return getResponseKo(ERROR, "The list of feeds could not be retrieved");
			}
			// Case of a particular feed (we can return)
		case 1:
			String feedname = uri.getIdentifiers().get(0);
			Feed feed = IotHubDataAccess.getInstance().getFeed(feedname);
			boolean doMeta = uri.getOptions().containsKey("meta");
			if (feed == null) {
				return super.getResponseKo(ERROR, "The feed " + feedname + " could not be retrieved");
			}
			else if (doMeta) {
				String jResponse = feed.getDescription();
				if (jResponse != null) {
					Log.d(TAG, "Returns the feed's meta");
					return getResponseOk(jResponse.toString());
				}
				else {
					Log.e(TAG, "The feed's meta could not be retrieved");
					return getResponseKo(ERROR, "The feed's meta could not be retrieved");
				}
			}
			else {
				switch(feed.getFeedType()) {
				case COMPOSED:
					boolean doStorage = uri.getOptions().containsKey("storage");
					return handleComposedFeed(feed, doStorage);
				case EXECUTABLE:
					return handleExecutableFeed(feed);
				case ATOMIC:
					return handleAtomicFeed(feed);
				default:
					return super.getResponseKo(ERROR, "The feed type is not supported yet");
				}
			}
		default:
			return super.getResponseKo(WRONG_URI, "The uri is inappropriate");
		}
	}

}
