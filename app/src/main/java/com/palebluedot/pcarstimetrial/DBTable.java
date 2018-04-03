package com.palebluedot.pcarstimetrial;

/**
 * Created by Daniel Ibanez on 2016-03-02.
 */

import android.provider.BaseColumns;

/**
 * Provides implicit _ID by implementing BaseColumns
 */
public class DBTable implements BaseColumns {
    public static final String TABLE_NAME = "pcarstt";
    public static final String COLUMN_TRACK = "track";
    public static final String COLUMN_CAR = "car";
    public static final String COLUMN_CLASS = "class";
    public static final String COLUMN_LAPTIME = "laptime";
    public static final String COLUMN_SECTOR1 = "sector1";
    public static final String COLUMN_SECTOR2 = "sector2";
    public static final String COLUMN_SECTOR3 = "sector3";
}
