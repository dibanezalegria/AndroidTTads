package com.palebluedot.pcarstimetrial;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by Daniel Ibanez on 2016-03-08.
 */
public class SessionAdapter extends ArrayAdapter<FastLap> {

    private static final String TAG = "DEBUG";

    // stores best laptime and best sectors
    private float[] bestTimes = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };

    public SessionAdapter(Context context, FastLap[] fastLaps) {
        super(context, 0, fastLaps);

        // extract best laptime and sectors from the list of fast laps
        for (int i = 0; i < fastLaps.length; i++) {
            if (fastLaps[i].isValid()) {
                if (fastLaps[i].getLaptime(0) > 0 && fastLaps[i].getLaptime(0) < bestTimes[0]) {
                    bestTimes[0] = fastLaps[i].getLaptime(0);
                }
                if (fastLaps[i].getLaptime(1) > 0 && fastLaps[i].getLaptime(1) < bestTimes[1]) {
                    bestTimes[1] = fastLaps[i].getLaptime(1);
                }
                if (fastLaps[i].getLaptime(2) > 0 && fastLaps[i].getLaptime(2) < bestTimes[2]) {
                    bestTimes[2] = fastLaps[i].getLaptime(2);
                }
                if (fastLaps[i].getLaptime(3) > 0 && fastLaps[i].getLaptime(3) < bestTimes[3]) {
                    bestTimes[3] = fastLaps[i].getLaptime(3);
                }
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FastLap fLap = getItem(position);
        if (convertView == null) {
            convertView = (LayoutInflater.from(getContext()))
                    .inflate(R.layout.session_item, parent, false);
        }

        TextView tvLapNumber = (TextView) convertView.findViewById(R.id.session_lapnumber_tv);
        TextView tvLapTime = (TextView) convertView.findViewById(R.id.session_laptime_tv);
        TextView tvSector1 = (TextView) convertView.findViewById(R.id.session_sector1_tv);
        TextView tvSector2 = (TextView) convertView.findViewById(R.id.session_sector2_tv);
        TextView tvSector3 = (TextView) convertView.findViewById(R.id.session_sector3_tv);

        tvLapNumber.setText(String.format("%s", fLap.getLapNumber()));

        tvLapTime.setText(FastLap.format(fLap.getLaptime(0)));
        if (!fLap.isValid()) {
            //tvLapTime.setBackgroundResource(R.color.invalidLap);
            tvLapTime.setTextColor(ContextCompat.getColor(getContext(), R.color.invalidLap));
            tvLapTime.setText("-invalid-");
        } else if (fLap.getLaptime(0) == bestTimes[0]) {
            //tvLapTime.setBackgroundResource(R.color.bestTime);
            tvLapTime.setTextColor(ContextCompat.getColor(getContext(), R.color.bestTime));
        } else {
            tvLapTime.setTextColor(ContextCompat.getColor(getContext(), R.color.fieldOutput));
        }

        tvSector1.setText(FastLap.format(fLap.getLaptime(1)));
        if (fLap.getLaptime(1) == bestTimes[1] && fLap.isValid()) {
            tvSector1.setBackgroundResource(R.color.bestSector);
        } else {
            tvSector1.setBackgroundResource(R.color.transparentBg);
        }

        tvSector2.setText(FastLap.format(fLap.getLaptime(2)));
        if (fLap.getLaptime(2) == bestTimes[2] && fLap.isValid()) {
            tvSector2.setBackgroundResource(R.color.bestSector);
        } else {
            tvSector2.setBackgroundResource(R.color.transparentBg);
        }

        tvSector3.setText(FastLap.format(fLap.getLaptime(3)));
        if (fLap.getLaptime(3) == bestTimes[3] && fLap.isValid()) {
            tvSector3.setBackgroundResource(R.color.bestSector);
        } else {
            tvSector3.setBackgroundResource(R.color.transparentBg);
        }

        return convertView;
    }
}
