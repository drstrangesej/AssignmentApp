package com.example.assignment_app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.assignment_app.R;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// ImageLoader - Handles image loading with memory and disk caching.
public class ImageLoader {
    // LRU memory cache for storing recently used images
    private final LruCache<String, Bitmap> memoryCache;
    // Disk-based cache to store images persistently
    private final DiskLruCache diskCache;
    // Application context
    private final Context context;
    // Map to track ongoing download tasks
    private final Map<String, Future<?>> taskMap = new ConcurrentHashMap<>();
    // Executor service for handling image loading in background threads
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB disk cache size
    private static final String DISK_CACHE_SUBDIR = "thumbnails"; // Subdirectory for disk cache

    // Constructor - Initializes memory and disk cache
    public ImageLoader(Context context) throws IOException {
        this.context = context.getApplicationContext();

        // Get max memory available to the app
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Allocate 1/8th of available memory for cache
        final int cacheSize = maxMemory / 8;

        // Initialize memory cache with LRU (Least Recently Used) strategy
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Initialize disk cache
        File cacheDir = getDiskCacheDir(context, DISK_CACHE_SUBDIR);
        diskCache = DiskLruCache.open(cacheDir, 1, 1, DISK_CACHE_SIZE);
    }

    // Loads an image from cache or downloads it if not available
    public void loadImage(String imageUrl, ImageView imageView) {
        cancelPotentialTask(imageView);

        // Try fetching from memory cache first
        Bitmap bitmap = getBitmapFromMemCache(imageUrl);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        // Set a placeholder image while loading
        imageView.setImageResource(R.drawable.placeholder);

        // Start background image loading task
        ImageLoaderTask task = new ImageLoaderTask(imageUrl, imageView);
        Future<?> futureTask = executorService.submit(task);
        taskMap.put(imageUrl, futureTask);
    }

    // Cancels all ongoing image loading tasks
    public void cancelAll() {
        for (Future<?> future : taskMap.values()) {
            future.cancel(true);
        }
        taskMap.clear();
    }

    // Cancels any previous loading task associated with an ImageView
    private void cancelPotentialTask(ImageView imageView) {
        Future<?> task = (Future<?>) imageView.getTag(R.id.image_loader_task);
        if (task != null) {
            task.cancel(true);
        }
    }

    // Retrieves an image from memory cache
    private Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    // Adds an image to memory cache
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    // Retrieves an image from disk cache
    private Bitmap getBitmapFromDiskCache(String key) {
        try {
            DiskLruCache.Snapshot snapshot = diskCache.get(getHashKey(key));
            if (snapshot != null) {
                InputStream is = snapshot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                snapshot.close();
                return bitmap;
            }
        } catch (IOException e) {
            Log.e("ImageLoader", "Disk cache read error", e);
        }
        return null;
    }

    // Adds an image to disk cache
    private void addBitmapToDiskCache(String key, Bitmap bitmap) {
        try {
            String hashKey = getHashKey(key);
            DiskLruCache.Editor editor = diskCache.edit(hashKey);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)) {
                    outputStream.close();
                    editor.commit(); // Save changes if compression was successful
                    diskCache.flush();
                } else {
                    editor.abort();
                }
            }
        } catch (IOException e) {
            Log.e("ImageLoader", "Disk cache write error", e);
        }
    }

    // Generates a unique hash key for each image URL
    private String getHashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(key.hashCode());
        }
    }

    // Gets the directory path for disk cache
    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable()) {
            // Use external storage if available
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            // Use internal storage otherwise
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    // Checks if the device has an active internet connection
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    // Background task to load images
    private class ImageLoaderTask implements Runnable {
        private final String imageUrl;
        private final WeakReference<ImageView> imageViewReference;

        // Constructor - Initializes image URL and ImageView reference
        public ImageLoaderTask(String imageUrl, ImageView imageView) {
            this.imageUrl = imageUrl;
            this.imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        public void run() {
            if (!isNetworkAvailable()) {
                Log.e("ImageLoader", "No internet connection");
                return;
            }

            // Try fetching from disk cache first
            Bitmap bitmap = getBitmapFromDiskCache(imageUrl);
            if (bitmap == null) {
                // Download if not available in cache
                bitmap = downloadImage(imageUrl);
                if (bitmap != null) {
                    addBitmapToMemoryCache(imageUrl, bitmap);
                    addBitmapToDiskCache(imageUrl, bitmap);
                }
            } else {
                addBitmapToMemoryCache(imageUrl, bitmap);
            }

            final Bitmap finalBitmap = bitmap;
            ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.post(() -> {
                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    }
                });
            }

            taskMap.remove(imageUrl);
        }

        // Downloads an image from a given URL
        private Bitmap downloadImage(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // Downsampling factor

                Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
                input.close();
                connection.disconnect();

                return bitmap;
            } catch (Exception e) {
                Log.e("ImageLoader", "Error downloading image", e);
                return null;
            }
        }
    }
}
