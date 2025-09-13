package com.example.kuripothub;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseTestActivity extends AppCompatActivity {
    
    private static final String TAG = "FirebaseTestActivity";
    
    private TextView statusText;
    private Button testButton;
    private FirebaseManager firebaseManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_test);
        
        statusText = findViewById(R.id.statusText);
        testButton = findViewById(R.id.testButton);
        
        firebaseManager = FirebaseManager.getInstance();
        
        testButton.setOnClickListener(v -> testFirebaseConnection());
        
        // Run initial test
        testFirebaseConnection();
    }
    
    private void testFirebaseConnection() {
        StringBuilder status = new StringBuilder();
        
        // Test network connectivity
        boolean hasNetwork = NetworkUtils.isNetworkAvailable(this);
        status.append("Network: ").append(hasNetwork ? "Connected" : "Disconnected").append("\n");
        status.append("Network Type: ").append(NetworkUtils.getNetworkType(this)).append("\n\n");
        
        // Test Firebase Auth instance
        FirebaseAuth auth = FirebaseAuth.getInstance();
        status.append("Firebase Auth: ").append(auth != null ? "Initialized" : "NULL").append("\n");
        
        // Test Firestore instance
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        status.append("Firestore: ").append(firestore != null ? "Initialized" : "NULL").append("\n\n");
        
        statusText.setText(status.toString());
        
        if (hasNetwork) {
            // Test Firestore read operation
            status.append("Testing Firestore read...\n");
            statusText.setText(status.toString());
            
            firebaseManager.checkUsernameAvailability("test_nonexistent_username_12345")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            status.append("Firestore Read: SUCCESS\n");
                            status.append("Query Results: ").append(task.getResult().size()).append(" documents\n");
                            Toast.makeText(this, "Firebase connection is working!", Toast.LENGTH_SHORT).show();
                        } else {
                            status.append("Firestore Read: FAILED\n");
                            if (task.getException() != null) {
                                status.append("Error: ").append(task.getException().getMessage()).append("\n");
                            }
                            Toast.makeText(this, "Firebase connection failed!", Toast.LENGTH_LONG).show();
                        }
                        statusText.setText(status.toString());
                        Log.d(TAG, status.toString());
                    });
        } else {
            status.append("Cannot test Firestore - No network connection\n");
            statusText.setText(status.toString());
            Toast.makeText(this, "No network connection", Toast.LENGTH_SHORT).show();
        }
    }
}
