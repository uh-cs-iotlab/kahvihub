/*
 * fi.helsinki.cs.iot.kahvihub.IotHubDatabaseSqliteJDBCImpl
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
package fi.helsinki.cs.iot.kahvihub.database.sqliteJdbc;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

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
import fi.helsinki.cs.iot.hub.model.feed.Field;
import fi.helsinki.cs.iot.hub.model.feed.FieldDescription;
import fi.helsinki.cs.iot.hub.model.service.Service;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo;
import fi.helsinki.cs.iot.hub.model.service.ServiceInfo.Type;
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 */
public class IotHubDatabaseSqliteJDBCImpl implements IotHubDatabase {

	private static final String TAG = "IotHubDatabaseSqliteJDBCImpl";
	private Connection connection;
	private boolean isOpen;
	private String dbName;

	public IotHubDatabaseSqliteJDBCImpl(String dbName) {
		this.connection = null;
		this.isOpen = false;
		this.dbName = dbName;
	}

	@Override
	public void open() throws IotHubDatabaseException {
		if (connection == null) {
			try {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbName);
			} catch (Exception e) {
				throw new IotHubDatabaseException(e.getMessage());
			}
		}
		if (!isOpen){
			isOpen = true;
		}
	}

	@Override
	public void close() throws IotHubDatabaseException {
		if (isOpen){
			isOpen = false;
		}
		try {
			connection.close();
			connection = null;
		} catch (SQLException e) {
			throw new IotHubDatabaseException(e.getMessage());
		}
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	private void checkOpenness() throws IotHubDatabaseException {
		if (!isOpen) {
			throw new IotHubDatabaseException("The database is not open");
		}
	}

	@Override
	public void enableForeignKeyConstraints() throws IotHubDatabaseException {
		executeUpdate("PRAGMA foreign_keys=ON;");
	}

	protected boolean isNew() {
		if (isOpen && connection != null) {
			String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + IotHubDataHandler.TABLE_FEED + "';";
			Statement statement;
			try {
				statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(sql);
				boolean isNew = !rs.next();
				rs.close();
				statement.close();
				return isNew;
			} catch (SQLException e) {
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

	@Override
	public void executeUpdate(String request) throws IotHubDatabaseException {
		checkOpenness();
		try {
			connection.setAutoCommit(false);
			Statement statement = connection.createStatement();
			statement.executeUpdate(request);
			statement.close();
			connection.commit();
		} catch (SQLException e) {
			throw new IotHubDatabaseException(e.getMessage());
		}
	}

	@Override
	public AtomicFeed addAtomicFeed(String name, String metadata,
			List<String> keywords, Feature feature) {
		AtomicFeed feed = null;
		if (feature == null) {
			Log.e(TAG, "One cannot create a composed feed with no feature");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			//First things first, insert the feed's values to the feed table
			String sqlFeedInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FEED + "("
					+ IotHubDataHandler.KEY_FEED_NAME + "," 
					+ IotHubDataHandler.KEY_FEED_METADATA + ","
					+ IotHubDataHandler.KEY_FEED_TYPE + ","
					+ IotHubDataHandler.KEY_FEED_STORAGE + "," 
					+ IotHubDataHandler.KEY_FEED_READABLE + "," 
					+ IotHubDataHandler.KEY_FEED_WRITABLE + ") VALUES (?,?,?,?,?,?)";
			PreparedStatement psFeedInsert = connection.prepareStatement(sqlFeedInsert, Statement.RETURN_GENERATED_KEYS);
			psFeedInsert.setString(1, name);
			psFeedInsert.setString(2, metadata);
			psFeedInsert.setString(3, IotHubDataHandler.ATOMIC_FEED);
			psFeedInsert.setInt(4, 0);
			psFeedInsert.setInt(5, 0);
			psFeedInsert.setInt(6, 0);
			psFeedInsert.executeUpdate();
			ResultSet genKeysFeed = psFeedInsert.getGeneratedKeys();
			if (genKeysFeed.next()) {
				long insertIdFeed = genKeysFeed.getLong(1);
				//Now we add the keywords
				addFeedKeywords(insertIdFeed, keywords);
				//Now we add the fields
				addFeedFeatureRelation(insertIdFeed, feature.getId());
				//At point we should have everything set so it is time to retrieve the atomic feed from the database
				//Log.d(TAG, "Now i will try to collect the atomic feed that was just added to the db");
				feed = getAtomicFeed(insertIdFeed);
				if (feed == null) {
					Log.e(TAG, "The feed should not be null");
				}
				//Now I want to make some checks
				if (!compareAtomicFeeds(feed, name, metadata, keywords, feature)) {
					Log.e(TAG, "Retrieving feed " + name + " did not work");
					feed = null;
				}
			}
			else {
				Log.e(TAG, "The insert of feed " + name + " did not work");
			}
			genKeysFeed.close();
			psFeedInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			feed = null;
		}
		try {
			if (feed == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return feed;
	}

	private boolean compareAtomicFeeds(AtomicFeed feed, String name, String metadata, 
			List<String> keywords, Feature feature) {
		// TODO Auto-generated method stub
		return true;
	}

	private void addFeedFeatureRelation(long feedId, long featureId) throws SQLException {
		String sqlInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FEED_FEATURE_REL +
				"(" + IotHubDataHandler.KEY_FEED_FEATURE_REL_FEED_ID + 
				"," + IotHubDataHandler.KEY_FEED_FEATURE_REL_FEATURE_ID + ") values (?,?)";
		PreparedStatement psInsert = connection.prepareStatement(sqlInsert);
		psInsert.setLong(1, feedId);
		psInsert.setLong(2, featureId);
		psInsert.executeUpdate();
		psInsert.close();
	}

	private AtomicFeed getAtomicFeed(long id) {
		AtomicFeed feed = null;
		try {
			checkOpenness();
			String feedIdFeed = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_ID;
			String relFeedId = IotHubDataHandler.TABLE_FEED_FEATURE_REL + "." + IotHubDataHandler.KEY_FEED_FEATURE_REL_FEED_ID;
			String relFeaturedId = IotHubDataHandler.TABLE_FEED_FEATURE_REL + "." + IotHubDataHandler.KEY_FEED_FEATURE_REL_FEATURE_ID;
			String attr1 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_NAME;
			String attr2 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_METADATA;
			String attrType = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_TYPE;
			String sql = "SELECT " + relFeaturedId + ", " + attr1 + ", " + attr2 +
					" FROM " + IotHubDataHandler.TABLE_FEED + 
					" INNER JOIN " + IotHubDataHandler.TABLE_FEED_FEATURE_REL + " ON " +
					feedIdFeed + " = " + relFeedId + 
					" WHERE " + feedIdFeed + " = ?" +
					" AND " + attrType + " = '" + IotHubDataHandler.ATOMIC_FEED + "'";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long featureId = rs.getLong(1);
				Feature feature = getFeature(featureId);
				if (feature != null) {
					String feedName = rs.getString(2);
					String feedMetadata = rs.getString(3);
					List<String> keywords = getFeedKeywords(id);
					feed = new AtomicFeed(id, feedName, feedMetadata, keywords, feature);
				}
				else {
					Log.e(TAG, "The feature does not exist");
				}
			}
			else {
				Log.e(TAG, "No results for this request: " + ps.toString());
			}
			rs.close();
			ps.close();
			return feed;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<AtomicFeed> getAtomicFeeds () {
		List<AtomicFeed> atomicFeedList = new ArrayList<AtomicFeed>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.ATOMIC_FEED + "'";
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long feedId = rs.getLong(IotHubDataHandler.KEY_FEED_ID);
				atomicFeedList.add(getAtomicFeed(feedId));
			}
			rs.close();
			statement.close();
			return atomicFeedList;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private void addFeedFields(long id, List<FieldDescription> fields) throws SQLException {
		if (fields == null || fields.size() == 0) {
			Log.e(TAG, "One cannot create a composed feed with no fields");
			return;
		}
		String sqlInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FIELD +
				"(" + IotHubDataHandler.KEY_FIELD_FEED_ID + 
				"," + IotHubDataHandler.KEY_FIELD_NAME +
				"," + IotHubDataHandler.KEY_FIELD_METADATA + 
				"," + IotHubDataHandler.KEY_FIELD_TYPE + 
				"," + IotHubDataHandler.KEY_FIELD_OPTIONAL + ") values (?,?,?,?,?)";
		PreparedStatement psInsert = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
		for (FieldDescription fd : fields) {
			psInsert.setLong(1, id);
			psInsert.setString(2, fd.getName());
			psInsert.setString(3, fd.getMetadata());
			psInsert.setString(4, fd.getType());
			psInsert.setInt(5, fd.isOptional() ? 1 : 0);
			psInsert.executeUpdate();
			ResultSet genKeysFeed = psInsert.getGeneratedKeys();
			if (genKeysFeed.next()) {
				long idField = genKeysFeed.getLong(1);
				addFieldKeywords(idField, fd.getKeywords());
			}
			genKeysFeed.close();
		}
		psInsert.close();
	}

	private boolean compareComposeFeeds(ComposedFeed feed, String name, String metadata, 
			boolean storage, boolean readable, boolean writable, 
			List<String> keywords, List<FieldDescription> fields) {
		//TODO auto generated stuff
		return true;
	}

	@Override
	public ComposedFeed addComposedFeed(String name, String metadata, 
			boolean storage, boolean readable, boolean writable, 
			List<String> keywords, List<FieldDescription> fields) {
		ComposedFeed composedFeed = null;
		if (fields == null || fields.size() == 0) {
			Log.e(TAG, "One cannot create a composed feed with no fields");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			//First things first, insert the feed's values to the feed table
			String sqlFeedInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FEED + "("
					+ IotHubDataHandler.KEY_FEED_NAME + "," 
					+ IotHubDataHandler.KEY_FEED_METADATA + ","
					+ IotHubDataHandler.KEY_FEED_TYPE + ","
					+ IotHubDataHandler.KEY_FEED_STORAGE + "," 
					+ IotHubDataHandler.KEY_FEED_READABLE + "," 
					+ IotHubDataHandler.KEY_FEED_WRITABLE + ") VALUES (?,?,?,?,?,?)";
			PreparedStatement psFeedInsert = connection.prepareStatement(sqlFeedInsert, Statement.RETURN_GENERATED_KEYS);
			psFeedInsert.setString(1, name);
			psFeedInsert.setString(2, metadata);
			psFeedInsert.setString(3, IotHubDataHandler.COMPOSED_FEED);
			psFeedInsert.setInt(4, storage ? 1 : 0);
			psFeedInsert.setInt(5, readable ? 1 : 0);
			psFeedInsert.setInt(6, writable ? 1 : 0);
			psFeedInsert.executeUpdate();
			ResultSet genKeysFeed = psFeedInsert.getGeneratedKeys();
			if (genKeysFeed.next()) {
				long insertIdFeed = genKeysFeed.getLong(1);
				//Now we add the keywords
				addFeedKeywords(insertIdFeed, keywords);
				//Now we add the fields
				addFeedFields(insertIdFeed, fields);
				//At point we should have everything set so it is time to retrieve the composed feed from the database
				//Log.d(TAG, "Now i will try to collect the composed feed that was just added to the db");
				composedFeed = getComposedFeed(insertIdFeed);
				if (composedFeed == null) {
					Log.e(TAG, "The feed should not be null");
				}
				//Now I want to make some checks
				if (!compareComposeFeeds(composedFeed, name, metadata, 
						storage, readable, writable, keywords, fields)) {
					Log.e(TAG, "Retrieving feed " + name + " did not work");
					composedFeed = null;
				}
			}
			else {
				Log.e(TAG, "The insert of feed " + name + " did not work");
			}
			genKeysFeed.close();
			psFeedInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			composedFeed = null;
		}
		try {
			if (composedFeed == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return composedFeed;
	}

	private ComposedFeed getComposedFeed(long id) {
		ComposedFeed composedFeed = null;
		try {
			checkOpenness();
			String feedIdFeed = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_ID;
			String fieldIdFeed = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_FEED_ID;
			String attr1 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_NAME;
			String attr2 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_METADATA;
			String attrType = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_TYPE;
			String attr3 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_STORAGE;
			String attr4 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_READABLE;
			String attr5 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_WRITABLE;
			String attr6 = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_ID;
			String attr7 = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_NAME;
			String attr8 = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_METADATA;
			String attr9 = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_TYPE;
			String attr10 = IotHubDataHandler.TABLE_FIELD + "." + IotHubDataHandler.KEY_FIELD_OPTIONAL;
			String sql = "SELECT " + attr1 + ", " + attr2 + ", " + attr3 + ", " + attr4 + ", " +
					attr5 + ", " + attr6 + ", " + attr7 + ", " + attr8 + ", " + attr9 + ", " + attr10 +
					" FROM " + IotHubDataHandler.TABLE_FEED + 
					" INNER JOIN " + IotHubDataHandler.TABLE_FIELD + " ON " +
					feedIdFeed + " = " + fieldIdFeed + 
					" WHERE " + feedIdFeed + " = ?" +
					" AND " + attrType + " = '" + IotHubDataHandler.COMPOSED_FEED + "'";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String feedName = rs.getString(1);
				String feedMetadata = rs.getString(2);
				boolean feedStorage = rs.getInt(3) != 0;
				boolean feedReadable = rs.getInt(4) != 0;
				boolean feedWritable = rs.getInt(5) != 0;
				Map<String, Field> fieldList = new HashMap<>();
				do {
					long fieldId = rs.getLong(6);
					String fieldName = rs.getString(7);
					String fieldMetadata = rs.getString(8);
					String fieldType = rs.getString(9);
					boolean fieldOptional = rs.getInt(10) != 0;
					List<String> keywords = getFieldKeywords(fieldId);
					Field field = new Field(fieldId, fieldName, fieldType, fieldMetadata, fieldOptional, keywords);
					fieldList.put(fieldName, field);
				} while (rs.next());
				List<String> keywords = getFeedKeywords(id);
				composedFeed = new ComposedFeed(id, feedName, feedMetadata, keywords, feedStorage, feedReadable, feedWritable, fieldList);
			}
			else {
				Log.e(TAG, "No results for this request: " + ps.toString());
			}
			rs.close();
			ps.close();
			return composedFeed;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<ComposedFeed> getComposedFeeds () {
		List<ComposedFeed> composedFeedList = new ArrayList<ComposedFeed>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.COMPOSED_FEED + "'";
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long feedId = rs.getLong(IotHubDataHandler.KEY_FEED_ID);
				composedFeedList.add(getComposedFeed(feedId));
			}
			rs.close();
			statement.close();
			return composedFeedList;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private ExecutableFeed getExecutableFeed(long id) {
		ExecutableFeed executableFeed = null;
		try {
			checkOpenness();
			String feedIdFeed = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_ID;
			String attr1 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_NAME;
			String attr2 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_METADATA;
			String attrType = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_TYPE;
			String attr3 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_READABLE;
			String attr4 = IotHubDataHandler.TABLE_FEED + "." + IotHubDataHandler.KEY_FEED_WRITABLE;
			String sql = "SELECT " + attr1 + ", " + attr2 + ", " + attr3 + ", " + attr4 +
					" FROM " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + feedIdFeed + " = ?" +
					" AND " + attrType + " = '" + IotHubDataHandler.EXECUTABLE_FEED + "'";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String feedName = rs.getString(1);
				String feedMetadata = rs.getString(2);
				boolean feedReadable = rs.getInt(3) != 0;
				boolean feedWritable = rs.getInt(4) != 0;
				//TODO make a table for the feed description and get the data from it
				ExecutableFeedDescription description = new ExecutableFeedDescription();
				List<String> keywords = getFeedKeywords(id);
				executableFeed = new ExecutableFeed(id, feedName, feedMetadata, keywords, feedReadable, feedWritable, description);
			}
			else {
				Log.e(TAG, "No results for this request: " + ps.toString());
			}
			rs.close();
			ps.close();
			return executableFeed;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public ExecutableFeed addExecutableFeed(String name, String metadata,
			boolean readable, boolean writable, List<String> keywords,
			ExecutableFeedDescription executableFeedDescription) {
		ExecutableFeed executableFeed = null;
		if (executableFeedDescription == null) {
			Log.e(TAG, "One cannot create an executable feed with no description");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			//First things first, insert the feed's values to the feed table
			String sqlFeedInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FEED + "("
					+ IotHubDataHandler.KEY_FEED_NAME + "," 
					+ IotHubDataHandler.KEY_FEED_METADATA + ","
					+ IotHubDataHandler.KEY_FEED_TYPE + ","
					+ IotHubDataHandler.KEY_FEED_STORAGE + "," 
					+ IotHubDataHandler.KEY_FEED_READABLE + "," 
					+ IotHubDataHandler.KEY_FEED_WRITABLE + ") VALUES (?,?,?,0,?,?)";
			PreparedStatement psFeedInsert = connection.prepareStatement(sqlFeedInsert, Statement.RETURN_GENERATED_KEYS);
			psFeedInsert.setString(1, name);
			psFeedInsert.setString(2, metadata);
			psFeedInsert.setString(3, IotHubDataHandler.EXECUTABLE_FEED);
			psFeedInsert.setInt(4, readable ? 1 : 0);
			psFeedInsert.setInt(5, writable ? 1 : 0);
			psFeedInsert.executeUpdate();
			ResultSet genKeysFeed = psFeedInsert.getGeneratedKeys();
			if (genKeysFeed.next()) {
				long insertIdFeed = genKeysFeed.getLong(1);
				//Now we add the keywords
				addFeedKeywords(insertIdFeed, keywords);
				//Now we add the fields
				addExecutableFeedDescription(insertIdFeed, executableFeedDescription);
				//At point we should have everything set so it is time to retrieve the composed feed from the database
				//Log.d(TAG, "Now i will try to collect the executable feed that was just added to the db");
				executableFeed = getExecutableFeed(insertIdFeed);
				if (executableFeed == null) {
					Log.e(TAG, "The feed should not be null");
				}
				//Now I want to make some checks
				if (!compareExecutableFeeds(executableFeed, name, metadata, 
						readable, writable, keywords, executableFeedDescription)) {
					Log.e(TAG, "Retrieving feed " + name + " did not work");
					executableFeed = null;
				}
			}
			else {
				Log.e(TAG, "The insert of feed " + name + " did not work");
			}
			genKeysFeed.close();
			psFeedInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			executableFeed = null;
		}
		try {
			if (executableFeed == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return executableFeed;
	}

	private boolean compareExecutableFeeds(ExecutableFeed executableFeed,
			String name, String metadata, boolean readable, boolean writable,
			List<String> keywords,
			ExecutableFeedDescription executableFeedDescription) {
		// TODO Auto-generated method stub
		return true;
	}

	private void addExecutableFeedDescription(long insertIdFeed,
			ExecutableFeedDescription executableFeedDescription) {
		// TODO Auto-generated method stub
		// Add a table to store the informations about executable feeds
	}

	private List<ExecutableFeed> getExecutableFeeds () {
		List<ExecutableFeed> executableFeedList = new ArrayList<>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.EXECUTABLE_FEED + "'";
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long feedId = rs.getLong(IotHubDataHandler.KEY_FEED_ID);
				executableFeedList.add(getExecutableFeed(feedId));
			}
			rs.close();
			statement.close();
			return executableFeedList;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Feed getFeed(long feedId) {
		Feed feed = null;
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_ID + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, feedId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_FEED_ID);
				String type = rs.getString(IotHubDataHandler.KEY_FEED_TYPE);
				if (IotHubDataHandler.COMPOSED_FEED.equals(type)) {
					feed = getComposedFeed(id);
				}
				else if (IotHubDataHandler.ATOMIC_FEED.equals(type)) {
					feed = getAtomicFeed(id);
				}
				else if (IotHubDataHandler.EXECUTABLE_FEED.equals(type)) {
					feed = getExecutableFeed(id);
				}
				else {
					Log.e(TAG, "Uknown type of feed '" + type + "', must be (" + 
							IotHubDataHandler.ATOMIC_FEED + ", " + 
							IotHubDataHandler.COMPOSED_FEED + ", " + 
							IotHubDataHandler.EXECUTABLE_FEED + ")");
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}	
		return feed;
	}

	@Override
	public Feed getFeed(String name) {
		Feed feed = null;
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_NAME + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_FEED_ID);
				String type = rs.getString(IotHubDataHandler.KEY_FEED_TYPE);
				if (IotHubDataHandler.COMPOSED_FEED.equals(type)) {
					feed = getComposedFeed(id);
				}
				else if (IotHubDataHandler.ATOMIC_FEED.equals(type)) {
					feed = getAtomicFeed(id);
				}
				else if (IotHubDataHandler.EXECUTABLE_FEED.equals(type)) {
					feed = getExecutableFeed(id);
				}
				else {
					Log.e(TAG, "Uknown type of feed '" + type + "', must be (" + 
							IotHubDataHandler.ATOMIC_FEED + ", " + 
							IotHubDataHandler.COMPOSED_FEED + ", " + 
							IotHubDataHandler.EXECUTABLE_FEED + ")");
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}	
		return feed;
	}

	@Override
	public List<Feed> getFeeds() {
		List<Feed> feeds = new ArrayList<Feed>();
		feeds.addAll(getAtomicFeeds());
		feeds.addAll(getComposedFeeds());
		feeds.addAll(getExecutableFeeds());
		return feeds;
	}

	@Override
	public Feed deleteFeed(String name) {
		Feed feed = null;
		try {
			checkOpenness();
			feed = getFeed(name);
			if (feed == null) {
				Log.e(TAG, "No feed was found to delete with name " + name);
				return null;
			}
			String sql = "DELETE FROM " + IotHubDataHandler.TABLE_FEED +
					" WHERE " + IotHubDataHandler.KEY_FEED_NAME + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, name);
			if (ps.executeUpdate() != 1) {
				Log.e(TAG, "Feed " + name + " was not deleted from the database");
			}
			else {
				Log.d(TAG, "Feed " + name + " was deleted from the database");
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return feed;
	}

	private void addFeedKeywords(long id, List<String> keywords) throws SQLException {
		addKeywords(id, keywords, true);
	}

	private void addFieldKeywords(long id, List<String> keywords) throws SQLException {
		addKeywords(id, keywords, false);
	}

	private void addKeywords(long id, List<String> keywords, boolean isFeed) throws SQLException {
		if (keywords == null || keywords.size() == 0) {
			Log.d(TAG, "No keywords to be added for " + (isFeed ? "feed" : "field") + " id: " + id);
			return;
		}
		String sqlSelectKeyword = "SELECT " + IotHubDataHandler.KEY_KEYWORD_ID + 
				" FROM " + IotHubDataHandler.TABLE_KEYWORD + 
				" WHERE " + IotHubDataHandler.KEY_KEYWORD_VALUE + " = ?";
		String sqlInsertKeyword = "insert into " + IotHubDataHandler.TABLE_KEYWORD +
				"(" + IotHubDataHandler.KEY_KEYWORD_VALUE + ") values (?)";
		String sqlInsertRelation = isFeed ? ("insert into " + IotHubDataHandler.TABLE_KEYWORD_FEED_REL +
				"(" + IotHubDataHandler.KEY_KEYWORD_FEED_KEYWORD_ID + 
				"," + IotHubDataHandler.KEY_KEYWORD_FEED_FEED_ID + ") values (?,?)") : (
						"insert into " + IotHubDataHandler.TABLE_KEYWORD_FIELD_REL +
						"(" + IotHubDataHandler.KEY_KEYWORD_FIELD_KEYWORD_ID + 
						"," + IotHubDataHandler.KEY_KEYWORD_FIELD_FIELD_ID + ") values (?,?)");
		PreparedStatement psSelectKeyword = connection.prepareStatement(sqlSelectKeyword);
		PreparedStatement psInsertKeyword = connection.prepareStatement(sqlInsertKeyword, Statement.RETURN_GENERATED_KEYS);
		PreparedStatement psInsertRel = connection.prepareStatement(sqlInsertRelation);
		for (String keyword : keywords) {
			psSelectKeyword.setString(1, keyword);
			ResultSet rs = psSelectKeyword.executeQuery();
			if (rs.next()) {
				long keywordId = rs.getLong(IotHubDataHandler.KEY_KEYWORD_ID);
				psInsertRel.setLong(1, keywordId);
				psInsertRel.setLong(2, id);
				if (psInsertRel.executeUpdate() <= 0) {
					Log.e(TAG, "Linking keyword " + keyword + " and " + (isFeed ? "feed " : "field ") + id + " did not work");
				}
			} else {
				psInsertKeyword.setString(1, keyword);
				psInsertKeyword.executeUpdate();
				ResultSet genKeysFeed = psInsertKeyword.getGeneratedKeys();
				if (genKeysFeed.next()) {
					long insertIdKeyword = genKeysFeed.getLong(1);
					psInsertRel.setLong(1, insertIdKeyword);
					psInsertRel.setLong(2, id);
					if (psInsertRel.executeUpdate() <= 0) {
						Log.e(TAG, "Linking keyword " + keyword + " and " + (isFeed ? "feed " : "field ") + id + " did not work");
					}
				}
				genKeysFeed.close();
			}
			rs.close();
		}
		psSelectKeyword.close();
		psInsertKeyword.close();
		psInsertRel.close();
	}

	private List<String> getFeedKeywords(long id) {
		List<String> keywords = new ArrayList<String>();
		try {
			checkOpenness();
			final String query = "SELECT " + IotHubDataHandler.KEY_KEYWORD_VALUE + " FROM " +
					IotHubDataHandler.TABLE_KEYWORD + " a INNER JOIN " +
					IotHubDataHandler.TABLE_KEYWORD_FEED_REL + " b ON a." +
					IotHubDataHandler.KEY_KEYWORD_ID + " = b." +
					IotHubDataHandler.KEY_KEYWORD_FEED_KEYWORD_ID +
					" WHERE b." + IotHubDataHandler.KEY_KEYWORD_FEED_FEED_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String keyword = rs.getString(IotHubDataHandler.KEY_KEYWORD_VALUE);
				keywords.add(keyword);
			}
			rs.close();
			ps.close();
			return keywords;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	private List<String> getFieldKeywords(long id) {
		List<String> keywords = new ArrayList<String>();
		try {
			checkOpenness();
			final String query = "SELECT " + IotHubDataHandler.KEY_KEYWORD_VALUE + " FROM " +
					IotHubDataHandler.TABLE_KEYWORD + " a INNER JOIN " +
					IotHubDataHandler.TABLE_KEYWORD_FIELD_REL + " b ON a." +
					IotHubDataHandler.KEY_KEYWORD_ID + " = b." +
					IotHubDataHandler.KEY_KEYWORD_FIELD_KEYWORD_ID +
					" WHERE b." + IotHubDataHandler.KEY_KEYWORD_FIELD_FIELD_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String keyword = rs.getString(IotHubDataHandler.KEY_KEYWORD_VALUE);
				keywords.add(keyword);
			}
			rs.close();
			ps.close();
			return keywords;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * TODO there should be more filters available in the future
	 * such as paging, max number of entries, timestamp, etc.
	 */
	@Override
	public List<FeedEntry> getFeedEntries(Feed feed) {
		if (feed == null) {
			Log.e(TAG, "Cannot get entries from a NULL feed");
			return null;
		}
		List<FeedEntry> entries = new ArrayList<>();
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_FEED_ENTRY +
					" WHERE " + IotHubDataHandler.KEY_FEED_ENTRY_FEED_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, feed.getId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_FEED_ENTRY_ID);
				Date timestamp = new Date(rs.getLong(IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP));
				JSONObject data = new JSONObject(rs.getString(IotHubDataHandler.KEY_FEED_ENTRY_DATA));
				entries.add(new FeedEntry(id, feed, timestamp, data));
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return entries;
	}

	private FeedEntry getFeedEntry(long id) {
		FeedEntry entry = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_FEED_ENTRY +
					" WHERE " + IotHubDataHandler.KEY_FEED_ENTRY_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long feedId = rs.getLong(IotHubDataHandler.KEY_FEED_ENTRY_FEED_ID);
				Date timestamp = new Date(rs.getLong(IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP));
				JSONObject data;
				try {
					data = new JSONObject(rs.getString(IotHubDataHandler.KEY_FEED_ENTRY_DATA));
					Feed feed = getFeed(feedId);
					if (feed != null) {
						entry = new FeedEntry(id, feed, timestamp, data);
					} else {
						Log.e(TAG, "The feed corresponding to the entry cannot be found");
					}
				} catch (JSONException e) {
					Log.e(TAG, "Could not retrieve the data from this entry");

				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return entry;
	}

	@Override
	public FeedEntry addFeedEntry(Feed feed, JSONObject data) {
		if (feed == null) {
			Log.e(TAG, "Cannot add entry from a NULL feed");
			return null;
		}
		FeedEntry entry = null;
		try {
			checkOpenness();
			final String query = "INSERT INTO " +
					IotHubDataHandler.TABLE_FEED_ENTRY +
					" ( " + IotHubDataHandler.KEY_FEED_ENTRY_FEED_ID + "," + 
					IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP + "," +
					IotHubDataHandler.KEY_FEED_ENTRY_DATA + ") VALUES (?,?,?)";
			PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, feed.getId());
			ps.setLong(2, new Date().getTime());
			ps.setString(3, data.toString());
			ps.executeUpdate();
			ResultSet genKeysFeed = ps.getGeneratedKeys();
			if (genKeysFeed.next()) {
				long insertIdEntry = genKeysFeed.getLong(1);
				entry = getFeedEntry(insertIdEntry);
			}
			genKeysFeed.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return entry;
	}

	@Override
	public FeedEntry deleteFeedEntry(FeedEntry entry) {
		if (entry == null) {
			Log.e(TAG, "Cannot delete null entry");
			return null;
		}
		try {
			checkOpenness();
			final String query = "DELETE FROM " +
					IotHubDataHandler.TABLE_FEED_ENTRY +
					" WHERE " + IotHubDataHandler.KEY_FEED_ENTRY_ID + " = ? ";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, entry.getId());
			if (ps.executeUpdate() != 1) {
				Log.e(TAG, "could not delete entry " + entry.toString());
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return entry;
	}

	// Related to plugins
	@Override
	public PluginInfo addJavascriptPlugin(String serviceName, String packageName, File file) {
		PluginInfo pluginInfo = null;
		if (serviceName == null) {
			Log.e(TAG, "One cannot create a plugin where service name is null");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			//First things first, insert the feed's values to the feed table
			String sqlPluginInsert = "INSERT INTO " + IotHubDataHandler.TABLE_PLUGIN_INFO + "("
					+ IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + "," 
					+ IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME + ","
					+ IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME + ","
					+ IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME + ") VALUES (?,?,?,?)";
			PreparedStatement psPluginInsert = connection.prepareStatement(sqlPluginInsert, Statement.RETURN_GENERATED_KEYS);
			psPluginInsert.setString(1, IotHubDataHandler.JAVASCRIPT_PLUGIN);
			psPluginInsert.setString(2, serviceName);
			psPluginInsert.setString(3, packageName);
			psPluginInsert.setString(4, file == null ? null : file.getName());
			psPluginInsert.executeUpdate();
			ResultSet genKeysPlugin = psPluginInsert.getGeneratedKeys();
			if (genKeysPlugin.next()) {
				long insertIdPlugin = genKeysPlugin.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the plugin that was just added to the db");
				pluginInfo = getPluginInfo(insertIdPlugin);
				if (pluginInfo == null) {
					Log.e(TAG, "The plugin should not be null");
				}
				//Now I want to make some checks
				if (!pluginInfo.isJavascript()) {
					Log.e(TAG, "The plugin " + pluginInfo.getId() + " is not javascript");
					pluginInfo = null;
				}
			}
			else {
				Log.e(TAG, "The insert of javascript plugin " + serviceName + " did not work");
			}
			genKeysPlugin.close();
			psPluginInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			pluginInfo = null;
		}
		try {
			if (pluginInfo == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pluginInfo;
	}

	@Override
	public PluginInfo addNativePlugin(String serviceName, String packageName, File file) {
		PluginInfo pluginInfo = null;
		if (serviceName == null || packageName == null) {
			Log.e(TAG, "One cannot create a plugin where service name is null");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			//First things first, insert the feed's values to the feed table
			String sqlPluginInsert = "INSERT INTO " + IotHubDataHandler.TABLE_PLUGIN_INFO + "("
					+ IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + "," 
					+ IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME + ","
					+ IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME + ","
					+ IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME + ") VALUES (?,?,?,?)";
			PreparedStatement psPluginInsert = connection.prepareStatement(sqlPluginInsert, Statement.RETURN_GENERATED_KEYS);
			psPluginInsert.setString(1, IotHubDataHandler.NATIVE_PLUGIN);
			psPluginInsert.setString(2, serviceName);
			psPluginInsert.setString(3, packageName);
			psPluginInsert.setString(4, file == null ? null : file.getName());
			psPluginInsert.executeUpdate();
			ResultSet genKeysPlugin = psPluginInsert.getGeneratedKeys();
			if (genKeysPlugin.next()) {
				long insertIdPlugin = genKeysPlugin.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the plugin that was just added to the db");
				pluginInfo = getPluginInfo(insertIdPlugin);
				if (pluginInfo == null) {
					Log.e(TAG, "The plugin should not be null");
				}
				//Now I want to make some checks
				if (!pluginInfo.isNative()) {
					Log.e(TAG, "The plugin " + pluginInfo.getId() + " is not native");
					pluginInfo = null;
				}
			}
			else {
				Log.e(TAG, "The insert of native plugin " + serviceName + " did not work");
			}
			genKeysPlugin.close();
			psPluginInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			pluginInfo = null;
		}
		try {
			if (pluginInfo == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pluginInfo;
	}


	@Override
	public PluginInfo deletePlugin(long id) {
		PluginInfo pluginInfo = null;

		try {
			checkOpenness();
			connection.setAutoCommit(false);

			pluginInfo = getPluginInfo(id);
			if (pluginInfo == null) {
				Log.e(TAG, "No plugin was found to delete with id " + id);
				return null;
			}
			String sql = "DELETE FROM " + IotHubDataHandler.TABLE_PLUGIN_INFO +
					" WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_ID + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, id);
			if (ps.executeUpdate() != 1) {
				Log.e(TAG, "Plugin " + id + " was not deleted from the database");
				pluginInfo = null;
			}
			else {
				Log.d(TAG, "Plugin " + id + " was deleted from the database");
			}
			ps.close();
		} 
		catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			pluginInfo = null;
		}

		try {
			if (pluginInfo == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		return pluginInfo;
	}

	@Override
	public List<PluginInfo> getJavascriptPlugins() {
		List<PluginInfo> pluginInfos = new ArrayList<PluginInfo>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_PLUGIN_INFO +
					" WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + " = '" + IotHubDataHandler.JAVASCRIPT_PLUGIN + "'";
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long pluginId = rs.getLong(IotHubDataHandler.KEY_PLUGIN_INFO_ID);
				pluginInfos.add(getPluginInfo(pluginId));
			}
			rs.close();
			statement.close();
			return pluginInfos;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<PluginInfo> getNativePlugins() {
		List<PluginInfo> pluginInfos = new ArrayList<PluginInfo>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_PLUGIN_INFO +
					" WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + " = '" + IotHubDataHandler.NATIVE_PLUGIN + "'";
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long pluginId = rs.getLong(IotHubDataHandler.KEY_PLUGIN_INFO_ID);
				pluginInfos.add(getPluginInfo(pluginId));
			}
			rs.close();
			statement.close();
			return pluginInfos;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<PluginInfo> getPlugins() {
		List<PluginInfo> list = getJavascriptPlugins();
		if (list != null) {
			list.addAll(getNativePlugins());
			return list;
		}
		else {
			return getNativePlugins();
		}
	}

	@Override
	public PluginInfo getPluginInfo(long pluginId) {
		PluginInfo pluginInfo = null;
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_PLUGIN_INFO +
					" WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_ID + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, pluginId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_PLUGIN_INFO_ID);
				String type = rs.getString(IotHubDataHandler.KEY_PLUGIN_INFO_TYPE);
				String packageName = rs.getString(IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME);
				String serviceName = rs.getString(IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME);
				String filename = rs.getString(IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME);
				if (IotHubDataHandler.NATIVE_PLUGIN.equals(type)) {
					pluginInfo = new PluginInfo(id, PluginInfo.Type.NATIVE, serviceName, packageName, filename);
				}
				else if (IotHubDataHandler.JAVASCRIPT_PLUGIN.equals(type)) {
					pluginInfo = new PluginInfo(id, PluginInfo.Type.JAVASCRIPT, serviceName, packageName, filename);
				}
				else {
					Log.e(TAG, "Uknown type of plugin '" + type + "', must be (" + 
							IotHubDataHandler.NATIVE_PLUGIN + ", " + 
							IotHubDataHandler.JAVASCRIPT_PLUGIN + ")");
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}	
		return pluginInfo;
	}

	@Override
	public Enabler addEnabler(String name, String metadata, PluginInfo plugin, String pluginInfoConfig) {
		Enabler enabler = null;
		if (name == null || plugin == null) {
			Log.e(TAG, "One cannot create a enabler where name is null or with no plugin");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			String sqlEnablerInsert = "INSERT INTO " + IotHubDataHandler.TABLE_ENABLER + "("
					+ IotHubDataHandler.KEY_ENABLER_NAME + "," 
					+ IotHubDataHandler.KEY_ENABLER_METADATA + ","
					+ IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO + ","
					+ IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG + ") VALUES (?,?,?,?)";
			PreparedStatement psEnablerInsert = connection.prepareStatement(sqlEnablerInsert, Statement.RETURN_GENERATED_KEYS);
			psEnablerInsert.setString(1, name);
			psEnablerInsert.setString(2, metadata);
			psEnablerInsert.setLong(3, plugin.getId());
			psEnablerInsert.setString(4, pluginInfoConfig);
			psEnablerInsert.executeUpdate();
			ResultSet genKeysEnabler = psEnablerInsert.getGeneratedKeys();
			if (genKeysEnabler.next()) {
				long insertIdEnabler= genKeysEnabler.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the enabler that was just added to the db");
				enabler = getEnabler(insertIdEnabler);
				if (enabler == null) {
					Log.e(TAG, "The enabler should not be null");
				}
				//TODO maybe check that the plugins are the same
			}
			else {
				Log.e(TAG, "The insert of enabler " + name + " did not work");
			}
			genKeysEnabler.close();
			psEnablerInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			enabler = null;
		}
		try {
			if (enabler == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return enabler;
	}

	@Override
	public Enabler deleteEnabler(Enabler enabler) {
		try {
			checkOpenness();
			final String query = "DELETE FROM " +
					IotHubDataHandler.TABLE_ENABLER +
					" WHERE " + IotHubDataHandler.KEY_ENABLER_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, enabler.getId());
			if (ps.executeUpdate() != 1) {
				enabler = null;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return enabler;
	}

	@Override
	public Enabler getEnabler(long id) {
		Enabler enabler = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_ENABLER +
					" WHERE " + IotHubDataHandler.KEY_ENABLER_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String name = rs.getString(IotHubDataHandler.KEY_ENABLER_NAME);
				String metadata = rs.getString(IotHubDataHandler.KEY_ENABLER_METADATA);
				long pluginId = rs.getLong(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO);
				String pluginConfiguration = rs.getString(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG);
				enabler = new Enabler(id, name, metadata, getPluginInfo(pluginId), pluginConfiguration);
				List<Feature> features = getFeaturesForEnabler(enabler);
				for (Feature feature : features) {
					enabler.addFeature(feature);
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return enabler;
	}

	@Override
	public Enabler getEnabler(String name) {
		Enabler enabler = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_ENABLER +
					" WHERE " + IotHubDataHandler.KEY_ENABLER_NAME + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_ENABLER_ID);
				String metadata = rs.getString(IotHubDataHandler.KEY_ENABLER_METADATA);
				long pluginId = rs.getLong(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO);
				String pluginConfiguration = rs.getString(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG);
				PluginInfo pluginInfo = getPluginInfo(pluginId);
				if (pluginInfo == null) {
					Log.e(TAG, "The plugin info should not be null when getting an enabler");
				}
				else {
					enabler = new Enabler(id, name, metadata, pluginInfo, pluginConfiguration);
					List<Feature> features = getFeaturesForEnabler(enabler);
					for (Feature feature : features) {
						enabler.addFeature(feature);
					}
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return enabler;
	}

	private List<Feature> getFeaturesForEnabler(Enabler enabler) {
		List<Feature> features = new ArrayList<Feature>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_FEATURE + 
					" where " + IotHubDataHandler.KEY_FEATURE_ENABLER_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, enabler.getId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long featureId = rs.getLong(IotHubDataHandler.KEY_FEATURE_ID);
				String name = rs.getString(IotHubDataHandler.KEY_FEATURE_NAME);
				String type = rs.getString(IotHubDataHandler.KEY_FEATURE_TYPE);
				boolean isFeed = rs.getBoolean(IotHubDataHandler.KEY_FEATURE_IS_FEED);
				Feature feature = new Feature(featureId, enabler, name, type);
				feature.setAtomicFeed(isFeed);
				features.add(feature);
				Log.d(TAG, "Got one feature from the db");
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return features;
	}

	@Override
	public List<Enabler> getEnablers() {
		List<Enabler> enablers = new ArrayList<Enabler>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_ENABLER;
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long enablerId = rs.getLong(IotHubDataHandler.KEY_ENABLER_ID);
				enablers.add(getEnabler(enablerId));
			}
			rs.close();
			statement.close();
			return enablers;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Enabler updateEnabler(Enabler enabler, String name, String metadata, String pluginInfoConfig) {
		if (enabler == null) {
			return null;
		}
		try {
			checkOpenness();
			String sql = "update " + IotHubDataHandler.TABLE_ENABLER + " set " + 
					IotHubDataHandler.KEY_ENABLER_NAME + "=?, " +
					IotHubDataHandler.KEY_ENABLER_METADATA + "=?, " +
					IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG + "=?" +
					" where " + IotHubDataHandler.KEY_ENABLER_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, name);
			ps.setString(2, metadata);
			ps.setString(3, pluginInfoConfig);
			ps.setLong(4, enabler.getId());
			if (ps.executeUpdate() == 1) {
				Enabler newEnabler = getEnabler(enabler.getId());
				return newEnabler;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@Override
	public Feature addFeature(Enabler enabler, String name, String type) {
		Feature feature = null;
		if (enabler == null || name == null || type == null) {
			Log.e(TAG, "One cannot create a feature where the enable is null, name is null or with no type");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			String sqlFeatureInsert = "INSERT INTO " + IotHubDataHandler.TABLE_FEATURE + "("
					+ IotHubDataHandler.KEY_FEATURE_ENABLER_ID + "," 
					+ IotHubDataHandler.KEY_FEATURE_NAME + ","
					+ IotHubDataHandler.KEY_FEATURE_TYPE + ","
					+ IotHubDataHandler.KEY_FEATURE_IS_FEED + ") VALUES (?,?,?,?)";
			PreparedStatement psFeatureInsert = connection.prepareStatement(sqlFeatureInsert, Statement.RETURN_GENERATED_KEYS);
			psFeatureInsert.setLong(1, enabler.getId());
			psFeatureInsert.setString(2, name);
			psFeatureInsert.setString(3, type);
			psFeatureInsert.setBoolean(4, false); //An added feature is never a feed
			psFeatureInsert.executeUpdate();
			ResultSet genKeysFeature = psFeatureInsert.getGeneratedKeys();
			if (genKeysFeature.next()) {
				long insertIdFeature = genKeysFeature.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the feature that was just added to the db");
				feature = getFeature(insertIdFeature);
				if (feature == null) {
					Log.e(TAG, "The feature should not be null");
				}
			}
			else {
				Log.e(TAG, "The insert of feature " + name + " did not work");
			}
			genKeysFeature.close();
			psFeatureInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			feature = null;
		}
		try {
			if (feature == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return feature;
	}

	@Override
	public Feature getFeature(long id) {
		Feature feature = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_FEATURE +
					" WHERE " + IotHubDataHandler.KEY_FEATURE_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long enablerId = rs.getLong(IotHubDataHandler.KEY_FEATURE_ENABLER_ID);
				Enabler enabler = getEnabler(enablerId);
				for (Feature ft : enabler.getFeatures()) {
					if (ft.getId() == id) {
						feature = ft;
						break;
					}
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return feature;
	}

	@Override
	public Feature updateFeature(Feature feature, String name, String type, boolean isFeed) {
		if (feature == null) {
			return null;
		}
		try {
			checkOpenness();
			String sql = "update " + IotHubDataHandler.TABLE_FEATURE + " set " + 
					IotHubDataHandler.KEY_FEATURE_NAME + "=?, " +
					IotHubDataHandler.KEY_FEATURE_TYPE + "=?, " +
					IotHubDataHandler.KEY_FEATURE_IS_FEED + "=?" +
					" where " + IotHubDataHandler.KEY_FEATURE_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, name);
			ps.setString(2, type);
			ps.setBoolean(3, isFeed);
			ps.setLong(4, feature.getId());
			if (ps.executeUpdate() == 1) {
				Feature newFeature = getFeature(feature.getId());
				return newFeature;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}



	/* Services functions */

	@Override
	public List<Feature> deleteFeaturesOfEnabler(Enabler enabler) {
		if (enabler == null) {
			return null;
		}
		List<Feature> features = getFeaturesForEnabler(enabler);
		try {
			checkOpenness();
			String sql = "delete from " + IotHubDataHandler.TABLE_FEATURE +

					" where " + IotHubDataHandler.KEY_FEATURE_ENABLER_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, enabler.getId());

			if (ps.executeUpdate() == features.size()) {
				return features;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	@Override
	public Service addService(ServiceInfo serviceInfo, 
			String name, String metadata, String config, boolean bootAtStartup) {
		Service service = null;
		if (serviceInfo == null || name == null) {
			Log.e(TAG, "One cannot create a service where the serviceInfo is null, or name is null");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			String sqlInsert = "INSERT INTO " + IotHubDataHandler.TABLE_SERVICE + "("
					+ IotHubDataHandler.KEY_SERVICE_NAME + "," 
					+ IotHubDataHandler.KEY_SERVICE_METADATA + ","
					+ IotHubDataHandler.KEY_SERVICE_SERVICE_INFO + ","
					+ IotHubDataHandler.KEY_SERVICE_CONFIG + ","
					+ IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP + ") VALUES (?,?,?,?,?)";
			PreparedStatement psInsert = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
			psInsert.setString(1, name);
			psInsert.setString(2, metadata);
			psInsert.setLong(3, serviceInfo.getId());
			psInsert.setString(4, config);
			psInsert.setBoolean(5,  bootAtStartup);
			psInsert.executeUpdate();
			ResultSet genKeys = psInsert.getGeneratedKeys();
			if (genKeys.next()) {
				long insertId = genKeys.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the service " + insertId + " that was just added to the db");
				service = getService(insertId);
				if (service == null) {
					Log.e(TAG, "The service " + name + " should not be null");
				}
			}
			else {
				Log.e(TAG, "The insert of service " + name + " did not work");
			}
			genKeys.close();
			psInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			service = null;
		}
		try {
			if (service == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return service;
	}

	@Override
	public ServiceInfo addServiceInfo(String name, File file) {
		ServiceInfo service = null;
		if (file == null || name == null) {
			Log.e(TAG, "One cannot create a serviceInfo where the file is null, or name is null");
			return null;
		}
		try {
			checkOpenness();
			connection.setAutoCommit(false);
			String sqlInsert = "INSERT INTO " + IotHubDataHandler.TABLE_SERVICE_INFO + "("
					+ IotHubDataHandler.KEY_SERVICE_INFO_SERVICE_NAME + "," 
					+ IotHubDataHandler.KEY_SERVICE_INFO_FILENAME + ") VALUES (?,?)";
			PreparedStatement psInsert = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
			psInsert.setString(1, name);
			psInsert.setString(2, file.getName());
			psInsert.executeUpdate();
			ResultSet genKeys = psInsert.getGeneratedKeys();
			if (genKeys.next()) {
				long insertId = genKeys.getLong(1);
				//At point we should have everything set so it is time to retrieve the plugin from the database
				//Log.d(TAG, "Now i will try to collect the service info " + name + " that was just added to the db");
				service = getServiceInfo(insertId);
				if (service == null) {
					Log.e(TAG, "The service info " + name + " should not be null");
				}
			}
			else {
				Log.e(TAG, "The insert of service info " + name + " did not work");
			}
			genKeys.close();
			psInsert.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			service = null;
		}
		try {
			if (service == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return service;
	}

	@Override
	public Service deleteService(Service service) {
		if (service == null) {
			Log.w(TAG, "Cannot delete a null service");
			return null;
		}
		try {
			checkOpenness();
			final String query = "DELETE FROM " +
					IotHubDataHandler.TABLE_SERVICE +
					" WHERE " + IotHubDataHandler.KEY_SERVICE_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, service.getId());
			if (ps.executeUpdate() != 1) {
				Log.i(TAG, "Service " + service.getId() + " delete from db");
			}
			else {
				Log.w(TAG, "Delete service " + service.getId() + " has not affected any row");
				service = null;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return service;
	}

	@Override
	public ServiceInfo deleteServiceInfo(long id) {
		ServiceInfo serviceInfo = null;

		try {
			checkOpenness();
			connection.setAutoCommit(false);

			serviceInfo = getServiceInfo(id);
			if (serviceInfo == null) {
				Log.e(TAG, "No service plugin was found to delete with id " + id);
				return null;
			}
			String sql = "DELETE FROM " + IotHubDataHandler.TABLE_SERVICE_INFO +
					" WHERE " + IotHubDataHandler.KEY_SERVICE_INFO_ID + " = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setLong(1, id);
			if (ps.executeUpdate() != 1) {
				Log.e(TAG, "Service plugin " + id + " was not deleted from the database");
				serviceInfo = null;
			}
			else {
				Log.d(TAG, "Service plugin " + id + " was deleted from the database");
			}
			ps.close();
		} 
		catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			serviceInfo = null;
		}

		try {
			if (serviceInfo == null) {
				connection.rollback();
			}
			connection.commit();
			connection.setAutoCommit(true);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		return serviceInfo;
	}

	@Override
	public Service getService(String name) {
		Service service = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_SERVICE +
					" WHERE " + IotHubDataHandler.KEY_SERVICE_NAME + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setString(1, name);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long serviceInfoId = rs.getLong(IotHubDataHandler.KEY_SERVICE_SERVICE_INFO);
				long id = rs.getLong(IotHubDataHandler.KEY_SERVICE_ID);
				String metadata = rs.getString(IotHubDataHandler.KEY_SERVICE_METADATA);
				String config = rs.getString(IotHubDataHandler.KEY_SERVICE_CONFIG);
				boolean bootOnStartup = rs.getBoolean(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP);
				ServiceInfo serviceInfo = getServiceInfo(serviceInfoId);
				if (serviceInfo != null) {
					service = new Service(id, serviceInfo, 
							name, metadata, config, bootOnStartup);
				}
				else {
					Log.e(TAG, "ServiceInfo " + serviceInfoId + " should not be null");
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return service;
	}

	@Override
	public Service getService(long id) {
		Service service = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_SERVICE +
					" WHERE " + IotHubDataHandler.KEY_SERVICE_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				long serviceInfoId = rs.getLong(IotHubDataHandler.KEY_SERVICE_SERVICE_INFO);
				String name = rs.getString(IotHubDataHandler.KEY_SERVICE_NAME);
				String metadata = rs.getString(IotHubDataHandler.KEY_SERVICE_METADATA);
				String config = rs.getString(IotHubDataHandler.KEY_SERVICE_CONFIG);
				boolean bootOnStartup = rs.getBoolean(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP);
				ServiceInfo serviceInfo = getServiceInfo(serviceInfoId);
				if (serviceInfo != null) {
					service = new Service(id, serviceInfo, 
							name, metadata, config, bootOnStartup);
				}
				else {
					Log.e(TAG, "ServiceInfo " + serviceInfoId + " should not be null");
				}
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return service;
	}

	@Override
	public ServiceInfo getServiceInfo(long id) {
		ServiceInfo serviceInfo = null;
		try {
			checkOpenness();
			final String query = "SELECT * FROM " +
					IotHubDataHandler.TABLE_SERVICE_INFO +
					" WHERE " + IotHubDataHandler.KEY_SERVICE_INFO_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(query);
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				String name = rs.getString(IotHubDataHandler.KEY_SERVICE_INFO_SERVICE_NAME);
				String filename = rs.getString(IotHubDataHandler.KEY_SERVICE_INFO_FILENAME);
				serviceInfo = new ServiceInfo(id, Type.JAVASCRIPT, name, filename);
			}
			rs.close();
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return serviceInfo;
	}

	@Override
	public List<ServiceInfo> getServiceInfos() {
		List<ServiceInfo> serviceInfos = new ArrayList<>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_SERVICE_INFO;
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_SERVICE_INFO_ID);
				serviceInfos.add(getServiceInfo(id));
			}
			rs.close();
			statement.close();
			return serviceInfos;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Service> getServices() {
		List<Service> services = new ArrayList<>();
		try {
			checkOpenness();
			String sql = "select * from " + IotHubDataHandler.TABLE_SERVICE;
			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			while (rs.next()) {
				long id = rs.getLong(IotHubDataHandler.KEY_SERVICE_ID);
				services.add(getService(id));
			}
			rs.close();
			statement.close();
			return services;
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Service updateService(Service service, String name, 
			String metadata, String config, boolean bootAtStartup) {
		if (service == null) {
			return null;
		}
		try {
			checkOpenness();
			String sql = "update " + IotHubDataHandler.TABLE_SERVICE + " set " + 
					IotHubDataHandler.KEY_SERVICE_NAME + "=?, " +
					IotHubDataHandler.KEY_SERVICE_METADATA + "=?, " +
					IotHubDataHandler.KEY_SERVICE_CONFIG + "=?, " +
					IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP + "=?" +
					" where " + IotHubDataHandler.KEY_SERVICE_ID + "=?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, name);
			ps.setString(2, metadata);
			ps.setString(3, config);
			ps.setBoolean(4, bootAtStartup);
			ps.setLong(5, service.getId());
			if (ps.executeUpdate() == 1) {
				Service newService = getService(service.getId());
				return newService;
			}
			ps.close();
		} catch (SQLException | IotHubDatabaseException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

}
