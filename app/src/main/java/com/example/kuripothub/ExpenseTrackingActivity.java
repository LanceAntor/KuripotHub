package com.example.kuripothub;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.cardview.widget.CardView;

public class ExpenseTrackingActivity extends AppCompatActivity {

    private BottomSheetDialog categoryBottomSheet;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.expense_tracking_activity);
        
        // Initialize the bottom sheet
        setupCategoryBottomSheet();

        // Set click listener for the add button
        CardView fabContainer = findViewById(R.id.fabContainer);
        fabContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCategoryOptions();
            }
        });
    }
    
    private void setupCategoryBottomSheet() {
        // Initialize the bottom sheet dialog
        categoryBottomSheet = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.category_bottom_sheet, null);
        categoryBottomSheet.setContentView(bottomSheetView);

        // Set up click listeners for each category
        View breakfastOption = bottomSheetView.findViewById(R.id.breakfastOption);
        View lunchOption = bottomSheetView.findViewById(R.id.lunchOption);
        View dinnerOption = bottomSheetView.findViewById(R.id.dinnerOption);
        View othersOption = bottomSheetView.findViewById(R.id.othersOption);

        // Set click listeners for each category
        breakfastOption.setOnClickListener(v -> handleCategorySelection("Breakfast"));
        lunchOption.setOnClickListener(v -> handleCategorySelection("Lunch"));
        dinnerOption.setOnClickListener(v -> handleCategorySelection("Dinner"));
        othersOption.setOnClickListener(v -> handleCategorySelection("Others"));
    }

    private void showCategoryOptions() {
        if (categoryBottomSheet != null) {
            categoryBottomSheet.show();
        }
    }

    private void handleCategorySelection(String category) {
        // Display a message showing which category was selected
        Toast.makeText(this, "Selected category: " + category, Toast.LENGTH_SHORT).show();
        
        // Close the bottom sheet
        if (categoryBottomSheet != null && categoryBottomSheet.isShowing()) {
            categoryBottomSheet.dismiss();
        }
        
        // TODO: Navigate to add expense form with the selected category
    }
}
