package com.example.assignment_app.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignment_app.R;
import com.example.assignment_app.model.ImageItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private final Context context;
    private List<ImageItem> imageItems;

    // LRU (Least Recently Used) Cache for storing images in memory
    private final LruCache<String, Bitmap> memoryCache;

    // Directory for caching images on disk
    private final File cacheDir;

    // Map to track which ImageView is displaying which image URL
    private final Map<ImageView, String> imageViewMap = new HashMap<>();

    public ImageAdapter(Context context, List<ImageItem> imageItems) {
        this.context = context;
        this.imageItems = imageItems;

        // Initialize memory cache with 1/8th of available memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<>(cacheSize);

        // Create disk cache directory
        cacheDir = new File(context.getCacheDir(), "image_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout for each image
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem imageItem = imageItems.get(position);

        // Construct full image URL
        String imageUrl = imageItem.getThumbnail().getDomain() + "/" +
                imageItem.getThumbnail().getBasePath() + "/0/" +
                imageItem.getThumbnail().getKey();

        // Store the ImageView and corresponding URL in the map
        imageViewMap.put(holder.imageView, imageUrl);

        // Try to load from memory cache first
        Bitmap cachedBitmap = memoryCache.get(imageUrl);
        if (cachedBitmap != null) {
            holder.imageView.setImageBitmap(cachedBitmap);
        } else {
            // Try to load from disk cache
            File imageFile = new File(cacheDir, String.valueOf(imageUrl.hashCode()));
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    memoryCache.put(imageUrl, bitmap); // Store in memory cache
                    holder.imageView.setImageBitmap(bitmap);
                    return;
                }
            }
            // If not found in cache, load asynchronously from the network
            new ImageLoaderTask(holder.imageView, imageUrl).execute();
        }
    }

    @Override
    public int getItemCount() {
        return Math.min(imageItems.size(), 100); // Limit to 100 images
    }

    // Method to update the list of images
    public void updateItems(List<ImageItem> newItems) {
        imageItems = newItems;
        notifyDataSetChanged();
    }

    // ViewHolder class for holding image views
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    // AsyncTask to load images from the internet
    private class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private final ImageView imageView;
        private final String imageUrl;

        ImageLoaderTask(ImageView imageView, String imageUrl) {
            this.imageView = imageView;
            this.imageUrl = imageUrl;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                // If the task is canceled or the ImageView has been reused, return null
                if (!imageViewMap.get(imageView).equals(imageUrl)) return null;

                // Open a connection to the image URL
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                // Read image data from the input stream
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                if (bitmap != null) {
                    // Store the bitmap in memory cache
                    memoryCache.put(imageUrl, bitmap);

                    // Store the bitmap in disk cache
                    File imageFile = new File(cacheDir, String.valueOf(imageUrl.hashCode()));
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    outputStream.flush();
                    outputStream.close();
                }
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // Ensure the correct image is set to the correct ImageView
            if (bitmap != null && imageViewMap.get(imageView).equals(imageUrl)) {
                imageView.setImageBitmap(bitmap);
            } else {
                // Set a placeholder image in case of failure
                imageView.setImageResource(R.drawable.error_placeholder);
            }
        }
    }
}
