package com.example.assignment_app.util;

import android.content.Context;
import android.widget.Toast;
import com.example.assignment_app.R;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ErrorHandler {

    // Handles network errors and displays an appropriate toast message.
    public static void handleNetworkError(Context context, Throwable error) {
        int errorMessageId;

        if (error instanceof UnknownHostException) { // No internet connection.
            errorMessageId = R.string.error_no_internet;
        } else if (error instanceof SocketTimeoutException) { // Network timeout.
            errorMessageId = R.string.error_timeout;
        } else { // Other general network errors.
            errorMessageId = R.string.error_generic;
        }

        Toast.makeText(context, errorMessageId, Toast.LENGTH_SHORT).show(); // Show error message.
    }
}
