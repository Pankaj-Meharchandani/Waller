package com.example.waller;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.io.IOException;
import java.util.Random;

import android.app.WallpaperManager;

public class MainActivity extends AppCompatActivity {
    private int startColor = 0xFF0000FF; // Default start color
    private int endColor = 0xFFFF0000;   // Default end color
    private GridView gridView;

    // Initialize color variables to 0 initially
    private int selectedColor1 = 0;
    private int selectedColor2 = 0;
    private int selectedColor3 = 0;
    private int selectedColor4 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.gridView);
        Button btnGenerate = findViewById(R.id.btnGenerate);

        // Set up the adapter for the GridView
        final ArrayAdapter<Bitmap> adapter = new ImageAdapter(this);
        gridView.setAdapter(adapter);

        // Generate 10 random images (either gradient or abstract)
        for (int i = 0; i < 10; i++) {
            Bitmap randomImage = generateRandomImage(0, 0);
            adapter.add(randomImage);
        }

        // Set up click listener for the Generate button
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear existing images
                adapter.clear();

                // Generate 10 random images (either gradient or abstract)
                for (int i = 0; i < 10; i++) {
                    Bitmap randomImage = generateRandomImage(0, 0);
                    adapter.add(randomImage);
                }
            }
        });

        Button btnSelectColors = findViewById(R.id.btnSelectColors);
        btnSelectColors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });

        // Set up click listener for GridView items
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Bitmap selectedImage = adapter.getItem(position);

                // Show AlertDialog for wallpaper options
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Set Wallpaper")
                        .setItems(new CharSequence[]{"Home Screen", "Lock Screen", "Both"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        setWallpaper(selectedImage, WallpaperManager.FLAG_SYSTEM);
                                        break;
                                    case 1:
                                        setWallpaper(selectedImage, WallpaperManager.FLAG_LOCK);
                                        break;
                                    case 2:
                                        setWallpaper(selectedImage, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                                        break;
                                }
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })

                        .create()
                        .show();
            }
        });
    }

    private void showColorPickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.color_picker_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final EditText editTextColor1 = dialog.findViewById(R.id.editTextColor1);
        final EditText editTextColor2 = dialog.findViewById(R.id.editTextColor2);
        final EditText editTextColor3 = dialog.findViewById(R.id.editTextColor3);
        final EditText editTextColor4 = dialog.findViewById(R.id.editTextColor4);

        // Update EditText fields with previously selected colors if available
        if (selectedColor1 != 0) {
            editTextColor1.setText(ColorUtils.colorToHexString(selectedColor1).substring(1)); // Remove '#'
        }
        if (selectedColor2 != 0) {
            editTextColor2.setText(ColorUtils.colorToHexString(selectedColor2).substring(1)); // Remove '#'
        }
        if (selectedColor3 != 0) {
            editTextColor3.setText(ColorUtils.colorToHexString(selectedColor3).substring(1)); // Remove '#'
        }
        if (selectedColor4 != 0) {
            editTextColor4.setText(ColorUtils.colorToHexString(selectedColor4).substring(1)); // Remove '#'
        }

        final View colorPreview = dialog.findViewById(R.id.colorPreview);

        Button btnSaveColors = dialog.findViewById(R.id.btnSaveColors);
        btnSaveColors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate and save selected colors from hex codes
                if (validateAndSaveColors(editTextColor1, editTextColor2, editTextColor3, editTextColor4)) {
                    // Regenerate images with the selected colors
                    regenerateImages(selectedColor2, selectedColor3);
                    dialog.dismiss();
                }
            }
        });
        Button btnLightTheme = dialog.findViewById(R.id.Light);
        btnLightTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fill textboxes with 'FFFFFF' for light theme
                editTextColor1.setText("FFFFFF");
                editTextColor2.setText("FFFFFF");
                editTextColor3.setText("FFFFFF");
                editTextColor4.setText("FFFFFF");
            }
        });
        Button btnDarkTheme = dialog.findViewById(R.id.Dark);
        btnDarkTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Fill textboxes with '000000' for dark theme
                editTextColor1.setText("000000");
                editTextColor2.setText("000000");
                editTextColor3.setText("000000");
                editTextColor4.setText("000000");
            }
        });
        // ... (additional code for dialog)

        dialog.show();
    }

    private boolean validateAndSaveColors(EditText editText1, EditText editText2, EditText editText3, EditText editText4) {
        if (validateAndSaveColor(editText1, 1) &&
                validateAndSaveColor(editText2, 2) &&
                validateAndSaveColor(editText3, 3) &&
                validateAndSaveColor(editText4, 4)) {
            return true;
        }

        else {
            //Toast.makeText(MainActivity.this, "Invalid color code(s)", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean validateAndSaveColor(EditText editText, int colorNumber) {
        String hexCode = editText.getText().toString();
        if (!TextUtils.isEmpty(hexCode)) {
            try {
                int color = ColorUtils.hexStringToColor("#" + hexCode);
                switch (colorNumber) {
                    case 1:
                        selectedColor1 = color;
                        break;
                    case 2:
                        selectedColor2 = color;
                        break;
                    case 3:
                        selectedColor3 = color;
                        break;
                    case 4:
                        selectedColor4 = color;
                        break;
                }
                return true;
            } catch (IllegalArgumentException e) {
                // Invalid color code
                Toast.makeText(MainActivity.this, "Invalid color code for Color " + colorNumber, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            // Empty color code
            Toast.makeText(MainActivity.this, "Empty color code for Color " + colorNumber, Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private void setWallpaper(Bitmap bitmap, int flags) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(MainActivity.this);
        try {
            wallpaperManager.setBitmap(bitmap, null, true, flags);
            Toast.makeText(MainActivity.this, "Wallpaper set successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Failed to set wallpaper", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateRandomImage(int color2, int color3) {
        // Get the dimensions of the device screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Ensure the width is always smaller than the height for portrait orientation
        int height = Math.min(screenWidth, screenHeight);
        int width = (int) (height / 2.5); // Adjust the aspect ratio as needed

        // Implement logic to generate a random gradient image
        // For simplicity, you can use GradientDrawable and convert it to a Bitmap
        GradientDrawable gradientDrawable = new GradientDrawable();

        // Set the gradient type (linear gradient in this example)
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        // Set the gradient orientation (you can experiment with different angles)
        gradientDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);

        // Set the colors for the gradient
        int startColor = (color2 != 0) ? shuffleColor(color2) : getRandomColor();
        int endColor = (color3 != 0) ? shuffleColor(color3) : getRandomColor();

        gradientDrawable.setColors(new int[]{startColor, endColor});

        // Convert the drawable to a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        gradientDrawable.setBounds(0, 0, width, height);
        Canvas canvas = new Canvas(bitmap);
        gradientDrawable.draw(canvas);

        return bitmap;
    }

    private int shuffleColor(int color) {
        Random random = new Random();
        int red = Color.red(color) + random.nextInt(201) - 100; // Adjust the range as needed
        int green = Color.green(color) + random.nextInt(201) - 100;
        int blue = Color.blue(color) + random.nextInt(201) - 100;

        // Ensure RGB values stay within valid range
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return Color.rgb(red, green, blue);
    }


    private void regenerateImages(int color2, int color3) {
        ImageAdapter adapter = (ImageAdapter) gridView.getAdapter();
        adapter.clear();

        for (int i = 0; i < 10; i++) {
            Bitmap randomImage = generateRandomImage(color2, color3);
            adapter.add(randomImage);
        }
    }

    private int getRandomColor() {
        return (int) (Math.random() * 0x1000000) | 0xFF000000; // Random color with full alpha
    }
}