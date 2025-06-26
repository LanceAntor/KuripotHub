package com.example.kuripothub;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import com.example.kuripothub.models.Expense;
import com.example.kuripothub.models.User;
import com.example.kuripothub.utils.FirebaseManager;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseTrackingActivity extends AppCompatActivity {

    private static final String TAG = "ExpenseTrackingActivity";
    
    private BottomSheetDialog categoryBottomSheet;
    private View categoryBottomSheetView;
    private double currentBudget = 2000.00; // Default budget
    private TextView budgetAmountText;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    // Track which meal categories have been used today
    private boolean breakfastUsed = false;
    private boolean lunchUsed = false;
    private boolean dinnerUsed = false;

    @Override
    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.expense_tracking_activity);
        
        firebaseManager = FirebaseManager.getInstance();
        
        // Check if user is logged in
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser == null) {
            // Redirect to login if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        currentUserId = currentUser.getUid();
        
        // Initialize budget amount text view
        budgetAmountText = findViewById(R.id.budgetAmount);
        
        // Load user data from Firebase
        loadUserData();
        
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
        
        // Set click listener for Budget Edit button
        View editContainer = findViewById(R.id.editContainer);
        editContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBudgetEditDialog();
            }
        });
        
        // Set click listener for Settings icon
        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });
    }

    private void setupCategoryBottomSheet() {
        // Initialize the bottom sheet dialog with transparent background (no dimming)
        categoryBottomSheet = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        categoryBottomSheetView = getLayoutInflater().inflate(R.layout.category_bottom_sheet, null);
        categoryBottomSheet.setContentView(categoryBottomSheetView);
        // Remove the dim effect
        if (categoryBottomSheet.getWindow() != null) {
            categoryBottomSheet.getWindow().setDimAmount(0f);
        }

        // Set up click listeners for each category
        View breakfastOption = categoryBottomSheetView.findViewById(R.id.breakfastOption);
        View lunchOption = categoryBottomSheetView.findViewById(R.id.lunchOption);
        View dinnerOption = categoryBottomSheetView.findViewById(R.id.dinnerOption);
        View othersOption = categoryBottomSheetView.findViewById(R.id.othersOption);

        // Set click listeners for each category
        breakfastOption.setOnClickListener(v -> handleCategorySelection("Breakfast"));
        lunchOption.setOnClickListener(v -> handleCategorySelection("Lunch"));
        dinnerOption.setOnClickListener(v -> handleCategorySelection("Dinner"));
        othersOption.setOnClickListener(v -> handleCategorySelection("Others"));
        
        // Update the category states when the bottom sheet is shown
        updateCategoryStates(categoryBottomSheetView);
    }    private void showCategoryOptions() {
        if (categoryBottomSheet != null) {
            // Update category states before showing
            updateCategoryStates(categoryBottomSheetView);
            
            // Set behavior to eliminate background dimming effect
            categoryBottomSheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            categoryBottomSheet.show();
        }
    }    private void handleCategorySelection(String category) {
        // Check if the category is disabled for meal types
        if (isCategoryDisabled(category)) {
            Toast.makeText(this, category + " has already been added today", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Close the bottom sheet
        if (categoryBottomSheet != null && categoryBottomSheet.isShowing()) {
            categoryBottomSheet.dismiss();
        }
        
        // Show amount entry modal for all categories including Others
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
            
            // Save expense to Firebase
            saveExpenseToFirebase(category, expenseAmount);
            
            // Decrease the budget
            updateBudget(expenseAmount);
            
            // Add the transaction to the transactions container
            addTransactionToView(category, amount);
            
            // Mark the category as used if it's a meal type
            markCategoryAsUsed(category);
            
            Toast.makeText(this, category + " expense added: P" + String.format("%.2f", expenseAmount), Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateBudget(double expenseAmount) {
        // Subtract the expense from the current budget
        currentBudget -= expenseAmount;
        
        // Allow budget to go negative to show overspending
        // Update the budget text view with color indication
        updateBudgetDisplay();
        
        // Update budget in Firebase
        updateBudgetInFirebase();
    }
    
    private void updateBudgetDisplay() {
        // Format the budget text
        String budgetText = "P" + String.format("%.2f", Math.abs(currentBudget));
        
        if (currentBudget < 0) {
            // Show negative budget in red with minus sign
            budgetAmountText.setText("-" + budgetText);
            budgetAmountText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            // Show positive budget in default color
            budgetAmountText.setText(budgetText);
            budgetAmountText.setTextColor(getResources().getColor(android.R.color.black));
        }
    }
    
    private void addTransactionToView(String category, String amountStr) {
        addTransactionToView(category, amountStr, null);
    }
    
    private void addTransactionToView(String category, String amountStr, String expenseId) {
        // Find the transactions container
        View transactionItem = getLayoutInflater().inflate(R.layout.transaction_item, null);
        
        // Store the expense ID as a tag for later deletion
        if (expenseId != null) {
            transactionItem.setTag(expenseId);
        }
        
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
            case "others":
                categoryIcon.setImageResource(R.drawable.other); // You can change this to a different icon for Others
                break;
            default:
                categoryIcon.setImageResource(R.drawable.coffee); // Default icon
                break;
        }
        
        // Add swipe functionality to the transaction item
        setupSwipeGesture(transactionItem, category, amountStr);
        
        // Ensure the transaction item can receive touch events properly
        transactionItem.setClickable(true);
        transactionItem.setFocusable(true);
        transactionItem.setFocusableInTouchMode(true);
        
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
        
        // Enable better touch handling for child views
        container.setMotionEventSplittingEnabled(false);
        container.setDescendantFocusability(LinearLayout.FOCUS_AFTER_DESCENDANTS);
    }
    
    private void setupSwipeGesture(View transactionItem, String category, String originalAmount) {
        View foregroundView = transactionItem.findViewById(R.id.foregroundView);
        View editBackground = transactionItem.findViewById(R.id.editBackground);
        View deleteBackground = transactionItem.findViewById(R.id.deleteBackground);
        
        final boolean[] isAnimating = {false};
        final float[] initialX = {0};
        final float[] initialY = {0};
        final boolean[] isSwipeStarted = {false};
        
        transactionItem.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isAnimating[0]) return true;
                
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX[0] = event.getX();
                        initialY[0] = event.getY();
                        isSwipeStarted[0] = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        if (foregroundView.getWidth() == 0) return false;
                        
                        float deltaX = event.getX() - initialX[0];
                        float deltaY = event.getY() - initialY[0];
                        
                        // Check if this is a horizontal swipe
                        if (!isSwipeStarted[0]) {
                            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
                                isSwipeStarted[0] = true;
                                // Request that parent views don't intercept touch events
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            } else if (Math.abs(deltaY) > 20) {
                                // This is a vertical scroll, let parent handle it
                                return false;
                            }
                        }
                        
                        if (isSwipeStarted[0]) {
                            int viewWidth = foregroundView.getWidth();
                            
                            // Limit the translation to view bounds
                            float translationX = Math.max(-viewWidth * 0.8f, Math.min(viewWidth * 0.8f, deltaX));
                            
                            foregroundView.setTranslationX(translationX);
                            
                            if (translationX > 50) {
                                // Swiping right - show edit background
                                editBackground.setVisibility(View.VISIBLE);
                                deleteBackground.setVisibility(View.GONE);
                                // Gradual alpha based on swipe distance
                                float alpha = Math.min(1.0f, Math.abs(translationX) / (viewWidth * 0.4f));
                                editBackground.setAlpha(alpha);
                            } else if (translationX < -50) {
                                // Swiping left - show delete background
                                deleteBackground.setVisibility(View.VISIBLE);
                                editBackground.setVisibility(View.GONE);
                                // Gradual alpha based on swipe distance
                                float alpha = Math.min(1.0f, Math.abs(translationX) / (viewWidth * 0.4f));
                                deleteBackground.setAlpha(alpha);
                            } else {
                                editBackground.setVisibility(View.GONE);
                                deleteBackground.setVisibility(View.GONE);
                            }
                            
                            return true;
                        }
                        break;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        
                        if (isSwipeStarted[0] && !isAnimating[0]) {
                            float currentTranslation = foregroundView.getTranslationX();
                            int viewWidth = foregroundView.getWidth();
                            
                            // Lower threshold for triggering actions (40% instead of 70%)
                            float threshold = viewWidth * 0.4f;
                            
                            if (Math.abs(currentTranslation) > threshold) {
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
                        return true;
                }
                
                return false;
            }
        });
    }

    private void snapBack(View foregroundView, View editBackground, View deleteBackground) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundView, "translationX", 0);
        animator.setDuration(150); // Faster animation
        animator.start();
        
        // Hide backgrounds immediately for better responsiveness
        editBackground.setVisibility(View.GONE);
        deleteBackground.setVisibility(View.GONE);
    }

    private void animateAndTriggerEdit(View foregroundView, View editBackground, View deleteBackground, 
                                     View transactionItem, String category, String originalAmount, boolean[] isAnimating) {
        // Animate to completely off-screen to the right
        int viewWidth = foregroundView.getWidth();
        ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundView, "translationX", viewWidth);
        animator.setDuration(200); // Faster animation
        animator.start();
        
        // Show edit dialog after a shorter delay
        foregroundView.postDelayed(() -> {
            snapBack(foregroundView, editBackground, deleteBackground);
            isAnimating[0] = false;
            onSwipeRight(transactionItem, category, originalAmount);
        }, 220);
    }

    private void animateAndTriggerDelete(View foregroundView, View editBackground, View deleteBackground,
                                       View transactionItem, String originalAmount, boolean[] isAnimating) {
        // Animate to completely off-screen to the left
        int viewWidth = foregroundView.getWidth();
        ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundView, "translationX", -viewWidth);
        animator.setDuration(200); // Faster animation
        animator.start();
        
        // Show delete dialog after a shorter delay
        foregroundView.postDelayed(() -> {
            snapBack(foregroundView, editBackground, deleteBackground);
            isAnimating[0] = false;
            onSwipeLeft(transactionItem, originalAmount);
        }, 220);
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
            // Get the category from the transaction item before deletion
            TextView categoryNameView = transactionItem.findViewById(R.id.categoryName);
            String category = categoryNameView != null ? categoryNameView.getText().toString() : "";
            
            // Parse the original amount and add it back to the budget
            double amountValue = Double.parseDouble(originalAmount.replaceAll("[^\\d.]", ""));
            currentBudget += amountValue;
            updateBudgetDisplay();
            
            // Get the expense ID from the transaction item's tag
            String expenseId = (String) transactionItem.getTag();
            
            if (expenseId != null) {
                // Delete from Firebase first
                firebaseManager.deleteExpense(expenseId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Expense deleted from Firebase successfully");
                            // Update budget in Firebase after successful deletion
                            updateBudgetInFirebase();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error deleting expense from Firebase", e);
                            Toast.makeText(this, "Failed to delete expense from database", Toast.LENGTH_SHORT).show();
                            // Revert budget change if deletion failed
                            currentBudget -= amountValue;
                            updateBudgetDisplay();
                            return;
                        });
            } else {
                Log.w(TAG, "No expense ID found for transaction item, skipping Firebase deletion");
                // Still update budget in Firebase for local transactions
                updateBudgetInFirebase();
            }
            
            // Remove the transaction item from the container
            LinearLayout container = findViewById(R.id.transactionsContainer);
            container.removeView(transactionItem);
            
            // Mark the category as unused if it's a meal type
            if (!category.isEmpty()) {
                markCategoryAsUnused(category);
                Log.d(TAG, "Marked category as unused: " + category);
            }
            
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
            
            // Update budget display (allow negative values)
            updateBudgetDisplay();
            
            // Get the expense ID from the transaction item's tag
            String expenseId = (String) transactionItem.getTag();
            
            if (expenseId != null) {
                // Create updated expense object
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                
                Expense updatedExpense = new Expense(
                    currentUserId,
                    category,
                    newAmountValue,
                    "", // Description can be empty for now
                    currentDate
                );
                updatedExpense.setId(expenseId);
                
                // Update in Firebase
                firebaseManager.updateExpense(expenseId, updatedExpense)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Expense updated in Firebase successfully");
                            // Update budget in Firebase after successful update
                            updateBudgetInFirebase();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error updating expense in Firebase", e);
                            Toast.makeText(this, "Failed to update expense in database", Toast.LENGTH_SHORT).show();
                            // Revert budget change if update failed
                            currentBudget += difference;
                            updateBudgetDisplay();
                            return;
                        });
            } else {
                Log.w(TAG, "No expense ID found for transaction item, skipping Firebase update");
                // Still update budget in Firebase for local transactions
                updateBudgetInFirebase();
            }
            
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
    
    private void showBudgetEditDialog() {
        final Dialog budgetDialog = new Dialog(this);
        budgetDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        budgetDialog.setContentView(R.layout.amount_entry_modal);
        
        // Make dialog background transparent and apply animations
        if (budgetDialog.getWindow() != null) {
            budgetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            budgetDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            
            // Set the dialog width to match parent with margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(budgetDialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            budgetDialog.getWindow().setAttributes(layoutParams);
        }

        // Set up UI elements
        TextView categoryTitle = budgetDialog.findViewById(R.id.categoryTitle);
        EditText amountInput = budgetDialog.findViewById(R.id.amountInput);
        CardView cancelButton = budgetDialog.findViewById(R.id.cancelButton);
        CardView confirmButton = budgetDialog.findViewById(R.id.confirmButton);

        // Set dialog title and pre-fill with current budget
        categoryTitle.setText("BUDGET");
        amountInput.setText(String.format("%.2f", currentBudget));
        amountInput.setHint("Enter new budget amount");

        // Set up button listeners
        cancelButton.setOnClickListener(v -> budgetDialog.dismiss());
        
        confirmButton.setOnClickListener(v -> {
            String newBudgetText = amountInput.getText().toString().trim();
            if (!newBudgetText.isEmpty()) {
                try {
                    double newBudget = Double.parseDouble(newBudgetText.replaceAll("[^\\d.]", ""));
                    if (newBudget >= 0) {
                        // Update the budget
                        currentBudget = newBudget;
                        updateBudgetDisplay();
                        
                        // Update budget in Firebase
                        updateBudgetInFirebase();
                        
                        Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show();
                        budgetDialog.dismiss();
                    } else {
                        Toast.makeText(this, "Budget cannot be negative", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter a budget amount", Toast.LENGTH_SHORT).show();
            }
        });

        budgetDialog.show();
    }

    // Firebase methods
    private void loadUserData() {
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            currentBudget = user.getBudget();
                            updateBudgetDisplay();
                            Log.d(TAG, "User budget loaded: " + currentBudget);
                        }
                    } else {
                        Log.d(TAG, "No user profile found, using default budget");
                        updateBudgetDisplay();
                    }
                    
                    // After loading user data, load today's expenses
                    loadTodaysExpenses();
                    
                    // Debug: Check all user expenses
                    debugUserExpenses();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading user data", e);
                    updateBudgetDisplay();
                    
                    // Even if user data fails, try to load expenses
                    loadTodaysExpenses();
                    
                    // Debug: Check all user expenses
                    debugUserExpenses();
                });
    }
    
    private void saveExpenseToFirebase(String category, double amount) {
        // Create current date string
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        
        Log.d(TAG, "Saving expense: " + category + " - P" + amount + " for date: " + currentDate);
        
        Expense expense = new Expense(
            currentUserId,
            category,
            amount,
            "", // Description can be empty for now
            currentDate
        );
        
        firebaseManager.addExpense(expense)
                .addOnSuccessListener(documentReference -> {
                    String expenseId = documentReference.getId();
                    Log.d(TAG, "Expense saved successfully to Firestore with ID: " + expenseId);
                    
                    // Find the most recently added transaction view and set its tag to the expense ID
                    LinearLayout container = findViewById(R.id.transactionsContainer);
                    if (container.getChildCount() > 0) {
                        View mostRecentTransaction = container.getChildAt(0); // Most recent is at index 0
                        mostRecentTransaction.setTag(expenseId);
                        Log.d(TAG, "Set expense ID tag on transaction view: " + expenseId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving expense to Firestore", e);
                    Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void updateBudgetInFirebase() {
        firebaseManager.updateUserBudget(currentUserId, currentBudget)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Budget updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating budget", e);
                });
    }

    private void showLogoutDialog() {
        final Dialog logoutDialog = new Dialog(this);
        logoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        logoutDialog.setContentView(R.layout.logout_dialog);
        
        // Make dialog background transparent and add animation
        if (logoutDialog.getWindow() != null) {
            logoutDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            logoutDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        // Set up button listeners
        CardView notNowButton = logoutDialog.findViewById(R.id.notNowButton);
        CardView yesButton = logoutDialog.findViewById(R.id.yesButton);

        notNowButton.setOnClickListener(v -> logoutDialog.dismiss());
        
        yesButton.setOnClickListener(v -> {
            logoutDialog.dismiss();
            performLogout();
        });

        logoutDialog.show();
    }
    
    private void performLogout() {
        firebaseManager.signOut();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void loadTodaysExpenses() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load expenses: currentUserId is null");
            return;
        }
        
        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        Log.d(TAG, "Loading today's expenses for user: " + currentUserId + ", date: " + today);
        
        // Try the simple date-based query first (no orderBy to avoid index issues)
        firebaseManager.getUserExpensesByDateSimple(currentUserId, today)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Simple date query returned " + queryDocumentSnapshots.size() + " expenses");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        // If no results, try a broader query to check if there are any expenses for this user
                        Log.d(TAG, "No expenses found for date, trying broader query...");
                        loadTodaysExpensesAlternative();
                        return;
                    }
                    
                    // Clear existing transaction views
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add each expense to the UI and sort manually if needed
                    List<Expense> todayExpenses = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId()); // Set the document ID
                            todayExpenses.add(expense);
                            Log.d(TAG, "Found expense: " + expense.getCategory() + " - P" + expense.getAmount() + " (Date: " + expense.getDate() + ")");
                        } else {
                            Log.w(TAG, "Failed to convert document to Expense object: " + document.getId());
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    todayExpenses.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    
                    // Add sorted expenses to UI
                    for (Expense expense : todayExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    // Check which categories are already used after loading expenses
                    checkExistingCategoriesForToday();
                    
                    Log.d(TAG, "Finished loading " + todayExpenses.size() + " expenses for today");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading today's expenses with simple date query", e);
                    // Try alternative method if date query fails
                    loadTodaysExpensesAlternative();
                });
    }
    
    private void loadTodaysExpensesAlternative() {
        // Fallback: Get all user expenses and filter locally for today
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        Log.d(TAG, "Using alternative method to load today's expenses");
        
        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Alternative query returned " + queryDocumentSnapshots.size() + " total expenses");
                    
                    // Clear existing transaction views
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    int todayCount = 0;
                    // Filter for today's expenses
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null && today.equals(expense.getDate())) {
                            addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), document.getId());
                            Log.d(TAG, "Found today's expense: " + expense.getCategory() + " - P" + expense.getAmount());
                            todayCount++;
                        }
                    }
                    
                    // Check which categories are already used after loading expenses
                    checkExistingCategoriesForToday();
                    
                    Log.d(TAG, "Found " + todayCount + " expenses for today using alternative method");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading expenses with alternative method", e);
                });
    }
    
    private void clearDynamicTransactions(LinearLayout container) {
        // Remove all views that are transaction items (not static headers)
        int childCount = container.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = container.getChildAt(i);
            // Check if it's a dynamically added transaction item
            // Transaction items are inflated from R.layout.transaction_item
            if (child.findViewById(R.id.categoryName) != null && 
                child.findViewById(R.id.transactionAmount) != null) {
                container.removeViewAt(i);
                Log.d(TAG, "Removed existing transaction item at index: " + i);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh today's expenses when the activity resumes
        if (currentUserId != null) {
            Log.d(TAG, "Activity resumed, refreshing today's expenses");
            // Reset category states first, they will be set correctly after loading expenses
            resetDailyCategoryStates();
            loadTodaysExpenses();
        }
    }
    
    private void debugUserExpenses() {
        if (currentUserId == null) return;
        
        Log.d(TAG, "=== DEBUG: Checking all user expenses ===");
        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Total user expenses: " + queryDocumentSnapshots.size());
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String today = dateFormat.format(new Date());
                    
                    int todayCount = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            Log.d(TAG, "Expense: " + expense.getCategory() + " - P" + expense.getAmount() + 
                                  " (Date: " + expense.getDate() + ", Today: " + today + ", Match: " + today.equals(expense.getDate()) + ")");
                            if (today.equals(expense.getDate())) {
                                todayCount++;
                            }
                        }
                    }
                    Log.d(TAG, "Expenses for today (" + today + "): " + todayCount);
                    Log.d(TAG, "=== END DEBUG ===");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Debug query failed", e);
                });
    }
    
    // Helper methods for category management
    private boolean isCategoryDisabled(String category) {
        switch (category.toLowerCase()) {
            case "breakfast":
                return breakfastUsed;
            case "lunch":
                return lunchUsed;
            case "dinner":
                return dinnerUsed;
            default:
                return false; // Others category is never disabled
        }
    }
    
    private void markCategoryAsUsed(String category) {
        switch (category.toLowerCase()) {
            case "breakfast":
                breakfastUsed = true;
                break;
            case "lunch":
                lunchUsed = true;
                break;
            case "dinner":
                dinnerUsed = true;
                break;
            // Others category doesn't get marked as used
        }
    }
    
    private void markCategoryAsUnused(String category) {
        switch (category.toLowerCase()) {
            case "breakfast":
                breakfastUsed = false;
                break;
            case "lunch":
                lunchUsed = false;
                break;
            case "dinner":
                dinnerUsed = false;
                break;
            // Others category doesn't get marked as unused
        }
    }
    
    private void updateCategoryStates(View bottomSheetView) {
        if (bottomSheetView == null) return;
        
        View breakfastOption = bottomSheetView.findViewById(R.id.breakfastOption);
        View lunchOption = bottomSheetView.findViewById(R.id.lunchOption);
        View dinnerOption = bottomSheetView.findViewById(R.id.dinnerOption);
        View othersOption = bottomSheetView.findViewById(R.id.othersOption);
        
        // Update breakfast state
        if (breakfastOption != null) {
            breakfastOption.setEnabled(!breakfastUsed);
            breakfastOption.setAlpha(breakfastUsed ? 0.5f : 1.0f);
        }
        
        // Update lunch state
        if (lunchOption != null) {
            lunchOption.setEnabled(!lunchUsed);
            lunchOption.setAlpha(lunchUsed ? 0.5f : 1.0f);
        }
        
        // Update dinner state
        if (dinnerOption != null) {
            dinnerOption.setEnabled(!dinnerUsed);
            dinnerOption.setAlpha(dinnerUsed ? 0.5f : 1.0f);
        }
        
        // Others option is always enabled
        if (othersOption != null) {
            othersOption.setEnabled(true);
            othersOption.setAlpha(1.0f);
        }
    }
    
    private void resetDailyCategoryStates() {
        breakfastUsed = false;
        lunchUsed = false;
        dinnerUsed = false;
    }
    
    private void checkExistingCategoriesForToday() {
        // This method will be called after loading today's expenses
        // to set the correct category states based on existing expenses
        LinearLayout container = findViewById(R.id.transactionsContainer);
        
        // Reset states first
        resetDailyCategoryStates();
        
        // Check each transaction item to see which categories are already used
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView categoryName = child.findViewById(R.id.categoryName);
            if (categoryName != null) {
                String category = categoryName.getText().toString().toLowerCase();
                markCategoryAsUsed(category);
            }
        }
        
        Log.d(TAG, "Category states after check - Breakfast: " + breakfastUsed + 
              ", Lunch: " + lunchUsed + ", Dinner: " + dinnerUsed);
    }
}
