package com.example.assignment_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ImageCache {

    // Memory cache for storing bitmaps
    private LruCache<String, Bitmap> memoryCache;

    // Disk cache directory for storing images
    private File cacheDir;

    public ImageCache(Context context) {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024); // Max available VM memory
        int cacheSize = maxMemory / 8; // Use 1/8th of available memory for caching

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024; // Measure cache size in KB
            }
        };

        cacheDir = new File(context.getCacheDir(), "images"); // Set up disk cache directory
        if (!cacheDir.exists()) cacheDir.mkdirs(); // Create directory if it doesn't exist
    }

    // Save bitmap to both memory and disk cache
    public void saveBitmapToCache(String url, Bitmap bitmap) {
        memoryCache.put(url, bitmap); // Save to memory cache
        File file = new File(cacheDir, generateFileKeyFromUrl(url)); // Generate file key

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); // Save as JPEG
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Retrieve bitmap from memory cache
    public Bitmap getBitmapFromMemory(String url) {
        return memoryCache.get(url);
    }

    // Retrieve bitmap from disk cache
    public Bitmap getBitmapFromDisk(String url) {
        File file = new File(cacheDir, generateFileKeyFromUrl(url));

        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) memoryCache.put(url, bitmap); // Add to memory cache
            return bitmap;
        }
        return null;
    }

    // Clear both memory and disk cache
    public void clearCache() {
        memoryCache.evictAll();
        for (File file : cacheDir.listFiles()) file.delete(); // Delete cached files
    }

    // Clear only memory cache
    public void clearMemoryCache() {
        memoryCache.evictAll();
    }

    // Generate a unique file key from URL using MD5
    private String generateFileKeyFromUrl(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes());
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(url.hashCode()); // Fallback using hashCode
        }
    }
}
