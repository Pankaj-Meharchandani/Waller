package com.example.waller;
// MainActivity.java
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.io.IOException;
import android.app.WallpaperManager;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridView = findViewById(R.id.gridView);
        Button btnGenerate = findViewById(R.id.btnGenerate);

        // Set up the adapter for the GridView
        final ArrayAdapter<Bitmap> adapter = new ImageAdapter(this);
        gridView.setAdapter(adapter);
        //generate 10 images by default
        for (int i = 0; i < 10; i++) {
            Bitmap gradientImage = generateRandomGradient();
            adapter.add(gradientImage);
        }
        // Set up click listener for the Generate button
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Generate 10 random gradient images
                for (int i = 0; i < 10; i++) {
                    Bitmap gradientImage = generateRandomGradient();
                    adapter.add(gradientImage);
                }
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
    private Bitmap generateRandomGradient() {
        // Get the dimensions of the device screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

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
        int width = screenWidth; // Use screen width
        int height = screenHeight; // Use screen height
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        gradientDrawable.setBounds(0, 0, width, height);
        Canvas canvas = new Canvas(bitmap);
        gradientDrawable.draw(canvas);

        return bitmap;
    }


    // Helper method to generate a random color
    private int getRandomColor() {
        return (int) (Math.random() * 0x1000000) | 0xFF000000; // Random color with full alpha
    }

}
