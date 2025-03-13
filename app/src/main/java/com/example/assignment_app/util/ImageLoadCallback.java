package com.example.assignment_app.util;

import android.graphics.Bitmap;

// Callback interface for image loading results
public interface ImageLoadCallback {
    void onSuccess(Bitmap bitmap); // Called when image loading is successful
    void onError(); // Called when image loading fails
}
