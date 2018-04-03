package com.palebluedot.pcarstimetrial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Created by Daniel Ibanez on 2016-03-07.
 */
public class SessionFragment extends Fragment {
    private static final String TAG = "DEBUG";
    private static SessionFragment mInstance;
    private ListView mListView;

    public static SessionFragment getInstance() {
        if (mInstance == null) {
            mInstance = new SessionFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "SessionFragment class: onCreateView()");
        View view = inflater.inflate(R.layout.session_fragment, container, false);
        mListView = (ListView) view.findViewById(R.id.session_listview);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Log.d(TAG, "SessionFragment -> OnDestroyView()");
    }

    /**
     *
     * @param adapter
     */
    public void update(ArrayAdapter<FastLap> adapter) {
        mListView.setAdapter(adapter);
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        if (mListView.getCount() == 0) {
            return true;
        } else {
            return false;
        }
    }

}
