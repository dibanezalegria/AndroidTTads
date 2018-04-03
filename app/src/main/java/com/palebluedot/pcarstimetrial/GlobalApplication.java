package com.palebluedot.pcarstimetrial;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by Daniel Ibanez on 2016-03-09.
 */

/**
 * GlobalApplication is available from start to finish
 * SQLiteDatabase and DBHelper are available from all classes at all times
 *
 */
public class GlobalApplication extends Application {

    private static final String TAG = "DEBUG";

    private static SQLiteDatabase database;
    private DBHelper mDBHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "GlobalApplication -> onCreate()");
        mDBHelper = DBHelper.getInstance(getApplicationContext());
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                database = mDBHelper.getWritableDatabase();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                // Log database content
                Cursor cursor = database.query(DBTable.TABLE_NAME, null, null, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    Log.d(TAG, cursor.getCount() + " items read from " + DBHelper.DATABASE_NAME +
                            "->" + DBTable.TABLE_NAME);
                    cursor.close();

                } else {
                    Log.d(TAG, "Database is empty");
                }
            }
        }.execute();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "GlobalApplication -> onTerminate()");
        database.close();
    }

    public static SQLiteDatabase getDatabase() {
        return database;
    }
}
