package com.example.kuripothub;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class KuripotHubApplication extends Application {
    
    private static final String TAG = "KuripotHubApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Initializing Firebase...");
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Enable Firestore offline persistence
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
        
        // Enable Firestore logging for debugging
        FirebaseFirestore.setLoggingEnabled(true);
        
        Log.d(TAG, "Firebase initialized successfully");
        
        // Test Firebase connection
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Log.d(TAG, "Firebase Auth instance: " + (auth != null ? "Available" : "NULL"));
        Log.d(TAG, "Firestore instance: " + (db != null ? "Available" : "NULL"));
    }
}
