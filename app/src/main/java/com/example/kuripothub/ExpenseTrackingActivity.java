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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.animation.ObjectAnimator;
import android.widget.FrameLayout;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

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
        
        // Set click listener for View All button
        TextView viewAllText = findViewById(R.id.viewAllText);
        viewAllText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseTrackingActivity.this, TransactionHistoryActivity.class);
                startActivity(intent);
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
        }
        
        // Add swipe functionality to the transaction item
        setupSwipeGesture(transactionItem, category, amountStr);
        
        // Add the transaction item to the container with proper layout parameters
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
    
    private void setupSwipeGesture(View transactionItem, String category, String originalAmount) {
        View foregroundView = transactionItem.findViewById(R.id.foregroundView);
        View editBackground = transactionItem.findViewById(R.id.editBackground);
        View deleteBackground = transactionItem.findViewById(R.id.deleteBackground);
        
        final boolean[] isAnimating = {false};
        
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 200; // Increased threshold
            private static final int SWIPE_VELOCITY_THRESHOLD = 150; // Increased velocity threshold

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1 == null || e2 == null || isAnimating[0]) return false;
                
                float diffX = e2.getX() - e1.getX();
                
                // Only handle horizontal scrolling
                if (Math.abs(diffX) > Math.abs(e2.getY() - e1.getY())) {
                    // Get the width of the view for full swipe calculation
                    int viewWidth = foregroundView.getWidth();
                    
                    // Limit the translation - allow full swipe to edges
                    float translationX = Math.max(-viewWidth, Math.min(viewWidth, diffX));
                    
                    foregroundView.setTranslationX(translationX);
                    
                    if (translationX > 0) {
                        // Swiping right - show edit background
                        editBackground.setVisibility(View.VISIBLE);
                        deleteBackground.setVisibility(View.GONE);
                        // Make background fully visible as we approach full swipe
                        editBackground.setAlpha(Math.min(1.0f, Math.abs(translationX) / (viewWidth * 0.3f)));
                    } else if (translationX < 0) {
                        // Swiping left - show delete background
                        deleteBackground.setVisibility(View.VISIBLE);
                        editBackground.setVisibility(View.GONE);
                        // Make background fully visible as we approach full swipe
                        deleteBackground.setAlpha(Math.min(1.0f, Math.abs(translationX) / (viewWidth * 0.3f)));
                    } else {
                        editBackground.setVisibility(View.GONE);
                        deleteBackground.setVisibility(View.GONE);
                    }
                    
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || isAnimating[0]) return false;
                
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    int viewWidth = foregroundView.getWidth();
                    
                    android.util.Log.d("SwipeGesture", "diffX: " + diffX + ", diffY: " + diffY + ", velocityX: " + velocityX + ", viewWidth: " + viewWidth);
                    
                    // Check if it's a horizontal swipe (more horizontal than vertical)
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        // Check if swipe distance is significant (at least 60% of view width) OR high velocity
                        boolean significantDistance = Math.abs(diffX) > (viewWidth * 0.6);
                        boolean highVelocity = Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD;
                        
                        if (significantDistance || (Math.abs(diffX) > SWIPE_THRESHOLD && highVelocity)) {
                            android.util.Log.d("SwipeGesture", "Swipe detected! Direction: " + (diffX > 0 ? "RIGHT" : "LEFT"));
                            
                            isAnimating[0] = true;
                            
                            if (diffX > 0) {
                                // Swipe from left to right - Edit transaction
                                android.util.Log.d("SwipeGesture", "Triggering EDIT");
                                animateAndTriggerEdit(foregroundView, editBackground, deleteBackground, transactionItem, category, originalAmount, isAnimating);
                                return true;
                            } else {
                                // Swipe from right to left - Delete transaction
                                android.util.Log.d("SwipeGesture", "Triggering DELETE");
                                animateAndTriggerDelete(foregroundView, editBackground, deleteBackground, transactionItem, originalAmount, isAnimating);
                                return true;
                            }
                        } else {
                            // Not enough swipe distance/velocity - snap back
                            snapBack(foregroundView, editBackground, deleteBackground);
                            android.util.Log.d("SwipeGesture", "Swipe too short or slow - snapping back. Distance: " + Math.abs(diffX) + ", Required: " + (viewWidth * 0.6));
                        }
                    } else {
                        // Not horizontal swipe - snap back
                        snapBack(foregroundView, editBackground, deleteBackground);
                        android.util.Log.d("SwipeGesture", "Not horizontal swipe");
                    }
                } catch (Exception exception) {
                    android.util.Log.e("SwipeGesture", "Error in swipe detection", exception);
                    exception.printStackTrace();
                }
                return false;
            }
        });

        transactionItem.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = gestureDetector.onTouchEvent(event);
                
                // Handle touch up event to snap back if not enough swipe
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (!isAnimating[0]) {
                        float currentTranslation = foregroundView.getTranslationX();
                        int viewWidth = foregroundView.getWidth();
                        
                        // Only trigger action if swiped more than 70% of the width
                        if (Math.abs(currentTranslation) > (viewWidth * 0.7)) {
                            isAnimating[0] = true;
                            if (currentTranslation > 0) {
                                // Complete edit swipe
                                animateAndTriggerEdit(foregroundView, editBackground, deleteBackground, transactionItem, category, originalAmount, isAnimating);
                            } else {
                                // Complete delete swipe
                                animateAndTriggerDelete(foregroundView, editBackground, deleteBackground, transactionItem, originalAmount, isAnimating);
                            }
                        } else {
                            // Not enough swipe - snap back
                            snapBack(foregroundView, editBackground, deleteBackground);
                        }
                    }
                }
                
                return result || event.getAction() != MotionEvent.ACTION_DOWN;
            }
        });
    }

    private void snapBack(View foregroundView, View editBackground, View deleteBackground) {
        ObjectAnimator.ofFloat(foregroundView, "translationX", 0).setDuration(200).start();
        editBackground.setVisibility(View.GONE);
        deleteBackground.setVisibility(View.GONE);
    }

    private void animateAndTriggerEdit(View foregroundView, View editBackground, View deleteBackground, 
                                     View transactionItem, String category, String originalAmount, boolean[] isAnimating) {
        // Animate to completely off-screen to the right
        int viewWidth = foregroundView.getWidth();
        ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundView, "translationX", viewWidth);
        animator.setDuration(300);
        animator.start();
        
        // Show edit dialog after animation completes
        foregroundView.postDelayed(() -> {
            snapBack(foregroundView, editBackground, deleteBackground);
            isAnimating[0] = false;
            onSwipeRight(transactionItem, category, originalAmount);
        }, 350);
    }

    private void animateAndTriggerDelete(View foregroundView, View editBackground, View deleteBackground,
                                       View transactionItem, String originalAmount, boolean[] isAnimating) {
        // Animate to completely off-screen to the left
        int viewWidth = foregroundView.getWidth();
        ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundView, "translationX", -viewWidth);
        animator.setDuration(300);
        animator.start();
        
        // Show delete dialog after animation completes
        foregroundView.postDelayed(() -> {
            snapBack(foregroundView, editBackground, deleteBackground);
            isAnimating[0] = false;
            onSwipeLeft(transactionItem, originalAmount);
        }, 350);
    }

    private void onSwipeLeft(View transactionItem, String originalAmount) {
        // Show confirmation dialog for delete
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTransaction(transactionItem, originalAmount);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onSwipeRight(View transactionItem, String category, String originalAmount) {
        // Show edit amount dialog
        showEditAmountDialog(transactionItem, category, originalAmount);
    }

    private void deleteTransaction(View transactionItem, String originalAmount) {
        try {
            // Parse the original amount and add it back to the budget
            double amountValue = Double.parseDouble(originalAmount.replaceAll("[^\\d.]", ""));
            currentBudget += amountValue;
            budgetAmountText.setText("P" + String.format("%.2f", currentBudget));
            
            // Remove the transaction item from the container
            LinearLayout container = findViewById(R.id.transactionsContainer);
            container.removeView(transactionItem);
            
            Toast.makeText(this, "Transaction deleted", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Error deleting transaction", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditAmountDialog(View transactionItem, String category, String originalAmount) {
        final Dialog editDialog = new Dialog(this);
        editDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editDialog.setContentView(R.layout.amount_entry_modal);
        
        // Make dialog background transparent and apply animations
        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            editDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            
            // Set the dialog width to match parent with margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(editDialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            editDialog.getWindow().setAttributes(layoutParams);
        }

        // Set up UI elements
        TextView categoryTitle = editDialog.findViewById(R.id.categoryTitle);
        EditText amountInput = editDialog.findViewById(R.id.amountInput);
        CardView cancelButton = editDialog.findViewById(R.id.cancelButton);
        CardView confirmButton = editDialog.findViewById(R.id.confirmButton);

        // Set category title and pre-fill with current amount
        categoryTitle.setText("EDIT " + category.toUpperCase());
        amountInput.setText(originalAmount.replaceAll("[^\\d.]", ""));

        // Set up button listeners
        cancelButton.setOnClickListener(v -> editDialog.dismiss());
        
        confirmButton.setOnClickListener(v -> {
            String newAmountText = amountInput.getText().toString().trim();
            if (!newAmountText.isEmpty()) {
                // Update the transaction with new amount
                updateTransaction(transactionItem, category, originalAmount, newAmountText);
                editDialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        editDialog.show();
    }

    private void updateTransaction(View transactionItem, String category, String originalAmount, String newAmount) {
        try {
            // Parse amounts
            double oldAmountValue = Double.parseDouble(originalAmount.replaceAll("[^\\d.]", ""));
            double newAmountValue = Double.parseDouble(newAmount.replaceAll("[^\\d.]", ""));
            
            // Calculate the difference and update budget
            double difference = newAmountValue - oldAmountValue;
            currentBudget -= difference;
            
            // Make sure budget doesn't go below zero
            if (currentBudget < 0) {
                currentBudget = 0;
            }
            
            // Update budget display
            budgetAmountText.setText("P" + String.format("%.2f", currentBudget));
            
            // Update the transaction amount display
            TextView transactionAmount = transactionItem.findViewById(R.id.transactionAmount);
            transactionAmount.setText("-P" + String.format("%.2f", newAmountValue));
            
            // Update the swipe gesture to use the new amount
            setupSwipeGesture(transactionItem, category, newAmount);
            
            Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
}
