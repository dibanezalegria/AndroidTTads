package com.palebluedot.pcarstimetrial;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by Daniel Ibanez on 2016-03-07.
 */
public class MyFragmentPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "Debug";
    private static final int PAGE_COUNT = 3;
    private final String[] tabTitles =
            new String[] { "Session laps", "Records", "Best versus records" };

    public MyFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return SessionFragment.getInstance();
            case 1:
                return MulticlassFragment.getInstance();
            case 2:
                return VersusFragment.getInstance();
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles[position];
    }
}
