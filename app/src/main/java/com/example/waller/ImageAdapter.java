package com.example.waller;

// ImageAdapter.java
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public class ImageAdapter extends ArrayAdapter<Bitmap> {

    public ImageAdapter(Context context) {
        super(context, 0);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(getContext());
            // Set the desired width and height for the ImageView
//            int imageSize = 500; // Adjust this value as needed
//            imageView.setLayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize));
            int imageW = 490; // Adjust this value as needed
            int imageH = 800;
            imageView.setLayoutParams(new ViewGroup.LayoutParams(imageW, imageH));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        // Set the bitmap to the ImageView
        Bitmap bitmap = getItem(position);
        imageView.setImageBitmap(bitmap);

        return imageView;
    }
}

