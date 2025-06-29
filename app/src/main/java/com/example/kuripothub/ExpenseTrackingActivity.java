package com.example.kuripothub;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.Gravity;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

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
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpenseTrackingActivity extends AppCompatActivity {

    private static final String TAG = "ExpenseTrackingActivity";
    
    private BottomSheetDialog categoryBottomSheet;
    private View categoryBottomSheetView;
    private double currentBudget = 2000.00; // Default budget
    private TextView budgetAmountText;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    // Flag to prevent multiple simultaneous expense loads
    private boolean isLoadingExpenses = false;
    
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
        
        // Initialize username text view
        TextView usernameText = findViewById(R.id.usernameText);
        
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
        
        // Set click listener for Analytics button
        TextView analyticsText = findViewById(R.id.analyticsText);
        analyticsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseTrackingActivity.this, ExpenseSummaryActivity.class);
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
        
        // Set click listener for Grid/Menu icon
        ImageView gridIcon = findViewById(R.id.gridIcon);
        gridIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPersonaActivity();
            }
        });
        
        // Set click listener for Chart icon
        ImageView chartIcon = findViewById(R.id.chartIcon);
        chartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseTrackingActivity.this, ExpenseSummaryActivity.class);
                startActivity(intent);
            }
        });
        
        // Set click listener for Profile icon
        com.google.android.material.imageview.ShapeableImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProfileModal();
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
        
        // Handle "Others" category by showing more categories modal
        if ("Others".equals(category)) {
            showMoreCategoriesModal();
        } else {
            // Show amount entry modal for meal categories
            showAmountEntryModal(category);
        }
    }
    
    private void showMoreCategoriesModal() {
        final BottomSheetDialog moreCategoriesDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View moreCategoriesView = getLayoutInflater().inflate(R.layout.more_categories_modal, null);
        moreCategoriesDialog.setContentView(moreCategoriesView);
        
        // Remove the dim effect to make background transparent
        if (moreCategoriesDialog.getWindow() != null) {
            moreCategoriesDialog.getWindow().setDimAmount(0f);
        }
        
        // Set behavior to eliminate background dimming effect and ensure proper display
        moreCategoriesDialog.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        
        // Get references to category options
        View snackOption = moreCategoriesView.findViewById(R.id.snackOption);
        View fareOption = moreCategoriesView.findViewById(R.id.fareOption);
        View mobileLoadOption = moreCategoriesView.findViewById(R.id.mobileLoadOption);
        View printingOption = moreCategoriesView.findViewById(R.id.printingOption);
        View schoolSuppliesOption = moreCategoriesView.findViewById(R.id.schoolSuppliesOption);
        View laundryOption = moreCategoriesView.findViewById(R.id.laundryOption);
        View otherOption = moreCategoriesView.findViewById(R.id.otherOption);
        
        // Set click listeners for each subcategory
        snackOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Snack");
        });
        
        fareOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Fare");
        });
        
        mobileLoadOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Mobile Load");
        });
        
        printingOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Printing");
        });
        
        schoolSuppliesOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("School Supplies");
        });
        
        laundryOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Laundry");
        });
        
        otherOption.setOnClickListener(v -> {
            moreCategoriesDialog.dismiss();
            showAmountEntryModal("Other");
        });
        
        moreCategoriesDialog.show();
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
        ImageView categoryIcon = amountDialog.findViewById(R.id.categoryIcon);
        EditText amountInput = amountDialog.findViewById(R.id.amountInput);
        CardView cancelButton = amountDialog.findViewById(R.id.cancelButton);
        CardView confirmButton = amountDialog.findViewById(R.id.confirmButton);

        // Set category title and icon
        categoryTitle.setText(category.toUpperCase());
        categoryIcon.setImageResource(getCategoryIconResource(category));

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
    }
    
    private int getCategoryIconResource(String category) {
        switch (category.toLowerCase()) {
            case "breakfast":
                return R.drawable.egg;
            case "lunch":
                return R.drawable.lunch;
            case "dinner":
                return R.drawable.wine_bottle;
            case "snack":
                return R.drawable.snack;
            case "fare":
                return R.drawable.fare;
            case "mobile load":
                return R.drawable.mobile_load;
            case "printing":
                return R.drawable.printing;
            case "school supplies":
                return R.drawable.school_supplies;
            case "laundry":
                return R.drawable.laundry;
            case "other":
            case "others":
            default:
                return R.drawable.other;
        }
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
        categoryIcon.setImageResource(getCategoryIconResource(category));
        
        // Mark category as used for meal restrictions
        markCategoryAsUsed(category);
        
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
                            // Remove from cache as well
                            removeExpenseFromCache(expenseId);
                            // Update budget in Firebase after successful deletion
                            updateBudgetInFirebase();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error deleting expense from Firebase", e);
                            
                            // If we're offline, still remove from cache
                            if (!isNetworkAvailable()) {
                                removeExpenseFromCache(expenseId);
                                Log.d(TAG, "Removed expense from cache while offline: " + expenseId);
                                // Update budget locally
                                updateBudgetInFirebase();
                            } else {
                                Toast.makeText(this, "Failed to delete expense from database", Toast.LENGTH_SHORT).show();
                                // Revert budget change if deletion failed
                                currentBudget -= amountValue;
                                updateBudgetDisplay();
                                return;
                            }
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
                            // Update in cache as well
                            updateExpenseInCache(expenseId, category, newAmountValue);
                            // Update budget in Firebase after successful update
                            updateBudgetInFirebase();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error updating expense in Firebase", e);
                            
                            // If we're offline, still update cache
                            if (!isNetworkAvailable()) {
                                updateExpenseInCache(expenseId, category, newAmountValue);
                                Log.d(TAG, "Updated expense in cache while offline: " + expenseId);
                                // Update budget locally
                                updateBudgetInFirebase();
                            } else {
                                Toast.makeText(this, "Failed to update expense in database", Toast.LENGTH_SHORT).show();
                                // Revert budget change if update failed
                                currentBudget += difference;
                                updateBudgetDisplay();
                                return;
                            }
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
        ImageView categoryIcon = budgetDialog.findViewById(R.id.categoryIcon);
        EditText amountInput = budgetDialog.findViewById(R.id.amountInput);
        CardView cancelButton = budgetDialog.findViewById(R.id.cancelButton);
        CardView confirmButton = budgetDialog.findViewById(R.id.confirmButton);

        // Set dialog title and show budget icon
        categoryTitle.setText("EDIT BUDGET");
        if (categoryIcon != null) {
            categoryIcon.setImageResource(R.drawable.budget); // Show budget icon
            categoryIcon.setVisibility(View.VISIBLE);
        }
        
        // Pre-fill with current budget
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
                            
                            // Update username display
                            TextView usernameText = findViewById(R.id.usernameText);
                            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                                usernameText.setText(user.getUsername());
                            } else {
                                usernameText.setText("User");
                            }
                            
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
        
        // Check if we're online or offline
        if (isNetworkAvailable()) {
            // Online: Try to save to Firebase first
            firebaseManager.addExpense(expense)
                    .addOnSuccessListener(documentReference -> {
                        String expenseId = documentReference.getId();
                        Log.d(TAG, "Expense saved successfully to Firestore with ID: " + expenseId);
                        
                        // Set the expense ID and add to cache
                        expense.setId(expenseId);
                        addExpenseToCache(expense);
                        
                        // Find the most recently added transaction view and set its tag to the expense ID
                        LinearLayout container = findViewById(R.id.transactionsContainer);
                        if (container.getChildCount() > 0) {
                            View mostRecentTransaction = container.getChildAt(0); // Most recent is at index 0
                            mostRecentTransaction.setTag(expenseId);
                            Log.d(TAG, "Set expense ID tag on transaction view: " + expenseId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error saving expense to Firestore while online", e);
                        // Even if Firebase fails while online, still add to cache with temp ID
                        String tempId = "temp_" + System.currentTimeMillis();
                        expense.setId(tempId);
                        addExpenseToCache(expense);
                        
                        // Update the transaction item tag
                        LinearLayout container = findViewById(R.id.transactionsContainer);
                        if (container.getChildCount() > 0) {
                            View mostRecentTransaction = container.getChildAt(0);
                            mostRecentTransaction.setTag(tempId);
                        }
//                        Toast.makeText(this, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Offline: Save directly to cache with temporary ID
            String tempId = "temp_" + System.currentTimeMillis();
            expense.setId(tempId);
            addExpenseToCache(expense);
            Log.d(TAG, "Added expense to cache with temporary ID while offline: " + tempId);
            
            // Find the most recently added transaction view and set its tag
            LinearLayout container = findViewById(R.id.transactionsContainer);
            if (container.getChildCount() > 0) {
                View mostRecentTransaction = container.getChildAt(0);
                mostRecentTransaction.setTag(tempId);
                Log.d(TAG, "Set temp ID tag on transaction view: " + tempId);
            }
            
//            Toast.makeText(this, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show();
        }
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

    private void showProfileModal() {
        final Dialog profileDialog = new Dialog(this);
        profileDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        profileDialog.setContentView(R.layout.profile_modal);
        
        // Make dialog background transparent, remove dimming, and add animation
        if (profileDialog.getWindow() != null) {
            profileDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            profileDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            profileDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            
            // Set gravity to top-left to prevent centering
            WindowManager.LayoutParams params = profileDialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            params.x = 0; // Distance from left edge
            params.y = 70; // Distance from top edge
            profileDialog.getWindow().setAttributes(params);
        }

        // Get username and set it
        TextView usernameText = profileDialog.findViewById(R.id.usernameText);
        
        // Load user data from Firebase
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
                            usernameText.setText(user.getUsername());
                        } else {
                            usernameText.setText("User");
                        }
                    } else {
                        usernameText.setText("Guest");
                    }
                })
                .addOnFailureListener(e -> {
                    usernameText.setText("Guest");
                });

        // Set up button listeners
        CardView editProfileButton = profileDialog.findViewById(R.id.editProfileButton);
        CardView logoutButton = profileDialog.findViewById(R.id.logoutButton);

        editProfileButton.setOnClickListener(v -> {

            profileDialog.dismiss();
            Toast.makeText(this, "Edit Profile - Coming Soon!", Toast.LENGTH_SHORT).show();
        });
        
        logoutButton.setOnClickListener(v -> {
            profileDialog.dismiss();
            showLogoutDialog();
        });

        // Dismiss when clicking outside
        profileDialog.setOnCancelListener(dialog -> profileDialog.dismiss());

        profileDialog.show();
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
    
    private void openPersonaActivity() {
        Intent intent = new Intent(this, PersonaActivity.class);
        startActivity(intent);
    }

    private void loadTodaysExpenses() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load expenses: currentUserId is null");
            return;
        }
        
        // Prevent multiple simultaneous loads
        if (isLoadingExpenses) {
            Log.d(TAG, "Already loading expenses, skipping duplicate request");
            return;
        }
        
        isLoadingExpenses = true;
        Log.d(TAG, "Starting to load today's expenses");
        
        // Clear existing transaction views IMMEDIATELY before any async operations
        LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
        clearDynamicTransactions(transactionsContainer);
        
        // Always load from cache first for instant display
        loadExpensesFromCache();
        
        // Then sync with Firebase if online
        if (isNetworkAvailable()) {
            Log.d(TAG, "Online - syncing with Firebase");
            syncWithFirebase();
        } else {
            Log.d(TAG, "Offline - using cached data only");
            isLoadingExpenses = false;
        }
    }
    
    private void syncWithFirebase() {
        // Get today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        Log.d(TAG, "Syncing today's expenses for user: " + currentUserId + ", date: " + today);
        
        // First, sync any offline expenses to Firebase and wait for completion
        syncOfflineExpensesToFirebaseAndThenLoadFromFirebase(today);
    }
    
    /**
     * Sync offline expenses to Firebase first, then load all expenses from Firebase
     */
    private void syncOfflineExpensesToFirebaseAndThenLoadFromFirebase(String today) {
        List<Expense> offlineExpenses = getOfflineExpensesFromCache();
        
        if (offlineExpenses.isEmpty()) {
            // No offline expenses to sync, proceed with Firebase loading
            loadExpensesFromFirebaseOnly(today);
            return;
        }
        
        Log.d(TAG, "Syncing " + offlineExpenses.size() + " offline expenses to Firebase first");
        
        // Keep track of sync operations
        final int totalOfflineExpenses = offlineExpenses.size();
        final AtomicInteger syncedCount = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        
        // Sync each offline expense
        for (Expense offlineExpense : offlineExpenses) {
            syncSingleOfflineExpenseWithCallback(offlineExpense, new SyncCallback() {
                @Override
                public void onSyncSuccess(String tempId, String firebaseId) {
                    int synced = syncedCount.incrementAndGet();
                    Log.d(TAG, "Synced " + synced + "/" + totalOfflineExpenses + " offline expenses");
                    
                    // Check if all sync operations are complete
                    if (synced + failedCount.get() == totalOfflineExpenses) {
                        // All sync operations completed, now load from Firebase
                        Log.d(TAG, "All offline expenses processed. Loading from Firebase...");
                        loadExpensesFromFirebaseOnly(today);
                    }
                }
                
                @Override
                public void onSyncFailure(String tempId, Exception e) {
                    int failed = failedCount.incrementAndGet();
                    Log.w(TAG, "Failed to sync offline expense " + tempId + ": " + failed + "/" + totalOfflineExpenses, e);
                    
                    // Check if all sync operations are complete
                    if (syncedCount.get() + failed == totalOfflineExpenses) {
                        // All sync operations completed, now load from Firebase
                        Log.d(TAG, "All offline expenses processed (some failed). Loading from Firebase...");
                        loadExpensesFromFirebaseOnly(today);
                    }
                }
            });
        }
    }
    
    /**
     * Load expenses only from Firebase (no offline merging)
     */
    private void loadExpensesFromFirebaseOnly(String today) {
        // Try the simple date-based query first (no orderBy to avoid index issues)
        firebaseManager.getUserExpensesByDateSimple(currentUserId, today)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase sync returned " + queryDocumentSnapshots.size() + " expenses");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        // If no results, try a broader query to check if there are any expenses for this user
                        Log.d(TAG, "No expenses found for date, trying broader query...");
                        syncWithFirebaseAlternative();
                        return;
                    }
                    
                    // Process expenses from Firebase only (no offline merging)
                    List<Expense> firebaseExpenses = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId()); // Set the document ID
                            firebaseExpenses.add(expense);
                            Log.d(TAG, "Firebase expense: " + expense.getCategory() + " - P" + expense.getAmount() + " (Date: " + expense.getDate() + ")");
                        } else {
                            Log.w(TAG, "Failed to convert document to Expense object: " + document.getId());
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    firebaseExpenses.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    
                    // Update cache with Firebase data only (offline expenses already synced)
                    cacheExpenses(firebaseExpenses);
                    
                    // Clear and reload UI with Firebase data
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add sorted expenses to UI
                    for (Expense expense : firebaseExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    Log.d(TAG, "Finished loading " + firebaseExpenses.size() + " expenses from Firebase");
                    isLoadingExpenses = false; // Reset flag
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error syncing with Firebase", e);
                    // Firebase sync failed, but cache data is already loaded
                    isLoadingExpenses = false;
                    Log.d(TAG, "Firebase sync failed, continuing with cached data");
                });
    }
    
    private void syncWithFirebaseAlternative() {
        // Don't set isLoadingExpenses here since this is called from loadExpensesFromFirebaseOnly
        
        // Fallback: Get all user expenses and filter locally for today
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        Log.d(TAG, "Using alternative method to sync today's expenses");
        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Alternative sync returned " + queryDocumentSnapshots.size() + " total expenses");
                    
                    int todayCount = 0;
                    List<Expense> firebaseExpenses = new ArrayList<>();
                    // Filter for today's expenses from Firebase only
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null && today.equals(expense.getDate())) {
                            expense.setId(document.getId()); // Set the document ID
                            firebaseExpenses.add(expense);
                            Log.d(TAG, "Found today's expense: " + expense.getCategory() + " - P" + expense.getAmount());
                            todayCount++;
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    firebaseExpenses.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    
                    // Update cache with Firebase data only (offline expenses already synced)
                    cacheExpenses(firebaseExpenses);
                    
                    // Clear and reload UI with Firebase data
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add sorted expenses to UI
                    for (Expense expense : firebaseExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    Log.d(TAG, "Found " + todayCount + " Firebase expenses using alternative sync method");
                    isLoadingExpenses = false; // Reset flag
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error syncing with alternative method", e);
                    isLoadingExpenses = false; // Reset flag
                });
    }
    
    private void clearDynamicTransactions(LinearLayout container) {
        // Reset category states when clearing transactions
        resetDailyCategoryStates();
        
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
        // Only refresh if we're not already loading expenses
        if (currentUserId != null && !isLoadingExpenses) {
            Log.d(TAG, "Activity resumed, refreshing today's expenses");
            
            // Add a small delay to prevent race conditions when coming back online
            new android.os.Handler().postDelayed(() -> {
                if (!isLoadingExpenses) { // Double check
                    // Reset category states first, they will be set correctly after loading expenses
                    resetDailyCategoryStates();
                    loadTodaysExpenses();
                }
            }, 100); // 100ms delay
            
        } else if (isLoadingExpenses) {
            Log.d(TAG, "Activity resumed but expenses are already loading, skipping");
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
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    private void checkExistingCategoriesForToday() {
        // This method will be called after loading today's expenses
        // to set the correct category states based on loaded transaction items
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
                Log.d(TAG, "Found existing UI expense for category: " + category);
            }
        }
        
        Log.d(TAG, "Category states after check - Breakfast: " + breakfastUsed + 
              ", Lunch: " + lunchUsed + ", Dinner: " + dinnerUsed);
    }
    
    // ================ OFFLINE CACHE METHODS ================
    
    private static final String CACHE_PREFS = "expense_cache";
    private static final String CACHE_KEY_PREFIX = "expenses_";
    private static final String CACHE_TIMESTAMP_PREFIX = "cache_time_";
    
    /**
     * Load expenses from local cache when offline
     */
    private void loadExpensesFromCache() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load from cache: currentUserId is null");
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            Log.d(TAG, "Loading expenses from cache");
            
            try {
                // Parse cached expenses
                JSONArray cachedArray = new JSONArray(cachedJson);
                List<Expense> cachedExpenses = new ArrayList<>();
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    Expense expense = new Expense();
                    expense.setId(expenseJson.getString("id"));
                    expense.setCategory(expenseJson.getString("category"));
                    expense.setAmount(expenseJson.getDouble("amount"));
                    expense.setDate(expenseJson.getString("date"));
                    expense.setTimestamp(expenseJson.getLong("timestamp"));
                    expense.setUserId(expenseJson.getString("userId"));
                    cachedExpenses.add(expense);
                }
                
                // Sort by timestamp (newest first)
                cachedExpenses.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                
                // Add to UI
                for (Expense expense : cachedExpenses) {
                    addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                }
                
                Log.d(TAG, "Loaded " + cachedExpenses.size() + " expenses from cache");
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cached expenses", e);
            }
        } else {
            Log.d(TAG, "No cached expenses found for today");
        }
    }
    
    /**
     * Cache expenses locally for offline access
     */
    private void cacheExpenses(List<Expense> expenses) {
        if (currentUserId == null || expenses == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        String timestampKey = CACHE_TIMESTAMP_PREFIX + currentUserId + "_" + today;
        
        try {
            JSONArray expenseArray = new JSONArray();
            for (Expense expense : expenses) {
                JSONObject expenseJson = new JSONObject();
                expenseJson.put("id", expense.getId());
                expenseJson.put("category", expense.getCategory());
                expenseJson.put("amount", expense.getAmount());
                expenseJson.put("date", expense.getDate());
                expenseJson.put("timestamp", expense.getTimestamp());
                expenseJson.put("userId", expense.getUserId());
                expenseArray.put(expenseJson);
            }
            
            prefs.edit()
                .putString(cacheKey, expenseArray.toString())
                .putLong(timestampKey, System.currentTimeMillis())
                .apply();
                
            Log.d(TAG, "Cached " + expenses.size() + " expenses for offline access");
        } catch (JSONException e) {
            Log.e(TAG, "Error caching expenses", e);
        }
    }
    
    /**
     * Add a single expense to cache (for new expenses added while offline)
     */
    private void addExpenseToCache(Expense expense) {
        if (currentUserId == null || expense == null) {
            return;
        }
        
        // Load existing cache
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        List<Expense> cachedExpenses = new ArrayList<>();
        String cachedJson = prefs.getString(cacheKey, null);
        
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    Expense cachedExpense = new Expense();
                    cachedExpense.setId(expenseJson.getString("id"));
                    cachedExpense.setCategory(expenseJson.getString("category"));
                    cachedExpense.setAmount(expenseJson.getDouble("amount"));
                    cachedExpense.setDate(expenseJson.getString("date"));
                    cachedExpense.setTimestamp(expenseJson.getLong("timestamp"));
                    cachedExpense.setUserId(expenseJson.getString("userId"));
                    cachedExpenses.add(cachedExpense);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing existing cache", e);
            }
        }
        
        // Add new expense
        cachedExpenses.add(expense);
        
        // Save updated cache
        cacheExpenses(cachedExpenses);
    }
    
    /**
     * Remove an expense from cache (for deleted expenses while offline)
     */
    private void removeExpenseFromCache(String expenseId) {
        try {
            if (currentUserId == null) {
                return;
            }
            
            // Load existing cache
            SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
            
            List<Expense> cachedExpenses = new ArrayList<>();
            String cachedJson = prefs.getString(cacheKey, null);
            
            if (cachedJson != null) {
                try {
                    JSONArray cachedArray = new JSONArray(cachedJson);
                    for (int i = 0; i < cachedArray.length(); i++) {
                        JSONObject expenseJson = cachedArray.getJSONObject(i);
                        Expense expense = new Expense();
                        expense.setId(expenseJson.getString("id"));
                        expense.setCategory(expenseJson.getString("category"));
                        expense.setAmount(expenseJson.getDouble("amount"));
                        expense.setDate(expenseJson.getString("date"));
                        expense.setTimestamp(expenseJson.getLong("timestamp"));
                        expense.setUserId(expenseJson.getString("userId"));
                        
                        // Skip the expense we want to delete
                        if (!expense.getId().equals(expenseId)) {
                            cachedExpenses.add(expense);
                        }
                    }
                    
                    // Save updated cache
                    cacheExpenses(cachedExpenses);
                    Log.d(TAG, "Removed expense " + expenseId + " from cache");
                } catch (JSONException e) {
                    Log.e(TAG, "Error removing expense from cache", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in removeExpenseFromCache", e);
        }
    }
    
    /**
     * Update an expense in cache (for edited expenses while offline)  
     */
    private void updateExpenseInCache(String expenseId, String newCategory, double newAmount) {
        if (currentUserId == null || expenseId == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                List<Expense> updatedExpenses = new ArrayList<>();
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    Expense expense = new Expense();
                    expense.setId(expenseJson.getString("id"));
                    expense.setCategory(expenseJson.getString("category"));
                    expense.setAmount(expenseJson.getDouble("amount"));
                    expense.setDate(expenseJson.getString("date"));
                    expense.setTimestamp(expenseJson.getLong("timestamp"));
                    expense.setUserId(expenseJson.getString("userId"));
                    
                    // Update if this is the expense we're looking for
                    if (expense.getId().equals(expenseId)) {
                        expense.setCategory(newCategory);
                        expense.setAmount(newAmount);
                        Log.d(TAG, "Updated expense " + expenseId + " in cache");
                    }
                    
                    updatedExpenses.add(expense);
                }
                
                // Save updated cache
                cacheExpenses(updatedExpenses);
            } catch (JSONException e) {
                Log.e(TAG, "Error updating expense in cache", e);
            }
        }
    }
    
    /**
     * Get only offline expenses (with temp IDs) from cache
     */
    private List<Expense> getOfflineExpensesFromCache() {
        List<Expense> offlineExpenses = new ArrayList<>();
        
        if (currentUserId == null) {
            return offlineExpenses;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    String expenseId = expenseJson.getString("id");
                    
                    // Only include expenses with temporary IDs (offline expenses)
                    if (expenseId.startsWith("temp_")) {
                        Expense expense = new Expense();
                        expense.setId(expenseId);
                        expense.setCategory(expenseJson.getString("category"));
                        expense.setAmount(expenseJson.getDouble("amount"));
                        expense.setDate(expenseJson.getString("date"));
                        expense.setTimestamp(expenseJson.getLong("timestamp"));
                        expense.setUserId(expenseJson.getString("userId"));
                        offlineExpenses.add(expense);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cached expenses for offline sync", e);
            }
        }
        
        return offlineExpenses;
    }
    
    /**
     * Sync a single offline expense to Firebase with callback
     */
    private void syncSingleOfflineExpenseWithCallback(Expense offlineExpense, SyncCallback callback) {
        String tempId = offlineExpense.getId();
        
        // Create a new expense without the temp ID
        Expense firebaseExpense = new Expense(
            offlineExpense.getUserId(),
            offlineExpense.getCategory(),
            offlineExpense.getAmount(),
            "", // Description
            offlineExpense.getDate()
        );
        firebaseExpense.setTimestamp(offlineExpense.getTimestamp());
        
        Log.d(TAG, "Syncing offline expense to Firebase: " + offlineExpense.getCategory() + " - P" + offlineExpense.getAmount());
        
        firebaseManager.addExpense(firebaseExpense)
                .addOnSuccessListener(documentReference -> {
                    String newFirebaseId = documentReference.getId();
                    Log.d(TAG, "Offline expense synced successfully. Old ID: " + tempId + ", New ID: " + newFirebaseId);
                    
                    // Update the expense with the real Firebase ID
                    firebaseExpense.setId(newFirebaseId);
                    
                    // Update transaction item tag with new ID
                    updateTransactionItemTag(tempId, newFirebaseId);
                    
                    // Remove the offline expense from cache (don't replace, just remove since we'll reload from Firebase)
                    removeOfflineExpenseFromCache(tempId);
                    
                    // Notify callback of success
                    callback.onSyncSuccess(tempId, newFirebaseId);
                    
//                    Toast.makeText(this, "Synced: " + offlineExpense.getCategory(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to sync offline expense: " + tempId, e);
                    // Notify callback of failure
                    callback.onSyncFailure(tempId, e);
                    // Keep the offline expense in cache for retry later
                });
    }
    
    /**
     * Remove an offline expense from cache by its temp ID
     */
    private void removeOfflineExpenseFromCache(String tempId) {
        if (currentUserId == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                List<Expense> updatedExpenses = new ArrayList<>();
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    String expenseId = expenseJson.getString("id");
                    
                    // Skip the temp expense (remove it)
                    if (!expenseId.equals(tempId)) {
                        Expense expense = new Expense();
                        expense.setId(expenseId);
                        expense.setCategory(expenseJson.getString("category"));
                        expense.setAmount(expenseJson.getDouble("amount"));
                        expense.setDate(expenseJson.getString("date"));
                        expense.setTimestamp(expenseJson.getLong("timestamp"));
                        expense.setUserId(expenseJson.getString("userId"));
                        updatedExpenses.add(expense);
                    }
                }
                
                // Save updated cache without the temp expense
                cacheExpenses(updatedExpenses);
                Log.d(TAG, "Removed offline expense " + tempId + " from cache");
            } catch (JSONException e) {
                Log.e(TAG, "Error removing offline expense from cache", e);
            }
        }
    }
    
    /**
     * Update transaction item tag from temp ID to real Firebase ID
     */
    private void updateTransactionItemTag(String oldTempId, String newFirebaseId) {
        LinearLayout container = findViewById(R.id.transactionsContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (oldTempId.equals(child.getTag())) {
                child.setTag(newFirebaseId);
                Log.d(TAG, "Updated transaction item tag from " + oldTempId + " to " + newFirebaseId);
                break;
            }
        }
    }
    
    // Callback interface for synchronization operations
    private interface SyncCallback {
        void onSyncSuccess(String tempId, String firebaseId);
        void onSyncFailure(String tempId, Exception e);
    }
}
