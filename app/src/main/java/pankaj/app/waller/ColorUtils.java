package pankaj.app.waller;

import android.graphics.Color;

public class ColorUtils {

    public static int colorToSeekBarProgress(int color) {
        // Extract RGB components
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // Convert RGB to grayscale
        int grayscale = (red + green + blue) / 3;

        // Convert grayscale to SeekBar progress (0-100)
        return (int) (grayscale / 2.55); // 2.55 = 255 / 100
    }

    public static int getSeekBarColor(int progress) {
        // Convert SeekBar progress to grayscale
        int grayscale = progress * 2; // 2 = 255 / 100

        // Create a color using grayscale values
        return Color.rgb(grayscale, grayscale, grayscale);
    }

    public static String colorToHexString(int color) {
        // Convert color to hexadecimal representation
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static int hexStringToColor(String hex) {
        // Remove "#" symbol if present
        hex = hex.replace("#", "");
        return Color.parseColor("#" + hex);
    }
}
