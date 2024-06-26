package com.example.waller;

import static android.graphics.drawable.GradientDrawable.LINE;
import static android.graphics.drawable.GradientDrawable.RECTANGLE;
import static android.graphics.drawable.GradientDrawable.RING;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.DynamicColors;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.io.IOException;
import java.util.Random;

import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener;
import eltos.simpledialogfragment.color.SimpleColorDialog;


public class MainActivity extends AppCompatActivity implements OnDialogResultListener {
    private GridView gridView;
    private Bitmap selectedImage;

    // Initialize color variables to 0 initially
    private int selectedColor1 = 0;
    private int selectedColor2 = 0;
    private int selectedColor3 = 0;
    private int selectedColor4 = 0;
    private final String TAG_PRIMARY_COLOR = "tagPrimary";
    private final String TAG_SECONDARY_COLOR = "tagSecondary";

    private EditText editPrimaryColor;
    private EditText editSecondaryColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCenter.start(getApplication(), "d96f022d-9006-48d0-aa2d-ad88ebfdf723",
                Analytics.class, Crashes.class);
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivityIfAvailable(this);
        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.gridView);
        Button btnGenerate = findViewById(R.id.btnGenerate);

        // Set up the adapter for the GridView
        final ArrayAdapter<Bitmap> adapter = new ImageAdapter(this);
        gridView.setAdapter(adapter);

        // Generate random images with different gradient types
        for (int i = 0; i < 16; i++) {
            Bitmap randomImage;

            // Switch between gradient types for each iteration
            switch (i % 4) {
                case 0:
                    randomImage = generateRandomImage(0, GradientType.LINEAR);
                    break;
                case 1:
                    randomImage = generateRandomImage(0, GradientType.ANGULAR);
                    break;
                case 2:
                    randomImage = generateRandomImage(0, GradientType.BILINEAR);
                    break;
                case 3:
                    randomImage = generateRandomImage(0, GradientType.DIAGONAL);
                    break;
                default:
                    randomImage = generateRandomImage(0, GradientType.LINEAR);
            }

            adapter.add(randomImage);
        }

        // Set up click listener for the Generate button
        btnGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear existing images
                adapter.clear();

                // Generate random images with different gradient types
                for (int i = 0; i < 16; i++) {
                    Bitmap randomImage;

                    // Switch between gradient types for each iteration
                    switch (i % 4) {
                        case 0:
                            randomImage = generateRandomImage(0, GradientType.LINEAR);
                            break;
                        case 1:
                            randomImage = generateRandomImage(0, GradientType.ANGULAR);
                            break;
                        case 2:
                            randomImage = generateRandomImage(0, GradientType.BILINEAR);
                            break;
                        case 3:
                            randomImage = generateRandomImage(0, GradientType.DIAGONAL);
                            break;
                        default:
                            randomImage = generateRandomImage(0, GradientType.LINEAR);
                    }

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
                final Bitmap clickedImage = adapter.getItem(position);

                // Show AlertDialog for wallpaper options
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Set Wallpaper")
                        .setItems(new CharSequence[]{"Home Screen", "Lock Screen", "Both"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        setWallpaper(clickedImage, WallpaperManager.FLAG_SYSTEM);
                                        break;
                                    case 1:
                                        setWallpaper(clickedImage, WallpaperManager.FLAG_LOCK);
                                        break;
                                    case 2:
                                        setWallpaper(clickedImage, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
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
                        .setNeutralButton("Download", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (clickedImage != null) {
                                    selectedImage = clickedImage;  // Update the selectedImage field
                                    checkAndRequestPermissions();
                                    // No need to save the image here, it will be saved after obtaining permission
                                } else {
                                    Toast.makeText(MainActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private static final int PERMISSION_REQUEST_CODE = 123;

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE
            );
        } else {
            // Permission is already granted, save the image
            saveImageToGallery(selectedImage);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, save the image
                saveImageToGallery(selectedImage);
            } else {
                Toast.makeText(this, "Permission denied. Unable to save the image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery(Bitmap image) {
        String savedImageURL = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                image,
                "Wallpaper_" + System.currentTimeMillis(),
                "Generated wallpaper"
        );

        // If the image is saved successfully, get the image URI
        if (savedImageURL != null) {
            Uri savedImageURI = Uri.parse(savedImageURL);

            // Broadcast the media scanner to add the new image to the gallery
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(savedImageURI);
            sendBroadcast(mediaScanIntent);

            // Show Toast message after successful download
            Toast.makeText(this, "Image downloaded successfully!", Toast.LENGTH_SHORT).show();
        } else {
            // Show Toast message if there is an issue with saving the image
            Toast.makeText(this, "Failed to save the image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showColorPickerDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.color_picker_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setOnDismissListener(dialog1 -> {
            editPrimaryColor = null;
            editSecondaryColor = null;
        });

        editPrimaryColor = dialog.findViewById(R.id.editColor2);
        editSecondaryColor = dialog.findViewById(R.id.editColor3);

        ImageButton colorPaletteButton = dialog.findViewById(R.id.colorPaletteButton);
        colorPaletteButton.setOnClickListener(v -> {
            // Call the method to show the color wheel dialog
            showColorWheelDialog(TAG_PRIMARY_COLOR);
        });

        ImageButton colorPaletteButton2 = dialog.findViewById(R.id.colorPaletteButton2);
        colorPaletteButton2.setOnClickListener(v -> {
            // Call the method to show the color wheel dialog
            showColorWheelDialog(TAG_SECONDARY_COLOR);
        });

        // Update EditText fields with previously selected colors if available
        if (selectedColor2 != 0) {
            editPrimaryColor.setText(ColorUtils.colorToHexString(selectedColor2).substring(1)); // Remove '#'
        }
        if (selectedColor3 != 0) {
            editSecondaryColor.setText(ColorUtils.colorToHexString(selectedColor3).substring(1)); // Remove '#'
        }
        // Add a TextWatcher to the first EditText
        editPrimaryColor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 6) { // Check if 6 digits have been entered
                    editSecondaryColor.requestFocus(); // Move focus to the next EditText
                }
            }
        });

        final View colorPreview = dialog.findViewById(R.id.colorPreview);

        Button btnSaveColors = dialog.findViewById(R.id.btnSaveColors);
        btnSaveColors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Validate and save selected colors from hex codes
                if (validateAndSaveColors()) {
                    selectedColor2 = ColorUtils.hexStringToColor("#" + editPrimaryColor.getText().toString());
                    selectedColor3 = ColorUtils.hexStringToColor("#" + editSecondaryColor.getText().toString());
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
                selectedColor3 = Color.WHITE;
                regenerateImages(selectedColor2, selectedColor3);
                dialog.dismiss();
            }
        });

        Button btnDarkTheme = dialog.findViewById(R.id.Dark);
        btnDarkTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set selectedColor3 to "000000" for dark theme
                selectedColor3 = Color.BLACK;
                // Regenerate images with the selected colors
                regenerateImages(selectedColor2, selectedColor3);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showColorWheelDialog(final String tag) {
        SimpleColorDialog.build()
                .title("Pick a Color")
                .colorPreset(Color.WHITE)
                .allowCustom(true)
                .show(this, tag);
    }

    private boolean validateAndSaveColors() {
        //Toast.makeText(MainActivity.this, "Invalid color code(s)", Toast.LENGTH_SHORT).show();
        return validateAndSaveColor(editPrimaryColor, 1) &&
                validateAndSaveColor(editSecondaryColor, 2);
    }

    private boolean validateAndSaveColor(EditText editText, int colorNumber) {
        String hexCode = editText.getText().toString();
        if (!TextUtils.isEmpty(hexCode)) {
            try {
                int color = Color.parseColor("#" + hexCode);
                switch (colorNumber) {
                    case 2:
                        selectedColor2 = color;
                        break;
                    case 3:
                        selectedColor3 = color;
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

    private Bitmap generateRandomImage(int color2, GradientType gradientType) {
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

        // Set the gradient type
        switch (gradientType) {
            case LINEAR:
                gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.RIGHT_LEFT);
                break;
            case ANGULAR:
                gradientDrawable.setGradientType(GradientDrawable.SWEEP_GRADIENT);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                break;
            case BILINEAR:
                gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                break;
            case DIAGONAL:
                // For diagonal gradient, you can use LinearGradient with a diagonal angle
                gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
                gradientDrawable.setOrientation(GradientDrawable.Orientation.TL_BR);
                break;
        }

        // Set the gradient colors
        int startColor = (selectedColor2 != 0) ? shuffleColor(selectedColor2) : getRandomColor();
        int endColor = (selectedColor3 != 0) ? shuffleColor(selectedColor3) : getRandomColor();

        gradientDrawable.setColors(new int[]{startColor, endColor});

        // Convert the drawable to a Bitmap
        selectedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        gradientDrawable.setBounds(0, 0, width, height);
        Canvas canvas = new Canvas(selectedImage);
        gradientDrawable.draw(canvas);

        return selectedImage;
    }

    @Override
    public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
        if (which == BUTTON_POSITIVE) {
            int selectedColor = extras.getInt(SimpleColorDialog.COLOR);

            // Get the hex code from the selected color
            String hexCode = String.format("%06X", (0xFFFFFF & selectedColor));

            if (dialogTag.equals(TAG_PRIMARY_COLOR) && editPrimaryColor != null) {
                editPrimaryColor.setText(hexCode);
            } else if (dialogTag.equals(TAG_SECONDARY_COLOR) && editSecondaryColor != null) {
                editSecondaryColor.setText(hexCode);
            } else {
                return false;
            }

            return true;
        }
        return false;
    }


    // Enumeration for gradient types
    private enum GradientType {
        LINEAR, ANGULAR, BILINEAR, DIAGONAL
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

        // Generate random images with different gradient types
        for (int i = 0; i < 16; i++) {
            Bitmap randomImage;

            // Switch between gradient types for each iteration
            switch (i % 4) {
                case 0:
                    randomImage = generateRandomImage(0, GradientType.LINEAR);
                    break;
                case 1:
                    randomImage = generateRandomImage(0, GradientType.ANGULAR);
                    break;
                case 2:
                    randomImage = generateRandomImage(0, GradientType.BILINEAR);
                    break;
                case 3:
                    randomImage = generateRandomImage(0, GradientType.DIAGONAL);
                    break;
                default:
                    randomImage = generateRandomImage(0, GradientType.LINEAR);
            }

            adapter.add(randomImage);
        }
    }

    private int getRandomColor() {
        return (int) (Math.random() * 0x1000000) | 0xFF000000; // Random color with full alpha
    }
}