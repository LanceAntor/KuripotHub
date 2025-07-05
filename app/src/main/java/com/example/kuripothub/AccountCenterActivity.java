package com.example.kuripothub;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.kuripothub.models.User;
import com.example.kuripothub.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountCenterActivity extends AppCompatActivity {

    private static final String TAG = "AccountCenterActivity";
    
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    // UI Elements
    private ImageView backArrow;
    private TextView usernameText;
    private TextView emailText;
    private TextView dateJoinedText;
    private TextView lastLoginText;
    private TextView loginDeviceText;
    private CardView editProfileButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_center);
        
        firebaseManager = FirebaseManager.getInstance();
        
        // Check if user is logged in
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        
        currentUserId = currentUser.getUid();
        
        initializeViews();
        setupClickListeners();
        loadUserData();
        loadAccountActivity();
    }
    
    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.emailText);
        dateJoinedText = findViewById(R.id.dateJoinedText);
        lastLoginText = findViewById(R.id.lastLoginText);
        loginDeviceText = findViewById(R.id.loginDeviceText);
        editProfileButton = findViewById(R.id.editProfileButton);
    }
    
    private void setupClickListeners() {
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement edit profile functionality
                Toast.makeText(AccountCenterActivity.this, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadUserData() {
        Log.d(TAG, "Loading user data for: " + currentUserId);
        
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            Log.d(TAG, "User data loaded successfully");
                            
                            // Update username
                            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                                usernameText.setText(user.getUsername());
                            } else {
                                usernameText.setText("Not set");
                            }
                            
                            // Update email
                            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                emailText.setText(user.getEmail());
                            } else {
                                // Fallback to Firebase Auth email
                                FirebaseUser currentUser = firebaseManager.getCurrentUser();
                                if (currentUser != null && currentUser.getEmail() != null) {
                                    emailText.setText(currentUser.getEmail());
                                } else {
                                    emailText.setText("Not available");
                                }
                            }
                        } else {
                            Log.w(TAG, "User object is null");
                            loadFallbackData();
                        }
                    } else {
                        Log.w(TAG, "User document does not exist");
                        loadFallbackData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    loadFallbackData();
                    Toast.makeText(AccountCenterActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadFallbackData() {
        // Use Firebase Auth data as fallback
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.getEmail() != null) {
                emailText.setText(currentUser.getEmail());
                // Extract username from email as fallback
                String emailUsername = currentUser.getEmail().split("@")[0];
                usernameText.setText(emailUsername);
            } else {
                usernameText.setText("Guest");
                emailText.setText("Not available");
            }
        } else {
            usernameText.setText("Guest");
            emailText.setText("Not available");
        }
    }
    
    private void loadAccountActivity() {
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser != null) {
            // Date joined (from Firebase Auth creation time)
            if (currentUser.getMetadata() != null && currentUser.getMetadata().getCreationTimestamp() != 0) {
                long creationTime = currentUser.getMetadata().getCreationTimestamp();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateJoined = dateFormat.format(new Date(creationTime));
                dateJoinedText.setText(dateJoined);
            } else {
                dateJoinedText.setText("Not available");
            }
            
            // Last login (from Firebase Auth)
            if (currentUser.getMetadata() != null && currentUser.getMetadata().getLastSignInTimestamp() != 0) {
                long lastSignInTime = currentUser.getMetadata().getLastSignInTimestamp();
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
                String lastLogin = dateTimeFormat.format(new Date(lastSignInTime));
                lastLoginText.setText(lastLogin);
            } else {
                lastLoginText.setText("Not available");
            }
            
            // Login device - get device model and Android version
            String deviceInfo = getDeviceInfo();
            loginDeviceText.setText(deviceInfo);
        } else {
            dateJoinedText.setText("Not available");
            lastLoginText.setText("Not available");
            loginDeviceText.setText("Not available");
        }
    }
    
    private String getDeviceInfo() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String model = android.os.Build.MODEL;
        String androidVersion = android.os.Build.VERSION.RELEASE;
        
        // Capitalize manufacturer name
        String capitalizedManufacturer = manufacturer.substring(0, 1).toUpperCase() + manufacturer.substring(1).toLowerCase();
        
        return capitalizedManufacturer + " " + model + " (Android " + androidVersion + ")";
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to this activity
        loadUserData();
        loadAccountActivity();
    }
}
