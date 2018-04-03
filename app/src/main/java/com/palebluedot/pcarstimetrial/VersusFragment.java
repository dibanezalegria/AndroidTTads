package com.palebluedot.pcarstimetrial;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by Daniel Ibanez on 2016-03-07.
 */
public class VersusFragment extends Fragment implements Spinner.OnItemSelectedListener {
    private static final String TAG = "DEBUG";
    private static VersusFragment mInstance;
    private SQLiteDatabase database;
    private Cursor cursor;
    private Spinner mSpinner;
    private TextView targetLapTV, targetS1TV, targetS2TV, targetS3TV;
    private TextView gapLapTV, gapS1TV, gapS2TV, gapS3TV;
    private TextView bestLapTV, bestS1TV, bestS2TV, bestS3TV;
    private FastLap mTargetLap, mBestLap;

    // track name gets updated from MainActivity
    private String mTrackName;

    public static VersusFragment getInstance() {
        if (mInstance == null) {
            mInstance = new VersusFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Log.d(TAG, "SessionFragment -> OnDestroyView()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "SessionFragment class: onCreateView()");
        // database
        database = GlobalApplication.getDatabase();
        View view = inflater.inflate(R.layout.versus_fragment, container, false);
        mSpinner = (Spinner) view.findViewById(R.id.versus_spinner);
        mSpinner.setOnItemSelectedListener(this);
        updateCarSpinner();
        // widgets in gridlayout
        targetLapTV = (TextView) view.findViewById(R.id.versus_targetLap);
        targetS1TV = (TextView) view.findViewById(R.id.versus_targetS1);
        targetS2TV = (TextView) view.findViewById(R.id.versus_targetS2);
        targetS3TV = (TextView) view.findViewById(R.id.versus_targetS3);
        gapLapTV = (TextView) view.findViewById(R.id.versus_gap0);
        gapS1TV = (TextView) view.findViewById(R.id.versus_gap1);
        gapS2TV = (TextView) view.findViewById(R.id.versus_gap2);
        gapS3TV = (TextView) view.findViewById(R.id.versus_gap3);
        bestLapTV = (TextView) view.findViewById(R.id.versus_bestLap);
        bestS1TV = (TextView) view.findViewById(R.id.versus_bestS1);
        bestS2TV = (TextView) view.findViewById(R.id.versus_bestS2);
        bestS3TV = (TextView) view.findViewById(R.id.versus_bestS3);
        reset();
        return view;
    }

    /**
     * Called from MainActivity when restarting race. Track name does not change between race restarts
     */
    public void restartRace() {
        gapLapTV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS1TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS2TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS3TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapLapTV.setText("   -.---");
        gapS1TV.setText("   -.---");
        gapS2TV.setText("   -.---");
        gapS3TV.setText("   -.---");
        bestLapTV.setText("--:--:---");
        bestS1TV.setText("--:--:---");
        bestS2TV.setText("--:--:---");
        bestS3TV.setText("--:--:---");
        mBestLap = null;
    }

    /**
     * MainActivity resets VersusFragment when starting a new race (not only a race restart)
     */
    public void reset() {
        restartRace();
        targetLapTV.setText("--:--:---");
        targetS1TV.setText("--:--:---");
        targetS2TV.setText("--:--:---");
        targetS3TV.setText("--:--:---");
        // reset instance variables
        mTrackName = null;
        mTargetLap = null;
        mSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                R.layout.custom_simple_spinner_item, new String[]{String.format("%-25s", "waiting for track info")}));
    }

