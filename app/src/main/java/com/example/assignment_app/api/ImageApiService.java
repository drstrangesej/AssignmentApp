package com.example.assignment_app.api;

import com.example.assignment_app.model.ImageItem;
import com.example.assignment_app.util.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

public class ImageApiService {
    // API endpoint URL for fetching images
    private static final String API_URL = "https://acharyaprashant.org/api/v2/content/misc/media-coverages?limit=100";

    // Interface to handle API responses asynchronously
    public interface ImageApiCallback {
        void onSuccess(List<ImageItem> images); // Called when data is successfully fetched
        void onError(Exception e); // Called if an error occurs
    }

    // Method to fetch images from the API
    public void getImages(int count, ImageApiService.ImageApiCallback callback) {
        // Creating a background thread to fetch data without blocking the UI
        new Thread(() -> {
            try {
                // Fetching JSON response from the API
                String jsonResponse = NetworkUtils.fetchData(API_URL);

                // Converting the response string into a JSON array
                JSONArray jsonArray = new JSONArray(jsonResponse);
                List<ImageItem> imageItems = new ArrayList<>();

                // Iterating through each JSON object in the array
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(i);

                    // Extracting image ID and title
                    String id = jsonObj.getString("id");
                    String title = jsonObj.getString("title");

                    // Extracting thumbnail details
                    JSONObject thumbObj = jsonObj.getJSONObject("thumbnail");
                    String domain = thumbObj.getString("domain");
                    String basePath = thumbObj.getString("basePath");
                    String key = thumbObj.getString("key");

                    // Extracting available image qualities
                    JSONArray qualitiesArray = thumbObj.getJSONArray("qualities");
                    List<Integer> qualities = new ArrayList<>();
                    for (int j = 0; j < qualitiesArray.length(); j++) {
                        qualities.add(qualitiesArray.getInt(j));
                    }

                    // Creating a Thumbnail object
                    ImageItem.Thumbnail thumbnail = new ImageItem.Thumbnail(domain, basePath, key, qualities);

                    // Creating an ImageItem object and adding it to the list
                    imageItems.add(new ImageItem(id, title, thumbnail));
                }

                // Ensuring callback execution happens on the main thread
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(imageItems));

            } catch (final Exception e) {
                // Handling errors and sending them to the callback on the main thread
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onError(e));
            }
        }).start(); // Start the background thread
    }
}
