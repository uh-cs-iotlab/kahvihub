/*
 * fi.helsinki.cs.iot.hub.database.IotHubDataHandler
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

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
 *
 */
public abstract class IotHubDataHandler {

	private String databaseName;
	private int databaseVersion;
	private boolean debugMode;

	public static final String TABLE_PLUGIN_INFO = "pluginInfo";
	public static final String KEY_PLUGIN_INFO_ID = "pluginInfoId";
	public static final String KEY_PLUGIN_INFO_TYPE = "pluginInfoType";
	public static final String KEY_PLUGIN_INFO_SERVICE_NAME = "pluginInfoServiceName";
	public static final String KEY_PLUGIN_INFO_PACKAGE_NAME = "pluginInfoPackageName";
	public static final String KEY_PLUGIN_INFO_FILENAME = "pluginInfoFilename";

	public static final String TABLE_ENABLER = "enabler";
	public static final String KEY_ENABLER_ID = "enablerId";
	public static final String KEY_ENABLER_NAME = "enablerName";
	public static final String KEY_ENABLER_PLUGIN_INFO = KEY_PLUGIN_INFO_ID;
	public static final String KEY_ENABLER_PLUGIN_INFO_CONFIG = "pluginInfoConfig";
	public static final String KEY_ENABLER_METADATA = "enablerMetadata";

	public static final String TABLE_FEATURE = "feature";
	public static final String KEY_FEATURE_ID = "featureId";
	public static final String KEY_FEATURE_ENABLER_ID = KEY_ENABLER_ID;
	public static final String KEY_FEATURE_NAME = "featureName";
	public static final String KEY_FEATURE_TYPE = "featureType";
	public static final String KEY_FEATURE_IS_FEED = "featureIsFeed";

	public static final String TABLE_FEED = "feed";
	public static final String KEY_FEED_ID = "feedId";
	public static final String KEY_FEED_NAME = "feedName";
	public static final String KEY_FEED_METADATA = "feedMetadata";
	public static final String KEY_FEED_TYPE = "feedType";
	public static final String KEY_FEED_STORAGE = "feedStorage";
	public static final String KEY_FEED_READABLE = "feedIsReadable";
	public static final String KEY_FEED_WRITABLE = "feedIsWritable";

	public static final String TABLE_FIELD = "field";
	public static final String KEY_FIELD_ID = "fieldId";
	public static final String KEY_FIELD_NAME = "fieldName";
	public static final String KEY_FIELD_METADATA = "fieldMetadata";
	public static final String KEY_FIELD_TYPE = "fieldType";
	public static final String KEY_FIELD_OPTIONAL = "fieldOptional";
	public static final String KEY_FIELD_FEED_ID = KEY_FEED_ID;

	//This one is used only for atomic feeds
	public static final String TABLE_FEED_FEATURE_REL = "feedFeatureRel";
	public static final String KEY_FEED_FEATURE_REL_FEED_ID = KEY_FEED_ID;
	public static final String KEY_FEED_FEATURE_REL_FEATURE_ID = KEY_FEATURE_ID;

	public static final String TABLE_FEED_ENTRY = "feedEntry";
	public static final String KEY_FEED_ENTRY_ID = "feedEntryId";
	public static final String KEY_FEED_ENTRY_FEED_ID = KEY_FEED_ID;
	public static final String KEY_FEED_ENTRY_TIMESTAMP = "feedEntryTimestamp";
	public static final String KEY_FEED_ENTRY_DATA = "feedEntryData";

	public static final String TABLE_KEYWORD = "Keyword";
	public static final String KEY_KEYWORD_ID = "keywordId";
	public static final String KEY_KEYWORD_VALUE = "keywordValue";

	public static final String TABLE_KEYWORD_FEED_REL = "KeywordFeedRel";
	public static final String KEY_KEYWORD_FEED_KEYWORD_ID = KEY_KEYWORD_ID;
	public static final String KEY_KEYWORD_FEED_FEED_ID = KEY_FEED_ID;

	public static final String TABLE_KEYWORD_FIELD_REL = "KeywordFieldRel";
	public static final String KEY_KEYWORD_FIELD_KEYWORD_ID = KEY_KEYWORD_ID;
	public static final String KEY_KEYWORD_FIELD_FIELD_ID = KEY_FIELD_ID;

