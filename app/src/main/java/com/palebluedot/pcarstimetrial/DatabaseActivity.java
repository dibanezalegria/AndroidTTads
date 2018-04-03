package com.palebluedot.pcarstimetrial;

import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Daniel Ibanez on 2016-03-09.
 */
public class DatabaseActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, Spinner.OnItemSelectedListener {
    private static final String TAG = "DEBUG";

    private Spinner trackSpinner, carSpinner, sortSpinner;
    private Button backButton;
    private ListView listView;
    private SQLiteDatabase database;
    private Cursor cursor;
    private static DatabaseAdapter adapter;
    private boolean permissionGranted;  // permission to read/write to external storage api 23+

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);
        Log.d(TAG, "DatabaseActivity.onCreate()");

        // get parameter from intent from MainActivity
        permissionGranted = getIntent().getExtras().getBoolean("permissionGranted");

        // keeps screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // find toolbar view inside the activity layout
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // set toolbar to act as the ActionBar for this activity window
        setSupportActionBar(toolbar);

        // database
        database = GlobalApplication.getDatabase();
        Log.d(TAG, "Database null? " + (database == null));

        // widgets
        listView = (ListView) findViewById(R.id.database_list_view);
        listView.setOnItemClickListener(this);

        backButton = (Button) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // spinners
        trackSpinner = (Spinner) findViewById(R.id.track_spinner);
        trackSpinner.setOnItemSelectedListener(this);
        fillTrackSpinner();
        carSpinner = (Spinner) findViewById(R.id.car_spinner);
        carSpinner.setOnItemSelectedListener(this);
        fillCarSpinner();
        sortSpinner = (Spinner) findViewById(R.id.laptime_spinner);
        sortSpinner.setOnItemSelectedListener(this);
        fillSortSpinner();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "DatabaseActivity.onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "DatabaseActivity.onResume()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu -> permissionGranted = " + permissionGranted);
        if (permissionGranted) {
            getMenuInflater().inflate(R.menu.menu_database, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_lite_database, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.backup_database_action:
                if (adapter.isEmpty()) {
                    showDialog("Backup database", "Database is empty.");
                    return true;
                }
                try {
                    backupDB();
                    showDialog("Backup database", "Database backed up successfully. " + "\n\n" +
                            "Now you can uninstall/update PCarsTT without losing your lap times. " +
                            "Do not forget to use the 'restore database' menu option after " +
                            "version updates or fresh installations.");
                } catch (IOException e) {
                    showDialog("Backup database", "An error has occurred. Unable to back up database");
                    e.printStackTrace();
                }
                return true;
            case R.id.restore_database_action:
                restoreDB();
                return true;
            case R.id.clear_database_action:
                if (adapter.isEmpty()) {
                    showDialog("Backup database", "Database is empty.");
                } else {
                    clearDB();
                }
                return true;
            case R.id.about_database:
                // info dialog
                showDialog("About your database", "Here you will find all your record laps per track/car. " +
                        "There is not need to backup/restore the database unless you are planning " +
                        "to update or re-install the app." + "\n\n" +
                        "PCarsTT saves lap times to the database automatically, as long as the game has informed it " +
                        "about the track/car used in the actual session." + "\n\n" + "Be patient, it can " +
                        "take a few laps sometimes :)"
                );
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Copy database items to file on external storage
     */
    private void backupDB() throws IOException {
        File dbFile = new File(database.getPath());
        FileInputStream fis = new FileInputStream(dbFile);
        String outputFileName = Environment.getExternalStorageDirectory() + "/db_copy.db";
        FileOutputStream fos = new FileOutputStream(outputFileName);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {   // IOException
            fos.write(buffer, 0, length);
        }
        fos.flush();
        fos.close();
        fis.close();
        Log.d(TAG, "saveTofile Path: " + outputFileName);
    }


    /**
     * Copy items from file on external storage to database
     */
    private void restoreDB() {
        // check if file exists
        String sourcePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/db_copy.db";
        final File source = new File(sourcePath);
        if (source.exists()) {
            Log.d(TAG, "File exists: " + source.getAbsolutePath());
            // restore db will remove current items in database
            AlertDialog.Builder builder = new AlertDialog.Builder(DatabaseActivity.this);
            builder.setTitle("Restore database")
                    .setMessage("Records in backup file will replace all items in current database.")
                    .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                File destination = new File(database.getPath());
                                FileInputStream fis = new FileInputStream(source);
                                FileOutputStream fos = new FileOutputStream(destination);
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = fis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, length);
                                }
                                fos.flush();
                                fis.close();
                                fos.close();
                            } catch (IOException e) {
                                showDialog("Restore database", "Problem restoring backup file.");
                                e.printStackTrace();
                            }
                            updateListView();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "No, do not delete it");

                        }
                    });
            builder.create().show();

        } else {
            showDialog("Restore database", "Backup file has not been found.");
            Log.d(TAG, "File does not exist: " + source.getAbsolutePath());
        }
    }

    /**
     * Delete all items from database
     */
    private void clearDB() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DatabaseActivity.this);
        builder.setTitle("Clear database")
                .setMessage("This will remove all the items from the database. ")
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        database.delete(DBTable.TABLE_NAME, null, null);
                        adapter.clear();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "No, do not clear database");

                    }
                });
        builder.create().show();
    }

    /**
     * Shows dialog with given title and message
     *
     * @param title
     * @param message
     */
    private void showDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(DatabaseActivity.this).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * Query database
     */
    private void queryToCursor() {
        String track = (String) trackSpinner.getSelectedItem();
        String car = (String) carSpinner.getSelectedItem();
        String sort = (String) sortSpinner.getSelectedItem();

        String selection = null;
        ArrayList<String> argsList = new ArrayList<>();

        if (track != null && !track.equals("All tracks")) {
            selection = DBTable.COLUMN_TRACK + " LIKE ?";
            argsList.add(track);
        }

        if (car != null && !car.equals("All cars")) {
            if (selection != null) {
                selection += " AND " + DBTable.COLUMN_CAR + " LIKE ?";
            } else {
                selection = DBTable.COLUMN_CAR + " LIKE ?";
            }
            argsList.add(car);
        }

        String[] args = null;
        if (argsList.size() > 0) {
            args = argsList.toArray(new String[argsList.size()]);
        }

        String sortOrder = DBTable.COLUMN_LAPTIME + " " + sort;
        cursor = database.query(DBTable.TABLE_NAME,
                null, selection, args, null, null, sortOrder);
    }

    /**
     * Reads database and creates DatabaseAdapter for an ArrayList
     */
    public void updateListView() {
        // load cursor with all rows in database sorted by track
        queryToCursor();
        // move rows from cursor to ArrayList
        ArrayList<FastLap> lapList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                FastLap fLap = new FastLap();
                fLap.setTrack(cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_TRACK)));
                fLap.setCar(cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_CAR)));
                fLap.setLaptime(0, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME)));
                fLap.setLaptime(1, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR1)));
                fLap.setLaptime(2, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR2)));
                fLap.setLaptime(3, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR3)));
                lapList.add(fLap);
            } while (cursor.moveToNext());
        }

        // adapter
        adapter = new DatabaseAdapter(getApplicationContext(), lapList);
        listView.setAdapter(adapter);
    }

    /**
     * Populate spinner with all tracks
     */
    private void fillTrackSpinner() {
        // query database
        cursor = database.query(DBTable.TABLE_NAME,
                null, null, null, null, null, DBTable.COLUMN_TRACK + " ASC");
        // create set of tracks
        Set<String> trackSet = new TreeSet<>();
        trackSet.add("All tracks");
        // cursor gets updated in updateListView()
        if (cursor.moveToFirst()) {
            do {
                trackSet.add(cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_TRACK)));
            } while (cursor.moveToNext());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                R.layout.custom_simple_spinner_item_db, trackSet.toArray(new String[trackSet.size()]));
        spinnerArrayAdapter.setDropDownViewResource(R.layout.custom_simple_spinner_dropdown_item_db);
        trackSpinner.setAdapter(spinnerArrayAdapter);
    }

    /**
     * Populate spinner with all cars
     */
    private void fillCarSpinner() {
        // query database
        cursor = database.query(DBTable.TABLE_NAME,
                null, null, null, null, null, DBTable.COLUMN_CAR + " ASC");
        // create set of tracks
        Set<String> carSet = new TreeSet<>();
        carSet.add("All cars");
        // cursor gets updated in updateListView()
        if (cursor.moveToFirst()) {
            do {
                carSet.add(cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_CAR)));
            } while (cursor.moveToNext());
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                R.layout.custom_simple_spinner_item_db, carSet.toArray(new String[carSet.size()]));
        spinnerArrayAdapter.setDropDownViewResource(R.layout.custom_simple_spinner_dropdown_item_db);
        carSpinner.setAdapter(spinnerArrayAdapter);
    }

    /**
     * Populate spinner with 'ASC' and 'DESC' values
     */
    private void fillSortSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter
                .createFromResource(this, R.array.laptime_array, R.layout.custom_simple_spinner_item_db);
        // specify the layout to use when the list of choices appears
        spinnerAdapter.setDropDownViewResource(R.layout.custom_simple_spinner_dropdown_item_db);
        sortSpinner.setAdapter(spinnerAdapter);
    }

    /**
     * Adapter interface implementation
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Item clicked: " + position);
        cursor.moveToPosition(position);
        Log.d(TAG, "Cursor at " + position + ": " +
                FastLap.format(cursor.getFloat(cursor
                        .getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME))));

        final String track = cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_TRACK));
        final String car = cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_CAR));
        final String time = FastLap.format(cursor.getFloat(
                cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME)));
        final int pos = position;

        AlertDialog.Builder builder = new AlertDialog.Builder(DatabaseActivity.this);
        builder.setTitle("Delete item");
        builder.setMessage(track + "  -  " + car + "  -  " + time)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete item from database
                        String selection = DBTable.COLUMN_TRACK + " LIKE ? AND " +
                                DBTable.COLUMN_CAR + " LIKE ?";
                        String[] args = {track, car};
                        database.delete(DBTable.TABLE_NAME, selection, args);
                        // remove item from adapter and update ListView
                        adapter.remove(adapter.getItem(pos));
                        adapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "No, do not delete it");
                    }
                }).create().show();
    }

    /**
     * Spinner interface implementation
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //Log.d(TAG, "Spinner -> onItemSelected()");
        updateListView();
    }

    /**
     * Spinner interface implementation
     *
     * @param parent
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

}
