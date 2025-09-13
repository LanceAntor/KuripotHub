package com.example.kuripothub.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

public class NetworkUtils {
    
    private static final String TAG = "NetworkUtils";
    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities != null) {
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                         networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    Log.d(TAG, "Network available: " + hasInternet);
                    return hasInternet;
                }
            }
        }
        
        Log.d(TAG, "No network available");
        return false;
    }
    
    public static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return "WiFi";
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return "Cellular";
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return "Ethernet";
                    }
                }
            }
        }
        
        return "Unknown";
    }
}