	public static final String TABLE_SERVICE_INFO = "serviceInfo";
	public static final String KEY_SERVICE_INFO_ID = "serviceInfoId";
	public static final String KEY_SERVICE_INFO_SERVICE_NAME = "serviceInfoServiceName";
	public static final String KEY_SERVICE_INFO_FILENAME = "serviceInfoFilename";

	public static final String TABLE_SERVICE= "service";
	public static final String KEY_SERVICE_ID = "serviceId";
	public static final String KEY_SERVICE_NAME = "serviceName";
	public static final String KEY_SERVICE_SERVICE_INFO = KEY_SERVICE_INFO_ID;
	public static final String KEY_SERVICE_CONFIG = "serviceConfig";
	public static final String KEY_SERVICE_METADATA = "serviceMetadata";
	public static final String KEY_SERVICE_BOOT_AT_STARTUP = "serviceBootAtStartup";

	public static final String COMPOSED_FEED = "composed";
	public static final String ATOMIC_FEED = "atomic";
	public static final String EXECUTABLE_FEED = "executable";

	public static final String NATIVE_PLUGIN = "native";
	public static final String JAVASCRIPT_PLUGIN = "javascript";

	private static final String CREATE_PLUGIN_INFO_TABLE =
			"CREATE TABLE " + TABLE_PLUGIN_INFO + " (" +
					KEY_PLUGIN_INFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					KEY_PLUGIN_INFO_TYPE + " TEXT NOT NULL CHECK (" + 
					KEY_PLUGIN_INFO_TYPE + " IN ('" + 
					NATIVE_PLUGIN + "','" + JAVASCRIPT_PLUGIN + "')), " +
					KEY_PLUGIN_INFO_SERVICE_NAME + " TEXT NOT NULL, "+
					KEY_PLUGIN_INFO_PACKAGE_NAME + " TEXT, "+
					KEY_PLUGIN_INFO_FILENAME + " TEXT);";

	private static final String CREATE_ENABLER_TABLE =
			"CREATE TABLE " + TABLE_ENABLER + " (" +
					KEY_ENABLER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					KEY_ENABLER_NAME + " TEXT UNIQUE NOT NULL, " +
					KEY_ENABLER_PLUGIN_INFO + " INTEGER NOT NULL, " +
					KEY_ENABLER_PLUGIN_INFO_CONFIG + " TEXT, " +
					KEY_ENABLER_METADATA + " TEXT, "
					+ "FOREIGN KEY(" + KEY_ENABLER_PLUGIN_INFO + ") REFERENCES "
					+ TABLE_PLUGIN_INFO + "(" + KEY_PLUGIN_INFO_ID + ") ON DELETE CASCADE"
					+ ");";

	private static final String CREATE_FEATURE_TABLE =
			"CREATE TABLE " + TABLE_FEATURE + " (" +
					KEY_FEATURE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					KEY_FEATURE_ENABLER_ID + " INTEGER NOT NULL, " +
					KEY_FEATURE_NAME + " TEXT NOT NULL, " +
					KEY_FEATURE_TYPE + " TEXT NOT NULL, " +
					KEY_FEATURE_IS_FEED + " INTEGER NOT NULL, "
					+ "FOREIGN KEY(" + KEY_FEATURE_ENABLER_ID + ") REFERENCES "
					+ TABLE_ENABLER + "(" + KEY_ENABLER_ID + ") ON DELETE CASCADE"
					+ ");";

	private static final String CREATE_FEED_TABLE = "CREATE TABLE " + TABLE_FEED + "("
			+ KEY_FEED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_FEED_NAME + " TEXT UNIQUE NOT NULL, "
			+ KEY_FEED_METADATA + " TEXT, "
			+ KEY_FEED_TYPE + " TEXT NOT NULL CHECK (" + KEY_FEED_TYPE + " IN ('"
			+ ATOMIC_FEED + "', '" + COMPOSED_FEED + "', '" + EXECUTABLE_FEED + "')), "
			+ KEY_FEED_STORAGE + " INTEGER NOT NULL, "
			+ KEY_FEED_READABLE + " INTEGER NOT NULL, "
			+ KEY_FEED_WRITABLE + " INTEGER NOT NULL);";

