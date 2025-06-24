package com.example.kuripothub;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class ExpenseTrackingActivity extends AppCompatActivity {

    private BottomSheetDialog categoryBottomSheet;
    private double currentBudget = 2000.00; // Initialize budget to 2000
    private TextView budgetAmountText;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.expense_tracking_activity);
          // Initialize budget amount text view
        budgetAmountText = findViewById(R.id.budgetAmount);
        budgetAmountText.setText("P" + String.format("%.2f", currentBudget));
        
        // Initialize the bottom sheet
        setupCategoryBottomSheet();
        
        // Ensure proper spacing in the transactions container
        configureTransactionsContainer();

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
        // Initialize the bottom sheet dialog with transparent background (no dimming)
        categoryBottomSheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.category_bottom_sheet, null);
        categoryBottomSheet.setContentView(bottomSheetView);
        // Remove the dim effect
        if (categoryBottomSheet.getWindow() != null) {
            categoryBottomSheet.getWindow().setDimAmount(0f);
        }

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
    }    private void showCategoryOptions() {
        if (categoryBottomSheet != null) {
            // Set behavior to eliminate background dimming effect
            categoryBottomSheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            categoryBottomSheet.show();
        }
    }    private void handleCategorySelection(String category) {
        // Close the bottom sheet
        if (categoryBottomSheet != null && categoryBottomSheet.isShowing()) {
            categoryBottomSheet.dismiss();
        }
        
        if (category.equals("Others")) {
            // Handle "Others" option differently as per requirement
            Toast.makeText(this, "Selected category: " + category, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show amount entry modal for breakfast, lunch, and dinner
        showAmountEntryModal(category);
    }
    
    private void showAmountEntryModal(String category) {        final Dialog amountDialog = new Dialog(this);
        amountDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        amountDialog.setContentView(R.layout.amount_entry_modal);
        
        // Make dialog background transparent and apply animations
        if (amountDialog.getWindow() != null) {
            amountDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            amountDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            
            // Set the dialog width to match parent with margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(amountDialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            amountDialog.getWindow().setAttributes(layoutParams);
        }

        // Set up UI elements
        TextView categoryTitle = amountDialog.findViewById(R.id.categoryTitle);
        EditText amountInput = amountDialog.findViewById(R.id.amountInput);
        CardView cancelButton = amountDialog.findViewById(R.id.cancelButton);
        CardView confirmButton = amountDialog.findViewById(R.id.confirmButton);

        // Set category title
        categoryTitle.setText(category.toUpperCase());

        // Set up button listeners
        cancelButton.setOnClickListener(v -> amountDialog.dismiss());
        
        confirmButton.setOnClickListener(v -> {
            String amountText = amountInput.getText().toString().trim();
            if (!amountText.isEmpty()) {
                // Process the amount entered
                processExpense(category, amountText);
                amountDialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        amountDialog.show();
    }    private void processExpense(String category, String amount) {
        try {
            // Parse the amount and update the budget
            double expenseAmount = Double.parseDouble(amount.replaceAll("[^\\d.]", ""));
            
            // Decrease the budget
            updateBudget(expenseAmount);
            
            // Add the transaction to the transactions container
            addTransactionToView(category, amount);
            
            // You can also save the expense data to database or perform other actions here
            Toast.makeText(this, category + " expense added: " + amount, Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateBudget(double expenseAmount) {
        // Subtract the expense from the current budget
        currentBudget -= expenseAmount;
        
        // Make sure budget doesn't go below zero
        if (currentBudget < 0) {
            currentBudget = 0;
        }
        
        // Update the budget text view
        budgetAmountText.setText("P" + String.format("%.2f", currentBudget));
    }
    
    private void addTransactionToView(String category, String amountStr) {
        // Find the transactions container
        View transactionItem = getLayoutInflater().inflate(R.layout.transaction_item, null);
        
        // Set the category name
        TextView categoryName = transactionItem.findViewById(R.id.categoryName);
        categoryName.setText(category.toUpperCase());
        
        // Set the amount with negative sign to show it's an expense
        TextView transactionAmount = transactionItem.findViewById(R.id.transactionAmount);
        
        // Format the amount with the peso sign
        try {
            double amountValue = Double.parseDouble(amountStr.replaceAll("[^\\d.]", ""));
            transactionAmount.setText("-P" + String.format("%.2f", amountValue));
        } catch (NumberFormatException e) {
            transactionAmount.setText("-P" + amountStr);
        }
          // Set the appropriate icon based on the category
        ImageView categoryIcon = transactionItem.findViewById(R.id.categoryIcon);
        switch(category.toLowerCase()) {
            case "breakfast":
                categoryIcon.setImageResource(R.drawable.coffee);
                break;
            case "lunch":
                categoryIcon.setImageResource(R.drawable.lunch);
                break;
            case "dinner":
                categoryIcon.setImageResource(R.drawable.wine_bottle);
                break;
            default:
                categoryIcon.setImageResource(R.drawable.coffee); // Default icon
                break;
        }        // Add the transaction item to the container with proper layout parameters
        findViewById(R.id.transactionsContainer).post(new Runnable() {
            @Override
            public void run() {
                LinearLayout container = findViewById(R.id.transactionsContainer);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                // Apply bottom margin explicitly in code
                params.setMargins(0, 0, 0, 70); // 70dp bottom margin
                
                // Add the view at the top of the list (index 0)
                container.addView(transactionItem, 0, params);
            }
        });
    }
    
    private void configureTransactionsContainer() {
        LinearLayout container = findViewById(R.id.transactionsContainer);
        
        // Apply spacing to the container
        container.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        container.setDividerDrawable(null); // No visual divider, just spacing
        
        // Convert 40dp to pixels for proper spacing
        final float scale = getResources().getDisplayMetrics().density;
        int spacingInPixels = (int) (40 * scale + 0.5f);
        
        // Set vertical spacing between items
        container.setDividerPadding(spacingInPixels);
        
        // Add some initial padding too
        container.setPadding(0, (int)(10 * scale + 0.5f), 0, 0);
    }
}
