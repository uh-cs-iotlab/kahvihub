/**
 * 
 */
package fi.helsinki.cs.iot.kahvihub;

import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDatabase;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;

/**
 * @author mineraud
 *
 */
public class IotHubDbHandlerSqliteJDBCImpl extends IotHubDataHandler {
	
	private IotHubDatabaseSqliteJDBCImpl database;
	
	public IotHubDbHandlerSqliteJDBCImpl(String databaseName, int databaseVersion,
			boolean debugMode) {
		super(databaseName, databaseVersion, debugMode);
		this.database = null;
	}

	@Override
	public IotHubDatabase openDatabase() throws IotHubDatabaseException{
		if (database == null) {
			database = new IotHubDatabaseSqliteJDBCImpl(getDatabaseName());
		}
		if (!database.isOpen()) {
			database.open();
			//TODO I would need to check if the database is present or not
			if (database.isNew()) {
				onCreate(database);
			}
			else if (database.isUpgraded()) {
				super.onUpgrade(database, 0, 1);
			}
			else {
				onOpen(database);
			}
		}
		//TODO make some checks if the database is opened and writable
		return database;
	}

	@Override
	public void closeDatabase() throws IotHubDatabaseException {
		if (database != null && database.isOpen()) {
			database.close();
		}
		database = null;
	}

}