    /**
     * Populate spinner with all cars
     * MainActivity calls this method when database gets updated so spinner keeps updated
     */
    public void updateCarSpinner() {
        // save spinner selection
        String selected = (String) mSpinner.getSelectedItem();

        // create set of cars
        Set<String> carSet = new TreeSet<>();
        if (mTrackName != null) {
            Log.d(TAG, "updateCarSpinner -> Track: " + mTrackName);
            // query database
            String selection = DBTable.COLUMN_TRACK + " LIKE ?";
            String[] args = {mTrackName};
            cursor = database.query(DBTable.TABLE_NAME,
                    null, selection, args, null, null, DBTable.COLUMN_CAR + " ASC");

            // cursor gets updated in updateListView()
            if (cursor.moveToFirst()) {
                do {
                    carSet.add(cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_CAR)));
                } while (cursor.moveToNext());
            }
            // no records for this track yet?
            if (carSet.isEmpty()) {
                carSet.add(String.format("%-25s", "no records found"));
            }
        } else {
            Log.d(TAG, "updateCarSpinner -> mTrackName is NULL");
            carSet.add(String.format("%-25s", "waiting for track info"));
        }

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(),
                R.layout.custom_simple_spinner_item, carSet.toArray(new String[carSet.size()]));
        spinnerArrayAdapter.setDropDownViewResource(R.layout.custom_simple_spinner_dropdown_item);
        mSpinner.setAdapter(spinnerArrayAdapter);

        // restore spinner selection
        int pos = spinnerArrayAdapter.getPosition(selected);
        if (pos != -1) {
            mSpinner.setSelection(pos);
        }
    }

    /**
     * When user manage a best lap, MainActivity informs VersusFragment
     * by calling this method
     *
     * @param best
     */
    public void setBest(FastLap best) {
        mBestLap = best;
        updateBestTV();
        updateGapTV();
    }


    /**
     * When track name gets available/updated MainActivity informs VersusFragment
     * by calling this method
     *
     * @param track
     */
    public void setTrackName(String track) {
        Log.d(TAG, "VersusFragment -> setTrackName: " + track);
        mTrackName = track;
        updateCarSpinner();
        updateGapTV();
    }

    /**
     * Used in MainActivity to check if track name has already been updated
     *
     * @return
     */
    public String getTrackName() {
        return mTrackName;
    }

    /**
     * Updates TextView(s) for target lap (lap and sectors)
     */
    private void updateTargetTV() {
        targetLapTV.setText(FastLap.format(mTargetLap.getLaptime(0)));
        targetS1TV.setText(FastLap.format(mTargetLap.getLaptime(1)));
        targetS2TV.setText(FastLap.format(mTargetLap.getLaptime(2)));
        targetS3TV.setText(FastLap.format(mTargetLap.getLaptime(3)));
    }

    /**
     * Updates TextView(s) for best lap (lap and sectors)
     */
    private void updateBestTV() {
        bestLapTV.setText(FastLap.format(mBestLap.getLaptime(0)));
        bestS1TV.setText(FastLap.format(mBestLap.getLaptime(1)));
        bestS2TV.setText(FastLap.format(mBestLap.getLaptime(2)));
        bestS3TV.setText(FastLap.format(mBestLap.getLaptime(3)));
    }

    /**
     * Updates TextView(s) for gaps between target lap and best lap (lap and sectors)
     */
    private void updateGapTV() {
        if (mTargetLap == null || mBestLap == null)
            return;

        // reset previous background
        gapLapTV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS1TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS2TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));
        gapS3TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.transparentBg));

        // lap
        String gap = getGap(mBestLap.getLaptime(0), mTargetLap.getLaptime(0));
        // if best lap then only update lap time background
        if (gap.equals("  -0.000")) {
            gapLapTV.setText("   0.000");
            gapS1TV.setText("   -.---");
            gapS2TV.setText("   -.---");
            gapS3TV.setText("   -.---");
        } else {
            // lap time
            gapLapTV.setText(String.format("%9s", gap));
            // sector 1
            gap = getGap(mBestLap.getLaptime(1), mTargetLap.getLaptime(1));
            gapS1TV.setText(String.format("%9s", gap));
            if (gap.contains("-")) {
                gapS1TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bestSector));
            }

            // sector 2
            gap = getGap(mBestLap.getLaptime(2), mTargetLap.getLaptime(2));
            gapS2TV.setText(String.format("%9s", gap));
            if (gap.contains("-")) {
                gapS2TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bestSector));
            }

            // sector 3
            gap = getGap(mBestLap.getLaptime(3), mTargetLap.getLaptime(3));
            gapS3TV.setText(String.format("%9s", gap));
            if (gap.contains("-")) {
                gapS3TV.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.bestSector));
            }
        }
    }

    /**
     * Returns the difference between to times given in seconds.milliseconds in String form
     *
     * @param t1
     * @param t2
     * @return
     */
    private String getGap(float t1, float t2) {
        float gap = t1 - t2;
        // truncate decimal part to 3 digits (rounds half up)
        gap = BigDecimal.valueOf(gap).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();
        String sign = "-";
        if (gap > 0) {
            sign = "+";
        }
        int decimal = Math.abs((int) ((gap * 1000) % 1000));
        String gapStr = String.format(Locale.ENGLISH, "%s%s.%03d", sign, Math.abs((int) gap), decimal);
        return String.format("%8s", gapStr);
    }

    /**
     * Handles spinner item selection
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mTrackName == null ||
                mSpinner.getSelectedItem().equals("No records found")) {
            return;
        }

        String selection = DBTable.COLUMN_TRACK + " LIKE ? AND " + DBTable.COLUMN_CAR + " LIKE ?";
        String[] args = {mTrackName, (String) mSpinner.getSelectedItem()};
        cursor = database.query(DBTable.TABLE_NAME,
                null, selection, args, null, null, null);

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            mTargetLap = new FastLap();
            mTargetLap.setLaptime(0, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME)));
            mTargetLap.setLaptime(1, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR1)));
            mTargetLap.setLaptime(2, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR2)));
            mTargetLap.setLaptime(3, cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_SECTOR3)));
            updateTargetTV();
            updateGapTV();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
