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
import com.example.kuripothub.models.BudgetHistory;
import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.utils.PreferenceBasedDateUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private double originalBudget = 2000.00; // Add this at the top of the class, or initialize from user profile if available
    private TextView budgetAmountText;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    // Flag to prevent multiple simultaneous expense loads
    private boolean isLoadingExpenses = false;
    
    // Flag to track if this is the initial load (prevents duplicate loading in onResume)
    private boolean isInitialLoad = true;
    
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
        
        // Add startup cleanup to ensure no duplicate UI elements from previous sessions
        performStartupCleanup();
        
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
        
        // Set click listener for Settings icon
        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseTrackingActivity.this, SettingsActivity.class);
                startActivity(intent);
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
            // Always update category states from UI and cache before showing
            // 1. Reset states
            resetDailyCategoryStates();
            // 2. Check UI for already added categories
            checkExistingCategoriesForToday();
            // 3. If offline, also check cache for today's expenses
            if (!isNetworkAvailable()) {
                SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
                String cachedJson = prefs.getString(cacheKey, null);
                if (cachedJson != null) {
                    try {
                        JSONArray cachedArray = new JSONArray(cachedJson);
                        for (int i = 0; i < cachedArray.length(); i++) {
                            JSONObject expenseJson = cachedArray.getJSONObject(i);
                            String category = expenseJson.getString("category").toLowerCase();
                            markCategoryAsUsed(category);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing cached expenses for category state", e);
                    }
                }
            }
            // 4. Update bottom sheet UI
            updateCategoryStates(categoryBottomSheetView);
            // 5. Show bottom sheet
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
            
            // Check spending limits before processing expense
            checkSpendingLimitAndProcess(category, expenseAmount);
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void checkSpendingLimitAndProcess(String category, double expenseAmount) {
        if (!PreferenceBasedDateUtils.shouldEnforceSpendingLimit(this)) {
            // No spending limit, proceed normally
            saveAndUpdateExpense(category, expenseAmount);
            return;
        }
        
        // Calculate current period expenses and check against limit
        calculateCurrentPeriodExpenses(currentPeriodExpenses -> {
            double spendingLimitPercentage = PreferenceBasedDateUtils.getSpendingLimitPercentage(this);
            double spendingLimit = currentBudget * spendingLimitPercentage;
            double newTotal = currentPeriodExpenses + expenseAmount;
            
            if (newTotal > spendingLimit) {
                // Show warning dialog
                String limitPercent = PreferenceActivity.getSpendingLimit(this);
                double overAmount = newTotal - spendingLimit;
                
                new AlertDialog.Builder(this)
                    .setTitle("Spending Limit Warning")
                    .setMessage("This expense will put you P" + String.format("%.2f", overAmount) + 
                               " over your " + limitPercent + " spending limit.\n\n" +
                               "Current spending: P" + String.format("%.2f", currentPeriodExpenses) + "\n" +
                               "Spending limit: P" + String.format("%.2f", spendingLimit) + "\n" +
                               "New expense: P" + String.format("%.2f", expenseAmount) + "\n\n" +
                               "Do you want to continue anyway?")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        saveAndUpdateExpense(category, expenseAmount);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Do nothing, just dismiss
                    })
                    .show();
            } else {
                // Within limit, proceed normally
                saveAndUpdateExpense(category, expenseAmount);
                
                // Show friendly reminder if close to limit
                double remainingLimit = spendingLimit - newTotal;
                if (remainingLimit < spendingLimit * 0.1) { // Less than 10% remaining
                    String limitPercent = PreferenceActivity.getSpendingLimit(this);
                    Toast.makeText(this, "Warning: Only P" + String.format("%.2f", remainingLimit) + 
                                  " left in your " + limitPercent + " spending limit", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void saveAndUpdateExpense(String category, double expenseAmount) {
        // Save expense to Firebase
        saveExpenseToFirebase(category, expenseAmount);
        
        // Decrease the budget
        updateBudget(expenseAmount);
        
        // Add the transaction to the transactions container
        addTransactionToView(category, String.format("%.2f", expenseAmount));
        
        // Mark the category as used if it's a meal type
        markCategoryAsUsed(category);
        
        Toast.makeText(this, category + " expense added: P" + String.format("%.2f", expenseAmount), Toast.LENGTH_SHORT).show();
    }
    
    private void updateBudget(double expenseAmount) {
        // Subtract the expense from the available budget (for spending logic)
        double availableBudget = getAvailableBudget() - expenseAmount;
        setAvailableBudget(availableBudget);
        
        // Immediately recalculate amount budget like Expense Summary approach
        calculateAndSetAmountBudget();
        
        // Update Firebase budget
        updateBudgetInFirebase();
        
        Log.d(TAG, "Budget updated - Expense: " + expenseAmount + ", Available Budget: " + availableBudget);
    }
    
    private void updateBudgetDisplay() {
        if (currentUserId == null) {
            budgetAmountText.setText("P0");
            return;
        }
        // Display the amountBudget (similar to saved card in summary)
        // Do NOT call checkAndResetAvailableBudgetIfNewPeriod() here!
        double amountBudget = getAmountBudget();
        String amountText = "P" + String.format("%.2f", amountBudget);
        budgetAmountText.setText(amountText);
        
        // Change color based on amountBudget value
        if (amountBudget < 0) {
            budgetAmountText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            budgetAmountText.setTextColor(getResources().getColor(android.R.color.black));
        }
        
        Log.d(TAG, "Budget display updated - AmountBudget: " + amountBudget + ", OriginalBudget: " + originalBudget);
    }
    
    private void calculateCurrentPeriodExpenses(ExpenseCalculationCallback callback) {
        if (currentUserId == null) {
            callback.onExpensesCalculated(0.0);
            return;
        }
        
        // Get the current week period based on user preferences (same as summary)
        String weekStart = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        String weekEnd = PreferenceBasedDateUtils.getCurrentPeriodEndDate(this);
        
        Log.d(TAG, "Calculating period expenses from " + weekStart + " to " + weekEnd);
        
        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalExpenses = 0.0;
                    int expenseCount = 0;
                    
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            String expenseDate = expense.getDate();
                            if (expenseDate != null && expenseDate.compareTo(weekStart) >= 0 && expenseDate.compareTo(weekEnd) <= 0) {
                                totalExpenses += expense.getAmount();
                                expenseCount++;
                                Log.d(TAG, "Expense in period: " + expense.getCategory() + " - P" + expense.getAmount() + " on " + expenseDate);
                            }
                        }
                    }
                    
                    Log.d(TAG, "Period calculation complete - Total: P" + totalExpenses + " from " + expenseCount + " expenses");
                    callback.onExpensesCalculated(totalExpenses);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error calculating period expenses", e);
                    callback.onExpensesCalculated(0.0);
                });
    }
    
    private interface ExpenseCalculationCallback {
        void onExpensesCalculated(double totalExpenses);
    }
    
    private interface SyncCallback {
        void onSyncSuccess(String tempId, String firebaseId);
        void onSyncFailure(String tempId, Exception e);
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
            
            // Parse the original amount and add it back to available budget
            double amountValue = Double.parseDouble(originalAmount.replaceAll("[^\\d.]", ""));
            double availableBudget = getAvailableBudget() + amountValue;
            setAvailableBudget(availableBudget);
            
            // Immediately recalculate amount budget like Expense Summary approach
            calculateAndSetAmountBudget();
            
            Log.d(TAG, "Transaction deleted - Restored amount: " + amountValue + ", Available Budget: " + availableBudget);
            
            // Get the expense ID from the transaction item's tag
            String expenseId = (String) transactionItem.getTag();
            
            if (expenseId != null) {
                // Check if this is an offline expense (temp ID) or Firebase expense
                if (expenseId.startsWith("temp_")) {
                    // This is an offline expense, handle it differently
                    Log.d(TAG, "Deleting offline expense: " + expenseId);
                    // Remove from cache immediately
                    removeExpenseFromCache(expenseId);
                    // Update budget calculations for offline deletion
                    Log.d(TAG, "Updating budget calculations for offline deletion");
                    calculateAndSetAmountBudget();
                    // No need to try Firebase for temp expenses
                } else {
                    // This is a Firebase expense, try to delete from Firebase first
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
                                
                                // If we're offline, still remove from cache and track as pending operation
                                if (!isNetworkAvailable()) {
                                    // Create expense object for the pending operation before removing from cache
                                    Expense deletedExpense = new Expense(currentUserId, category, amountValue, "", 
                                            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                                    deletedExpense.setId(expenseId);
                                    
                                    removeExpenseFromCache(expenseId);
                                    // Track this delete as a pending operation for later sync
                                    addPendingOperation(OP_TYPE_DELETE, expenseId, deletedExpense);
                                    Log.d(TAG, "Expense delete saved offline and will sync when online: " + expenseId);
                                    
                                    // Update budget locally
                                    updateBudgetInFirebase();
                                    
                                    // Immediately update budget calculations for offline deletion
                                    Log.d(TAG, "Updating budget calculations for offline deletion");
                                    calculateAndSetAmountBudget();
                                } else {
                                    Toast.makeText(this, "Failed to delete expense from database", Toast.LENGTH_SHORT).show();
                                    // Revert available budget change if deletion failed
                                    double revertedAvailableBudget = getAvailableBudget() - amountValue;
                                    setAvailableBudget(revertedAvailableBudget);
                                    
                                    // Recalculate amount budget after reverting
                                    calculateAndSetAmountBudget();
                                    return;
                                }
                            });
                }
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
            double oldAmountValue = Double.parseDouble(originalAmount.replaceAll("[^\\d.]", ""));
            double newAmountValue = Double.parseDouble(newAmount.replaceAll("[^\\d.]", ""));
            double difference = newAmountValue - oldAmountValue;
            
            // Update available budget by the difference
            double availableBudget = getAvailableBudget() - difference;
            setAvailableBudget(availableBudget);
            
            // Immediately recalculate amount budget like Expense Summary approach
            calculateAndSetAmountBudget();
            
            Log.d(TAG, "Transaction updated - Difference: " + difference + ", Available Budget: " + availableBudget);
            
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
                
                // Check if this is an offline expense (temp ID) or Firebase expense
                if (expenseId.startsWith("temp_")) {
                    // This is an offline expense, handle it differently
                    Log.d(TAG, "Updating offline expense: " + expenseId);
                    // Update in cache immediately
                    updateExpenseInCache(expenseId, category, newAmountValue);
                    // Update budget calculations for offline edit
                    Log.d(TAG, "Updating budget calculations for offline edit");
                    calculateAndSetAmountBudget();
                    // No need to try Firebase for temp expenses
                } else {
                    // This is a Firebase expense, try to update in Firebase first
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
                                
                                // If we're offline, still update cache and track as pending operation
                                if (!isNetworkAvailable()) {
                                    updateExpenseInCache(expenseId, category, newAmountValue);
                                    // Track this edit as a pending operation for later sync
                                    addPendingOperation(OP_TYPE_EDIT, expenseId, updatedExpense);
                                    Log.d(TAG, "Expense edit saved offline and will sync when online");
                                    
                                    // Update budget locally
                                    updateBudgetInFirebase();
                                    
                                    // Immediately update budget calculations for offline edit
                                    Log.d(TAG, "Updating budget calculations for offline edit");
                                    calculateAndSetAmountBudget();
                                } else {
                                    Toast.makeText(this, "Failed to update expense in database", Toast.LENGTH_SHORT).show();
                                    // Revert budget change if update failed
                                    currentBudget += difference;
                                    updateBudgetDisplay();
                                    return;
                                }
                            });
                }
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
        
        // Pre-fill with original budget
        amountInput.setText(String.format("%.2f", originalBudget));
        amountInput.setHint("Enter new budget amount");

        // Set up button listeners
        cancelButton.setOnClickListener(v -> budgetDialog.dismiss());
        
        confirmButton.setOnClickListener(v -> {
            String newBudgetText = amountInput.getText().toString().trim();
            if (!newBudgetText.isEmpty()) {
                try {
                    double newOriginalBudget = Double.parseDouble(newBudgetText.replaceAll("[^\\d.]", ""));
                    if (newOriginalBudget >= 0) {
                        double oldOriginalBudget = originalBudget;
                        originalBudget = newOriginalBudget;
                        
                        // Update available budget by the difference
                        double availableBudget = getAvailableBudget() + (newOriginalBudget - oldOriginalBudget);
                        setAvailableBudget(availableBudget);
                        
                        // Immediately recalculate amount budget with new original budget (like Expense Summary)
                        calculateAndSetAmountBudget();

                        // Update only the originalBudgetAmount TextView
                        TextView originalBudgetAmount = findViewById(R.id.originalBudgetAmount);
                        if (originalBudgetAmount != null) {
                            originalBudgetAmount.setText("P" + String.format("%.2f", originalBudget));
                        }
                        
                        // Save budget history when budget changes
                        saveBudgetChangeToHistory(oldOriginalBudget, newOriginalBudget);
                        
                        updateBudgetInFirebase();
                        budgetDialog.dismiss();
                        Toast.makeText(this, "Original budget updated", Toast.LENGTH_SHORT).show();
                        
                        Log.d(TAG, "Original budget updated from " + oldOriginalBudget + " to " + newOriginalBudget +
                              ", Available Budget: " + availableBudget);
                    } else {
                        Toast.makeText(this, "Budget must be non-negative", Toast.LENGTH_SHORT).show();
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

    private void saveBudgetChangeToHistory(double oldBudget, double newBudget) {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot save budget history: currentUserId is null");
            return;
        }
        
        // Get current week start date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentWeekStartDate = getCurrentWeekStartDate();
        
        // Calculate the end date for the old budget (day before current week starts)
        Calendar currentWeekStart = Calendar.getInstance();
        try {
            Date weekStartDate = dateFormat.parse(currentWeekStartDate);
            currentWeekStart.setTime(weekStartDate);
            currentWeekStart.add(Calendar.DAY_OF_YEAR, -1); // Day before current week
        } catch (Exception e) {
            Log.e(TAG, "Error parsing current week start date", e);
            currentWeekStart.add(Calendar.DAY_OF_YEAR, -1); // Fallback to yesterday
        }
        String oldBudgetEndDate = dateFormat.format(currentWeekStart.getTime());
        
        Log.d(TAG, "=== BUDGET CHANGE LOGIC ===");
        Log.d(TAG, "Changing budget from ₱" + oldBudget + " to ₱" + newBudget);
        Log.d(TAG, "Current week starts on: " + currentWeekStartDate);
        Log.d(TAG, "Old budget should end on: " + oldBudgetEndDate);
        
        // First, check if we need to create initial budget history for previous weeks
        firebaseManager.getUserBudgetHistory(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No budget history exists, create initial entry for previous weeks
                        Log.d(TAG, "No budget history found, creating historical budget entry");
                        
                        String firstWeekStartDate = getFirstWeekStartDate();
                        
                        // Create the old budget entry that covers weeks 1 and 2
                        BudgetHistory oldBudgetHistory = new BudgetHistory(currentUserId, oldBudget, firstWeekStartDate);
                        oldBudgetHistory.setEndDate(oldBudgetEndDate);
                        
                        Log.d(TAG, "Creating historical budget: ₱" + oldBudget + " from " + firstWeekStartDate + " to " + oldBudgetEndDate);
                        
                        firebaseManager.addBudgetHistory(oldBudgetHistory)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "Historical budget entry created, ID: " + documentReference.getId());
                                    // Now create the new budget for current week onwards
                                    createNewBudgetPeriod(newBudget, currentWeekStartDate);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to create historical budget entry", e);
                                });
                    } else {
                        // Budget history exists, end the current period properly
                        Log.d(TAG, "Budget history exists (" + queryDocumentSnapshots.size() + " entries), ending current period");
                        
                        firebaseManager.endCurrentBudgetPeriod(currentUserId, oldBudgetEndDate)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "Current budget period ended successfully");
                                    } else {
                                        Log.w(TAG, "Failed to end current budget period", task.getException());
                                    }
                                    // Create new budget period regardless
                                    createNewBudgetPeriod(newBudget, currentWeekStartDate);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check budget history", e);
                });
    }
    
    private void createNewBudgetPeriod(double newBudget, String startDate) {
        Log.d(TAG, "Creating new budget period: ₱" + newBudget + " starting from " + startDate);
        
        BudgetHistory newBudgetHistory = new BudgetHistory(currentUserId, newBudget, startDate);
        
        firebaseManager.addBudgetHistory(newBudgetHistory)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✓ New budget period created successfully with ID: " + documentReference.getId());
                    Log.d(TAG, "✓ Budget: ₱" + newBudget + " from " + startDate + " onwards");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to create new budget period", e);
                });
    }

    private void initializeBudgetHistoryIfNeeded() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot initialize budget history: currentUserId is null");
            return;
        }
        
        Log.d(TAG, "Checking if budget history needs initialization for user: " + currentUserId);
        
        // Check if user already has budget history
        firebaseManager.getUserBudgetHistory(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No budget history exists, create initial entry
                        Log.d(TAG, "No budget history found, creating initial entry");
                        
                        // Use the start date of the first week instead of current date
                        String firstWeekStartDate = getFirstWeekStartDate();
                        
                        BudgetHistory initialBudget = new BudgetHistory(currentUserId, originalBudget, firstWeekStartDate);
                        
                        firebaseManager.addBudgetHistory(initialBudget)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "Initial budget history created with ID: " + documentReference.getId() + " starting from: " + firstWeekStartDate);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to create initial budget history", e);
                                });
                    } else {
                        Log.d(TAG, "Budget history already exists (" + queryDocumentSnapshots.size() + " entries)");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check budget history", e);
                });
    }

    private int getUserPreferredWeekStartDay() {
        // Get user's preferred start day from preferences
        String userStartDay = PreferenceActivity.getStartDay(this); // e.g., "Monday", "Sunday"
        switch (userStartDay.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private String getFirstWeekStartDate() {
        try {
            // Calculate the first week start date more accurately
            // If we're currently in week 3, we need to go back to week 1
            Calendar currentDate = Calendar.getInstance();
            int weekStartDay = getUserPreferredWeekStartDay();
            
            // Set the calendar to use the preferred week start day
            currentDate.setFirstDayOfWeek(weekStartDay);
            
            // Calculate weeks to go back: if current is week 3, go back 2 weeks
            int weeksToGoBack = 2; // For week 3, go back 2 weeks to get to week 1
            currentDate.add(Calendar.WEEK_OF_YEAR, -weeksToGoBack);
            
            // Set to the start of that week
            currentDate.set(Calendar.DAY_OF_WEEK, weekStartDay);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String firstWeekStart = dateFormat.format(currentDate.getTime());
            
            Log.d(TAG, "Calculated first week start date: " + firstWeekStart + " (going back " + weeksToGoBack + " weeks)");
            return firstWeekStart;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating first week start date", e);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            // Fallback: use 2 weeks ago from today
            Calendar fallback = Calendar.getInstance();
            fallback.add(Calendar.WEEK_OF_YEAR, -2);
            return dateFormat.format(fallback.getTime());
        }
    }

    // Firebase methods
    private void loadUserData() {
        Log.d(TAG, "Loading user data for userId: " + currentUserId);
        Log.d(TAG, "Before loading - originalBudget: " + originalBudget + ", currentBudget: " + currentBudget);
        
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            double oldOriginalBudget = originalBudget;
                            double oldCurrentBudget = currentBudget;
                            
                            currentBudget = user.getBudget();
                            originalBudget = currentBudget; // Initialize original budget from user profile

                            Log.d(TAG, "User budget loaded from Firebase:");
                            Log.d(TAG, "  - Old originalBudget: " + oldOriginalBudget + " -> New: " + originalBudget);
                            Log.d(TAG, "  - Old currentBudget: " + oldCurrentBudget + " -> New: " + currentBudget);
                            
                            // Update username display
                            TextView usernameText = findViewById(R.id.usernameText);
                            if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                                usernameText.setText(user.getUsername());
                            } else {
                                usernameText.setText("User");
                            }

                            Log.d(TAG, "User budget loaded: " + currentBudget);
                            
                            // Initialize budget history if this is a new user
                            initializeBudgetHistoryIfNeeded();
                            
                            // Immediately calculate amount budget from current period expenses (like Expense Summary)
                            calculateAndSetAmountBudget();
                        } else {
                            Log.w(TAG, "User object is null from Firebase document");
                            calculateAndSetAmountBudget();
                        }
                    } else {
                        Log.d(TAG, "No user profile found in Firebase, using default budget");
                        Log.d(TAG, "Default originalBudget: " + originalBudget + ", currentBudget: " + currentBudget);
                        
                        // Even with default budget, calculate amount budget from current period expenses
                        calculateAndSetAmountBudget();
                    }

                    // After loading user data, recalculate available budget for the period
                    checkAndResetAvailableBudgetIfNewPeriod();

                    // After recalculation, load today's expenses
                    loadTodaysExpenses();

                    // Debug: Check all user expenses
                    debugUserExpenses();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading user data from Firebase", e);
                    Log.d(TAG, "Using fallback - originalBudget: " + originalBudget + ", currentBudget: " + currentBudget);

                    // Even if user data fails, calculate amount budget from current period expenses
                    calculateAndSetAmountBudget();

                    // Still recalculate available budget for the period
                    checkAndResetAvailableBudgetIfNewPeriod();

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
        
        // Set timestamp for proper sorting
        expense.setTimestamp(System.currentTimeMillis());
        
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
            
            // Immediately update budget calculations to reflect offline expense
            Log.d(TAG, "Updating budget calculations for offline expense");
            calculateAndSetAmountBudget();
            
