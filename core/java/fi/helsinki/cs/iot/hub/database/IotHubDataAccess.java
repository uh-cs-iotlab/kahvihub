/*
 * fi.helsinki.cs.iot.hub.database.IotHubDataAccess
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
package fi.helsinki.cs.iot.hub.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.feed.ComposedFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeedDescription;
import fi.helsinki.cs.iot.hub.model.feed.Feed;
import fi.helsinki.cs.iot.hub.model.feed.FeedEntry;
import fi.helsinki.cs.iot.hub.model.feed.FieldDescription;
import fi.helsinki.cs.iot.hub.model.listener.EnablerChangeListener;
import fi.helsinki.cs.iot.hub.model.listener.FeedDataChangeListener;
import fi.helsinki.cs.iot.hub.model.listener.FeedListChangeListener;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public class IotHubDataAccess {
	
	private static final String TAG = "IotHubDataAccess";
	private IotHubDatabase database;
	private IotHubDataHandler handler;
	private List<FeedListChangeListener> feedListChangeListeners;
	private List<FeedDataChangeListener> feedDataChangeListeners;
    private List<EnablerChangeListener> enablerChangeListeners;
    
    private static IotHubDataAccess instance;
    
    private IotHubDataAccess(IotHubDataHandler handler) {
    	this.handler = handler;
    	feedListChangeListeners = new ArrayList<>();
    	feedDataChangeListeners = new ArrayList<>();
        enablerChangeListeners = new ArrayList<>();
    }
    
    /**
     * Get the instance of the singleton
     * @return
     */
    public static void setInstance(IotHubDataHandler handler) {
    	if (instance == null && handler != null) {
    		instance = new IotHubDataAccess(handler);
    	}
    	//TODO do something for the other cases
    }
    
    /**
     * Get the instance of the singleton
     * @return
     */
    public static IotHubDataAccess getInstance() {
        return instance;
    }
    
    public void open() throws IotHubDatabaseException{
        if (database == null || !database.isOpen()) {
            database = handler.openDatabase();
        }
    }

    public void close() throws IotHubDatabaseException {
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
        }
        handler.closeDatabase();
    }
    
    public boolean addFeedListChangeListener(FeedListChangeListener listener) {
        return feedListChangeListeners.add(listener);
    }

    public boolean removeFeedListChangeListener(FeedListChangeListener listener) {
        return feedListChangeListeners.remove(listener);
    }
    
    public boolean addFeedDataChangeListener(FeedDataChangeListener listener) {
        return feedDataChangeListeners.add(listener);
    }

    public boolean removeFeedDataChangeListener(FeedDataChangeListener listener) {
        return feedDataChangeListeners.remove(listener);
    }

    public boolean addEnablerChangeListener(EnablerChangeListener listener) {
        return enablerChangeListeners.add(listener);
    }

    public boolean removeEnablerChangeListener(EnablerChangeListener listener) {
        return enablerChangeListeners.remove(listener);
    }

    private void notifyAddedFeed(Feed feed) {
        for (FeedListChangeListener listener : feedListChangeListeners) {
            listener.feedAdded(feed);
        }
    }
    
    private void notifyRemovedFeed(Feed feed) {
        for (FeedListChangeListener listener : feedListChangeListeners) {
            listener.feedRemoved(feed);
        }
    }
    
    private void notifyAddedFeedEntry(FeedEntry entry) {
        for (FeedDataChangeListener listener : feedDataChangeListeners) {
            listener.feedEntryAdded(entry);
        }
    }
    
    private void notifyRemovedFeedEntry(FeedEntry entry) {
    	for (FeedDataChangeListener listener : feedDataChangeListeners) {
            listener.feedEntryRemoved(entry);
        }
    }
    
    public List<Feed> getFeeds() {
    	if (database != null && database.isOpen()) {
    		return database.getFeeds();
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
    }
    
    public ComposedFeed addComposedFeed(String name, String metadata, boolean storage, 
    		boolean readable, boolean writable, List<String> keywords, List<FieldDescription> fields) {
    	if (database != null && database.isOpen()) {
    		ComposedFeed feed = database.addComposedFeed(name, metadata, storage, readable, writable, keywords, fields);
    		if (feed != null) {
    			this.notifyAddedFeed(feed);
    		}
    		return feed;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
    }

	public Feed getFeed(String name) {
		if (database != null && database.isOpen()) {
    		return database.getFeed(name);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Feed deleteFeed(String name) {
		if (database != null && database.isOpen()) {
    		Feed feed = database.deleteFeed(name);
    		if (feed != null) {
    			this.notifyRemovedFeed(feed);
    		}
    		return feed;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public List<FeedEntry> getFeedEntries(Feed feed) {
		if (database != null && database.isOpen()) {
    		return database.getFeedEntries(feed);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public FeedEntry addFeedEntry(Feed feed, JSONObject data) {
		if (database != null && database.isOpen()) {
    		FeedEntry entry = database.addFeedEntry(feed, data);
    		if (entry != null) {
    			this.notifyAddedFeedEntry(entry);
    		}
    		return entry;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public FeedEntry deleteFeedEntry(FeedEntry entry) {
		if (database != null && database.isOpen()) {
			FeedEntry delEntry = database.deleteFeedEntry(entry);
			if (delEntry != null) {
    			this.notifyRemovedFeedEntry(delEntry);
    		}
    		return entry;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}

	public ExecutableFeed addExecutableFeed(String name, String metadata,
			boolean readable, boolean writable, List<String> keywords,
			ExecutableFeedDescription executableFeedDescription) {
		if (database != null && database.isOpen()) {
			ExecutableFeed feed = database.addExecutableFeed(name, metadata, readable, writable,
					keywords, executableFeedDescription);
			if (feed != null) {
    			this.notifyAddedFeed(feed);
    		}
    		return feed;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	//Parts for the plugins
	public List<PluginInfo> getPlugins() {
		if (database != null && database.isOpen()) {
			return database.getPlugins();
		}
		else {
			Log.e(TAG, "The database was neither set nor opened");
    		return null;
		}
	}
	
	public List<PluginInfo> getNativePlugins() {
		if (database != null && database.isOpen()) {
			return database.getNativePlugins();
		}
		else {
			Log.e(TAG, "The database was neither set nor opened");
    		return null;
		}
	}
	
	public List<PluginInfo> getJavascriptPlugins() {
		if (database != null && database.isOpen()) {
			return database.getJavascriptPlugins();
		}
		else {
			Log.e(TAG, "The database was neither set nor opened");
    		return null;
		}
	}
	
	public PluginInfo getPluginInfo(long id) {
		if (database != null && database.isOpen()) {
			PluginInfo pluginInfo = database.getPluginInfo(id);
    		return pluginInfo;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public PluginInfo addNativePlugin(String serviceName, String packageName, File file) {
		if (database != null && database.isOpen()) {
			PluginInfo pluginInfo = database.addNativePlugin(serviceName, packageName, file);
    		return pluginInfo;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public PluginInfo addJavascriptPlugin(String serviceName, String packageName, File file) {
		if (database != null && database.isOpen()) {
			PluginInfo pluginInfo = database.addJavascriptPlugin(serviceName, packageName, file);
    		return pluginInfo;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public List<Enabler> getEnablers() {
		if (database != null && database.isOpen()) {
			return database.getEnablers();
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Enabler getEnabler(long id) {
		if (database != null && database.isOpen()) {
			return database.getEnabler(id);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Enabler addEnabler(String name, String metadata, PluginInfo plugin, String pluginInfoConfig) {
		if (database != null && database.isOpen()) {
			return database.addEnabler(name, metadata, plugin, pluginInfoConfig);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Enabler updateEnabler(Enabler enabler, String pluginInfoConfig) {
		return updateEnabler(enabler, enabler.getName(), enabler.getMetadata(), pluginInfoConfig);
	}
	
	public Enabler updateEnabler(Enabler enabler, String name, String metadata, String pluginInfoConfig) {
		if (database != null && database.isOpen()) {
			return database.updateEnabler(enabler, name, metadata, pluginInfoConfig);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Enabler deleteEnabler(Enabler enabler) {
		if (database != null && database.isOpen()) {
			return database.deleteEnabler(enabler);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Feature addFeature(Enabler enabler, String featureName, String featureType) {
		if (database != null && database.isOpen()) {
			return database.addFeature(enabler, featureName, featureType);
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Feature updateFeature(Feature feature, boolean isFeed) {
		if (database != null && database.isOpen()) {
			if (feature.isAtomicFeed() != isFeed) {
				Log.d(TAG, "The feature will change its state");
				boolean feedUpdateWorked = false;
				String name = "atomicFeature" + feature.getId();
				if (isFeed) {
					//In this case I need to add an atomic feed to the database
					if (database.addAtomicFeed(name, null, null, feature) != null) {
						feedUpdateWorked = true;
					}
				}
				else if (database.deleteFeed(name) != null) {
						feedUpdateWorked = true;
				}
				if (feedUpdateWorked) { 
					return database.updateFeature(feature, feature.getName(), feature.getType(), isFeed);
				}
				else {
					return null;
				}
			}
			return null;
    	}
    	else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	/* Service function */
	public List<ServiceInfo> getServiceInfos() {
		if (database != null && database.isOpen()) {
			return database.getServiceInfos();
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public ServiceInfo getServiceInfo(long id) {
		if (database != null && database.isOpen()) {
			return database.getServiceInfo(id);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public ServiceInfo addServiceInfo(String name, File file) {
		if (database != null && database.isOpen()) {
			return database.addServiceInfo(name, file);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public ServiceInfo deleteServiceInfo(String name) {
		if (database != null && database.isOpen()) {
			return database.deleteServiceInfo(name);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public List<Service> getServices() {
		if (database != null && database.isOpen()) {
			return database.getServices();
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}

	public Service getService(long id) {
		if (database != null && database.isOpen()) {
			return database.getService(id);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Service getService(String name) {
		if (database != null && database.isOpen()) {
			return database.getService(name);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Service addService(ServiceInfo serviceInfo, 
			String name, String metadata, String config, boolean bootAtStartup) {
		if (database != null && database.isOpen()) {
			return database.addService(serviceInfo, name, metadata, config, bootAtStartup);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Service deleteService(String name) {
		if (database != null && database.isOpen()) {
			return database.deleteService(name);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
	
	public Service updateService(Service service, 
			String name, String metadata, String config, boolean bootAtStartup) {
		if (database != null && database.isOpen()) {
			return database.updateService(service, 
					name, metadata, config, bootAtStartup);
		}
		else {
    		Log.e(TAG, "The database was neither set nor opened");
    		return null;
    	}
	}
}

    