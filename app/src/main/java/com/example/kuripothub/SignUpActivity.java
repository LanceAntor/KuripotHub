package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.kuripothub.models.User;
import com.example.kuripothub.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private EditText emailInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private CardView submitButton;
    private TextView loginLink;
    
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Hide the action bar for full screen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firebaseManager = FirebaseManager.getInstance();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        submitButton = findViewById(R.id.submitButton);
        loginLink = findViewById(R.id.loginLink);
    }

    private void setupClickListeners() {
        // Submit button click listener
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });

        // Login link click listener
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to login screen
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleSignUp() {
        String email = emailInput.getText().toString().trim();
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            return;
        }

        if (username.length() < 3) {
            usernameInput.setError("Username must be at least 3 characters");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Please confirm password");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        if (!isValidEmail(email)) {
            emailInput.setError("Please enter a valid email address");
            return;
        }

        // Check if username is already taken
        submitButton.setEnabled(false);
        
        firebaseManager.checkUsernameAvailability(username)
                .addOnCompleteListener(this, usernameTask -> {
                    if (usernameTask.isSuccessful()) {
                        if (usernameTask.getResult().isEmpty()) {
                            // Username is available, proceed with registration
                            proceedWithSignUp(email, username, password);
                        } else {
                            // Username is taken
                            submitButton.setEnabled(true);
                            usernameInput.setError("Username is already taken");
                        }
                    } else {
                        // Error checking username
                        submitButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, 
                            "Error checking username availability. Please try again.", 
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void proceedWithSignUp(String email, String username, String password) {
        firebaseManager.signUpWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                        
                        if (firebaseUser != null) {
                            // Create user profile in Firestore
                            User user = new User(
                                firebaseUser.getUid(),
                                email,
                                "", // Name can be updated later
                                username,
                                1000.0 // Default budget
                            );
                            
                            firebaseManager.createUserProfile(user)
                                    .addOnCompleteListener(profileTask -> {
                                        // Re-enable submit button
                                        submitButton.setEnabled(true);
                                        
                                        if (profileTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this, 
                                                "Registration successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUpActivity.this, PrefaceActivity.class));
                                            finish();
                                        } else {
                                            Log.w(TAG, "Failed to create user profile", profileTask.getException());
                                            Toast.makeText(SignUpActivity.this, 
                                                "Registration successful, but failed to create profile. Please try logging in.", 
                                                Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        // Re-enable submit button
                        submitButton.setEnabled(true);
                        
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = "Registration failed.";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email address is already in use")) {
                                    errorMessage = "This email is already registered";
                                } else if (exceptionMessage.contains("email address is badly formatted")) {
                                    errorMessage = "Invalid email format";
                                } else if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "Password must be at least 6 characters";
                                } else {
                                    errorMessage = exceptionMessage;
                                }
                            }
                        }
                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // You can add custom back button behavior here if needed
    }
}
