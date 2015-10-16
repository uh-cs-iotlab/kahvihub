package fi.helsinki.cs.iot.kahvihub.database;

import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import fi.helsinki.cs.iot.hub.database.IotHubDatabase;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;
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
 * Created by mineraud on 16.10.2015.
 */
public class KahvihubDatabase implements IotHubDatabase {

    private static final String TAG = "KahvihubDatabase";
    private KahvihubDbHelper helper;
    private SQLiteDatabase database;
    private boolean isOpen;

    public KahvihubDatabase(KahvihubDbHelper helper) {
        this.database = null;
        this.isOpen = false;
        this.helper = helper;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void close() throws IotHubDatabaseException {
        if (database != null) {
            database.close();
            database = null;
        }
        isOpen = true;
    }

    @Override
    public void open() throws IotHubDatabaseException {
        if (database == null) {
            database = helper.getWritableDatabase();
        }
        isOpen = true;
    }

    private void checkOpenness() throws IotHubDatabaseException {
        if (!isOpen) {
            throw new IotHubDatabaseException("The database is not open");
        }
    }

    protected boolean isNew() {
        //TODO check where I use this function
        if (isOpen && database != null) {
            return true;
        }
        return false;
    }

    protected boolean isUpgraded() {
        return false;
    }

    @Override
    public void executeUpdate(String request) throws IotHubDatabaseException {
        checkOpenness();
        database.beginTransaction();
        database.execSQL(request);
        database.endTransaction();
    }

    @Override
    public void enableForeignKeyConstraints() throws IotHubDatabaseException {
        checkOpenness();
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public List<PluginInfo> getPlugins() {
        return null;
    }

    @Override
    public List<PluginInfo> getNativePlugins() {
        return null;
    }

    @Override
    public List<PluginInfo> getJavascriptPlugins() {
        return null;
    }

    @Override
    public PluginInfo getPluginInfo(long id) {
        return null;
    }

    @Override
    public PluginInfo addNativePlugin(String serviceName, String packageName, File file) {
        return null;
    }

    @Override
    public PluginInfo addJavascriptPlugin(String serviceName, String packageName, File file) {
        return null;
    }

    @Override
    public PluginInfo deletePlugin(long id) {
        return null;
    }

    @Override
    public Enabler addEnabler(String name, String metadata, PluginInfo plugin, String pluginInfoConfig) {
        return null;
    }

    @Override
    public List<Enabler> getEnablers() {
        return null;
    }

    @Override
    public Enabler getEnabler(long id) {
        return null;
    }

    @Override
    public Enabler getEnabler(String name) {
        return null;
    }

    @Override
    public Enabler updateEnabler(Enabler enabler, String name, String metadata, String pluginInfoConfig) {
        return null;
    }

    @Override
    public Enabler deleteEnabler(Enabler enabler) {
        return null;
    }

    @Override
    public Feature addFeature(Enabler enabler, String name, String type) {
        return null;
    }

    @Override
    public Feature updateFeature(Feature feature, String name, String type, boolean isFeed) {
        return null;
    }

    @Override
    public Feature getFeature(long id) {
        return null;
    }

    @Override
    public List<Feature> deleteFeaturesOfEnabler(Enabler enabler) {
        return null;
    }

    @Override
    public AtomicFeed addAtomicFeed(String name, String metadata, List<String> keywords, Feature feature) {
        return null;
    }

    @Override
    public List<Feed> getFeeds() {
        return null;
    }

    @Override
    public Feed getFeed(String name) {
        return null;
    }

    @Override
    public ComposedFeed addComposedFeed(String name, String metadata, boolean storage, boolean readable, boolean writable, List<String> keywords, List<FieldDescription> fields) {
        return null;
    }

    @Override
    public Feed deleteFeed(String name) {
        return null;
    }

    @Override
    public List<FeedEntry> getFeedEntries(Feed feed) {
        return null;
    }

    @Override
    public FeedEntry addFeedEntry(Feed feed, JSONObject data) {
        return null;
    }

    @Override
    public FeedEntry deleteFeedEntry(FeedEntry entry) {
        return null;
    }

    @Override
    public ExecutableFeed addExecutableFeed(String name, String metadata, boolean readable, boolean writable, List<String> keywords, ExecutableFeedDescription executableFeedDescription) {
        return null;
    }

    @Override
    public List<ServiceInfo> getServiceInfos() {
        return null;
    }

    @Override
    public ServiceInfo getServiceInfo(long id) {
        return null;
    }

    @Override
    public ServiceInfo addServiceInfo(String name, File file) {
        return null;
    }

    @Override
    public ServiceInfo deleteServiceInfo(long id) {
        return null;
    }

    @Override
    public List<Service> getServices() {
        return null;
    }

    @Override
    public Service getService(long id) {
        return null;
    }

    @Override
    public Service getService(String name) {
        return null;
    }

    @Override
    public Service addService(ServiceInfo serviceInfo, String name, String metadata, String config, boolean bootAtStartup) {
        return null;
    }

    @Override
    public Service deleteService(Service service) {
        return null;
    }

    @Override
    public Service updateService(Service service, String name, String metadata, String config, boolean bootAtStartup) {
        return null;
    }
}
