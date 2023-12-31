package com.example.waller;

// ImageAdapter.java
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

public class ImageAdapter extends ArrayAdapter<Bitmap> {

    public ImageAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(getContext());
            // Set the desired width and height for the ImageView
            int imageSize = 800; // Adjust this value as needed
            imageView.setLayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize));
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

