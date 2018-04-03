package com.palebluedot.pcarstimetrial;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Created by Daniel Ibanez on 2016-03-08.
 */
public class MulticlassAdapter extends CursorAdapter {

    private static final String TAG = "DEBUG";
    private String actualCar;
    private float fastestLap;

    public MulticlassAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // get fastest lap
        if (cursor.isFirst()) {
            fastestLap = cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME));
            //Log.d(TAG, "Cursor fastest: " + fastestLap);
        }
        return LayoutInflater.from(context).inflate(R.layout.multiclass_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tvLapTime = (TextView) view.findViewById(R.id.multiclass_laptime_tv);
        TextView tvDifference = (TextView) view.findViewById(R.id.multiclass_diff_tv);
        TextView tvCarName = (TextView) view.findViewById(R.id.multiclass_car_tv);

        float time = cursor.getFloat(cursor.getColumnIndexOrThrow(DBTable.COLUMN_LAPTIME));
        float gap = BigDecimal.valueOf(time - fastestLap).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();
        String carName = cursor.getString(cursor.getColumnIndexOrThrow(DBTable.COLUMN_CAR));

        tvLapTime.setText(FastLap.format(time));
        if (gap == 0) {
            tvDifference.setText(String.format(Locale.ENGLISH, "%9s", "-"));
        } else {
            tvDifference.setText(String.format(Locale.ENGLISH, "%5s%1s%03d",
                    (int)gap, ".", (int)((gap * 1000) % 1000)));
        }
        tvCarName.setText(carName);
        if (carName.equals(actualCar)) {
            tvCarName.setTextColor(ContextCompat.getColor(context, R.color.validLap));
        }
        else {
            tvCarName.setTextColor(ContextCompat.getColor(context, R.color.fieldOutput));
        }
    }

    public void setActualCar(String name) {
        actualCar = name;
    }
}
