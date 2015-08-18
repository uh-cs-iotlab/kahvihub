/*
 * fi.helsinki.cs.iot.hub.database.IotHubDatabase
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
import java.util.List;

import org.json.JSONObject;

import fi.helsinki.cs.iot.hub.model.enabler.Enabler;
import fi.helsinki.cs.iot.hub.model.enabler.Feature;
import fi.helsinki.cs.iot.hub.model.enabler.PluginInfo;
import fi.helsinki.cs.iot.hub.model.feed.AtomicFeed;
import fi.helsinki.cs.iot.hub.model.feed.ComposedFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeed;
import fi.helsinki.cs.iot.hub.model.feed.ExecutableFeedDescription;
import fi.helsinki.cs.iot.hub.model.feed.Feed;
import fi.helsinki.cs.iot.hub.model.feed.FeedEntry;
import fi.helsinki.cs.iot.hub.model.feed.FieldDescription;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public interface IotHubDatabase {
	
	public void executeUpdate(String request) throws IotHubDatabaseException;
	public void enableForeignKeyConstraints() throws IotHubDatabaseException;
	public boolean isOpen();
	public void close() throws IotHubDatabaseException;
	public void open() throws IotHubDatabaseException;
	
	/* Plugin functions */
	public List<PluginInfo> getPlugins();
	public List<PluginInfo> getNativePlugins();
	public List<PluginInfo> getJavascriptPlugins();
	public PluginInfo getPluginInfo(long id);
	public PluginInfo addNativePlugin(String serviceName, String packageName, File file);
	public PluginInfo addJavascriptPlugin(String serviceName, String packageName, File file);
	public PluginInfo deletePlugin(long id);
	
	/* Enabler functions */
	public Enabler addEnabler(String name, String metadata, PluginInfo plugin, String pluginInfoConfig);
	public List<Enabler> getEnablers();
	public Enabler getEnabler(long id);
	public Enabler getEnabler(String name);
	public Enabler updateEnabler(Enabler enabler, String name, String metadata, String pluginInfoConfig);
	public Enabler deleteEnabler(Enabler enabler);
	
	/* Features functions */
	public Feature addFeature(Enabler enabler, String name, String type);
	public Feature updateFeature(Feature feature, String name, String type, boolean isFeed);
	public Feature getFeature(long id);
	public List<Feature> deleteFeaturesOfEnabler(Enabler enabler);
	
	/* Feed functions */
	public AtomicFeed addAtomicFeed(String name, String metadata,
			List<String> keywords, Feature feature);
	public List<Feed> getFeeds();
	public Feed getFeed(String name);
	public ComposedFeed addComposedFeed(String name, String metadata, boolean storage,
			boolean readable, boolean writable, List<String> keywords,
			List<FieldDescription> fields);
	public Feed deleteFeed(String name);
	public List<FeedEntry> getFeedEntries(Feed feed);
	public FeedEntry addFeedEntry(Feed feed, JSONObject data);
	public FeedEntry deleteFeedEntry(FeedEntry entry);
	public ExecutableFeed addExecutableFeed(String name, String metadata,
			boolean readable, boolean writable, List<String> keywords,
			ExecutableFeedDescription executableFeedDescription);
	
	/* Service function */
	public List<ServiceInfo> getServiceInfos();
	public ServiceInfo getServiceInfo(long id);
	public ServiceInfo addServiceInfo(String name, File file);
	public ServiceInfo deleteServiceInfo(String name);
	
	public List<Service> getServices();
	public Service getService(long id);
	public Service getService(String name);
	public Service addService(ServiceInfo serviceInfo, String name, String metadata, String config, boolean bootAtStartup);
	public Service deleteService(String name);
	public Service updateService(Service service, String name, String metadata, String config, boolean bootAtStartup);
	
}
