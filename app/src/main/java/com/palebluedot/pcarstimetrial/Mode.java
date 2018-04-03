package com.palebluedot.pcarstimetrial;

/**
 * Created by Daniel Ibanez on 2016-02-26.
 */

public enum Mode {
    BOOTING(0),
    MENU(1),
    RACE(82),
    PRACTICE(18),
    TT(98),
    QUALIFYING(50),
    WARMUP(34),
    BETWEEN_SESSIONS(2),
    RESTART(4);

    private int value;

    Mode(int value) {
        this.value = value;
    }

    boolean equals(int aValue) {
        return aValue == value;
    }

    static String getString(int aValue) {
        String str = "";
        switch (aValue) {
            case 18:
                str = "Practice";
                break;
            case 50:
                str = "Qualifying";
                break;
            case 34:
                str = "Warmup";
                break;
            case 82:
                str = "Race";
                break;
            default:
                str = "";
                break;
        }

        return str;
    }
}

