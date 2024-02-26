package com.example.waller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public class ImageAdapter extends ArrayAdapter<Bitmap> {

    public ImageAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RoundedCornerImageView imageView;
        if (convertView == null) {
            imageView = new RoundedCornerImageView(getContext());
            // Set the desired width and height for the ImageView
            int imageW = 490; // Adjust this value as needed
            int imageH = 800;
            imageView.setLayoutParams(new ViewGroup.LayoutParams(imageW, imageH));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (RoundedCornerImageView) convertView;
        }

        // Set the bitmap to the ImageView
        Bitmap bitmap = getItem(position);
        imageView.setImageBitmap(bitmap);

        return imageView;
    }

    // Custom ImageView class for rounded corners
    private static class RoundedCornerImageView extends androidx.appcompat.widget.AppCompatImageView {
        private static final float CORNER_RADIUS = 30f; // Adjust the radius as needed

        private Path path;
        private RectF rectF;

        public RoundedCornerImageView(Context context) {
            super(context);
            init();
        }

        private void init() {
            path = new Path();
            rectF = new RectF();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            rectF.set(0, 0, getWidth(), getHeight());
            path.addRoundRect(rectF, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW);
            canvas.clipPath(path);
            super.onDraw(canvas);
        }
    }
}
