package com.example.kuripothub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class PersonaActivity extends AppCompatActivity {

    private CardView studentCard;
    private CardView professionalCard;
    private ImageView backButton;
    private View studentIndicator;
    private View professionalIndicator;
    
    private String currentPersona = "student"; // Default persona

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persona);
        
        initializeViews();
        setupClickListeners();
        updatePersonaIndicators();
    }
    
    private void initializeViews() {
        studentCard = findViewById(R.id.studentCard);
        professionalCard = findViewById(R.id.professionalCard);
        backButton = findViewById(R.id.backButton);
        studentIndicator = findViewById(R.id.studentIndicator);
        professionalIndicator = findViewById(R.id.professionalIndicator);
    }
    
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
        
        // Student card
        studentCard.setOnClickListener(v -> {
            selectPersona("student");
        });
        
        // Professional card (locked)
        professionalCard.setOnClickListener(v -> {
            showLockedMessage();
        });
    }
    
    private void selectPersona(String persona) {
        if (persona.equals("student")) {
            currentPersona = "student";
            updatePersonaIndicators();
            Toast.makeText(this, "Student persona selected", Toast.LENGTH_SHORT).show();
            

            // For now, we'll just update the UI and go back to expense tracking
            
            // Navigate back to ExpenseTrackingActivity
            Intent intent = new Intent(this, ExpenseTrackingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Close PersonaActivity
        }
    }
    
    private void showLockedMessage() {
        Toast.makeText(this, "Professional persona is coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void updatePersonaIndicators() {
        if (currentPersona.equals("student")) {
            studentIndicator.setBackgroundResource(R.drawable.circle_yellow_background);
            professionalIndicator.setBackgroundResource(R.drawable.circle_gray_background);
        } else if (currentPersona.equals("professional")) {
            studentIndicator.setBackgroundResource(R.drawable.circle_gray_background);
            professionalIndicator.setBackgroundResource(R.drawable.circle_yellow_background);
        }
    }
}
