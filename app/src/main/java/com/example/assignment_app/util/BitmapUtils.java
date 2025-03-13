package com.example.assignment_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    // Calculates the optimal sample size for downscaling an image
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Decodes a bitmap from a byte array with optimized sampling
    public static Bitmap decodeSampledBitmapFromByteArray(byte[] imageData, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // Read image size without loading it
        BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false; // Load the actual image now

        return BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
    }

    // Saves a bitmap to a file in the cache directory
    public static void saveBitmapToFile(File cacheDir, Bitmap bitmap, String filename) {
        File file = new File(cacheDir, filename);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // Save as PNG
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) outputStream.close(); // Close stream
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Loads a bitmap from a cached file if it exists
    public static Bitmap loadBitmapFromFile(File cacheDir, String filename) {
        File file = new File(cacheDir, filename);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath()); // Load bitmap from file
        }
        return null;
    }

    // Calculates the image width based on screen size and number of columns
    public static int calculateImageWidth(Context context, int numColumns) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels; // Get screen width
        return screenWidth / numColumns; // Calculate column width
    }
}
