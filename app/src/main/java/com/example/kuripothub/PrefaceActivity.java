package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class PrefaceActivity extends AppCompatActivity {

    private static final String TAG = "PrefaceActivity";
    
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preface_activity);

        // Hide the action bar for full screen experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        finishButton = findViewById(R.id.finishButton);
    }

    private void setupClickListeners() {
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to the main expense tracking activity
                Intent intent = new Intent(PrefaceActivity.this, ExpenseTrackingActivity.class);
                startActivity(intent);
                finish(); // Finish this activity so user can't go back to it
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Override back button to do nothing or exit app
        // This prevents users from going back to login after successful registration
        finishAffinity(); // This will close the entire app
    }
}