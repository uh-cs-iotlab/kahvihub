/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub.database.sqlite4Java;

import java.io.File;
import java.util.List;

import org.json.JSONObject;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
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
 * @author mineraud
 *
 */
public class IotHubDatabaseSqlite4JavaImpl implements IotHubDatabase {

	private static final String TAG = "IotHubDatabaseSqlite4JavaImpl";
	private boolean isOpen;
	private String dbName;
	private SQLiteConnection connection;
	
	public IotHubDatabaseSqlite4JavaImpl(String dbName) {
		this.isOpen = false;
		this.dbName = dbName;
		this.connection = null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return isOpen;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#close()
	 */
	@Override
	public void close() throws IotHubDatabaseException {
		connection.dispose();
		isOpen = false;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#open()
	 */
	@Override
	public void open() throws IotHubDatabaseException {
		connection = new SQLiteConnection(new File(dbName));
		try {
			connection.open(true);
			isOpen = true;
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new IotHubDatabaseException(e.getMessage());
		}
	}

	@Override
	public void enableForeignKeyConstraints() throws IotHubDatabaseException {
		executeUpdate("PRAGMA foreign_keys=ON;");
	}

	protected boolean isNew() {
		if (isOpen && connection != null) {
			String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + IotHubDataHandler.TABLE_FEED + "';";
			SQLiteStatement statement;
			try {
				statement = connection.prepare(sql);
				boolean isNew = !statement.step();
				statement.dispose();
				return isNew;
			} catch (SQLiteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return true;
			}
		}
		return false;
	}

	protected boolean isUpgraded() {
		return false;
	}
	
	private void checkOpenness() throws IotHubDatabaseException {
		if (!isOpen) {
			throw new IotHubDatabaseException("The database is not open");
		}
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#executeUpdate(java.lang.String)
	 */
	@Override
	public void executeUpdate(String request) throws IotHubDatabaseException {
		checkOpenness();
		try {
			connection.exec("BEGIN");
			SQLiteStatement statement = connection.prepare(request);
			statement.step();
			statement.dispose();
			connection.exec("COMMIT");
		} catch (SQLiteException e) {
			throw new IotHubDatabaseException(e.getMessage());
		}
	}
	
	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getPlugins()
	 */
	@Override
	public List<PluginInfo> getPlugins() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getNativePlugins()
	 */
	@Override
	public List<PluginInfo> getNativePlugins() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getJavascriptPlugins()
	 */
	@Override
	public List<PluginInfo> getJavascriptPlugins() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getPluginInfo(long)
	 */
	@Override
	public PluginInfo getPluginInfo(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addNativePlugin(java.lang.String, java.lang.String, java.io.File)
	 */
	@Override
	public PluginInfo addNativePlugin(String serviceName, String packageName, File file) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addJavascriptPlugin(java.lang.String, java.lang.String, java.io.File)
	 */
	@Override
	public PluginInfo addJavascriptPlugin(String serviceName, String packageName, File file) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deletePlugin(long)
	 */
	@Override
	public PluginInfo deletePlugin(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addEnabler(java.lang.String, java.lang.String, fi.helsinki.cs.iot.hub.model.enabler.PluginInfo, java.lang.String)
	 */
	@Override
	public Enabler addEnabler(String name, String metadata, PluginInfo plugin, String pluginInfoConfig) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getEnablers()
	 */
	@Override
	public List<Enabler> getEnablers() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getEnabler(long)
	 */
	@Override
	public Enabler getEnabler(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getEnabler(java.lang.String)
	 */
	@Override
	public Enabler getEnabler(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#updateEnabler(fi.helsinki.cs.iot.hub.model.enabler.Enabler, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Enabler updateEnabler(Enabler enabler, String name, String metadata, String pluginInfoConfig) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteEnabler(fi.helsinki.cs.iot.hub.model.enabler.Enabler)
	 */
	@Override
	public Enabler deleteEnabler(Enabler enabler) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addFeature(fi.helsinki.cs.iot.hub.model.enabler.Enabler, java.lang.String, java.lang.String)
	 */
	@Override
	public Feature addFeature(Enabler enabler, String name, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#updateFeature(fi.helsinki.cs.iot.hub.model.enabler.Feature, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public Feature updateFeature(Feature feature, String name, String type, boolean isFeed) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getFeature(long)
	 */
	@Override
	public Feature getFeature(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteFeaturesOfEnabler(fi.helsinki.cs.iot.hub.model.enabler.Enabler)
	 */
	@Override
	public List<Feature> deleteFeaturesOfEnabler(Enabler enabler) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addAtomicFeed(java.lang.String, java.lang.String, java.util.List, fi.helsinki.cs.iot.hub.model.enabler.Feature)
	 */
	@Override
	public AtomicFeed addAtomicFeed(String name, String metadata, List<String> keywords, Feature feature) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getFeeds()
	 */
	@Override
	public List<Feed> getFeeds() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getFeed(java.lang.String)
	 */
	@Override
	public Feed getFeed(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addComposedFeed(java.lang.String, java.lang.String, boolean, boolean, boolean, java.util.List, java.util.List)
	 */
	@Override
	public ComposedFeed addComposedFeed(String name, String metadata, boolean storage, boolean readable,
			boolean writable, List<String> keywords, List<FieldDescription> fields) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteFeed(java.lang.String)
	 */
	@Override
	public Feed deleteFeed(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getFeedEntries(fi.helsinki.cs.iot.hub.model.feed.Feed)
	 */
	@Override
	public List<FeedEntry> getFeedEntries(Feed feed) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addFeedEntry(fi.helsinki.cs.iot.hub.model.feed.Feed, org.json.JSONObject)
	 */
	@Override
	public FeedEntry addFeedEntry(Feed feed, JSONObject data) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteFeedEntry(fi.helsinki.cs.iot.hub.model.feed.FeedEntry)
	 */
	@Override
	public FeedEntry deleteFeedEntry(FeedEntry entry) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addExecutableFeed(java.lang.String, java.lang.String, boolean, boolean, java.util.List, fi.helsinki.cs.iot.hub.model.feed.ExecutableFeedDescription)
	 */
	@Override
	public ExecutableFeed addExecutableFeed(String name, String metadata, boolean readable, boolean writable,
			List<String> keywords, ExecutableFeedDescription executableFeedDescription) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getServiceInfos()
	 */
	@Override
	public List<ServiceInfo> getServiceInfos() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getServiceInfo(long)
	 */
	@Override
	public ServiceInfo getServiceInfo(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addServiceInfo(java.lang.String, java.io.File)
	 */
	@Override
	public ServiceInfo addServiceInfo(String name, File file) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteServiceInfo(long)
	 */
	@Override
	public ServiceInfo deleteServiceInfo(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getServices()
	 */
	@Override
	public List<Service> getServices() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getService(long)
	 */
	@Override
	public Service getService(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#getService(java.lang.String)
	 */
	@Override
	public Service getService(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#addService(fi.helsinki.cs.iot.hub.model.service.ServiceInfo, java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public Service addService(ServiceInfo serviceInfo, String name, String metadata, String config,
			boolean bootAtStartup) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#deleteService(fi.helsinki.cs.iot.hub.model.service.Service)
	 */
	@Override
	public Service deleteService(Service service) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see fi.helsinki.cs.iot.hub.database.IotHubDatabase#updateService(fi.helsinki.cs.iot.hub.model.service.Service, java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public Service updateService(Service service, String name, String metadata, String config, boolean bootAtStartup) {
		// TODO Auto-generated method stub
		return null;
	}

}
