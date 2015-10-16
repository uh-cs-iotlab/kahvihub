package fi.helsinki.cs.iot.kahvihub.database;

import android.content.Context;

import fi.helsinki.cs.iot.hub.database.IotHubDataHandler;
import fi.helsinki.cs.iot.hub.database.IotHubDatabase;
import fi.helsinki.cs.iot.hub.database.IotHubDatabaseException;

/**
 * Created by mineraud on 16.10.2015.
 */
public class KahvihubDataHandler extends IotHubDataHandler {

    private KahvihubDatabase database;
    private KahvihubDbHelper helper;

    public KahvihubDataHandler(Context context) {
        super(KahvihubDbHelper.DATABASE_NAME, KahvihubDbHelper.DATABASE_VERSION, KahvihubDbHelper.debugMode);
        this.database = null;
        this.helper = new KahvihubDbHelper(context);
    }

    @Override
    public IotHubDatabase openDatabase() throws IotHubDatabaseException {
        if (database == null) {
            database = new KahvihubDatabase(helper);
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
