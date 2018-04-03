package com.palebluedot.pcarstimetrial;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Daniel Ibanez on 2016-02-28.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DEBUG";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PCarsTTrial.db";

    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE " +
            DBTable.TABLE_NAME + " (" +
            DBTable._ID + " INTEGER PRIMARY KEY," +
            DBTable.COLUMN_TRACK + " TEXT," +
            DBTable.COLUMN_CAR + " TEXT," +
            DBTable.COLUMN_CLASS + " TEXT," +
            DBTable.COLUMN_LAPTIME + " REAL," +
            DBTable.COLUMN_SECTOR1 + " REAL," +
            DBTable.COLUMN_SECTOR2 + " REAL," +
            DBTable.COLUMN_SECTOR3 + " REAL)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + DBTable.TABLE_NAME;

    private static DBHelper sInstance;

    /**
     * Constructor should be private to prevent direct instantiation.
     * Call getInstance() instead.
     *
     * @param context
     */
    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // singleton
    public static synchronized DBHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DBHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        Log.d(TAG, "DBHelper: Database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

}




