package com.palebluedot.pcarstimetrial;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Daniel Ibanez on 2016-03-09.
 */
public class DatabaseAdapter extends ArrayAdapter<FastLap> {

    public DatabaseAdapter(Context context, ArrayList<FastLap> fastLaps) {
        super(context, 0, fastLaps);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FastLap fLap = getItem(position);
        if (convertView == null) {
            convertView = (LayoutInflater.from(getContext()))
                    .inflate(R.layout.database_item, parent, false);
        }

        TextView tvTrackName = (TextView) convertView.findViewById(R.id.tvTrackName);
        TextView tvCarName = (TextView) convertView.findViewById(R.id.tvCarName);
        TextView tvLapTime = (TextView) convertView.findViewById(R.id.tvLaptime);

        tvTrackName.setText(fLap.getTrack());
        tvCarName.setText(fLap.getCar());
        tvLapTime.setText(FastLap.format(fLap.getLaptime(0)));

        return convertView;
    }
}