//            Toast.makeText(this, "Saved offline - will sync when online", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateBudgetInFirebase() {
        // Save the original budget to Firebase instead of the current budget
        firebaseManager.updateUserBudget(currentUserId, originalBudget)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Original budget updated successfully in Firebase");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating original budget", e);
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
            Intent intent = new Intent(ExpenseTrackingActivity.this, ProfileActivity.class);
            startActivity(intent);
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
        Log.d(TAG, "Starting to load today's expenses (isInitialLoad: " + isInitialLoad + ")");
        debugCacheState("BEFORE_LOAD_EXPENSES");
        
        if (isNetworkAvailable()) {
            Log.d(TAG, "Online - syncing with Firebase (will handle offline expenses)");
            
            // When online, save current transactions to cache before clearing UI to preserve any offline changes
            // This is important in case sync fails - we don't want to lose offline expenses
            saveCurrentTransactionsToCache();
            
            // Clear existing transaction views AFTER saving them to cache
            LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
            clearDynamicTransactions(transactionsContainer);
            
            // When online, sync offline expenses first, then load everything from Firebase
            // This prevents duplicates by making Firebase the single source of truth
            syncWithFirebase();
        } else {
            Log.d(TAG, "Offline - preserving existing UI and ensuring all expenses are cached");
            
            // When offline, we need to be more careful about preserving the UI
            // First save any current transactions to ensure they're in cache
            saveCurrentTransactionsToCache();
            
            // Check if we already have expenses loaded in the UI
            LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
            boolean hasExistingTransactions = hasTransactionItems(transactionsContainer);
            
            if (hasExistingTransactions) {
                Log.d(TAG, "Offline mode: Found existing transactions in UI, preserving them");
                // Don't clear UI if we already have transactions and we're offline
                // Just ensure budget is updated and flag is reset
                isLoadingExpenses = false;
                recalculateAmountBudget();
                return;
            } else {
                Log.d(TAG, "Offline mode: No existing transactions in UI, loading from cache");
                // Only clear and reload if UI is empty - load from cache
                clearDynamicTransactions(transactionsContainer);
                debugCacheState("LOADING_OFFLINE_EXPENSES");
                loadExpensesFromCache();
                isLoadingExpenses = false;
            }
        }
        
        // Force recalculate amount budget after expenses are loaded (only for online mode)
        if (isNetworkAvailable()) {
            recalculateAmountBudget();
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
        debugCacheState("BEFORE_SYNC");
        
        List<Expense> offlineExpenses = getOfflineExpensesFromCache();
        List<JSONObject> pendingOperations = getPendingOperations();
        
        int totalOperations = offlineExpenses.size() + pendingOperations.size();
        
        Log.d(TAG, "Found " + offlineExpenses.size() + " offline expenses and " + 
              pendingOperations.size() + " pending operations to sync");
        
        if (totalOperations == 0) {
            // No offline expenses or pending operations to sync
            // Check if we're online or offline to determine how to load
            if (isNetworkAvailable()) {
                Log.d(TAG, "No operations to sync and online - loading from Firebase with fallback for any unsynced cached data");
                loadExpensesFromFirebaseWithOfflineFallback(today);
            } else {
                Log.d(TAG, "No operations to sync and offline - loading from cache only");
                loadExpensesFromCache();
                isLoadingExpenses = false; // Reset flag since we're handling it directly
            }
            return;
        }
        
        Log.d(TAG, "Syncing " + offlineExpenses.size() + " offline expenses and " + 
              pendingOperations.size() + " pending operations to Firebase first");
        
        // Keep track of sync operations
        final AtomicInteger syncedCount = new AtomicInteger(0);
        final AtomicInteger failedCount = new AtomicInteger(0);
        
        // Sync offline expenses (new additions)
        for (Expense offlineExpense : offlineExpenses) {
            syncSingleOfflineExpenseWithCallback(offlineExpense, new SyncCallback() {
                @Override
                public void onSyncSuccess(String tempId, String firebaseId) {
                    int synced = syncedCount.incrementAndGet();
                    Log.d(TAG, "Synced " + synced + "/" + totalOperations + " operations (add)");
                    
                    // Check if all sync operations are complete
                    if (synced + failedCount.get() == totalOperations) {
                        // All sync operations completed
                        if (failedCount.get() == 0) {
                            // All synced successfully, clear pending operations and load from Firebase
                            Log.d(TAG, "All offline operations synced successfully. Clearing pending operations and loading from Firebase...");
                            clearPendingOperations();
                            
                            // Add a small delay to ensure all cache removal operations complete
                            // This prevents race conditions where temp expenses might still be in cache
                            new android.os.Handler().postDelayed(() -> {
                                loadExpensesFromFirebaseOnly(today);
                            }, 100); // 100ms delay
                        } else {
                            // Some failed, DON'T clear pending operations, use fallback loading
                            Log.d(TAG, "Some offline operations failed. Loading from Firebase while preserving failed operations...");
                            loadExpensesFromFirebaseWithOfflineFallback(today);
                        }
                    }
                }
                
                @Override
                public void onSyncFailure(String tempId, Exception e) {
                    int failed = failedCount.incrementAndGet();
                    Log.w(TAG, "Failed to sync offline expense " + tempId + ": " + failed + "/" + totalOperations, e);
                    
                    // Check if all sync operations are complete
                    if (syncedCount.get() + failed == totalOperations) {
                        // All sync operations completed (some failed), DON'T clear pending operations yet
                        // Load from Firebase but keep offline expenses that failed to sync
                        Log.d(TAG, "All offline operations processed (some failed). Loading from Firebase while preserving failed syncs...");
                        loadExpensesFromFirebaseWithOfflineFallback(today);
                    }
                }
            });
        }
        
        // Sync pending operations (edits and deletes)
        for (JSONObject operation : pendingOperations) {
            syncPendingOperation(operation, new SyncCallback() {
                @Override
                public void onSyncSuccess(String expenseId, String firebaseId) {
                    int synced = syncedCount.incrementAndGet();
                    Log.d(TAG, "Synced " + synced + "/" + totalOperations + " operations (pending)");
                    
                    // Check if all sync operations are complete
                    if (synced + failedCount.get() == totalOperations) {
                        // All sync operations completed
                        if (failedCount.get() == 0) {
                            // All synced successfully, clear pending operations and load from Firebase
                            Log.d(TAG, "All offline operations synced successfully. Clearing pending operations and loading from Firebase...");
                            clearPendingOperations();
                            
                            // Add a small delay to ensure all cache removal operations complete
                            // This prevents race conditions where temp expenses might still be in cache
                            new android.os.Handler().postDelayed(() -> {
                                loadExpensesFromFirebaseOnly(today);
                            }, 100); // 100ms delay
                        } else {
                            // Some failed, DON'T clear pending operations, use fallback loading
                            Log.d(TAG, "Some offline operations failed. Loading from Firebase while preserving failed operations...");
                            loadExpensesFromFirebaseWithOfflineFallback(today);
                        }
                    }
                }
                
                @Override
                public void onSyncFailure(String expenseId, Exception e) {
                    int failed = failedCount.incrementAndGet();
                    Log.w(TAG, "Failed to sync pending operation " + expenseId + ": " + failed + "/" + totalOperations, e);
                    
                    // Check if all sync operations are complete
                    if (syncedCount.get() + failed == totalOperations) {
                        // Some operations failed, DON'T clear pending operations yet
                        // Load from Firebase but preserve failed operations for retry
                        Log.d(TAG, "All offline operations processed (some failed). Loading from Firebase while preserving failed operations...");
                        loadExpensesFromFirebaseWithOfflineFallback(today);
                    }
                }
            });
        }
    }
    
    /**
     * Load expenses only from Firebase (no offline merging)
     * This method is called after offline expenses have been synced to Firebase
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
                    
                    // Process expenses from Firebase only (no cache merging to prevent duplicates)
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
                    
                    // IMPORTANT: Replace cache entirely with Firebase data to prevent duplicates
                    // This removes any offline expenses that were successfully synced
                    cacheExpenses(firebaseExpenses);
                    
                    // Additional cleanup to ensure no temp expenses remain
                    cleanupCacheAfterSync();
                    
                    // Clear UI again and reload with clean Firebase data (no duplicates)
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add sorted expenses to UI
                    for (Expense expense : firebaseExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    Log.d(TAG, "Finished loading " + firebaseExpenses.size() + " expenses from Firebase (cache updated)");
                    isLoadingExpenses = false; // Reset flag
                    
                    // Recalculate amount budget after Firebase sync
                    recalculateAmountBudget();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error syncing with Firebase", e);
                    // Firebase sync failed, load from cache as fallback
                    Log.d(TAG, "Firebase sync failed, falling back to cached data");
                    loadExpensesFromCache();
                    isLoadingExpenses = false;
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
                    
                    // IMPORTANT: Replace cache entirely with Firebase data to prevent duplicates
                    cacheExpenses(firebaseExpenses);
                    
                    // Additional cleanup to ensure no temp expenses remain
                    cleanupCacheAfterSync();
                    
                    // Clear UI again and reload with clean Firebase data
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add sorted expenses to UI
                    for (Expense expense : firebaseExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    Log.d(TAG, "Found " + todayCount + " Firebase expenses using alternative sync method (cache updated)");
                    isLoadingExpenses = false; // Reset flag
                    
                    // Recalculate amount budget after alternative sync
                    recalculateAmountBudget();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error syncing with alternative method", e);
                    // Firebase sync failed completely, load from cache as fallback
                    loadExpensesFromCache();
                    isLoadingExpenses = false; // Reset flag
                });
    }
    
    /**
     * Load expenses from Firebase but include unsynced offline expenses that failed to sync
     * This ensures that offline expenses are not lost even if sync fails
     */
    private void loadExpensesFromFirebaseWithOfflineFallback(String today) {
        Log.d(TAG, "Loading from Firebase with offline fallback for failed syncs");
        
        // Check if we're offline first - if so, just load from cache directly
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline detected - loading from cache directly instead of trying Firebase");
            loadExpensesFromCache();
            isLoadingExpenses = false;
            return;
        }
        
        // Load Firebase expenses first (only when online)
        firebaseManager.getUserExpensesByDateSimple(currentUserId, today)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Firebase fallback returned " + queryDocumentSnapshots.size() + " expenses");
                    
                    // Process expenses from Firebase
                    List<Expense> firebaseExpenses = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId()); // Set the document ID
                            firebaseExpenses.add(expense);
                            Log.d(TAG, "Firebase expense: " + expense.getCategory() + " - P" + expense.getAmount());
                        }
                    }
                    
                    // Add unsynced offline expenses that still need to be synced
                    List<Expense> offlineExpenses = getOfflineExpensesFromCache();
                    for (Expense offlineExpense : offlineExpenses) {
                        // Only add offline expenses that have temp IDs (haven't been synced yet)
                        if (offlineExpense.getId() != null && offlineExpense.getId().startsWith("temp_")) {
                            // Additional check: make sure this expense isn't already in Firebase data
                            // by checking for duplicates based on category, amount, date, and timestamp
                            boolean isDuplicate = false;
                            for (Expense firebaseExpense : firebaseExpenses) {
                                if (firebaseExpense.getCategory().equals(offlineExpense.getCategory()) &&
                                    Math.abs(firebaseExpense.getAmount() - offlineExpense.getAmount()) < 0.01 &&
                                    firebaseExpense.getDate().equals(offlineExpense.getDate()) &&
                                    Math.abs(firebaseExpense.getTimestamp() - offlineExpense.getTimestamp()) < 5000) { // 5 second tolerance
                                    isDuplicate = true;
                                    Log.d(TAG, "Skipping offline expense - already synced to Firebase: " + offlineExpense.getId());
                                    break;
                                }
                            }
                            
                            if (!isDuplicate) {
                                firebaseExpenses.add(offlineExpense);
                                Log.d(TAG, "Including unsynced offline expense: " + offlineExpense.getCategory() + " - P" + offlineExpense.getAmount());
                            }
                        }
                    }
                    
                    // Sort by timestamp (newest first)
                    firebaseExpenses.sort((e1, e2) -> Long.compare(e2.getTimestamp(), e1.getTimestamp()));
                    
                    // Update cache with combined data
                    cacheExpenses(firebaseExpenses);
                    
                    // Clear UI and reload with combined Firebase + unsynced offline data
                    LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
                    clearDynamicTransactions(transactionsContainer);
                    
                    // Add sorted expenses to UI
                    for (Expense expense : firebaseExpenses) {
                        addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    }
                    
                    Log.d(TAG, "Finished loading " + firebaseExpenses.size() + " expenses with offline fallback");
                    isLoadingExpenses = false; // Reset flag
                    
                    // Recalculate amount budget
                    recalculateAmountBudget();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Firebase fallback also failed, using cache only", e);
                    // If Firebase completely fails, fall back to cache
                    loadExpensesFromCache();
                    isLoadingExpenses = false;
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
        
        // Skip the initial onResume call that happens right after onCreate
        // This prevents duplicate loading when the app first starts
        if (isInitialLoad) {
            Log.d(TAG, "Skipping initial onResume to prevent duplicate loading");
            isInitialLoad = false;
            return;
        }
        
        // Only refresh if we're not already loading expenses
        if (currentUserId != null && !isLoadingExpenses) {
            Log.d(TAG, "Activity resumed, refreshing today's expenses");
            debugCacheState("ON_RESUME_START");

            // Add a small delay to prevent race conditions when coming back online
            new android.os.Handler().postDelayed(() -> {
                if (!isLoadingExpenses) { // Double check
                    // Reset category states first, they will be set correctly after loading expenses
                    resetDailyCategoryStates();
                    // Recalculate available budget if new period
                    checkAndResetAvailableBudgetIfNewPeriod();
                    
                    // DO NOT clean up cache here - let the sync process handle cleanup
                    // Cleaning up here would remove offline expenses before they sync
                    Log.d(TAG, "Preserving all cached expenses for sync process");
                    
                    // Check network status before deciding how to load
                    if (isNetworkAvailable()) {
                        Log.d(TAG, "Network available on resume - will attempt sync");
                    } else {
                        Log.d(TAG, "No network on resume - will load from cache only");
                    }
                    
                    loadTodaysExpenses();
                }
            }, 200); // 200ms delay to ensure network state is stable

        } else if (isLoadingExpenses) {
            Log.d(TAG, "Activity resumed but expenses are already loading, skipping");
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Always save current state when pausing to ensure all expenses are preserved
        // This is especially important for offline expenses that haven't synced yet
        if (currentUserId != null) {
            Log.d(TAG, "Activity pausing - ensuring all expenses are cached for preservation");
            debugCacheState("BEFORE_ON_PAUSE");
            // Force save any pending transactions that might not be cached yet
            saveCurrentTransactionsToCache();
            debugCacheState("AFTER_ON_PAUSE");
        }
    }
    
    /**
     * Save current transaction items to cache to ensure they persist across activity transitions
     */
    private void saveCurrentTransactionsToCache() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot save transactions to cache: currentUserId is null");
            return;
        }
        
        LinearLayout container = findViewById(R.id.transactionsContainer);
        List<Expense> currentExpenses = new ArrayList<>();
        
        Log.d(TAG, "Saving current transactions to cache - found " + container.getChildCount() + " child views");
        
        // Extract expenses from current transaction items
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            TextView categoryName = child.findViewById(R.id.categoryName);
            TextView transactionAmount = child.findViewById(R.id.transactionAmount);
            
            if (categoryName != null && transactionAmount != null) {
                String category = categoryName.getText().toString();
                String amountText = transactionAmount.getText().toString();
                String expenseId = (String) child.getTag();
                
                Log.d(TAG, "Processing UI transaction: " + category + " - " + amountText + " (ID: " + expenseId + ")");
                
                if (expenseId != null) {
                    try {
                        // Parse amount (remove -P and formatting)
                        double amount = Double.parseDouble(amountText.replaceAll("[^\\d.]", ""));
                        
                        // Create expense object
                        Expense expense = new Expense();
                        expense.setId(expenseId);
                        expense.setCategory(category);
                        expense.setAmount(amount);
                        expense.setUserId(currentUserId);
                        expense.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                        
                        // IMPORTANT: Try to preserve original timestamp from cache if it exists
                        // This prevents timestamp conflicts when saving UI transactions to cache
                        long originalTimestamp = getTimestampFromCache(expenseId);
                        if (originalTimestamp > 0) {
                            expense.setTimestamp(originalTimestamp);
                            Log.d(TAG, "Preserved original timestamp for " + expenseId + ": " + originalTimestamp);
                        } else {
                            expense.setTimestamp(System.currentTimeMillis());
                            Log.d(TAG, "Using current timestamp for " + expenseId);
                        }
                        
                        currentExpenses.add(expense);
                        Log.d(TAG, "Preserved transaction: " + category + " - P" + amount + " (ID: " + expenseId + ")");
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse amount for transaction: " + amountText, e);
                    }
                } else {
                    Log.w(TAG, "Transaction has no ID tag: " + category + " - " + amountText);
                }
            }
        }
        
        if (!currentExpenses.isEmpty()) {
            // Save to cache
            cacheExpenses(currentExpenses);
            Log.d(TAG, "Successfully cached " + currentExpenses.size() + " transactions for preservation");
        } else {
            Log.d(TAG, "No transactions to cache");
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
                    
                    int todayCount =  0;
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
    }    private void resetDailyCategoryStates() {
        breakfastUsed = false;
        lunchUsed = false;
        dinnerUsed = false;
    }
    
    private void checkAndResetAvailableBudgetIfNewPeriod() {
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String lastPeriodKey = prefs.getString("last_period_key_" + currentUserId, "");
        String currentPeriodKey = getCurrentPeriodKey();
        
        if (!currentPeriodKey.equals(lastPeriodKey)) {
            // New week/period detected, recalculate both budgets from database
            Log.d(TAG, "New period detected, recalculating budgets from originalBudget: " + originalBudget);
            
            // Use the same ExpenseSummary logic for period reset too
            calculateWeeklyExpensesUsingExpenseSummaryLogic(totalWeeklyExpenses -> {
                // Calculate available budget (for spending logic)
                double newAvailableBudget = originalBudget - totalWeeklyExpenses;
                setAvailableBudget(newAvailableBudget);
                
                // Calculate amount budget (same as "saved" in expense summary: originalBudget - totalExpenses)
                double newAmountBudget = originalBudget - totalWeeklyExpenses;
                setAmountBudget(newAmountBudget);
                
                // Only update the display here, after recalculation
                runOnUiThread(this::updateBudgetDisplay);
                // Save the new period key
                prefs.edit().putString("last_period_key_" + currentUserId, currentPeriodKey).apply();
                Log.d(TAG, "Period reset complete - AmountBudget (saved): " + newAmountBudget + ", AvailableBudget: " + newAvailableBudget + ", WeeklyExpenses: " + totalWeeklyExpenses);
            });
        } else {
            // If not a new period, still calculate amount budget immediately (like Expense Summary)
            Log.d(TAG, "Same period, calculating amount budget like Expense Summary...");
            calculateAndSetAmountBudget();
        }
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
    private static final String PENDING_OPERATIONS_KEY = "pending_operations_";
    
    // Pending operation types
    private static final String OP_TYPE_EDIT = "edit";
    private static final String OP_TYPE_DELETE = "delete";
    
    /**
     * Load expenses from local cache when offline
     */
    private void loadExpensesFromCache() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot load from cache: currentUserId is null");
            return;
        }
        
        Log.d(TAG, "Loading expenses from cache for offline/fallback usage");
        debugCacheState("LOAD_FROM_CACHE_START");
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            Log.d(TAG, "Loading expenses from cache - Cache key: " + cacheKey);
            Log.d(TAG, "Cached JSON data: " + cachedJson.substring(0, Math.min(100, cachedJson.length())) + "...");
            
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
                
                // Add to UI with proper ID tags for edit/delete functionality
                for (Expense expense : cachedExpenses) {
                    addTransactionToView(expense.getCategory(), String.format("%.2f", expense.getAmount()), expense.getId());
                    Log.d(TAG, "Restored cached expense: " + expense.getCategory() + " - P" + expense.getAmount() + " (ID: " + expense.getId() + ")");
                }
                
                Log.d(TAG, "Loaded " + cachedExpenses.size() + " expenses from cache successfully");
                debugCacheState("LOAD_FROM_CACHE_SUCCESS");
                
                // Update budget calculations to reflect cached expenses when offline
                Log.d(TAG, "Updating budget calculations for cached expenses (offline mode)");
                calculateAndSetAmountBudget();
                
                // Check existing categories to set their states correctly
                checkExistingCategoriesForToday();
                
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cached expenses", e);
                debugCacheState("LOAD_FROM_CACHE_ERROR");
            }
        } else {
            Log.d(TAG, "No cached expenses found for today - cache key: " + cacheKey);
            debugCacheState("LOAD_FROM_CACHE_EMPTY");
            
            // Still update budget calculations even if no cached expenses
            Log.d(TAG, "Updating budget calculations (no cached expenses)");
            calculateAndSetAmountBudget();
        }
    }
    
    /**
     * Cache expenses locally for offline access
     */
    private void cacheExpenses(List<Expense> expenses) {
        if (currentUserId == null || expenses == null) {
            Log.w(TAG, "Cannot cache expenses: currentUserId=" + currentUserId + ", expenses=" + expenses);
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        String timestampKey = CACHE_TIMESTAMP_PREFIX + currentUserId + "_" + today;
        
        Log.d(TAG, "Caching " + expenses.size() + " expenses with key: " + cacheKey);
        
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
                Log.d(TAG, "Caching expense: " + expense.getCategory() + " - P" + expense.getAmount() + " (ID: " + expense.getId() + ")");
            }
            
            boolean success = prefs.edit()
                .putString(cacheKey, expenseArray.toString())
                .putLong(timestampKey, System.currentTimeMillis())
                .commit(); // Use commit() instead of apply() to ensure immediate write
                
            Log.d(TAG, "Cache write " + (success ? "successful" : "failed") + " for " + expenses.size() + " expenses");
        } catch (JSONException e) {
            Log.e(TAG, "Error caching expenses", e);
        }
    }
    
    /**
     * Add a single expense to cache (for new expenses added while offline)
     */
    private void addExpenseToCache(Expense expense) {
        if (currentUserId == null || expense == null) {
            Log.w(TAG, "Cannot add expense to cache: currentUserId=" + currentUserId + ", expense=" + expense);
            return;
        }
        
        Log.d(TAG, "Adding expense to cache: " + expense.getCategory() + " - P" + expense.getAmount() + " (ID: " + expense.getId() + ")");
        
        // Load existing cache
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        List<Expense> cachedExpenses = new ArrayList<>();
        String cachedJson = prefs.getString(cacheKey, null);
        
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                Log.d(TAG, "Found existing cache with " + cachedArray.length() + " expenses");
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
        } else {
            Log.d(TAG, "No existing cache found, creating new cache");
        }
        
        // Add new expense
        cachedExpenses.add(expense);
        Log.d(TAG, "Cache now contains " + cachedExpenses.size() + " expenses total");
        
        // Save updated cache
        cacheExpenses(cachedExpenses);
        Log.d(TAG, "Successfully added expense to cache");
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
                    
                    // Save updated cache without the temp expense
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
            Log.w(TAG, "Cannot get offline expenses: currentUserId is null");
            return offlineExpenses;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        Log.d(TAG, "Looking for offline expenses in cache with key: " + cacheKey);
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            Log.d(TAG, "Found cached data: " + cachedJson.substring(0, Math.min(200, cachedJson.length())) + "...");
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                
                Log.d(TAG, "Parsing " + cachedArray.length() + " cached expenses to find offline ones");
                
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
                        Log.d(TAG, "Found offline expense: " + expense.getCategory() + " - P" + expense.getAmount() + " (ID: " + expenseId + ")");
                    } else {
                        Log.d(TAG, "Skipping synced expense: " + expenseId);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cached expenses for offline sync", e);
            }
        } else {
            Log.d(TAG, "No cached data found for key: " + cacheKey);
        }
        
        Log.d(TAG, "Returning " + offlineExpenses.size() + " offline expenses from cache");
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
                    removeExpenseFromCache(tempId);
                    
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
     * Sync a pending operation (edit or delete) to Firebase
     */
    private void syncPendingOperation(JSONObject operation, SyncCallback callback) {
        try {
            String operationType = operation.getString("type");
            String expenseId = operation.getString("expenseId");
            
            Log.d(TAG, "Syncing pending " + operationType + " operation for expense: " + expenseId);
            
            if (OP_TYPE_EDIT.equals(operationType)) {
                // Handle edit operation
                JSONObject expenseDataJson = operation.getJSONObject("expenseData");
                
                Expense updatedExpense = new Expense(
                    expenseDataJson.getString("userId"),
                    expenseDataJson.getString("category"),
                    expenseDataJson.getDouble("amount"),
                    "", // Description
                    expenseDataJson.getString("date")
                );
                updatedExpense.setId(expenseId);
                updatedExpense.setTimestamp(expenseDataJson.getLong("timestamp"));
                
                firebaseManager.updateExpense(expenseId, updatedExpense)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Pending edit synced successfully: " + expenseId);
                            removePendingOperation(OP_TYPE_EDIT, expenseId);
                            callback.onSyncSuccess(expenseId, expenseId);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to sync pending edit: " + expenseId, e);
                            callback.onSyncFailure(expenseId, e);
                        });
                        
            } else if (OP_TYPE_DELETE.equals(operationType)) {
                // Handle delete operation
                firebaseManager.deleteExpense(expenseId)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Pending delete synced successfully: " + expenseId);
                            removePendingOperation(OP_TYPE_DELETE, expenseId);
                            callback.onSyncSuccess(expenseId, expenseId);
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to sync pending delete: " + expenseId, e);
                            callback.onSyncFailure(expenseId, e);
                        });
            } else {
                Log.w(TAG, "Unknown pending operation type: " + operationType);
                callback.onSyncFailure(expenseId, new Exception("Unknown operation type"));
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing pending operation", e);
            callback.onSyncFailure("unknown", e);
        }
    }

    private String getCurrentPeriodKey() {
        // Use week start date as key
        return PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
    }

    private double getAvailableBudget() {
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String key = "available_budget_" + currentUserId + "_" + getCurrentPeriodKey();
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(originalBudget)));
    }

    private void setAvailableBudget(double value) {
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String key = "available_budget_" + currentUserId + "_" + getCurrentPeriodKey();
        prefs.edit().putLong(key, Double.doubleToLongBits(value)).apply();
    }

    private double getAmountBudget() {
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String key = "amount_budget_" + currentUserId + "_" + getCurrentPeriodKey();
        double value = Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(originalBudget)));
        Log.d(TAG, "getAmountBudget() - Key: " + key + ", Value: " + value + ", OriginalBudget: " + originalBudget);
        return value;
    }

    private void setAmountBudget(double value) {
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String key = "amount_budget_" + currentUserId + "_" + getCurrentPeriodKey();
        prefs.edit().putLong(key, Double.doubleToLongBits(value)).apply();
        Log.d(TAG, "setAmountBudget() - Key: " + key + ", Value: " + value);
    }

    // Call this at the start of a new period/week
    private void resetAvailableBudgetToOriginal() {
        setAvailableBudget(originalBudget);
        setAmountBudget(originalBudget);
        Log.d(TAG, "Reset both budgets to original amount: " + originalBudget);
    }
    
    /**
     * Calculate and set amount budget immediately (like Expense Summary approach)
     */
    private void calculateAndSetAmountBudget() {
        Log.d(TAG, "Calculating and setting amount budget using exact ExpenseSummary logic...");
        Log.d(TAG, "Current originalBudget: " + originalBudget);
        
        // Use exact same logic as ExpenseSummaryActivity SAVED calculation
        calculateWeeklyExpensesUsingExpenseSummaryLogic(totalWeeklyExpenses -> {
            // Calculate saved amount exactly like ExpenseSummaryActivity (allow negative values)
            double weeklySavings = originalBudget - totalWeeklyExpenses;
            setAmountBudget(weeklySavings);
            
            // Update display immediately
            runOnUiThread(this::updateBudgetDisplay);
            
            Log.d(TAG, "Amount budget calculated using ExpenseSummary logic: P" + weeklySavings + 
                  " (Original budget: P" + originalBudget + " - Weekly expenses: P" + totalWeeklyExpenses + ")");
                  
            // Additional debug logging for new users
            if (totalWeeklyExpenses == 0.0 && weeklySavings != originalBudget) {
                Log.w(TAG, "WARNING: New user with no expenses but amountBudget != originalBudget!");
                Log.w(TAG, "Expected amountBudget: " + originalBudget + ", Actual: " + weeklySavings);
            }
        });
    }
    
    /**
     * Calculate weekly expenses using the exact same logic as ExpenseSummaryActivity
     * This ensures the amountBudget matches the SAVED value in Expense Summary
     */
    private void calculateWeeklyExpensesUsingExpenseSummaryLogic(ExpenseCalculationCallback callback) {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot calculate weekly expenses: currentUserId is null");
            callback.onExpensesCalculated(0.0);
            return;
        }
        
        // Check if we're online or offline to determine how to calculate expenses
        if (isNetworkAvailable()) {
            // Online: Get expenses from Firebase + merge with any cached offline expenses
            calculateWeeklyExpensesOnline(callback);
        } else {
            // Offline: Use only cached expenses
            calculateWeeklyExpensesOffline(callback);
        }
    }
    
    /**
     * Calculate weekly expenses when online (Firebase + cached offline expenses)
     */
    private void calculateWeeklyExpensesOnline(ExpenseCalculationCallback callback) {
        // Get all expenses for the user using the same approach as ExpenseSummaryActivity
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.collection("expenses")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Expense> allExpenses = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId());
                            allExpenses.add(expense);
                        }
                    }
                    
                    // Add any cached offline expenses that haven't been synced yet
                    List<Expense> offlineExpenses = getOfflineExpensesFromCache();
                    allExpenses.addAll(offlineExpenses);
                    
                    Log.d(TAG, "Loaded " + (allExpenses.size() - offlineExpenses.size()) + " Firebase expenses + " 
                          + offlineExpenses.size() + " offline expenses for weekly calculation");
                    
                    // Calculate weekly total
                    double weeklyTotal = calculateWeeklyTotalFromExpenses(allExpenses);
                    callback.onExpensesCalculated(weeklyTotal);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading expenses for weekly calculation, falling back to offline", e);
                    // Fallback to offline calculation if Firebase fails
                    calculateWeeklyExpensesOffline(callback);
                });
    }
    
    /**
     * Calculate weekly expenses when offline (cached expenses only)
     */
    private void calculateWeeklyExpensesOffline(ExpenseCalculationCallback callback) {
        Log.d(TAG, "Calculating weekly expenses offline using cached data");
        
        // Get all cached expenses for the current week period
        List<Expense> allCachedExpenses = getAllCachedExpensesForWeek();
        
        Log.d(TAG, "Loaded " + allCachedExpenses.size() + " cached expenses for weekly calculation");
        
        // Calculate weekly total
        double weeklyTotal = calculateWeeklyTotalFromExpenses(allCachedExpenses);
        callback.onExpensesCalculated(weeklyTotal);
    }
    
    /**
     * Calculate weekly total from a list of expenses
     */
    private double calculateWeeklyTotalFromExpenses(List<Expense> allExpenses) {
        // Calculate the current weekly period based on user's preferred start day (same as ExpenseSummaryActivity)
        String weekStartDate = getCurrentWeekStartDate();
        String weekEndDate = getCurrentWeekEndDate();
        
        Log.d(TAG, "Current week period: " + weekStartDate + " to " + weekEndDate);
        
        // Filter expenses to only include those in the current week (same as ExpenseSummaryActivity)
        double totalWeeklyExpenses = 0.0;
        int weeklyExpenseCount = 0;
        
        for (Expense expense : allExpenses) {
            String expenseDate = expense.getDate();
            if (expenseDate != null && isDateInCurrentWeek(expenseDate, weekStartDate, weekEndDate)) {
                totalWeeklyExpenses += expense.getAmount();
                weeklyExpenseCount++;
                Log.d(TAG, "Weekly expense included: " + expense.getCategory() + " - P" + expense.getAmount() + " on " + expenseDate + " (ID: " + expense.getId() + ")");
            }
        }
        
        Log.d(TAG, "Total weekly expenses: P" + totalWeeklyExpenses + " from " + weeklyExpenseCount + " expenses");
        return totalWeeklyExpenses;
    }
    
    /**
     * Get all cached expenses for the current week period
     */
    private List<Expense> getAllCachedExpensesForWeek() {
        List<Expense> weekExpenses = new ArrayList<>();
        
        if (currentUserId == null) {
            return weekExpenses;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String weekStartDate = getCurrentWeekStartDate();
        String weekEndDate = getCurrentWeekEndDate();
        
        // Get cached expenses for each day in the current week
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        try {
            java.util.Date startDate = dateFormat.parse(weekStartDate);
            java.util.Date endDate = dateFormat.parse(weekEndDate);
            
            calendar.setTime(startDate);
            
            while (!calendar.getTime().after(endDate)) {
                String currentDate = dateFormat.format(calendar.getTime());
                String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + currentDate;
                
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
                            weekExpenses.add(expense);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing cached expenses for date: " + currentDate, e);
                    }
                }
                
                // Move to next day
                calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }
        } catch (java.text.ParseException e) {
            Log.e(TAG, "Error parsing week dates", e);
        }
        
        return weekExpenses;
    }
    
    /**
     * Get current week start date based on user's preferred start day
     * Copied exactly from ExpenseSummaryActivity
     */
    private String getCurrentWeekStartDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get user's preferred start day
        String userStartDay = PreferenceActivity.getStartDay(this);
        int preferredStartDay = getDayOfWeekConstant(userStartDay);
        
        // Get current day of week (Sunday = 1, Monday = 2, ..., Saturday = 7)
        int currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        
        // Calculate days to go back to reach the preferred start day
        int daysToGoBack = calculateDaysToGoBack(currentDayOfWeek, preferredStartDay);
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToGoBack);
        String startDate = dateFormat.format(calendar.getTime());
        
        Log.d(TAG, "Week start calculation: Current day=" + currentDayOfWeek + 
               ", Preferred start day=" + preferredStartDay + " (" + userStartDay + ")" +
               ", Days to go back=" + daysToGoBack + ", Start date=" + startDate);
        
        return startDate;
    }
    
    /**
     * Get current week end date based on user's preferred start day
     * Copied exactly from ExpenseSummaryActivity
     */
    private String getCurrentWeekEndDate() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get user's preferred start day
        String userStartDay = PreferenceActivity.getStartDay(this);
        int preferredStartDay = getDayOfWeekConstant(userStartDay);
        
        // Get current day of week
        int currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        
        // Calculate days to go back to reach the preferred start day
        int daysToGoBack = calculateDaysToGoBack(currentDayOfWeek, preferredStartDay);
        
        // Go back to start of week, then add 6 days to get end of week
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToGoBack + 6);
        String endDate = dateFormat.format(calendar.getTime());
        
        Log.d(TAG, "Week end calculation: End date=" + endDate);
        
        return endDate;
    }
    
    /**
     * Convert day name to Calendar constant
     * Copied exactly from ExpenseSummaryActivity
     */
    private int getDayOfWeekConstant(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return java.util.Calendar.SUNDAY;
            case "monday": return java.util.Calendar.MONDAY;
            case "tuesday": return java.util.Calendar.TUESDAY;
            case "wednesday": return java.util.Calendar.WEDNESDAY;
            case "thursday": return java.util.Calendar.THURSDAY;
            case "friday": return java.util.Calendar.FRIDAY;
            case "saturday": return java.util.Calendar.SATURDAY;
            default: return java.util.Calendar.MONDAY; // Default to Monday if unknown
        }
    }
    
    /**
     * Calculate days to go back to reach preferred start day
     * Copied exactly from ExpenseSummaryActivity
     */
    private int calculateDaysToGoBack(int currentDay, int preferredStartDay) {
        int daysToGoBack = (currentDay - preferredStartDay + 7) % 7;
        Log.d(TAG, "Days to go back calculation: currentDay=" + currentDay + 
               ", preferredStartDay=" + preferredStartDay + ", result=" + daysToGoBack);
        return daysToGoBack;
    }
    
    /**
     * Check if a date is in the current week
     * Copied exactly from ExpenseSummaryActivity
     */
    private boolean isDateInCurrentWeek(String expenseDate, String weekStart, String weekEnd) {
        if (expenseDate == null || weekStart == null || weekEnd == null) {
            return false;
        }
        
        boolean inWeek = expenseDate.compareTo(weekStart) >= 0 && expenseDate.compareTo(weekEnd) <= 0;
        Log.d(TAG, "Date check: " + expenseDate + " in [" + weekStart + " to " + weekEnd + "] = " + inWeek);
        
        return inWeek;
    }
    
    /**
     * Force recalculate amount budget based on current expenses
     */
    private void recalculateAmountBudget() {
        if (currentUserId == null) {
            Log.w(TAG, "Cannot recalculate amount budget: currentUserId is null");
            return;
        }
        
        Log.d(TAG, "Force recalculating amount budget using ExpenseSummary logic...");
        // Use the same ExpenseSummary logic for consistency
        calculateWeeklyExpensesUsingExpenseSummaryLogic(totalWeeklyExpenses -> {
            double newAmountBudget = originalBudget - totalWeeklyExpenses;
            setAmountBudget(newAmountBudget);
            runOnUiThread(this::updateBudgetDisplay);
            Log.d(TAG, "Amount budget force recalculated: " + newAmountBudget + 
                  " (originalBudget: " + originalBudget + " - weeklyExpenses: " + totalWeeklyExpenses + ")");
        });
    }
    
    /**
     * Clean up cache to remove any temporary/offline expenses after Firebase sync
     * This ensures the cache only contains real Firebase expenses to prevent duplicates
     */
    private void cleanupCacheAfterSync() {
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
                List<Expense> cleanedExpenses = new ArrayList<>();
                int removedCount = 0;
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    String expenseId = expenseJson.getString("id");
                    
                    // Only keep expenses with real Firebase IDs (no temp_ prefixes)
                    if (!expenseId.startsWith("temp_")) {
                        Expense expense = new Expense();
                        expense.setId(expenseId);
                        expense.setCategory(expenseJson.getString("category"));
                        expense.setAmount(expenseJson.getDouble("amount"));
                        expense.setDate(expenseJson.getString("date"));
                        expense.setTimestamp(expenseJson.getLong("timestamp"));
                        expense.setUserId(expenseJson.getString("userId"));
                        cleanedExpenses.add(expense);
                    } else {
                        removedCount++;
                        Log.d(TAG, "Removed temp expense from cache during cleanup: " + expenseId);
                    }
                }
                
                // Update cache with cleaned data
                if (removedCount > 0) {
                    cacheExpenses(cleanedExpenses);
                    Log.d(TAG, "Cache cleanup complete: removed " + removedCount + " temp expenses, kept " + cleanedExpenses.size() + " real expenses");
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error during cache cleanup", e);
            }
        }
    }
    
    /**
     * Performs aggressive cleanup on app startup to prevent any duplicate UI elements
     * but preserves offline expenses that haven't been synced yet
     */
    private void performStartupCleanup() {
        Log.d(TAG, "Performing startup cleanup to prevent duplicates while preserving offline data");
        
        // Clear the transactions container immediately on startup
        LinearLayout transactionsContainer = findViewById(R.id.transactionsContainer);
        if (transactionsContainer != null) {
            clearDynamicTransactions(transactionsContainer);
        }
        
        // Reset category states
        resetDailyCategoryStates();
        
        // DON'T clean up cache on startup - this would remove offline expenses!
        // Only clean up cache after successful Firebase sync
        Log.d(TAG, "Preserving cache data on startup - offline expenses maintained");
        
        Log.d(TAG, "Startup cleanup completed (offline data preserved)");
    }
    
    // ================ PENDING OPERATIONS TRACKING ================
    
    /**
     * Add a pending operation to be synced later (for offline edits/deletes)
     */
    private void addPendingOperation(String operationType, String expenseId, Expense expenseData) {
        if (currentUserId == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String pendingKey = PENDING_OPERATIONS_KEY + currentUserId + "_" + today;
        
        try {
            // Get existing pending operations
            String existingOps = prefs.getString(pendingKey, "[]");
            JSONArray pendingArray = new JSONArray(existingOps);
            
            // Create new operation
            JSONObject operation = new JSONObject();
            operation.put("type", operationType);
            operation.put("expenseId", expenseId);
            operation.put("timestamp", System.currentTimeMillis());
            
            if (expenseData != null) {
                JSONObject expenseJson = new JSONObject();
                expenseJson.put("id", expenseData.getId());
                expenseJson.put("userId", expenseData.getUserId());
                expenseJson.put("category", expenseData.getCategory());
                expenseJson.put("amount", expenseData.getAmount());
                expenseJson.put("date", expenseData.getDate());
                expenseJson.put("timestamp", expenseData.getTimestamp());
                operation.put("expenseData", expenseJson);
            }
            
            // Add to pending operations
            pendingArray.put(operation);
            
            // Save back to preferences
            prefs.edit().putString(pendingKey, pendingArray.toString()).apply();
            
            Log.d(TAG, "Added pending " + operationType + " operation for expense: " + expenseId);
        } catch (JSONException e) {
            Log.e(TAG, "Error adding pending operation", e);
        }
    }
    
    /**
     * Get all pending operations for today
     */
    private List<JSONObject> getPendingOperations() {
        List<JSONObject> operations = new ArrayList<>();
        
        if (currentUserId == null) {
            return operations;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String pendingKey = PENDING_OPERATIONS_KEY + currentUserId + "_" + today;
        
        try {
            String pendingOps = prefs.getString(pendingKey, "[]");
            JSONArray pendingArray = new JSONArray(pendingOps);
            
            for (int i = 0; i < pendingArray.length(); i++) {
                operations.add(pendingArray.getJSONObject(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading pending operations", e);
        }
        
        return operations;
    }
    
    /**
     * Clear all pending operations for today (after successful sync)
     */
    private void clearPendingOperations() {
        if (currentUserId == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String pendingKey = PENDING_OPERATIONS_KEY + currentUserId + "_" + today;
        
        prefs.edit().remove(pendingKey).apply();
        Log.d(TAG, "Cleared all pending operations for today");
    }
    
    /**
     * Remove a specific pending operation after successful sync
     */
    private void removePendingOperation(String operationType, String expenseId) {
        if (currentUserId == null) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String pendingKey = PENDING_OPERATIONS_KEY + currentUserId + "_" + today;
        
        try {
            String existingOps = prefs.getString(pendingKey, "[]");
            JSONArray pendingArray = new JSONArray(existingOps);
            JSONArray newArray = new JSONArray();
            
            for (int i = 0; i < pendingArray.length(); i++) {
                JSONObject op = pendingArray.getJSONObject(i);
                String opType = op.getString("type");
                String opExpenseId = op.getString("expenseId");
                
                // Keep operations that don't match the one we want to remove
                if (!opType.equals(operationType) || !opExpenseId.equals(expenseId)) {
                    newArray.put(op);
                }
            }
            
            prefs.edit().putString(pendingKey, newArray.toString()).apply();
            Log.d(TAG, "Removed pending " + operationType + " operation for expense: " + expenseId);
        } catch (JSONException e) {
            Log.e(TAG, "Error removing pending operation", e);
        }
    }
    
    /**
     * Update transaction item tag from temp ID to real Firebase ID
     */
    private void updateTransactionItemTag(String oldTempId, String newFirebaseId) {
        LinearLayout container = findViewById(R.id.transactionsContainer);
        if (container == null) {
            return;
        }
        
        // Search through all transaction items to find the one with the old temp ID
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child != null && child.getTag() != null) {
                String tag = (String) child.getTag();
                if (oldTempId.equals(tag)) {
                    // Update the tag with the new Firebase ID
                    child.setTag(newFirebaseId);
                    Log.d(TAG, "Updated transaction item tag from " + oldTempId + " to " + newFirebaseId);
                    return;
                }
            }
        }
        
        Log.d(TAG, "Transaction item with temp ID " + oldTempId + " not found in UI");
    }
    
    // ================ BUDGET STORAGE METHODS ================

    /**
     * Debug method to check current cache state
     */
    private void debugCacheState(String context) {
        if (currentUserId == null) {
            Log.d(TAG, "[CACHE DEBUG " + context + "] currentUserId is null");
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String cacheKey = CACHE_KEY_PREFIX + currentUserId + "_" + today;
        
        String cachedJson = prefs.getString(cacheKey, null);
        if (cachedJson != null) {
            try {
                JSONArray cachedArray = new JSONArray(cachedJson);
                Log.d(TAG, "[CACHE DEBUG " + context + "] Found " + cachedArray.length() + " cached expenses");
                
                for (int i = 0; i < cachedArray.length(); i++) {
                    JSONObject expenseJson = cachedArray.getJSONObject(i);
                    String expenseId = expenseJson.getString("id");
                    String category = expenseJson.getString("category");
                    double amount = expenseJson.getDouble("amount");
                    boolean isOffline = expenseId.startsWith("temp_");
                    Log.d(TAG, "[CACHE DEBUG " + context + "] " + (i + 1) + ". " + category + " - P" + amount + 
                          " (ID: " + expenseId + ", Offline: " + isOffline + ")");
                }
            } catch (JSONException e) {
                Log.e(TAG, "[CACHE DEBUG " + context + "] Error parsing cache", e);
            }
        } else {
            Log.d(TAG, "[CACHE DEBUG " + context + "] No cache found for key: " + cacheKey);
        }
    }
    
    /**
     * Get the original timestamp for an expense from cache
     * This helps preserve timestamp integrity when saving UI transactions to cache
     */
    private long getTimestampFromCache(String expenseId) {
        if (currentUserId == null || expenseId == null) {
            return 0;
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
                    String cachedId = expenseJson.getString("id");
                    if (expenseId.equals(cachedId)) {
                        return expenseJson.getLong("timestamp");
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error getting timestamp from cache for " + expenseId, e);
            }
        }
        return 0;
    }
    
    /**
     * Check if the transactions container has any transaction items
     */
    private boolean hasTransactionItems(LinearLayout container) {
        if (container == null) {
            return false;
        }
        
        // Check if any child views are transaction items
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            // Transaction items have categoryName and transactionAmount TextViews
            if (child.findViewById(R.id.categoryName) != null && 
                child.findViewById(R.id.transactionAmount) != null) {
                Log.d(TAG, "Found transaction item in UI at index " + i);
                return true;
            }
        }
        
        Log.d(TAG, "No transaction items found in UI (container has " + container.getChildCount() + " children)");
        return false;
    }
}