	private static final String CREATE_FEED_FEATURE_REL_TABLE = "CREATE TABLE " + TABLE_FEED_FEATURE_REL + "("
			+ KEY_FEED_FEATURE_REL_FEED_ID + " INTEGER NOT NULL, "
			+ KEY_FEED_FEATURE_REL_FEATURE_ID + " INTEGER NOT NULL, "
			+ "FOREIGN KEY(" + KEY_FEED_FEATURE_REL_FEED_ID + ") REFERENCES "
			+ TABLE_FEED + "(" + KEY_FEED_ID + ") ON DELETE CASCADE, "
			+ "FOREIGN KEY(" + KEY_FEED_FEATURE_REL_FEATURE_ID + ") REFERENCES "
			+ TABLE_FEATURE + "(" + KEY_FEATURE_ID + ") ON DELETE CASCADE, "
			+ "UNIQUE (" + KEY_FEED_FEATURE_REL_FEED_ID + "," + KEY_FEED_FEATURE_REL_FEATURE_ID + "));";

	private static final String CREATE_FIELD_TABLE = "CREATE TABLE " + TABLE_FIELD + " ("
			+ KEY_FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_FIELD_NAME + " TEXT NOT NULL, "
			+ KEY_FIELD_METADATA + " TEXT, "
			+ KEY_FIELD_TYPE + " TEXT NOT NULL, "
			+ KEY_FIELD_OPTIONAL + " INTEGER NOT NULL, "
			+ KEY_FIELD_FEED_ID + " INTEGER NOT NULL, "
			+ "FOREIGN KEY(" + KEY_FIELD_FEED_ID + ") REFERENCES "
			+ TABLE_FEED + "(" + KEY_FEED_ID + ") ON DELETE CASCADE, "
			+ "UNIQUE (" + KEY_FIELD_FEED_ID + "," + KEY_FIELD_NAME + ")"
			+ ");";

	private static final String CREATE_FEED_ENTRY_TABLE = "CREATE TABLE " + TABLE_FEED_ENTRY + " ("
			+ KEY_FEED_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_FEED_ENTRY_FEED_ID + " INTEGER NOT NULL, "
			+ KEY_FEED_ENTRY_TIMESTAMP + " INTEGER, "
			+ KEY_FEED_ENTRY_DATA + " TEXT NOT NULL, "
			+ "FOREIGN KEY(" + KEY_FEED_ENTRY_FEED_ID + ") REFERENCES "
			+ TABLE_FEED + "(" + KEY_FEED_ID + ") ON DELETE CASCADE"
			+ ");";

	private static final String CREATE_KEYWORD_TABLE = "CREATE TABLE " + TABLE_KEYWORD + " ("
			+ KEY_KEYWORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ KEY_KEYWORD_VALUE + " TEXT NOT NULL UNIQUE);";

	private static final String CREATE_KEYWORD_FEED_REL_TABLE = "CREATE TABLE " + TABLE_KEYWORD_FEED_REL + " ("
			+ KEY_KEYWORD_FEED_KEYWORD_ID + " INTEGER NOT NULL, "
			+ KEY_KEYWORD_FEED_FEED_ID + " INTEGER NOT NULL, "
			+ "FOREIGN KEY(" + KEY_KEYWORD_FEED_KEYWORD_ID + ") REFERENCES "
			+ TABLE_KEYWORD + "(" + KEY_KEYWORD_ID + ") ON DELETE CASCADE, "
			+ "FOREIGN KEY(" + KEY_KEYWORD_FEED_FEED_ID + ") REFERENCES "
			+ TABLE_FEED + "(" + KEY_FEED_ID + ") ON DELETE CASCADE, "
			+ "UNIQUE (" + KEY_KEYWORD_FEED_FEED_ID + "," + KEY_KEYWORD_FEED_KEYWORD_ID + "));";

	private static final String CREATE_KEYWORD_FIELD_REL_TABLE = "CREATE TABLE " + TABLE_KEYWORD_FIELD_REL + " ("
			+ KEY_KEYWORD_FIELD_KEYWORD_ID + " INTEGER NOT NULL, "
			+ KEY_KEYWORD_FIELD_FIELD_ID + " INTEGER NOT NULL, "
			+ "FOREIGN KEY(" + KEY_KEYWORD_FEED_KEYWORD_ID + ") REFERENCES "
			+ TABLE_KEYWORD + "(" + KEY_KEYWORD_ID + ") ON DELETE CASCADE, "
			+ "FOREIGN KEY(" + KEY_KEYWORD_FIELD_FIELD_ID + ") REFERENCES "
			+ TABLE_FIELD + "(" + KEY_FIELD_ID + ") ON DELETE CASCADE, "
			+ "UNIQUE (" + KEY_KEYWORD_FIELD_FIELD_ID + "," + KEY_KEYWORD_FEED_KEYWORD_ID + "));";

