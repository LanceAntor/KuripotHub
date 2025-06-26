package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private CardView submitButton;
    private TextView loginLink;
    private ProgressBar progressBar;
    
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
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        submitButton = findViewById(R.id.submitButton);
        loginLink = findViewById(R.id.loginLink);
        
        // Try to find progress bar
        progressBar = findViewById(R.id.progressBar);
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
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
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

        showProgress(true);
        
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
                                1000.0 // Default budget
                            );
                            
                            firebaseManager.createUserProfile(user)
                                    .addOnCompleteListener(profileTask -> {
                                        showProgress(false);
                                        
                                        if (profileTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this, 
                                                "Registration successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUpActivity.this, ExpenseTrackingActivity.class));
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
                        showProgress(false);
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
    
    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        submitButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        confirmPasswordInput.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // You can add custom back button behavior here if needed
    }
}
