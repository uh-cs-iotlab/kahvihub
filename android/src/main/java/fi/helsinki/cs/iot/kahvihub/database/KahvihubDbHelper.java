package fi.helsinki.cs.iot.kahvihub.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by mineraud on 16.10.2015.
 */
public class KahvihubDbHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "kahvihub.db";
    static final int DATABASE_VERSION = 1;
    static final boolean debugMode = true;

    public KahvihubDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
