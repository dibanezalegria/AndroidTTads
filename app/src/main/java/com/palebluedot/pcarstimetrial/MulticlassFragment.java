package com.palebluedot.pcarstimetrial;

/**
 * Created by Daniel Ibanez on 2016-03-07.
 */
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * Created by Daniel Ibanez on 2016-03-07.
 */
public class MulticlassFragment extends Fragment {
    private final String TAG = "DEBUG";
    private static MulticlassFragment mInstance;
    private ListView mListView;

    public static MulticlassFragment getInstance() {
        if (mInstance == null) {
            mInstance = new MulticlassFragment();
        }
        return mInstance;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.d(TAG, "MulticlassFragment class: onCreateView()");
        View view = inflater.inflate(R.layout.multiclass_fragment, container, false);
        mListView = (ListView) view.findViewById(R.id.multiclass_listview);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //Log.d(TAG, "MulticlassFragment -> OnDestroyView()");
    }

    /**
     *
     * @param adapter
     */
    public void update(CursorAdapter adapter) {
        mListView.setAdapter(adapter);
    }

}

