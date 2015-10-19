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
    private static final String DATABASE_NAME = "kahvihub.db";
    private static final int DATABASE_VERSION = 1;
    private static final boolean DEBUG_MODE = true;
    private final Context context;

    public KahvihubDataHandler(Context context) {
        super(DATABASE_NAME, DATABASE_VERSION, DEBUG_MODE);
        this.database = null;
        this.context = context;
    }

    @Override
    public IotHubDatabase openDatabase() throws IotHubDatabaseException {
        if (database == null) {
            database = new KahvihubDatabase(context, DATABASE_NAME, DATABASE_VERSION);
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
