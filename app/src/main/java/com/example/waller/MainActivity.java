package com.example.waller;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import android.app.WallpaperManager;

public class MainActivity extends AppCompatActivity {
    private int startColor = 0xFF0000FF; // Default start color
    private int endColor = 0xFFFF0000;   // Default end color
    private GridView gridView;

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
            Bitmap randomImage = generateRandomImage();
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
                    Bitmap randomImage = generateRandomImage();
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

        final EditText editTextStartColor = dialog.findViewById(R.id.editTextStartColor);
        final EditText editTextEndColor = dialog.findViewById(R.id.editTextEndColor);

        editTextStartColor.setText(ColorUtils.colorToHexString(startColor));
        editTextEndColor.setText(ColorUtils.colorToHexString(endColor));

        final View colorPreview = dialog.findViewById(R.id.colorPreview);

        Button btnSaveColors = dialog.findViewById(R.id.btnSaveColors);
        btnSaveColors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save selected colors from hex codes
                startColor = ColorUtils.hexStringToColor(editTextStartColor.getText().toString());
                endColor = ColorUtils.hexStringToColor(editTextEndColor.getText().toString());

                // Regenerate images with the selected colors
                regenerateImages();

                dialog.dismiss();
            }
        });

        // ... (additional code for dialog)

        dialog.show();
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

    private Bitmap generateRandomImage() {
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

        // Set random start color
        int startColor = getRandomColor();
        // Set random end color
        int endColor = getRandomColor();

        // Set the gradient type (linear gradient in this example)
        gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);

        // Set the gradient orientation (you can experiment with different angles)
        gradientDrawable.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);

        // Set the colors for the gradient
        gradientDrawable.setColors(new int[]{startColor, endColor});

        // Convert the drawable to a Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        gradientDrawable.setBounds(0, 0, width, height);
        Canvas canvas = new Canvas(bitmap);
        gradientDrawable.draw(canvas);

        return bitmap;
    }

    // Add this method to regenerate images when colors are changed
    private void regenerateImages() {
        ImageAdapter adapter = (ImageAdapter) gridView.getAdapter();
        adapter.clear();

        for (int i = 0; i < 10; i++) {
            Bitmap randomImage = generateRandomImage();
            adapter.add(randomImage);
        }
    }

    // Helper method to generate a random color
    private int getRandomColor() {
        return (int) (Math.random() * 0x1000000) | 0xFF000000; // Random color with full alpha
    }
}
