package fi.helsinki.cs.iot.kahvihub.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

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
import fi.helsinki.cs.iot.hub.utils.Log;

/**
 * Created by mineraud on 16.10.2015.
 */
public class KahvihubDatabase implements IotHubDatabase {

    private static final String TAG = "KahvihubDatabase";
    private KahvihubDbHelper helper;
    private SQLiteDatabase database;
    private boolean isOpen;

    public KahvihubDatabase(Context context, String databaseName, int databaseVersion) {
        this.database = null;
        this.isOpen = false;
        this.helper = new KahvihubDbHelper(context, databaseName, databaseVersion);
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
        return isOpen && database != null;
    }

    protected boolean isUpgraded() {
        return false;
    }

    @Override
    public void executeUpdate(String request) throws IotHubDatabaseException {
        checkOpenness();
        database.beginTransaction();
        database.execSQL(request);
        database.setTransactionSuccessful();
        database.endTransaction();
    }

    @Override
    public void enableForeignKeyConstraints() throws IotHubDatabaseException {
        checkOpenness();
        database.setForeignKeyConstraintsEnabled(true);
    }

    private void addFeedKeywords(long id, List<String> keywords) {
        addKeywords(id, keywords, true);
    }

    private void addFieldKeywords(long id, List<String> keywords) {
        addKeywords(id, keywords, false);
    }

