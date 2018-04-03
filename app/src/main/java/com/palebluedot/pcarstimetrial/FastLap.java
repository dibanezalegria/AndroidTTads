package com.palebluedot.pcarstimetrial;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Created by Daniel Ibanez on 2016-02-26.
 */
public class FastLap {
    private String mTrack;
    private String mCar;
    private String mClassGroup;
    private float[] mLaptime;    // [0] full lap, [1] sector1, [2] sector 2 and [3] sector3
    private int mLapNumber;
    private boolean mValid;

    public FastLap() {
        this("", "", new float[4], 0);
    }

    public FastLap(int lapNumber) {
        this("", "", new float[4], lapNumber);
        mLapNumber = lapNumber;
    }

    public FastLap(String track, String car, float[] laptime, int lapNumber) {
        mTrack = track;
        mCar = car;
        mLaptime = laptime;
        mLapNumber = lapNumber;
        mValid = true;
    }

    public String getCar() {
        return mCar;
    }

    public void setCar(String car) {
        mCar = car;
    }

    public String getClassGroup() {
        return mClassGroup;
    }

    public void setClassGroup(String classGroup) {
        mClassGroup = classGroup;
    }

    public float[] getLaptime() {
        return mLaptime;
    }

    public float getLaptime(int index) {
        return mLaptime[index];
    }

    public void setLaptime(float[] laptime) {
        mLaptime = laptime;
    }

    public void setLaptime(int index, float time) {
        mLaptime[index] = time;
    }

    public String getTrack() {
        return mTrack;
    }

    public void setTrack(String track) {
        mTrack = track;
    }

    public int getLapNumber() {
        return mLapNumber;
    }

    public void setLapNumber(int lapNumber) {
        mLapNumber = lapNumber;
    }

    public boolean isValid() {
        return mValid;
    }

    public void setValid(boolean valid) {
        mValid = valid;
    }

    public static String format(float time) {
        if (time <= 0 || time == Float.MAX_VALUE) {
            return "--:--:---";
        }

        time = BigDecimal.valueOf(time).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();  // truncate decimal part to 3 decimals

        int minutes = (int) time / 60;
        int seconds = (int) time % 60;
        int msec = ((int) (time * 1000)) % 1000;
        return String.format(Locale.ENGLISH, "%02d", minutes) +
                ":" + String.format(Locale.ENGLISH,"%02d", seconds) + ":" +
                String.format(Locale.ENGLISH,"%03d", msec);
    }

    @Override
    public String toString() {
        String lapStr;
        if (mValid) {
            lapStr = format(mLaptime[0]) + "     ";

        } else {
            lapStr = "-invalid-" + "     ";
        }

        lapStr += format(mLaptime[1]) + "   " +
                format(mLaptime[2]) + "   " +
                format(mLaptime[3]);

        return String.format(Locale.ENGLISH, "%-4d %s", mLapNumber, lapStr);
    }
}