	private static final String CREATE_SERVICE_INFO_TABLE =
			"CREATE TABLE " + TABLE_SERVICE_INFO + " (" +
					KEY_SERVICE_INFO_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					KEY_SERVICE_INFO_SERVICE_NAME + " TEXT NOT NULL UNIQUE, "+
					KEY_SERVICE_INFO_FILENAME + " TEXT);";

	private static final String CREATE_SERVICE_TABLE =
			"CREATE TABLE " + TABLE_SERVICE + " (" +
					KEY_SERVICE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					KEY_SERVICE_NAME + " TEXT UNIQUE NOT NULL UNIQUE, " +
					KEY_SERVICE_SERVICE_INFO + " INTEGER NOT NULL, " +
					KEY_SERVICE_CONFIG + " TEXT, " +
					KEY_SERVICE_METADATA + " TEXT, "
					+ KEY_SERVICE_BOOT_AT_STARTUP + " INTEGER NOT NULL, "
					+ "FOREIGN KEY(" + KEY_SERVICE_SERVICE_INFO + ") REFERENCES "
					+ TABLE_SERVICE_INFO + "(" + KEY_SERVICE_INFO_ID + ") ON DELETE CASCADE"
					+ ");";

	protected IotHubDataHandler(String databaseName, int databaseVersion, boolean debugMode) {
		this.databaseName = databaseName;
		this.databaseVersion = databaseVersion;
		this.debugMode = debugMode;
	}

	private void createTables(IotHubDatabase db) throws IotHubDatabaseException {
		db.executeUpdate(CREATE_PLUGIN_INFO_TABLE);
		db.executeUpdate(CREATE_ENABLER_TABLE);
		db.executeUpdate(CREATE_FEATURE_TABLE);
		db.executeUpdate(CREATE_FEED_TABLE);
		db.executeUpdate(CREATE_FIELD_TABLE);
		db.executeUpdate(CREATE_FEED_FEATURE_REL_TABLE);
		db.executeUpdate(CREATE_FEED_ENTRY_TABLE);
		db.executeUpdate(CREATE_KEYWORD_TABLE);
		db.executeUpdate(CREATE_KEYWORD_FEED_REL_TABLE);
		db.executeUpdate(CREATE_KEYWORD_FIELD_REL_TABLE);
		db.executeUpdate(CREATE_SERVICE_INFO_TABLE);
		db.executeUpdate(CREATE_SERVICE_TABLE);
	}

	private void dropTables(IotHubDatabase db) throws IotHubDatabaseException {
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_SERVICE);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_SERVICE_INFO);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_KEYWORD_FIELD_REL);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_KEYWORD_FEED_REL);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_KEYWORD_FIELD_REL);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_KEYWORD);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_FEED_ENTRY);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_FEED_FEATURE_REL);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_FIELD);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_FEED);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_FEATURE);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_ENABLER);
		db.executeUpdate("DROP TABLE IF EXISTS " + TABLE_PLUGIN_INFO);
	}

	public void onCreate(IotHubDatabase db) throws IotHubDatabaseException {
		createTables(db);
	}

	public void onUpgrade(IotHubDatabase db, int oldVersion, int newVersion) throws IotHubDatabaseException {
		dropTables(db);
		createTables(db);
	}

	public void onOpen(IotHubDatabase db) throws IotHubDatabaseException {
		if (debugMode) {
			dropTables(db);
			createTables(db);
		}
		db.enableForeignKeyConstraints();
	}

	public abstract IotHubDatabase openDatabase() throws IotHubDatabaseException;
	public abstract void closeDatabase() throws IotHubDatabaseException;

	protected String getDatabaseName() {
		return databaseName;
	}

	protected int getDatabaseVersion() {
		return databaseVersion;
	}

	protected boolean isDebugMode() {
		return debugMode;
	}

}
