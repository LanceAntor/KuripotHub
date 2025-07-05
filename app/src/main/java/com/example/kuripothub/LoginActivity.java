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

import com.example.kuripothub.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText usernameInput;
    private EditText passwordInput;
    private CardView submitButton;
    private TextView signUpLink;
    
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseManager = FirebaseManager.getInstance();
        
        // Check if user is already logged in
        if (firebaseManager.isUserLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, ExpenseTrackingActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        
        // Check if email was passed from SignUpActivity for prefilling
        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            usernameInput.setText(prefillEmail);
            passwordInput.requestFocus(); // Move focus to password field
        }
    }

    private void initializeViews() {
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        submitButton = findViewById(R.id.submitButton);
        signUpLink = findViewById(R.id.signUpLink);
    }

    private void setupClickListeners() {
        // Submit button click
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        // Sign up link click
        signUpLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleLogin() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Basic validation
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        // Disable submit button during login
        submitButton.setEnabled(false);
        
        // First, find the user by username to get their email
        firebaseManager.getUserByUsername(username)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Username found, get the email
                            String email = task.getResult().getDocuments().get(0).getString("email");
                            if (email != null) {
                                // Now login with email and password
                                loginWithEmail(email, password);
                            } else {
                                submitButton.setEnabled(true);
                                Toast.makeText(LoginActivity.this, "User data corrupted", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Username not found
                            submitButton.setEnabled(true);
                            usernameInput.setError("Username not found");
                        }
                    } else {
                        submitButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Error checking username", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithEmail(String email, String password) {
        firebaseManager.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Re-enable submit button
                    submitButton.setEnabled(true);
                    
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = firebaseManager.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, ExpenseTrackingActivity.class));
                            finish();
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = "Authentication failed.";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage = "Invalid password";
                                } else if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "Account not found";
                                } else {
                                    errorMessage = "Login failed. Please check your credentials.";
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
