package com.example.waller;

import android.graphics.Color;

public class ColorUtils {

    public static int colorToSeekBarProgress(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return (int) (hsv[0] / 3.6); // Convert hue to SeekBar progress (0-100)
    }

    public static int getSeekBarColor(int progress) {
        float hue = progress * 3.6f; // Convert SeekBar progress to hue (0-360)
        return Color.HSVToColor(new float[]{hue, 1, 1});
    }

    public static String colorToHexString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static int hexStringToColor(String hex) {
        return Color.parseColor(hex);
    }
}


