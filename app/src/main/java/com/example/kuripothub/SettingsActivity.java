package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Back button
        CardView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Account Center (Available)
        CardView accountCenterCard = findViewById(R.id.accountCenterCard);
        accountCenterCard.setOnClickListener(v -> {
            // Navigate to Account Center (placeholder for now)
            Toast.makeText(this, "Account Center - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // Budget Preference (Available - Navigate to PreferenceActivity)
        CardView budgetPreferenceCard = findViewById(R.id.budgetPreferenceCard);
        budgetPreferenceCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        });

        // Smart Tipid Features (Coming Soon)
        CardView smartTipidCard = findViewById(R.id.smartTipidCard);
        smartTipidCard.setOnClickListener(v -> {
            Toast.makeText(this, "Smart Tipid Features - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // Saving Goals (Coming Soon)
        CardView savingGoalsCard = findViewById(R.id.savingGoalsCard);
        savingGoalsCard.setOnClickListener(v -> {
            Toast.makeText(this, "Saving Goals - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // Appearance Display (Coming Soon)
        CardView appearanceCard = findViewById(R.id.appearanceCard);
        appearanceCard.setOnClickListener(v -> {
            Toast.makeText(this, "Appearance Display - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // Notification (Coming Soon)
        CardView notificationCard = findViewById(R.id.notificationCard);
        notificationCard.setOnClickListener(v -> {
            Toast.makeText(this, "Notification - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // Data Backup (Coming Soon)
        CardView dataBackupCard = findViewById(R.id.dataBackupCard);
        dataBackupCard.setOnClickListener(v -> {
            Toast.makeText(this, "Data Backup - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        // About KuripotHub (Coming Soon)
        CardView aboutCard = findViewById(R.id.aboutCard);
        aboutCard.setOnClickListener(v -> {
            Toast.makeText(this, "About KuripotHub - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
    }
}
