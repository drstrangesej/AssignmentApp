package com.example.assignment_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.assignment_app.adapter.ImageAdapter;
import com.example.assignment_app.api.ImageApiService;
import com.example.assignment_app.model.ImageItem;
import com.example.assignment_app.util.ErrorHandler;
import com.example.assignment_app.util.NetworkUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView; // RecyclerView to display images
    private View progressBar; // Progress bar to indicate loading state
    private ImageAdapter imageAdapter; // Adapter for RecyclerView
    private ImageApiService apiService; // API service to fetch images
    private LruCache<String, Bitmap> memoryCache; // LRU cache for image caching

    private static final int GRID_COLUMN_COUNT = 3; // Number of columns in grid layout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.imageRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        apiService = new ImageApiService();

        // Set up memory cache (Adjust the size as needed)
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // Use 1/8th of the available memory

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024; // Size in KB
            }
        };

        setupRecyclerView();
        loadImages();
    }

    // Method to set up RecyclerView with GridLayoutManager
    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(this, new ArrayList<>());
        GridLayoutManager layoutManager = new GridLayoutManager(this, GRID_COLUMN_COUNT);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(imageAdapter);

        // Optimization for smooth scrolling
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // Defer image loading while scrolling fast
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    imageAdapter.notifyDataSetChanged(); // Load images when scrolling stops
                }
            }
        });
    }

    // Method to fetch images from API and update UI
    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE); // Show loading indicator

        // Check for internet connection before fetching images
        if (!NetworkUtils.isNetworkAvailable(this)) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch images from API
        apiService.getImages(100, new ImageApiService.ImageApiCallback() {
            @Override
            public void onSuccess(List<ImageItem> images) {
                imageAdapter.updateItems(images); // Update RecyclerView with fetched images
                progressBar.setVisibility(View.GONE); // Hide loading indicator
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE); // Hide loading indicator on error
                ErrorHandler.handleNetworkError(MainActivity.this, e);
            }
        });
    }

    // AsyncTask to download and cache images in the background
    static class ImageLoaderTask extends AsyncTask<String, Void, Bitmap> {
        private String imageUrl; // URL of the image to be downloaded

        @Override
        protected Bitmap doInBackground(String... urls) {
            imageUrl = urls[0];
            return downloadImage(imageUrl);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                // Cache image and update UI (implementation needed)
            }
        }

        // Method to download an image from a URL
        private Bitmap downloadImage(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input); // Convert input stream to Bitmap
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