    private void addKeywords(long id, List<String> keywords, boolean isFeed) {

        if (keywords == null || keywords.size() == 0) {
            Log.d(TAG, "No keywords to be added for " + (isFeed ? "feed" : "field") + " id: " + id);
            return;
        }

        String sqlSelectKeyword = "SELECT " + IotHubDataHandler.KEY_KEYWORD_ID +
                " FROM " + IotHubDataHandler.TABLE_KEYWORD +
                " WHERE " + IotHubDataHandler.KEY_KEYWORD_VALUE + " = ?";
        for (String keyword : keywords) {
            Cursor cursor = database.rawQuery(sqlSelectKeyword, new String[]{keyword});
            if (cursor.moveToFirst()) {
                long keywordId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_KEYWORD_ID));
                //Create the content
                ContentValues values = new ContentValues();
                values.put(IotHubDataHandler.KEY_KEYWORD_FEED_KEYWORD_ID, keywordId);
                if (isFeed) {
                    values.put(IotHubDataHandler.KEY_KEYWORD_FEED_FEED_ID, id);
                } else {
                    values.put(IotHubDataHandler.KEY_KEYWORD_FIELD_FIELD_ID, id);
                }
                if (database.insert(isFeed ? IotHubDataHandler.TABLE_KEYWORD_FEED_REL : IotHubDataHandler.TABLE_KEYWORD_FIELD_REL,
                        null, values) == -1) {
                    Log.e(TAG, "Linking keyword " + keyword + " and " + (isFeed ? "feed " : "field ") + id + " did not work");
                }
            } else {
                ContentValues values = new ContentValues();
                values.put(IotHubDataHandler.KEY_KEYWORD_VALUE, keyword);
                long insertId;
                if ((insertId = database.insert(IotHubDataHandler.TABLE_KEYWORD,
                        null, values)) != -1) {
                    values = new ContentValues();
                    values.put(IotHubDataHandler.KEY_KEYWORD_FEED_KEYWORD_ID, insertId);
                    if (isFeed) {
                        values.put(IotHubDataHandler.KEY_KEYWORD_FEED_FEED_ID, id);
                    } else {
                        values.put(IotHubDataHandler.KEY_KEYWORD_FIELD_FIELD_ID, id);
                    }
                    if (database.insert(isFeed ? IotHubDataHandler.TABLE_KEYWORD_FEED_REL : IotHubDataHandler.TABLE_KEYWORD_FIELD_REL,
                            null, values) == -1) {
                        Log.e(TAG, "Linking keyword " + keyword + " and " + (isFeed ? "feed " : "field ") + id + " did not work");
                    }
                }
            }
            cursor.close();
        }
    }

    private List<String> getFeedKeywords(long id) {
        List<String> keywords = new ArrayList<>();
        try {
            checkOpenness();
            final String query = "SELECT " + IotHubDataHandler.KEY_KEYWORD_VALUE + " FROM " +
                    IotHubDataHandler.TABLE_KEYWORD + " a INNER JOIN " +
                    IotHubDataHandler.TABLE_KEYWORD_FEED_REL + " b ON a." +
                    IotHubDataHandler.KEY_KEYWORD_ID + " = b." +
                    IotHubDataHandler.KEY_KEYWORD_FEED_KEYWORD_ID +
                    " WHERE b." + IotHubDataHandler.KEY_KEYWORD_FEED_FEED_ID + "=?";
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            while (cursor.moveToNext()) {
                String keyword = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_KEYWORD_VALUE));
                keywords.add(keyword);
            }
            cursor.close();
            return keywords;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<String> getFieldKeywords(long id) {
        List<String> keywords = new ArrayList<>();
        try {
            checkOpenness();
            final String query = "SELECT " + IotHubDataHandler.KEY_KEYWORD_VALUE + " FROM " +
                    IotHubDataHandler.TABLE_KEYWORD + " a INNER JOIN " +
                    IotHubDataHandler.TABLE_KEYWORD_FIELD_REL + " b ON a." +
                    IotHubDataHandler.KEY_KEYWORD_ID + " = b." +
                    IotHubDataHandler.KEY_KEYWORD_FIELD_KEYWORD_ID +
                    " WHERE b." + IotHubDataHandler.KEY_KEYWORD_FIELD_FIELD_ID + "=?";
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            while (cursor.moveToNext()) {
                String keyword = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_KEYWORD_VALUE));
                keywords.add(keyword);
            }
            cursor.close();
            return keywords;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean compareAtomicFeeds(AtomicFeed feed, String name, String metadata,
                                       List<String> keywords, Feature feature) {
        // TODO Auto-generated method stub
        return true;
    }

    private void addFeedFeatureRelation(long feedId, long featureId) {
        ContentValues values = new ContentValues();
        values.put(IotHubDataHandler.KEY_FEED_FEATURE_REL_FEED_ID, feedId);
        values.put(IotHubDataHandler.KEY_FEED_FEATURE_REL_FEATURE_ID, featureId);
        database.insert(IotHubDataHandler.TABLE_FEED_FEATURE_REL, null, values);
    }

    @Override
    public AtomicFeed addAtomicFeed(String name, String metadata, List<String> keywords, Feature feature) {
        AtomicFeed feed = null;
        if (feature == null) {
            Log.e(TAG, "One cannot create a composed feed with no feature");
            return null;
        }
        try {
            checkOpenness();
            database.beginTransaction();

            //Create the content
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEED_NAME, name);
            values.put(IotHubDataHandler.KEY_FEED_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_FEED_TYPE, IotHubDataHandler.ATOMIC_FEED);
            values.put(IotHubDataHandler.KEY_FEED_STORAGE, 0);
            values.put(IotHubDataHandler.KEY_FEED_READABLE, 0);
            values.put(IotHubDataHandler.KEY_FEED_WRITABLE, 0);

            long insertIdFeed = database.insert(
                    IotHubDataHandler.TABLE_FEED,
                    null,
                    values);

            if (insertIdFeed != -1) {
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
            } else {
                Log.e(TAG, "The insert of feed " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            feed = null;
        }
        if (feed != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
        return feed;
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
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(id)});
            if (cursor.moveToNext()) {
                long featureId = cursor.getLong(0);
                Feature feature = getFeature(featureId);
                if (feature != null) {
                    String feedName = cursor.getString(1);
                    String feedMetadata = cursor.getString(2);
                    List<String> keywords = getFeedKeywords(id);
                    feed = new AtomicFeed(id, feedName, feedMetadata, keywords, feature);
                } else {
                    Log.e(TAG, "The feature does not exist");
                }
            } else {
                Log.e(TAG, "No results for this request: " + sql);
            }
            cursor.close();
            return feed;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<AtomicFeed> getAtomicFeeds() {
        List<AtomicFeed> atomicFeedList = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
                    " WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.ATOMIC_FEED + "'";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long feedId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ID));
                atomicFeedList.add(getAtomicFeed(feedId));
            }
            cursor.close();
            return atomicFeedList;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addFeedFields(long id, List<FieldDescription> fields) {
        if (fields == null || fields.size() == 0) {
            Log.e(TAG, "One cannot create a composed feed with no fields");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(IotHubDataHandler.KEY_FIELD_FEED_ID, id);
        for (FieldDescription fd : fields) {
            values.put(IotHubDataHandler.KEY_FIELD_NAME, fd.getName());
            values.put(IotHubDataHandler.KEY_FIELD_METADATA, fd.getMetadata());
            values.put(IotHubDataHandler.KEY_FIELD_TYPE, fd.getType());
            values.put(IotHubDataHandler.KEY_FIELD_OPTIONAL, fd.isOptional() ? 1 : 0);
            long insertId = database.insert(
                    IotHubDataHandler.TABLE_FIELD,
                    null,
                    values);
            if (insertId != -1) {
                addFieldKeywords(insertId, fd.getKeywords());
            }
        }
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
            database.beginTransaction();
            //First things first, insert the feed's values to the feed table
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEED_NAME, name);
            values.put(IotHubDataHandler.KEY_FEED_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_FEED_TYPE, IotHubDataHandler.COMPOSED_FEED);
            values.put(IotHubDataHandler.KEY_FEED_STORAGE, storage ? 1 : 0);
            values.put(IotHubDataHandler.KEY_FEED_READABLE, readable ? 1 : 0);
            values.put(IotHubDataHandler.KEY_FEED_WRITABLE, writable ? 1 : 0);
            long insertId = database.insert(
                    IotHubDataHandler.TABLE_FEED,
                    null,
                    values);
            if (insertId != -1) {
                //Now we add the keywords
                addFeedKeywords(insertId, keywords);
                //Now we add the fields
                addFeedFields(insertId, fields);
                //At point we should have everything set so it is time to retrieve the composed feed from the database
                //Log.d(TAG, "Now i will try to collect the composed feed that was just added to the db");
                composedFeed = getComposedFeed(insertId);
                if (composedFeed == null) {
                    Log.e(TAG, "The feed should not be null");
                }
                //Now I want to make some checks
                if (!compareComposeFeeds(composedFeed, name, metadata,
                        storage, readable, writable, keywords, fields)) {
                    Log.e(TAG, "Retrieving feed " + name + " did not work");
                    composedFeed = null;
                }
            } else {
                Log.e(TAG, "The insert of feed " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            composedFeed = null;
        }
        if (composedFeed != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();

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
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(id)});
            if (cursor.moveToFirst()) {
                String feedName = cursor.getString(0);
                String feedMetadata = cursor.getString(1);
                boolean feedStorage = cursor.getInt(2) != 0;
                boolean feedReadable = cursor.getInt(3) != 0;
                boolean feedWritable = cursor.getInt(4) != 0;
                Map<String, Field> fieldList = new HashMap<>();
                do {
                    long fieldId = cursor.getLong(5);
                    String fieldName = cursor.getString(6);
                    String fieldMetadata = cursor.getString(7);
                    String fieldType = cursor.getString(8);
                    boolean fieldOptional = cursor.getInt(9) != 0;
                    List<String> keywords = getFieldKeywords(fieldId);
                    Field field = new Field(fieldId, fieldName, fieldType, fieldMetadata, fieldOptional, keywords);
                    fieldList.put(fieldName, field);
                } while (cursor.moveToNext());
                List<String> keywords = getFeedKeywords(id);
                composedFeed = new ComposedFeed(id, feedName, feedMetadata, keywords, feedStorage, feedReadable, feedWritable, fieldList);
            } else {
                Log.e(TAG, "No results for this request: " + sql);
            }
            cursor.close();
            return composedFeed;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<ComposedFeed> getComposedFeeds() {
        List<ComposedFeed> composedFeedList = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
                    " WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.COMPOSED_FEED + "'";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long feedId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ID));
                composedFeedList.add(getComposedFeed(feedId));
            }
            cursor.close();
            return composedFeedList;
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(id)});
            if (cursor.moveToFirst()) {
                String feedName = cursor.getString(0);
                String feedMetadata = cursor.getString(1);
                boolean feedReadable = cursor.getInt(2) != 0;
                boolean feedWritable = cursor.getInt(3) != 0;
                //TODO make a table for the feed description and get the data from it
                ExecutableFeedDescription description = new ExecutableFeedDescription();
                List<String> keywords = getFeedKeywords(id);
                executableFeed = new ExecutableFeed(id, feedName, feedMetadata, keywords, feedReadable, feedWritable, description);
            } else {
                Log.e(TAG, "No results for this request: " + sql);
            }
            cursor.close();
            return executableFeed;
        } catch (IotHubDatabaseException e) {
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
            database.beginTransaction();
            //First things first, insert the feed's values to the feed table
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEED_NAME, name);
            values.put(IotHubDataHandler.KEY_FEED_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_FEED_TYPE, IotHubDataHandler.EXECUTABLE_FEED);
            values.put(IotHubDataHandler.KEY_FEED_STORAGE, 0);
            values.put(IotHubDataHandler.KEY_FEED_READABLE, readable ? 1 : 0);
            values.put(IotHubDataHandler.KEY_FEED_WRITABLE, writable ? 1 : 0);
            long insertId = database.insert(
                    IotHubDataHandler.TABLE_FEED,
                    null,
                    values);
            if (insertId != -1) {
                //Now we add the keywords
                addFeedKeywords(insertId, keywords);
                //Now we add the fields
                addExecutableFeedDescription(insertId, executableFeedDescription);
                //At point we should have everything set so it is time to retrieve the composed feed from the database
                //Log.d(TAG, "Now i will try to collect the executable feed that was just added to the db");
                executableFeed = getExecutableFeed(insertId);
                if (executableFeed == null) {
                    Log.e(TAG, "The feed should not be null");
                }
                //Now I want to make some checks
                if (!compareExecutableFeeds(executableFeed, name, metadata,
                        readable, writable, keywords, executableFeedDescription)) {
                    Log.e(TAG, "Retrieving feed " + name + " did not work");
                    executableFeed = null;
                }
            } else {
                Log.e(TAG, "The insert of feed " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            executableFeed = null;
        }
        if (executableFeed != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();

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

    private List<ExecutableFeed> getExecutableFeeds() {
        List<ExecutableFeed> executableFeedList = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_FEED +
                    " WHERE " + IotHubDataHandler.KEY_FEED_TYPE + " = '" + IotHubDataHandler.EXECUTABLE_FEED + "'";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long feedId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ID));
                executableFeedList.add(getExecutableFeed(feedId));
            }
            cursor.close();
            return executableFeedList;
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(feedId)});
            if (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ID));
                String type = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_TYPE));
                if (IotHubDataHandler.COMPOSED_FEED.equals(type)) {
                    feed = getComposedFeed(id);
                } else if (IotHubDataHandler.ATOMIC_FEED.equals(type)) {
                    feed = getAtomicFeed(id);
                } else if (IotHubDataHandler.EXECUTABLE_FEED.equals(type)) {
                    feed = getExecutableFeed(id);
                } else {
                    Log.e(TAG, "Uknown type of feed '" + type + "', must be (" +
                            IotHubDataHandler.ATOMIC_FEED + ", " +
                            IotHubDataHandler.COMPOSED_FEED + ", " +
                            IotHubDataHandler.EXECUTABLE_FEED + ")");
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(sql, new String[]{name});
            if (cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ID));
                String type = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_TYPE));
                if (IotHubDataHandler.COMPOSED_FEED.equals(type)) {
                    feed = getComposedFeed(id);
                } else if (IotHubDataHandler.ATOMIC_FEED.equals(type)) {
                    feed = getAtomicFeed(id);
                } else if (IotHubDataHandler.EXECUTABLE_FEED.equals(type)) {
                    feed = getExecutableFeed(id);
                } else {
                    Log.e(TAG, "Uknown type of feed '" + type + "', must be (" +
                            IotHubDataHandler.ATOMIC_FEED + ", " +
                            IotHubDataHandler.COMPOSED_FEED + ", " +
                            IotHubDataHandler.EXECUTABLE_FEED + ")");
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return feed;
    }

    @Override
    public List<Feed> getFeeds() {
        List<Feed> feeds = new ArrayList<>();
        feeds.addAll(getAtomicFeeds());
        feeds.addAll(getComposedFeeds());
        feeds.addAll(getExecutableFeeds());
        return feeds;
    }

    @Override
    public Feed deleteFeed(String name) {
        Feed feed;
        try {
            checkOpenness();
            feed = getFeed(name);
            if (feed == null) {
                Log.e(TAG, "No feed was found to delete with name " + name);
                return null;
            }
            int res = database.delete(IotHubDataHandler.TABLE_FEED,
                    IotHubDataHandler.KEY_FEED_NAME + " = ?", new String[]{name});
            if (res != 1) {
                Log.e(TAG, "Feed " + name + " was not deleted from the database");
            } else {
                Log.d(TAG, "Feed " + name + " was deleted from the database");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return feed;
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(feed.getId())});
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_ID));
                Date timestamp = new Date(cursor.getLong(
                        cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP)));
                JSONObject data = new JSONObject(cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_DATA)));
                entries.add(new FeedEntry(id, feed, timestamp, data));
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            if (cursor.moveToNext()) {
                long feedId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_FEED_ID));
                Date timestamp = new Date(cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP)));
                JSONObject data;
                try {
                    data = new JSONObject(cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEED_ENTRY_DATA)));
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
            cursor.close();
        } catch (IotHubDatabaseException e) {
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

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEED_ENTRY_FEED_ID, feed.getId());
            values.put(IotHubDataHandler.KEY_FEED_ENTRY_TIMESTAMP, new Date().getTime());
            values.put(IotHubDataHandler.KEY_FEED_ENTRY_DATA, data.toString());
            long insertId = database.insert(
                    IotHubDataHandler.TABLE_FEED_ENTRY,
                    null,
                    values);
            if (insertId != -1) {
                entry = getFeedEntry(insertId);
            }
        } catch (IotHubDatabaseException e) {
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
            int res = database.delete(IotHubDataHandler.TABLE_FEED_ENTRY,
                    IotHubDataHandler.KEY_FEED_ENTRY_ID + " = ? ",
                    new String[]{Long.toString(entry.getId())});
            if (res != 1) {
                Log.e(TAG, "could not delete entry " + entry.toString());
            }
        } catch (IotHubDatabaseException e) {
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
            database.beginTransaction();
            //First things first, insert the feed's values to the feed table
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_TYPE, IotHubDataHandler.JAVASCRIPT_PLUGIN);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME, serviceName);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME, packageName);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME, file == null ? null : file.getName());

            long insertId = database.insert(
                    IotHubDataHandler.TABLE_PLUGIN_INFO, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the plugin that was just added to the db");
                pluginInfo = getPluginInfo(insertId);
                if (pluginInfo == null) {
                    Log.e(TAG, "The plugin should not be null");
                }
                //Now I want to make some checks
                if (pluginInfo != null && !pluginInfo.isJavascript()) {
                    Log.e(TAG, "The plugin " + pluginInfo.getId() + " is not javascript");
                    pluginInfo = null;
                }
            } else {
                Log.e(TAG, "The insert of javascript plugin " + serviceName + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            pluginInfo = null;
        }
        if (pluginInfo != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
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
            database.beginTransaction();
            //First things first, insert the feed's values to the feed table
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_TYPE, IotHubDataHandler.NATIVE_PLUGIN);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME, serviceName);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME, packageName);
            values.put(IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME, file == null ? null : file.getName());
            long insertId = database.insert(
                    IotHubDataHandler.TABLE_PLUGIN_INFO, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the plugin that was just added to the db");
                pluginInfo = getPluginInfo(insertId);
                if (pluginInfo == null) {
                    Log.e(TAG, "The plugin should not be null");
                }
                //Now I want to make some checks
                if (!pluginInfo.isNative()) {
                    Log.e(TAG, "The plugin " + pluginInfo.getId() + " is not native");
                    pluginInfo = null;
                }
            } else {
                Log.e(TAG, "The insert of javascript plugin " + serviceName + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            pluginInfo = null;
        }
        if (pluginInfo != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
        return pluginInfo;
    }


    @Override
    public PluginInfo deletePlugin(long id) {
        PluginInfo pluginInfo = null;
        try {
            checkOpenness();
            database.beginTransaction();

            pluginInfo = getPluginInfo(id);
            if (pluginInfo == null) {
                Log.e(TAG, "No plugin was found to delete with id " + id);
                return null;
            }
            int res = database.delete(IotHubDataHandler.TABLE_PLUGIN_INFO,
                    IotHubDataHandler.KEY_PLUGIN_INFO_ID + " = ?",
                    new String[]{Long.toString(id)});
            if (res != 1) {
                Log.e(TAG, "Plugin " + id + " was not deleted from the database");
                pluginInfo = null;
            } else {
                Log.d(TAG, "Plugin " + id + " was deleted from the database");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            pluginInfo = null;
        }

        if (pluginInfo != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
        return pluginInfo;
    }

    @Override
    public List<PluginInfo> getJavascriptPlugins() {
        List<PluginInfo> pluginInfos = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_PLUGIN_INFO +
                    " WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + " = '" + IotHubDataHandler.JAVASCRIPT_PLUGIN + "'";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long pluginId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_ID));
                pluginInfos.add(getPluginInfo(pluginId));
            }
            cursor.close();
            return pluginInfos;
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<PluginInfo> getNativePlugins() {
        List<PluginInfo> pluginInfos = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_PLUGIN_INFO +
                    " WHERE " + IotHubDataHandler.KEY_PLUGIN_INFO_TYPE + " = '" + IotHubDataHandler.NATIVE_PLUGIN + "'";
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long pluginId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_ID));
                pluginInfos.add(getPluginInfo(pluginId));
            }
            cursor.close();
            return pluginInfos;
        } catch (IotHubDatabaseException e) {
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
        } else {
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
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(pluginId)});
            if (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_ID));
                String type = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_TYPE));
                String packageName = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_PACKAGE_NAME));
                String serviceName = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_SERVICE_NAME));
                String filename = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_PLUGIN_INFO_FILENAME));
                if (IotHubDataHandler.NATIVE_PLUGIN.equals(type)) {
                    pluginInfo = new PluginInfo(id, PluginInfo.Type.NATIVE, serviceName, packageName, filename);
                } else if (IotHubDataHandler.JAVASCRIPT_PLUGIN.equals(type)) {
                    pluginInfo = new PluginInfo(id, PluginInfo.Type.JAVASCRIPT, serviceName, packageName, filename);
                } else {
                    Log.e(TAG, "Uknown type of plugin '" + type + "', must be (" +
                            IotHubDataHandler.NATIVE_PLUGIN + ", " +
                            IotHubDataHandler.JAVASCRIPT_PLUGIN + ")");
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_ENABLER_NAME, name);
            values.put(IotHubDataHandler.KEY_ENABLER_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO, plugin.getId());
            values.put(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG, pluginInfoConfig);

            long insertId = database.insert(
                    IotHubDataHandler.TABLE_ENABLER, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the enabler that was just added to the db");
                enabler = getEnabler(insertId);
                if (enabler == null) {
                    Log.e(TAG, "The enabler should not be null");
                }
                //TODO maybe check that the plugins are the same
            } else {
                Log.e(TAG, "The insert of enabler " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            enabler = null;
        }
        if (enabler != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();

        return enabler;
    }

    @Override
    public Enabler deleteEnabler(Enabler enabler) {
        try {
            checkOpenness();
            int res = database.delete(IotHubDataHandler.TABLE_ENABLER,
                    IotHubDataHandler.KEY_ENABLER_ID + "=?",
                    new String[]{Long.toString(enabler.getId())});
            if (res != 1) {
                enabler = null;
            }
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            if (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_NAME));
                String metadata = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_METADATA));
                long pluginId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO));
                String pluginConfiguration = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG));
                enabler = new Enabler(id, name, metadata, getPluginInfo(pluginId), pluginConfiguration);
                List<Feature> features = getFeaturesForEnabler(enabler);
                for (Feature feature : features) {
                    enabler.addFeature(feature);
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(query, new String[]{name});
            if (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_ID));
                String metadata = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_METADATA));
                long pluginId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO));
                String pluginConfiguration = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG));
                enabler = new Enabler(id, name, metadata, getPluginInfo(pluginId), pluginConfiguration);
                List<Feature> features = getFeaturesForEnabler(enabler);
                for (Feature feature : features) {
                    enabler.addFeature(feature);
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return enabler;
    }

    private List<Feature> getFeaturesForEnabler(Enabler enabler) {
        List<Feature> features = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_FEATURE +
                    " where " + IotHubDataHandler.KEY_FEATURE_ENABLER_ID + "=?";
            Cursor cursor = database.rawQuery(sql, new String[]{Long.toString(enabler.getId())});
            while (cursor.moveToNext()) {
                long featureId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEATURE_ID));
                String name = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEATURE_NAME));
                String type = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_FEATURE_TYPE));
                boolean isFeed = cursor.getInt(cursor.getColumnIndex(IotHubDataHandler.KEY_FEATURE_IS_FEED)) == 1;
                Feature feature = new Feature(featureId, enabler, name, type);
                feature.setAtomicFeed(isFeed);
                features.add(feature);
                Log.d(TAG, "Got one feature from the db");
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return features;
    }

    @Override
    public List<Enabler> getEnablers() {
        List<Enabler> enablers = new ArrayList<>();
        try {
            checkOpenness();
            String sql = "select * from " + IotHubDataHandler.TABLE_ENABLER;
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long enablerId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_ENABLER_ID));
                enablers.add(getEnabler(enablerId));
            }
            cursor.close();
            return enablers;
        } catch (IotHubDatabaseException e) {
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

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_ENABLER_NAME, name);
            values.put(IotHubDataHandler.KEY_ENABLER_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_ENABLER_PLUGIN_INFO_CONFIG, pluginInfoConfig);
            String selection = IotHubDataHandler.KEY_ENABLER_ID + " LIKE ?";
            String[] selectionArgs = {String.valueOf(enabler.getId())};
            int count = database.update(
                    IotHubDataHandler.TABLE_ENABLER,
                    values,
                    selection,
                    selectionArgs);
            if (count == 1) {
                Enabler newEnabler = getEnabler(enabler.getId());
                return newEnabler;
            }
        } catch (IotHubDatabaseException e) {
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
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEATURE_ENABLER_ID, enabler.getId());
            values.put(IotHubDataHandler.KEY_FEATURE_NAME, name);
            values.put(IotHubDataHandler.KEY_FEATURE_TYPE, type);
            values.put(IotHubDataHandler.KEY_FEATURE_IS_FEED, 0);

            long insertId = database.insert(
                    IotHubDataHandler.TABLE_FEATURE, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the feature that was just added to the db");
                feature = getFeature(insertId);
                if (feature == null) {
                    Log.e(TAG, "The feature should not be null");
                }
            } else {
                Log.e(TAG, "The insert of feature " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            feature = null;
        }
        if (feature != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            if (cursor.moveToNext()) {
                long enablerId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_FEATURE_ENABLER_ID));
                Enabler enabler = getEnabler(enablerId);
                for (Feature ft : enabler.getFeatures()) {
                    if (ft.getId() == id) {
                        feature = ft;
                        break;
                    }
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_FEATURE_NAME, name);
            values.put(IotHubDataHandler.KEY_FEATURE_TYPE, type);
            values.put(IotHubDataHandler.KEY_FEATURE_IS_FEED, isFeed ? 1 : 0);


            String selection = IotHubDataHandler.KEY_FEATURE_ID + " LIKE ?";
            String[] selectionArgs = {String.valueOf(feature.getId())};
            int count = database.update(
                    IotHubDataHandler.TABLE_FEATURE,
                    values,
                    selection,
                    selectionArgs);
            if (count == 1) {
                return getFeature(feature.getId());
            }
        } catch (IotHubDatabaseException e) {
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
            int res = database.delete(IotHubDataHandler.TABLE_FEATURE,
                    IotHubDataHandler.KEY_FEATURE_ENABLER_ID + "=?",
                    new String[]{Long.toString(enabler.getId())});
            if (res == features.size()) {
                return features;
            }
        } catch (IotHubDatabaseException e) {
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
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_SERVICE_NAME, name);
            values.put(IotHubDataHandler.KEY_SERVICE_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_SERVICE_SERVICE_INFO, serviceInfo.getId());
            values.put(IotHubDataHandler.KEY_SERVICE_CONFIG, config);
            values.put(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP, bootAtStartup ? 1 : 0);

            long insertId = database.insert(
                    IotHubDataHandler.TABLE_SERVICE, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the service " + insertId + " that was just added to the db");
                service = getService(insertId);
                if (service == null) {
                    Log.e(TAG, "The service " + name + " should not be null");
                }
            } else {
                Log.e(TAG, "The insert of service " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            service = null;
        }
        if (service != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
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
            database.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_SERVICE_INFO_SERVICE_NAME, name);
            values.put(IotHubDataHandler.KEY_SERVICE_INFO_FILENAME, file.getName());

            long insertId = database.insert(
                    IotHubDataHandler.TABLE_SERVICE_INFO, null, values);
            if (insertId != -1) {
                //At point we should have everything set so it is time to retrieve the plugin from the database
                //Log.d(TAG, "Now i will try to collect the service info " + name + " that was just added to the db");
                service = getServiceInfo(insertId);
                if (service == null) {
                    Log.e(TAG, "The service info " + name + " should not be null");
                }
            } else {
                Log.e(TAG, "The insert of service info " + name + " did not work");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            service = null;
        }
        if (service != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
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
            int res = database.delete(IotHubDataHandler.TABLE_SERVICE,
                    IotHubDataHandler.KEY_SERVICE_ID + "=?",
                    new String[]{Long.toString(service.getId())});
            if (res != 1) {
                Log.i(TAG, "Service " + service.getId() + " delete from db");
            } else {
                Log.w(TAG, "Delete service " + service.getId() + " has not affected any row");
                service = null;
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return service;
    }

    @Override
    public ServiceInfo deleteServiceInfo(long id) {
        ServiceInfo serviceInfo;
        try {
            checkOpenness();
            database.beginTransaction();

            serviceInfo = getServiceInfo(id);
            if (serviceInfo == null) {
                Log.e(TAG, "No service plugin was found to delete with id " + id);
                return null;
            }

            int res = database.delete(IotHubDataHandler.TABLE_SERVICE_INFO,
                    IotHubDataHandler.KEY_SERVICE_INFO_ID + " = ?",
                    new String[]{Long.toString(id)});
            if (res != 1) {
                Log.e(TAG, "Service plugin " + id + " was not deleted from the database");
                serviceInfo = null;
            } else {
                Log.d(TAG, "Service plugin " + id + " was deleted from the database");
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            serviceInfo = null;
        }

        if (serviceInfo != null) {
            database.setTransactionSuccessful();
        }
        database.endTransaction();
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
            Cursor cursor = database.rawQuery(query, new String[]{name});
            if (cursor.moveToNext()) {
                long serviceInfoId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_SERVICE_INFO));
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_ID));
                String metadata = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_METADATA));
                String config = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_CONFIG));
                boolean bootOnStartup = cursor.getInt(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP)) == 1;
                ServiceInfo serviceInfo = getServiceInfo(serviceInfoId);
                if (serviceInfo != null) {
                    service = new Service(id, serviceInfo,
                            name, metadata, config, bootOnStartup);
                } else {
                    Log.e(TAG, "ServiceInfo " + serviceInfoId + " should not be null");
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            if (cursor.moveToFirst()) {
                long serviceInfoId = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_SERVICE_INFO));
                String name = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_NAME));
                String metadata = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_METADATA));
                String config = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_CONFIG));
                boolean bootOnStartup = cursor.getInt(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP)) == 1;
                ServiceInfo serviceInfo = getServiceInfo(serviceInfoId);
                if (serviceInfo != null) {
                    service = new Service(id, serviceInfo,
                            name, metadata, config, bootOnStartup);
                } else {
                    Log.e(TAG, "ServiceInfo " + serviceInfoId + " should not be null");
                }
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(query, new String[]{Long.toString(id)});
            if (cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_INFO_SERVICE_NAME));
                String filename = cursor.getString(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_INFO_FILENAME));
                serviceInfo = new ServiceInfo(id, ServiceInfo.Type.JAVASCRIPT, name, filename);
            }
            cursor.close();
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_INFO_ID));
                serviceInfos.add(getServiceInfo(id));
            }
            cursor.close();
            return serviceInfos;
        } catch (IotHubDatabaseException e) {
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
            Cursor cursor = database.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(IotHubDataHandler.KEY_SERVICE_ID));
                services.add(getService(id));
            }
            cursor.close();
            return services;
        } catch (IotHubDatabaseException e) {
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

            ContentValues values = new ContentValues();
            values.put(IotHubDataHandler.KEY_SERVICE_NAME, name);
            values.put(IotHubDataHandler.KEY_SERVICE_METADATA, metadata);
            values.put(IotHubDataHandler.KEY_SERVICE_CONFIG, config);
            values.put(IotHubDataHandler.KEY_SERVICE_BOOT_AT_STARTUP, bootAtStartup ? 1 : 0);

            String selection = IotHubDataHandler.KEY_SERVICE_ID + " LIKE ?";
            String[] selectionArgs = {String.valueOf(service.getId())};
            int count = database.update(
                    IotHubDataHandler.TABLE_SERVICE,
                    values,
                    selection,
                    selectionArgs);
            if (count == 1) {
                Service newService = getService(service.getId());
                return newService;
            }
        } catch (IotHubDatabaseException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
