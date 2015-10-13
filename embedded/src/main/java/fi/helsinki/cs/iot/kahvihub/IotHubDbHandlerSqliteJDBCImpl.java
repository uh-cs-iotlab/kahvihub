/*
 * fi.helsinki.cs.iot.kahvihub.IotHubDbHandlerSqliteJDBCImpl
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
package fi.helsinki.cs.iot.kahvihub;

import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDatabase;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;

/**
 * 
 * @author Julien Mineraud <julien.mineraud@cs.helsinki.fi>
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
