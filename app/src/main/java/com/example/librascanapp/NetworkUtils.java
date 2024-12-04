package com.example.librascanapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkUtils {
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    public static void handleSignIn(Context context) {
        if (isInternetAvailable(context)) {
            Toast.makeText(context, "Signing in...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show();
        }
    }
}
