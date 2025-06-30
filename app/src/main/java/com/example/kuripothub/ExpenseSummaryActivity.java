package com.example.kuripothub;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.Legend;
import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.utils.PreferenceBasedDateUtils;
import com.example.kuripothub.models.Expense;
import com.example.kuripothub.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/*
 * WEEKLY ANALYTICS WITH USER-CONFIGURABLE START DAY:
 * 
 * This analytics screen uses REAL data from Firebase and implements proper weekly periods
 * based on the user's preferences.
 * 
 * PREFERENCES (USER-CONFIGURABLE):
 * - Start Day: Configurable (Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
 * - Budget Reset: Configurable (Every week, Every month, Do not reset)
 * - Spending Limit: Configurable (No Limit, or custom limit)
 * 
 * WEEKLY PERIOD CALCULATION:
 * - Week starts and ends based on user's selected start day preference
 * - For example, if user selects Monday as start day:
 *   Current week runs from Monday to Sunday
 * - If user selects Saturday as start day:
 *   Current week runs from Saturday to Friday
 * - Budget resets based on user's budget reset preference
 * 
 * FOUR CATEGORIES:
 * 1. FOODS: breakfast, lunch, dinner, snack
 * 2. SCHOOL ESSENTIAL: Printing, SchoolSupplies
 * 3. PERSONAL NEEDS: Laundry, Mobile Load
 * 4. MISCELLANEOUS: All other categories
 * 
 * The pie chart displays expenses from the current week only, respecting the user's
 * preferred week start day and budget reset preferences.
 */

public class ExpenseSummaryActivity extends AppCompatActivity {

    private static final String TAG = "ExpenseSummary";
    
    private FirebaseManager firebaseManager;
    private String currentUserId;
    private double currentBudget = 0.0;
    
    // UI Components
    private PieChart categoryPieChart;
    private TextView[] dayTotalTextViews = new TextView[7];
    private TextView[] dayLabelTextViews = new TextView[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_summary);

        // Initialize Firebase
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager.getCurrentUser() != null) {
            currentUserId = firebaseManager.getCurrentUser().getUid();
        }

        initializeViews();
        setupBackButton();
        
        // FOR TESTING: Temporarily disabled to focus on real data
        // addTestDataManually();
        
        loadUserDataAndAnalytics();
    }

    private void initializeViews() {
        categoryPieChart = findViewById(R.id.savingsBarChart); // Use the bar chart view for pie chart
        
        // Set up dynamic daily spending layout
        setupDynamicDailySpendingLayout();
    }

    private void setupBackButton() {
        CardView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadUserDataAndAnalytics() {
        if (currentUserId == null) {
            Log.w(TAG, "User not logged in");
            return;
        }

        // Load user budget first
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            currentBudget = user.getBudget();
                        }
                    }
                    
                    // After loading budget, load analytics data
                    loadAnalyticsData();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading user data", e);
                    loadAnalyticsData(); // Continue with default budget
                });
    }

    private void loadAnalyticsData() {
        Log.d(TAG, "Loading analytics with REAL database data and user preferences...");

        // GET USER PREFERENCES FROM PREFERENCE ACTIVITY
        Log.d(TAG, "=== LOADING USER PREFERENCES ===");
        String userStartDay = PreferenceActivity.getStartDay(this);
        String userBudgetReset = PreferenceActivity.getBudgetReset(this);
        String userSpendingLimit = PreferenceActivity.getSpendingLimit(this);

        Log.d(TAG, "User preferences loaded:");
        Log.d(TAG, "  Start day: " + userStartDay);
        Log.d(TAG, "  Budget reset: " + userBudgetReset);
        Log.d(TAG, "  Spending limit: " + userSpendingLimit);

        if (currentUserId == null) {
            Log.w(TAG, "No user ID available, trying to get current user...");
            if (firebaseManager.getCurrentUser() != null) {
                currentUserId = firebaseManager.getCurrentUser().getUid();
                Log.d(TAG, "Retrieved user ID: " + currentUserId);
            } else {
                Log.e(TAG, "No Firebase user logged in!");
                showEmptyState();
                return;
            }
        }

        Log.d(TAG, "Fetching expenses for user ID: " + currentUserId);

        // Try all possible user IDs to fetch expenses for analytics
        tryMultipleUserIdApproaches();
    }
    
    private void processRealExpenseData(List<Expense> allExpenses) {
        Log.d(TAG, "Processing real expense data from database...");
        
        // Calculate total expenses
        double totalExpenses = 0.0;
        for (Expense expense : allExpenses) {
            totalExpenses += expense.getAmount();
            Log.d(TAG, "Real expense: " + expense.getCategory() + " - ₱" + expense.getAmount() + " on " + expense.getDate());
        }
        
        Log.d(TAG, "Total expenses from database: ₱" + totalExpenses);
        
        // Update charts and UI with real categorized data
        updateCategoryPieChart(allExpenses); // This now uses categorization
        String weekStartDate = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        updateWeeklyDailySpending(allExpenses, weekStartDate); // Use the new dynamic method
        updateSummaryCards(totalExpenses);
        
        Log.d(TAG, "Analytics updated with real categorized data successfully");
    }
    
    // Removed old fallback data methods - now only using real database data

    private void updateCategoryPieChart(List<Expense> expenses) {
        Log.d(TAG, "Updating category pie chart with " + expenses.size() + " total expenses");
        
        // Filter expenses to only include those from the past week
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date oneWeekAgo = calendar.getTime();
        
        Map<String, Float> categoryTotals = new HashMap<>();
        float totalExpenseAmount = 0f;
        int weekExpenseCount = 0;
        
        for (Expense expense : expenses) {
            String expenseDateStr = expense.getDate();
            if (expenseDateStr != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date expenseDate = sdf.parse(expenseDateStr);
                    
                    if (expenseDate != null && expenseDate.after(oneWeekAgo)) {
                        String mainCategory = getMainCategory(expense.getCategory());
                        float amount = (float) expense.getAmount();
                        categoryTotals.put(mainCategory, categoryTotals.getOrDefault(mainCategory, 0f) + amount);
                        totalExpenseAmount += amount;
                        weekExpenseCount++;
                        Log.d(TAG, "Past week expense: " + expense.getCategory() + " -> " + mainCategory + " (₱" + amount + ")");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse expense date: " + expenseDateStr);
                }
            }
        }
        
        Log.d(TAG, "Past week expenses: " + weekExpenseCount + " expenses, total amount: ₱" + totalExpenseAmount);
        Log.d(TAG, "Category breakdown: " + categoryTotals.toString());
        
        if (categoryTotals.isEmpty()) {
            Log.d(TAG, "No expenses found for the past week, pie chart will be empty");
            categoryPieChart.clear();
            categoryPieChart.setNoDataText("No expenses\nin past week");
            categoryPieChart.invalidate();
            return;
        }
        
        // Create pie entries for only the four specified categories
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        String[] mainCategories = {"Foods", "School Essential", "Personal Needs", "Miscellaneous"};
        int[] colors = {
            Color.rgb(255, 7, 7),   // Amber for Foods
            Color.rgb(76, 175, 80),   // Green for School Essential
            Color.rgb(33, 150, 243),  // Blue for Personal Needs
            Color.rgb(156, 39, 176)   // Purple for Miscellaneous
        };
        
        ArrayList<Integer> pieColors = new ArrayList<>();
        
        for (int i = 0; i < mainCategories.length; i++) {
            String category = mainCategories[i];
            Float amount = categoryTotals.get(category);
            if (amount != null && amount > 0) {
                pieEntries.add(new PieEntry(amount, category));
                pieColors.add(colors[i]);
                Log.d(TAG, "Added to pie chart: " + category + " = ₱" + amount);
            }
        }
        
        if (pieEntries.isEmpty()) {
            Log.d(TAG, "No valid category data for pie chart");
            categoryPieChart.clear();
            categoryPieChart.setNoDataText("No category data");
            categoryPieChart.invalidate();
            return;
        }
        
        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(pieColors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₱" + String.format("%.0f", value);
            }
        });
        
        PieData pieData = new PieData(dataSet);
        categoryPieChart.setData(pieData);
        
        // Configure pie chart appearance
        categoryPieChart.setUsePercentValues(false);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleColor(Color.TRANSPARENT);
        categoryPieChart.setHoleRadius(40f);
        categoryPieChart.setTransparentCircleRadius(45f);
        categoryPieChart.setCenterText("Past Week\nExpenses");
        categoryPieChart.setCenterTextSize(14f);
        categoryPieChart.setCenterTextColor(Color.GRAY);
        categoryPieChart.setDrawEntryLabels(true);
        categoryPieChart.setEntryLabelTextSize(10f);
        categoryPieChart.setEntryLabelColor(Color.BLACK);
        
        // Configure legend
        Legend legend = categoryPieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        
        categoryPieChart.animateY(1000);
        categoryPieChart.invalidate();
        
        Log.d(TAG, "Pie chart updated successfully with " + pieEntries.size() + " categories");
        
        Log.d(TAG, "Category breakdown pie chart updated with " + categoryTotals.size() + " categories using real database data");
    }

    private void updatePeriodIndicator(String periodDescription) {
        // Optional: Add a TextView with id 'periodIndicator' to your layout to show the current period
        // For now, we'll just log the period description
        Log.d(TAG, "Current period: " + periodDescription);
        
        // If you want to show the period, you can add a TextView to your layout with id="periodIndicator"
        // and uncomment the following lines:
        /*
        TextView periodIndicator = findViewById(R.id.periodIndicator);
        if (periodIndicator != null) {
            periodIndicator.setText(periodDescription);
        }
        */
    }

    /* COMMENTED OUT - OLD METHOD, REPLACED BY updateWeeklyDailySpending
    private void updateDailySpending(List<Expense> expenses) {
        // Get the date range for the current period
        String startDate = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        String endDate = PreferenceBasedDateUtils.getCurrentPeriodEndDate(this);
        String budgetReset = PreferenceActivity.getBudgetReset(this);
        
        Log.d(TAG, "Updating daily spending for period: " + startDate + " to " + endDate);
        
        // Create a map of daily totals within the current period
        Map<String, Double> dailyTotals = new HashMap<>();
        
        for (Expense expense : expenses) {
            String date = expense.getDate();
            // For weekly, include all expenses from start date onward
            if (budgetReset.equals("Every week")) {
                if (date != null && date.compareTo(startDate) >= 0) {
                    dailyTotals.put(date, dailyTotals.getOrDefault(date, 0.0) + expense.getAmount());
                    Log.d(TAG, "Daily spending: " + date + " += " + expense.getAmount());
                }
            } else {
                // For monthly/no reset, use full period check
                if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0) {
                    dailyTotals.put(date, dailyTotals.getOrDefault(date, 0.0) + expense.getAmount());
                }
            }
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault()); // Short day format
        
        TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total};
        
        // Find corresponding day labels
        TextView day1Label = findViewById(R.id.day1Label);
        TextView day2Label = findViewById(R.id.day2Label);
        TextView day3Label = findViewById(R.id.day3Label);
        TextView day4Label = findViewById(R.id.day4Label);
        TextView day5Label = findViewById(R.id.day5Label);
        TextView[] dayLabels = {day1Label, day2Label, day3Label, day4Label, day5Label};
        
        try {
            // For weekly reset, show the days starting from the week start
            if (budgetReset.equals("Every week")) {
                Date startDateObj = dateFormat.parse(startDate);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(startDateObj);
                
                Log.d(TAG, "Starting daily spending from: " + dateFormat.format(calendar.getTime()));
                
                // Show the first 5 days of the week starting from the start day
                for (int i = 0; i < 5 && i < 7; i++) {
                    String date = dateFormat.format(calendar.getTime());
                    String dayName = dayFormat.format(calendar.getTime());
                    
                    // Check if this date is today or in the past (don't show future days that haven't happened)
                    Calendar today = Calendar.getInstance();
                    String todayStr = dateFormat.format(today.getTime());
                    
                    if (date.compareTo(todayStr) <= 0) {
                        // This day has happened, show the spending
                        double dayTotal = dailyTotals.getOrDefault(date, 0.0);
                        
                        if (i < dayLabels.length && dayLabels[i] != null) {
                            dayLabels[i].setText(dayName);
                        }
                        dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
                        
                        Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = P" + String.format("%.0f", dayTotal));
                    } else {
                        // Future day - show as upcoming
                        if (i < dayLabels.length && dayLabels[i] != null) {
                            dayLabels[i].setText(dayName);
                        }
                        dayTotals[i].setText("Total: P0");
                        
                        Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = Future day");
                    }
                    
                    // Move to next day
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else {
                // For monthly reset or no reset, show the last 5 days within the period
                Calendar endCalendar = Calendar.getInstance();
                Date endDateObj = dateFormat.parse(endDate);
                if (endDateObj.before(endCalendar.getTime())) {
                    endCalendar.setTime(endDateObj);
                }
                
                // Go back 4 days from end date to show last 5 days
                endCalendar.add(Calendar.DAY_OF_MONTH, -4);
                
                for (int i = 0; i < 5; i++) {
                    String date = dateFormat.format(endCalendar.getTime());
                    String dayName = dayFormat.format(endCalendar.getTime());
                    
                    // Only show days within the current period
                    if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0) {
                        double dayTotal = dailyTotals.getOrDefault(date, 0.0);
                        
                        if (i < dayLabels.length && dayLabels[i] != null) {
                            dayLabels[i].setText(dayName);
                        }
                        dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
                    } else {
                        if (i < dayLabels.length && dayLabels[i] != null) {
                            dayLabels[i].setText("-");
                        }
                        dayTotals[i].setText("Total: P0");
                    }
                    
                    // Move to next day
                    endCalendar.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing dates for daily spending", e);
            
            // Fallback: show last 5 days from today
            Calendar calendar = Calendar.getInstance();
            for (int i = 0; i < 5; i++) {
                String date = dateFormat.format(calendar.getTime());
                String dayName = dayFormat.format(calendar.getTime());
                
                double dayTotal = dailyTotals.getOrDefault(date, 0.0);
                
                if (i < dayLabels.length && dayLabels[i] != null) {
                    dayLabels[i].setText(dayName);
                }
                dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
                
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
        }
    }
    END OF COMMENTED OUT METHOD */

    private String getMainCategory(String category) {
        if (category == null) return "Miscellaneous";
        
        String lowerCategory = category.toLowerCase();
        
        // Foods: breakfast, lunch, dinner, snack
        if (lowerCategory.contains("breakfast") || lowerCategory.contains("lunch") || 
            lowerCategory.contains("dinner") || lowerCategory.contains("snack") ||
            lowerCategory.contains("food") || lowerCategory.contains("meal") ||
            lowerCategory.contains("eating") || lowerCategory.contains("dining") ||
            lowerCategory.contains("restaurant") || lowerCategory.contains("grocery") ||
            lowerCategory.contains("coffee") || lowerCategory.contains("drinks")) {
            return "Foods";
        }
        
        // School Essential: Printing, SchoolSupplies
        if (lowerCategory.contains("printing") || lowerCategory.contains("schoolsupplies") ||
            lowerCategory.contains("school supplies") || lowerCategory.contains("school") ||
            lowerCategory.contains("education") || lowerCategory.contains("book") ||
            lowerCategory.contains("supplies") || lowerCategory.contains("stationery") ||
            lowerCategory.contains("notebook") || lowerCategory.contains("pen") ||
            lowerCategory.contains("paper") || lowerCategory.contains("tuition")) {
            return "School Essential";
        }
        
        // Personal Needs: Laundry, Mobile Load
        if (lowerCategory.contains("laundry") || lowerCategory.contains("mobile load") ||
            lowerCategory.contains("load") || lowerCategory.contains("personal") ||
            lowerCategory.contains("hygiene") || lowerCategory.contains("toiletries") ||
            lowerCategory.contains("soap") || lowerCategory.contains("shampoo") ||
            lowerCategory.contains("toothpaste") || lowerCategory.contains("phone") ||
            lowerCategory.contains("data") || lowerCategory.contains("prepaid")) {
            return "Personal Needs";
        }
        
        // Everything else goes to Miscellaneous
        return "Miscellaneous";
    }

    private void updateSummaryCards(double totalExpenses) {
        Log.d(TAG, "Updating summary cards...");
        
        // Use real budget from user profile, fallback to 2000 if not set
        double budget = currentBudget > 0 ? currentBudget : 2000.0;
        double savings = Math.max(0, budget - totalExpenses);
        
        // Update budget card
        TextView budgetText = findViewById(R.id.budgetAmountText);
        if (budgetText != null) {
            budgetText.setText(String.format("%.0f", budget));
        }
        
        // Update expense card
        TextView expenseText = findViewById(R.id.expenseAmountText);
        if (expenseText != null) {
            expenseText.setText(String.format("%.0f", totalExpenses));
        }
        
        // Update saved card
        TextView savedText = findViewById(R.id.savedAmountText);
        if (savedText != null) {
            savedText.setText(String.format("%.0f", savings));
        }
        
        Log.d(TAG, "Summary cards updated - Budget: " + budget + ", Expenses: " + totalExpenses + ", Savings: " + savings);
    }

    private void testExpenseDataDirectly() {
        Log.d(TAG, "=== TESTING EXPENSE DATA DIRECTLY ===");
        
        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Total documents in Firebase: " + queryDocumentSnapshots.size());
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "NO EXPENSES FOUND IN DATABASE AT ALL!");
                        return;
                    }
                    
                    Log.d(TAG, "Sample of ALL expenses in database:");
                    int count = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            count++;
                            Log.d(TAG, "  Expense " + count + ": Date='" + expense.getDate() + "', Amount=" + expense.getAmount() + ", Category='" + expense.getCategory() + "'");
                            
                            // Only show first 5 to avoid log spam
                            if (count >= 5) {
                                Log.d(TAG, "  ... (showing first 5 of " + queryDocumentSnapshots.size() + " expenses)");
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get expenses from Firebase", e);
                });
        
        Log.d(TAG, "=== END EXPENSE DATA TEST ===");
    }

    private void addTestExpenseIfNeeded() {
        Log.d(TAG, "=== ADDING TEST EXPENSE FOR JUNE 28 ===");
        
        // Create a test expense for June 28, 2025 (Saturday) - the start of the current week
        com.example.kuripothub.models.Expense testExpense = new com.example.kuripothub.models.Expense();
        testExpense.setUserId(currentUserId);
        testExpense.setAmount(250.0);
        testExpense.setCategory("Food");
        testExpense.setDate("2025-06-28");
        testExpense.setDescription("Test expense for Saturday (week start)");
        
        firebaseManager.addExpense(testExpense)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Test expense added successfully with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add test expense", e);
                });
    }

    private void simpleDebugTest() {
        Log.d(TAG, "=== SIMPLE DEBUG TEST ===");
        
        // Check current preferences BEFORE forcing them
        android.content.SharedPreferences prefs = getSharedPreferences("KuripotHubPreferences", MODE_PRIVATE);
        String currentStartDay = prefs.getString("start_day", "Monday");
        String currentBudgetReset = prefs.getString("budget_reset", "Every week");
        String currentSpendingLimit = prefs.getString("spending_limit", "No Limit");
        
        Log.d(TAG, "CURRENT preferences BEFORE forcing:");
        Log.d(TAG, "  Start Day: '" + currentStartDay + "'");
        Log.d(TAG, "  Budget Reset: '" + currentBudgetReset + "'");
        Log.d(TAG, "  Spending Limit: '" + currentSpendingLimit + "'");
        
        // Force set preferences for testing
        Log.d(TAG, "FORCING PREFERENCES FOR TEST...");
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("start_day", "Saturday");
        editor.putString("budget_reset", "Every week");
        editor.putString("spending_limit", "No Limit");
        editor.apply();
        Log.d(TAG, "Preferences forced!");
        
        // 1. Show current preferences after forcing
        String startDay = PreferenceActivity.getStartDay(this);
        String budgetReset = PreferenceActivity.getBudgetReset(this);
        String spendingLimit = PreferenceActivity.getSpendingLimit(this);
        
        Log.d(TAG, "User Preferences AFTER forcing:");
        Log.d(TAG, "  Start Day: '" + startDay + "'");
        Log.d(TAG, "  Budget Reset: '" + budgetReset + "'");
        Log.d(TAG, "  Spending Limit: '" + spendingLimit + "'");
        
        // 2. Show calculated period
        String periodStart = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        String periodEnd = PreferenceBasedDateUtils.getCurrentPeriodEndDate(this);
        
        Log.d(TAG, "Calculated Period:");
        Log.d(TAG, "  Start: '" + periodStart + "'");
        Log.d(TAG, "  End: '" + periodEnd + "'");
        
        // 3. Test the problematic date directly
        String testDate = "2025-06-28";
        boolean isIncluded = PreferenceBasedDateUtils.isExpenseInCurrentPeriod(this, testDate);
        Log.d(TAG, "Test Date Check:");
        Log.d(TAG, "  Date: '" + testDate + "'");
        Log.d(TAG, "  Is in current period: " + isIncluded);
        
        // 4. Manual string comparison
        Log.d(TAG, "Manual String Comparison:");
        Log.d(TAG, "  '" + testDate + "' >= '" + periodStart + "' ? " + (testDate.compareTo(periodStart) >= 0));
        Log.d(TAG, "  '" + testDate + "' <= '" + periodEnd + "' ? " + (testDate.compareTo(periodEnd) <= 0));
        
        Log.d(TAG, "=== END SIMPLE DEBUG TEST ===");
    }

    private void debugDateCalculations() {
        Log.d(TAG, "=== DEBUG: Date Calculations ===");
        
        // Test the week calculation logic first
        PreferenceBasedDateUtils.testWeekCalculation(this);
        
        Log.d(TAG, "Current preferences:");
        Log.d(TAG, "  Start Day: " + PreferenceActivity.getStartDay(this));
        Log.d(TAG, "  Budget Reset: " + PreferenceActivity.getBudgetReset(this));
        Log.d(TAG, "  Spending Limit: " + PreferenceActivity.getSpendingLimit(this));
        
        String startDate = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        String endDate = PreferenceBasedDateUtils.getCurrentPeriodEndDate(this);
        String description = PreferenceBasedDateUtils.getCurrentPeriodDescription(this);
        
        Log.d(TAG, "Calculated period:");
        Log.d(TAG, "  Start Date: " + startDate);
        Log.d(TAG, "  End Date: " + endDate);
        Log.d(TAG, "  Description: " + description);
        
        // Test with today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        boolean todayInPeriod = PreferenceBasedDateUtils.isExpenseInCurrentPeriod(this, today);
        Log.d(TAG, "Today (" + today + ") in current period: " + todayInPeriod);
        
        // Test with the specific dates from your screenshot
        String[] testDates = {
            "2025-06-28", // Your expense date (Saturday)
            "2025-06-29", // Today (Sunday)
            "2025-06-27", // Friday
            "2025-06-30", // Monday
            "2025-07-01", // Tuesday
            "2025-07-04"  // Friday (end of week if Saturday start)
        };
        
        for (String testDate : testDates) {
            boolean inPeriod = PreferenceBasedDateUtils.isExpenseInCurrentPeriod(this, testDate);
            Log.d(TAG, "Test date " + testDate + " in period: " + inPeriod);
        }
        
        // Debug specific scenario: Saturday start, Sunday current
        Log.d(TAG, "=== SATURDAY START WEEK DEBUG ===");
        Calendar cal = Calendar.getInstance();
        Log.d(TAG, "Current day of week (Calendar constant): " + cal.get(Calendar.DAY_OF_WEEK));
        Log.d(TAG, "Saturday constant: " + Calendar.SATURDAY);
        Log.d(TAG, "Sunday constant: " + Calendar.SUNDAY);
        
        Log.d(TAG, "=== END DEBUG ===");
    }

    /* COMMENTED OUT - OLD METHOD
    private void updateSimpleDailySpending() {
        Log.d(TAG, "Updating simple daily spending...");
        
        // Simple daily spending data for this week
        String[] days = {"Day 1", "Day 2", "Day 3", "Day 4", "Day 5"};
        double[] amounts = {750.0, 500.0, 45.0, 180.0, 25.0}; // Matches the test expenses
        
        TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total};
        TextView[] dayLabels = {
            findViewById(R.id.day1Label),
            findViewById(R.id.day2Label),
            findViewById(R.id.day3Label),
            findViewById(R.id.day4Label),
            findViewById(R.id.day5Label}
        };
        
        // Update each day
        for (int i = 0; i < 5; i++) {
            if (i < days.length && i < dayLabels.length && dayLabels[i] != null) {
                dayLabels[i].setText(days[i]);
            }
            
            if (i < amounts.length && i < dayTotals.length && dayTotals[i] != null) {
                dayTotals[i].setText("Total: P" + String.format("%.0f", amounts[i]));
            }
            
            Log.d(TAG, "Day " + (i+1) + ": " + days[i] + " = P" + String.format("%.0f", amounts[i]));
        }
        
        Log.d(TAG, "Simple daily spending updated");
    }
    END OF COMMENTED OUT METHOD */

    /* COMMENTED OUT - OLD METHOD, REPLACED BY updateWeeklyDailySpending
    private void updateRealDailySpending(List<Expense> allExpenses) {
        Log.d(TAG, "Updating daily spending with real data...");
        
        // Create a map to store daily totals
        Map<String, Double> dailyTotals = new HashMap<>();
        
        // Calculate daily totals from real expenses
        for (Expense expense : allExpenses) {
            String date = expense.getDate();
            if (date != null) {
                double currentTotal = dailyTotals.getOrDefault(date, 0.0);
                dailyTotals.put(date, currentTotal + expense.getAmount());
            }
        }
        
        // Get current date for comparison
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar currentDate = Calendar.getInstance();
        
        // Get the last 5 days including today
        List<String> last5Days = new ArrayList<>();
        List<String> dayNames = new ArrayList<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -4); // Go back 4 days to show last 5 days
        
        for (int i = 0; i < 5; i++) {
            String date = dateFormat.format(cal.getTime());
            String dayName = dayFormat.format(cal.getTime());
            last5Days.add(date);
            dayNames.add(dayName);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Update UI elements
        TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total};
        TextView[] dayLabels = {
            findViewById(R.id.day1Label),
            findViewById(R.id.day2Label),
            findViewById(R.id.day3Label),
            findViewById(R.id.day4Label),
            findViewById(R.id.day5Label}
        };
        
        // Update each day with real data
        for (int i = 0; i < 5 && i < last5Days.size(); i++) {
            String date = last5Days.get(i);
            String dayName = dayNames.get(i);
            double dayTotal = dailyTotals.getOrDefault(date, 0.0);
            
            if (i < dayLabels.length && dayLabels[i] != null) {
                dayLabels[i].setText(dayName);
            }
            
            if (i < dayTotals.length && dayTotals[i] != null) {
                dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
            }
            
            Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = P" + String.format("%.0f", dayTotal));
        }
        
        Log.d(TAG, "Real daily spending updated with " + dailyTotals.size() + " unique dates");
    }
    */
    
    private void processRealExpenseDataWithWeeklyPeriod(List<Expense> allExpenses) {
        Log.d(TAG, "Processing real expense data with weekly period filtering...");
        
        // Debug the filtering logic
        debugRealDataFiltering();
        
        // Calculate the current weekly period based on user's preferred start day
        String weekStartDate = getCurrentWeekStartDate(); // e.g., June 28, 2025 if Saturday is selected
        String weekEndDate = getCurrentWeekEndDate();     // July 4, 2025 (Friday)
        
        Log.d(TAG, "Current week period: " + weekStartDate + " to " + weekEndDate);
        
        // Filter expenses to only include those in the current week
        List<Expense> weeklyExpenses = new ArrayList<>();
        double totalWeeklyExpenses = 0.0;
        
        for (Expense expense : allExpenses) {
            String expenseDate = expense.getDate();
            if (expenseDate != null && isDateInCurrentWeek(expenseDate, weekStartDate, weekEndDate)) {
                weeklyExpenses.add(expense);
                totalWeeklyExpenses += expense.getAmount();
                Log.d(TAG, "Weekly expense included: " + expense.getCategory() + " - ₱" + expense.getAmount() + " on " + expenseDate);
            }
        }
        
        Log.d(TAG, "Total weekly expenses: ₱" + totalWeeklyExpenses + " from " + weeklyExpenses.size() + " expenses");
        
        // If no weekly data, show recent data instead
        if (weeklyExpenses.isEmpty()) {
            Log.w(TAG, "No expenses found for current week, showing recent data instead");
            showRecentDataFallback(allExpenses);
            return;
        }
        
        // Update charts and UI with weekly filtered data
        updateCategoryPieChartForWeek(weeklyExpenses);
        updateWeeklyDailySpending(weeklyExpenses, weekStartDate);
        updateSummaryCardsWithWeeklyBudget(totalWeeklyExpenses);
        
        Log.d(TAG, "Analytics updated with weekly period data successfully");
    }
    
    /**
     * Get the start date of the current week based on user preference
     * Uses the user's selected start day from preferences (Monday, Tuesday, etc.)
     */
    private String getCurrentWeekStartDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Get user's preferred start day
        String userStartDay = PreferenceActivity.getStartDay(this);
        int preferredStartDay = getDayOfWeekConstant(userStartDay);
        
        // Get current day of week (Sunday = 1, Monday = 2, ..., Saturday = 7)
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Calculate days to go back to reach the preferred start day
        int daysToGoBack = calculateDaysToGoBack(currentDayOfWeek, preferredStartDay);
        
        calendar.add(Calendar.DAY_OF_YEAR, -daysToGoBack);
        String startDate = dateFormat.format(calendar.getTime());
        
        Log.d(TAG, "Week start calculation: Current day=" + currentDayOfWeek + 
               ", Preferred start day=" + preferredStartDay + " (" + userStartDay + ")" +
               ", Days to go back=" + daysToGoBack + ", Start date=" + startDate);
        
        return startDate;
    }
    
    /**
     * Convert day name to Calendar constant
     */
    private int getDayOfWeekConstant(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY; // Default to Monday if unknown
        }
    }
    
    /**
     * Calculate how many days to go back to reach the most recent occurrence of the start day
     * 
     * LOGIC EXPLANATION:
     * - If today IS the selected start day (e.g., today is Monday, selected Monday): start from today (0 days back)
     * - If today is AFTER the selected start day in the same week (e.g., today is Wednesday, selected Monday): 
     *   go back to the most recent Monday (2 days back)
     * - If today is BEFORE the selected start day in the week (e.g., today is Monday, selected Wednesday):
     *   go back to the most recent Wednesday from previous week
     * 
     * EXAMPLES:
     * - Today: Monday (June 30), Selected: Monday → Week starts June 30 (0 days back)
     * - Today: Monday (June 30), Selected: Saturday → Week starts June 28 (2 days back) 
     * - Today: Monday (June 30), Selected: Wednesday → Week starts June 25 (5 days back)
     */
    private int calculateDaysToGoBack(int currentDay, int startDay) {
        if (currentDay == startDay) {
            // Today is the start day - start from today
            return 0;
        } else if (currentDay > startDay) {
            // Current day is later in the week than start day
            // Go back to the most recent occurrence of start day
            return currentDay - startDay;
        } else {
            // Current day is earlier in the week than start day
            // Need to go back to previous week's occurrence of start day
            // Example: Today is Monday (2), start day is Wednesday (4)
            // Days back = (7 - 4) + 2 = 5 days (go back to previous Wednesday)
            return (7 - startDay) + currentDay;
        }
    }
    
    /**
     * Get the end date of the current week based on user's start day preference
     * Calculates the end of the 7-day week starting from the user's preferred start day
     */
    private String getCurrentWeekEndDate() {
        String startDate = getCurrentWeekStartDate();
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(startDate));
            
            // Add 6 days to get the last day of the week (7-day period)
            calendar.add(Calendar.DAY_OF_YEAR, 6);
            
            String endDate = dateFormat.format(calendar.getTime());
            Log.d(TAG, "Week end calculation: Start=" + startDate + ", End=" + endDate);
            
            return endDate;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating week end date", e);
            return startDate; // Fallback
        }
    }
    
    /**
     * Check if a date falls within the current week period
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
     * Update pie chart with only weekly expenses
     */
    private void updateCategoryPieChartForWeek(List<Expense> weeklyExpenses) {
        Log.d(TAG, "Updating category pie chart with " + weeklyExpenses.size() + " weekly expenses");
        
        Map<String, Float> categoryTotals = new HashMap<>();
        float totalExpenseAmount = 0f;
        
        for (Expense expense : weeklyExpenses) {
            String mainCategory = getMainCategory(expense.getCategory());
            float amount = (float) expense.getAmount();
            categoryTotals.put(mainCategory, categoryTotals.getOrDefault(mainCategory, 0f) + amount);
            totalExpenseAmount += amount;
            Log.d(TAG, "Weekly expense: " + expense.getCategory() + " -> " + mainCategory + " (₱" + amount + ")");
        }
        
        Log.d(TAG, "Weekly category breakdown: " + categoryTotals.toString());
        Log.d(TAG, "Total weekly amount: ₱" + totalExpenseAmount);
        
        if (categoryTotals.isEmpty()) {
            Log.d(TAG, "No weekly expenses found, pie chart will be empty");
            categoryPieChart.clear();
            categoryPieChart.setNoDataText("No expenses\nthis week");
            categoryPieChart.invalidate();
            return;
        }
        
        // Create pie entries for only the four specified categories
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        String[] mainCategories = {"Foods", "School Essential", "Personal Needs", "Miscellaneous"};
        int[] colors = {
            Color.rgb(255, 7, 7),   // Amber for Foods
            Color.rgb(76, 175, 80),   // Green for School Essential
            Color.rgb(33, 150, 243),  // Blue for Personal Needs
            Color.rgb(156, 39, 176)   // Purple for Miscellaneous
        };
        
        ArrayList<Integer> pieColors = new ArrayList<>();
        
        for (int i = 0; i < mainCategories.length; i++) {
            String category = mainCategories[i];
            Float amount = categoryTotals.get(category);
            if (amount != null && amount > 0) {
                pieEntries.add(new PieEntry(amount, category));
                pieColors.add(colors[i]);
                Log.d(TAG, "Added to pie chart: " + category + " = ₱" + amount);
            }
        }
        
        if (pieEntries.isEmpty()) {
            Log.d(TAG, "No valid category data for pie chart");
            categoryPieChart.clear();
            categoryPieChart.setNoDataText("No category data");
            categoryPieChart.invalidate();
            return;
        }
        
        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(pieColors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "₱" + String.format("%.0f", value);
            }
        });
        
        PieData pieData = new PieData(dataSet);
        categoryPieChart.setData(pieData);
        
        // Configure pie chart appearance
        categoryPieChart.setUsePercentValues(false);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleColor(Color.TRANSPARENT);
        categoryPieChart.setHoleRadius(40f);
        categoryPieChart.setTransparentCircleRadius(45f);
        categoryPieChart.setCenterText("This Week\nExpenses");
        categoryPieChart.setCenterTextSize(10f);
        categoryPieChart.setCenterTextColor(Color.BLACK);
        categoryPieChart.setDrawEntryLabels(true);
        categoryPieChart.setEntryLabelTextSize(10f);
        categoryPieChart.setEntryLabelColor(Color.BLACK);
        
        // Configure legend
        Legend legend = categoryPieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        
        categoryPieChart.animateY(1000);
        categoryPieChart.invalidate();
        
        Log.d(TAG, "Weekly pie chart updated successfully with " + pieEntries.size() + " categories");
    }
    
    /**
     * Update daily spending for the current week based on user's start day preference - All 7 days
     */
    private void updateWeeklyDailySpending(List<Expense> weeklyExpenses, String weekStartDate) {
        Log.d(TAG, "Updating weekly daily spending starting from " + weekStartDate + " (all 7 days)");
        
        // IMPORTANT: Recreate the dynamic layout to ensure correct day order based on current preferences
        setupDynamicDailySpendingLayout();
        
        // Create a map to store daily totals for the week
        Map<String, Double> dailyTotals = new HashMap<>();
        
        // Calculate daily totals from weekly expenses
        for (Expense expense : weeklyExpenses) {
            String date = expense.getDate();
            if (date != null) {
                double currentTotal = dailyTotals.getOrDefault(date, 0.0);
                dailyTotals.put(date, currentTotal + expense.getAmount());
            }
        }
        
        // Get all 7 days of the current week starting from user's preferred day
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(weekStartDate));
            
            TextView[] dayTotals = dayTotalTextViews;
            TextView[] dayLabels = dayLabelTextViews;
            
            // Show all 7 days of the week starting from user's preferred day
            // But only show actual data for days that have already occurred
            Calendar today = Calendar.getInstance();
            String todayDateStr = dateFormat.format(today.getTime());
            
            for (int i = 0; i < 7; i++) {
                String date = dateFormat.format(calendar.getTime());
                String dayName = dayFormat.format(calendar.getTime());
                double dayTotal = dailyTotals.getOrDefault(date, 0.0);
                
                // Check if this date is in the future
                boolean isFutureDate = date.compareTo(todayDateStr) > 0;
                
                if (i < dayLabels.length && dayLabels[i] != null) {
                    dayLabels[i].setText(dayName);
                }
                
                if (i < dayTotals.length && dayTotals[i] != null) {
                    if (isFutureDate) {
                        // Future date - show as not yet happened
                        dayTotals[i].setText("------");
                        Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = Future date, showing ------");
                    } else if (dayTotal > 0) {
                        // Past/today date with spending
                        dayTotals[i].setText("Total: ₱" + String.format("%.0f", dayTotal));
                        Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = ₱" + String.format("%.0f", dayTotal));
                    } else {
                        // Past/today date with no spending
                        dayTotals[i].setText("------");
                        Log.d(TAG, "Day " + (i+1) + ": " + dayName + " (" + date + ") = ₱0, showing ------");
                    }
                }
                
                // Move to next day
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating weekly daily spending", e);
            
            // Fallback: show generic labels based on user's preferred start day
            TextView[] dayTotals = dayTotalTextViews;
            TextView[] dayLabels = dayLabelTextViews;
            
            String[] fallbackDays = getFallbackDayNames();
            for (int i = 0; i < 7; i++) {
                if (i < dayLabels.length && dayLabels[i] != null) {
                    dayLabels[i].setText(fallbackDays[i]);
                }
                if (i < dayTotals.length && dayTotals[i] != null) {
                    dayTotals[i].setText("------");
                }
            }
        }
        
        Log.d(TAG, "Weekly daily spending updated for all 7 days");
    }
    
    /**
     * Update summary cards with weekly budget consideration
     * Budget resets based on user's preference (weekly, monthly, or never)
     */
    private void updateSummaryCardsWithWeeklyBudget(double weeklyExpenses) {
        Log.d(TAG, "Updating summary cards with weekly budget reset logic...");
        
        // Use the original budget value (resets based on user preference)
        double weeklyBudget = currentBudget > 0 ? currentBudget : 2000.0;
        double weeklySavings = Math.max(0, weeklyBudget - weeklyExpenses);
        
        String userStartDay = PreferenceActivity.getStartDay(this);
        String budgetReset = PreferenceActivity.getBudgetReset(this);
        
        Log.d(TAG, "Weekly budget calculation:");
        Log.d(TAG, "  Original budget (resets " + budgetReset.toLowerCase() + " starting " + userStartDay + "): ₱" + weeklyBudget);
        Log.d(TAG, "  Weekly expenses: ₱" + weeklyExpenses);
        Log.d(TAG, "  Weekly savings: ₱" + weeklySavings);
        
        // Update budget card
        TextView budgetText = findViewById(R.id.budgetAmountText);
        if (budgetText != null) {
            budgetText.setText(String.format("%.0f", weeklyBudget));
        }
        
        // Update expense card
        TextView expenseText = findViewById(R.id.expenseAmountText);
        if (expenseText != null) {
            expenseText.setText(String.format("%.0f", weeklyExpenses));
        }
        
        // Update saved card
        TextView savedText = findViewById(R.id.savedAmountText);
        if (savedText != null) {
            savedText.setText(String.format("%.0f", weeklySavings));
        }
        
        Log.d(TAG, "Summary cards updated with weekly values");
    }
    
    /**
     * Show empty state when no data is available
     */
    private void showEmptyState() {
        Log.d(TAG, "Showing empty state - no real data available");
        
        // Clear pie chart
        categoryPieChart.clear();
        categoryPieChart.setNoDataText("No expenses found\nAdd some expenses to see analytics");
        categoryPieChart.invalidate();
        
        // Clear daily spending for all 7 days
        TextView[] dayTotals = dayTotalTextViews;
        TextView[] dayLabels = dayLabelTextViews;
        
        String[] dayNames = getFallbackDayNames();
        
        for (int i = 0; i < 7; i++) {
            if (i < dayLabels.length && dayLabels[i] != null) {
                dayLabels[i].setText(dayNames[i]);
            }
            if (i < dayTotals.length && dayTotals[i] != null) {
                dayTotals[i].setText("------");
            }
        }
        
        // Update summary cards with zero values
        updateSummaryCardsWithWeeklyBudget(0.0);
        
        Log.d(TAG, "Empty state displayed");
    }
    
    /**
     * Debug method to test real data filtering for the current week
     */
    private void debugRealDataFiltering() {
        Log.d(TAG, "=== DEBUG: Real Data Filtering ===");
        
        // First debug the week start calculation
        debugWeekStartCalculation();
        
        // Test the logic for different start days
        testWeekStartLogicForDifferentDays();
        
        String weekStart = getCurrentWeekStartDate();
        String weekEnd = getCurrentWeekEndDate();
        
        Log.d(TAG, "Current week period: " + weekStart + " to " + weekEnd);
        Log.d(TAG, "Today is: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        
        // Test sample dates
        String[] testDates = {
            "2025-06-27", // Friday before (should NOT be included)
            "2025-06-28", // Saturday start (should be included)
            "2025-06-29", // Sunday (should be included)
            "2025-06-30", // Monday (should be included)
            "2025-07-01", // Tuesday (should be included)
            "2025-07-02", // Wednesday (should be included)
            "2025-07-03", // Thursday (should be included)
            "2025-07-04", // Friday end (should be included)
            "2025-07-05"  // Saturday next week (should NOT be included)
        };
        
        for (String testDate : testDates) {
            boolean included = isDateInCurrentWeek(testDate, weekStart, weekEnd);
            Log.d(TAG, "Date " + testDate + " included: " + included);
        }
        
        Log.d(TAG, "=== END DEBUG ===");
    }
    
    /**
     * Show recent data when no current week data is available
     */
    private void showRecentDataFallback(List<Expense> allExpenses) {
        Log.d(TAG, "Showing recent data fallback with " + allExpenses.size() + " total expenses");
        
        // Sort expenses by date (most recent first)
        allExpenses.sort((e1, e2) -> {
            if (e1.getDate() == null) return 1;
            if (e2.getDate() == null) return -1;
            return e2.getDate().compareTo(e1.getDate());
        });
        
        // Take the most recent expenses (last 30 days or up to 20 expenses)
        List<Expense> recentExpenses = new ArrayList<>();
        double totalRecentExpenses = 0.0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        try {
            Calendar thirtyDaysAgo = Calendar.getInstance();
            thirtyDaysAgo.add(Calendar.DAY_OF_YEAR, -30);
            String thirtyDaysAgoStr = dateFormat.format(thirtyDaysAgo.getTime());
            
            int count = 0;
            for (Expense expense : allExpenses) {
                if (count >= 20) break; // Limit to 20 recent expenses
                
                String expenseDate = expense.getDate();
                if (expenseDate != null && expenseDate.compareTo(thirtyDaysAgoStr) >= 0) {
                    recentExpenses.add(expense);
                    totalRecentExpenses += expense.getAmount();
                    count++;
                    Log.d(TAG, "Recent expense: " + expense.getCategory() + " - ₱" + expense.getAmount() + " on " + expenseDate);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering recent expenses, using all available", e);
            // Fallback: use first 10 expenses
            for (int i = 0; i < Math.min(10, allExpenses.size()); i++) {
                recentExpenses.add(allExpenses.get(i));
                totalRecentExpenses += allExpenses.get(i).getAmount();
            }
        }
        
        Log.d(TAG, "Using " + recentExpenses.size() + " recent expenses, total: ₱" + totalRecentExpenses);
        
        // Update UI with recent data
        updateCategoryPieChartForWeek(recentExpenses); // Reuse the same method
        String weekStartDate = PreferenceBasedDateUtils.getCurrentPeriodStartDate(this);
        updateWeeklyDailySpending(recentExpenses, weekStartDate); // Use the new dynamic method
        updateSummaryCardsWithWeeklyBudget(totalRecentExpenses);
        
        // Show message about using recent data
        categoryPieChart.setCenterText("Recent\nExpenses");
        categoryPieChart.invalidate();
        
        Log.d(TAG, "Recent data fallback displayed successfully");
    }
    
    /**
     * Update daily spending with recent data (last 7 days)
     * DEPRECATED: This method is replaced by updateWeeklyDailySpending() which uses dynamic UI
     */
    /*
    private void updateRecentDailySpending(List<Expense> recentExpenses) {
        Log.d(TAG, "Updating daily spending with recent data...");
        
        // Create a map to store daily totals
        Map<String, Double> dailyTotals = new HashMap<>();
        
        // Calculate daily totals from recent expenses
        for (Expense expense : recentExpenses) {
            String date = expense.getDate();
            if (date != null) {
                double currentTotal = dailyTotals.getOrDefault(date, 0.0);
                dailyTotals.put(date, currentTotal + expense.getAmount());
            }
        }
        
        // Get the last 7 days including today
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            
            TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total, day6Total, day7Total};
            TextView[] dayLabels = {
                findViewById(R.id.day1Label),
                findViewById(R.id.day2Label),
                findViewById(R.id.day3Label),
                findViewById(R.id.day4Label),
                findViewById(R.id.day5Label),
                findViewById(R.id.day6Label),
                findViewById(R.id.day7Label)
            };
            
            // Show the last 7 days (including today, going backwards)
            for (int i = 0; i < 7; i++) {
                String date = dateFormat.format(calendar.getTime());
                String dayName = dayFormat.format(calendar.getTime());
                double dayTotal = dailyTotals.getOrDefault(date, 0.0);
                
                if (i < dayLabels.length && dayLabels[i] != null) {
                    dayLabels[i].setText(dayName);
                }
                
                if (i < dayTotals.length && dayTotals[i] != null) {
                    dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
                }
                
                Log.d(TAG, "Recent day " + (i+1) + ": " + dayName + " (" + date + ") = P" + String.format("%.0f", dayTotal));
                
                // Move to previous day
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating recent daily spending", e);
            
            // Fallback: show generic labels
            TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total, day6Total, day7Total};
            TextView[] dayLabels = {
                findViewById(R.id.day1Label),
                findViewById(R.id.day2Label),
                findViewById(R.id.day3Label),
                findViewById(R.id.day4Label),
                findViewById(R.id.day5Label),
                findViewById(R.id.day6Label),
                findViewById(R.id.day7Label)
            };
            
            String[] fallbackDays = {"Today", "Yesterday", "Day-2", "Day-3", "Day-4", "Day-5", "Day-6"};
            for (int i = 0; i < 7; i++) {
                if (i < dayLabels.length && dayLabels[i] != null) {
                    dayLabels[i].setText(fallbackDays[i]);
                }
                if (i < dayTotals.length && dayTotals[i] != null) {
                    dayTotals[i].setText("Total: P0");
                }
            }
        }
        
        Log.d(TAG, "Recent daily spending updated for last 7 days");
    }
    */
    
    /**
     * Add sample test data if the database is completely empty
     */
    private void addSampleDataIfEmpty() {
        Log.d(TAG, "Adding sample data to database for testing...");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // Create sample expenses for the current week and past days
        List<com.example.kuripothub.models.Expense> sampleExpenses = new ArrayList<>();
        
        // Sample data for June 28 (Saturday - current week start)
        calendar.set(2025, Calendar.JUNE, 28);
        sampleExpenses.add(createSampleExpense("breakfast", 250.0, dateFormat.format(calendar.getTime()), "Breakfast at cafeteria"));
        
        // Sample data for June 29 (Sunday)
        calendar.set(2025, Calendar.JUNE, 29);
        sampleExpenses.add(createSampleExpense("lunch", 180.0, dateFormat.format(calendar.getTime()), "Lunch meal"));
        
        // Sample data for June 30 (Monday - today)
        calendar.set(2025, Calendar.JUNE, 30);
        sampleExpenses.add(createSampleExpense("snack", 45.0, dateFormat.format(calendar.getTime()), "Afternoon snack"));
        sampleExpenses.add(createSampleExpense("Printing", 25.0, dateFormat.format(calendar.getTime()), "Document printing"));
        
        // Sample data for previous days
        calendar.set(2025, Calendar.JUNE, 27);
        sampleExpenses.add(createSampleExpense("Mobile Load", 100.0, dateFormat.format(calendar.getTime()), "Phone load"));
        
        calendar.set(2025, Calendar.JUNE, 26);
        sampleExpenses.add(createSampleExpense("dinner", 320.0, dateFormat.format(calendar.getTime()), "Dinner"));
        
        // Add each sample expense to Firebase
        for (com.example.kuripothub.models.Expense expense : sampleExpenses) {
            firebaseManager.addExpense(expense)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Sample expense added: " + expense.getCategory() + " - ₱" + expense.getAmount());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add sample expense: " + expense.getCategory(), e);
                    });
        }
        
        Log.d(TAG, "Added " + sampleExpenses.size() + " sample expenses to database");
        
        // Reload data after adding samples
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            Log.d(TAG, "Reloading analytics after adding sample data...");
            loadAnalyticsData();
        }, 2000); // Wait 2 seconds for Firebase to process
    }
    
    private com.example.kuripothub.models.Expense createSampleExpense(String category, double amount, String date, String description) {
        com.example.kuripothub.models.Expense expense = new com.example.kuripothub.models.Expense();
        expense.setUserId(currentUserId);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setDate(date);
        expense.setDescription(description);
        return expense;
    }

    /**
     * Manual method to add test data for debugging (can be called from logs or debugging)
     */
    private void addTestDataManually() {
        Log.d(TAG, "=== MANUALLY ADDING TEST DATA ===");
        addSampleDataIfEmpty();
    }

    /**
     * Debug method to fetch ALL expenses from database (not filtered by user)
     */
    private void debugFetchAllExpenses() {
        Log.d(TAG, "=== DEBUG: Fetching ALL expenses from database ===");
        
        // Use Firebase directly since we may not have access to FirebaseManager's db field
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.collection("expenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " total expenses in database:");
                    
                    int count = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        count++;
                        Map<String, Object> data = document.getData();
                        if (data != null) {
                            Log.d(TAG, "  Expense " + count + ": " + data.toString());
                        }
                        
                        if (count >= 10) break; // Show first 10 only
                    }
                    
                    Log.d(TAG, "Current user ID being used: " + currentUserId);
                    
                    // Now also try to fetch data without user ID filter
                    fetchExpensesWithoutUserFilter();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch all expenses", e);
                });
    }
    
    /**
     * Try to fetch expenses that match the current week regardless of user ID
     */
    private void fetchExpensesWithoutUserFilter() {
        Log.d(TAG, "=== Fetching expenses without user filter ===");
        
        // Get the correct week dates based on user's preference
        String weekStartDate = getCurrentWeekStartDate();
        String weekEndDate = getCurrentWeekEndDate();
        Log.d(TAG, "Using user's preferred week: " + weekStartDate + " to " + weekEndDate);
        
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        db.collection("expenses")
                .whereGreaterThanOrEqualTo("date", weekStartDate)
                .whereLessThanOrEqualTo("date", weekEndDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " expenses in current week (all users):");
                    
                    List<Expense> weekExpenses = new ArrayList<>();
                    double totalAmount = 0.0;
                    
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId());
                            weekExpenses.add(expense);
                            totalAmount += expense.getAmount();
                            Log.d(TAG, "  Week expense: " + expense.getCategory() + " - ₱" + expense.getAmount() + 
                                     " on " + expense.getDate() + " (User: " + expense.getUserId() + ")");
                        }
                    }
                    
                    if (!weekExpenses.isEmpty()) {
                        Log.d(TAG, "Using current week data regardless of user filter: " + weekExpenses.size() + " expenses, total: ₱" + totalAmount);
                        
                        // Use this data for display with the correct user-preferred week start date
                        String userWeekStartDate = getCurrentWeekStartDate(); // Use user's preference instead of hardcoded date
                        updateCategoryPieChartForWeek(weekExpenses);
                        updateWeeklyDailySpending(weekExpenses, userWeekStartDate);
                        updateSummaryCardsWithWeeklyBudget(totalAmount);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch week expenses", e);
                });
    }

    /**
     * Try different approaches to fetch user expenses
     */
    private void tryMultipleUserIdApproaches() {
        Log.d(TAG, "=== Trying multiple user ID approaches ===");
        
        // Approach 1: Use current Firebase user ID
        if (firebaseManager.getCurrentUser() != null) {
            String firebaseUid = firebaseManager.getCurrentUser().getUid();
            Log.d(TAG, "Firebase Auth UID: " + firebaseUid);
            
            fetchExpensesForSpecificUser(firebaseUid, "Firebase Auth UID");
        }
        
        // Approach 2: Use the currentUserId we have
        if (currentUserId != null) {
            Log.d(TAG, "Current User ID: " + currentUserId);
            fetchExpensesForSpecificUser(currentUserId, "Current User ID");
        }
        
        // Approach 3: Use the user ID from the screenshot
        String screenshotUserId = "m0xkOwgP2lYZIRsqP4b46ziA6OE3";
        Log.d(TAG, "Screenshot User ID: " + screenshotUserId);
        fetchExpensesForSpecificUser(screenshotUserId, "Screenshot User ID");
    }
    
    private void fetchExpensesForSpecificUser(String userId, String userIdType) {
        Log.d(TAG, "Fetching expenses for " + userIdType + ": " + userId);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, userIdType + " found " + queryDocumentSnapshots.size() + " expenses");
                    
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Expense> userExpenses = new ArrayList<>();
                        double totalAmount = 0.0;
                        
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Expense expense = document.toObject(Expense.class);
                            if (expense != null) {
                                expense.setId(document.getId());
                                userExpenses.add(expense);
                                totalAmount += expense.getAmount();
                                Log.d(TAG, "  " + userIdType + " expense: " + expense.getCategory() + 
                                         " - ₱" + expense.getAmount() + " on " + expense.getDate());
                            }
                        }
                        
                        Log.d(TAG, userIdType + " success! Using " + userExpenses.size() + 
                                 " expenses, total: ₱" + totalAmount);
                        
                        // Use the first successful result
                        if (!userExpenses.isEmpty()) {
                            currentUserId = userId; // Update the current user ID
                            processRealExpenseDataWithWeeklyPeriod(userExpenses);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch expenses for " + userIdType, e);
                });
    }
    
    /**
     * Generate fallback day names based on user's preferred start day
     */
    private String[] getFallbackDayNames() {
        String userStartDay = PreferenceActivity.getStartDay(this);
        String[] allDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String[] result = new String[7];
        
        int startIndex = 0;
        switch (userStartDay.toLowerCase()) {
            case "sunday": startIndex = 0; break;
            case "monday": startIndex = 1; break;
            case "tuesday": startIndex = 2; break;
            case "wednesday": startIndex = 3; break;
            case "thursday": startIndex = 4; break;
            case "friday": startIndex = 5; break;
            case "saturday": startIndex = 6; break;
            default: startIndex = 1; // Default to Monday
        }
        
        for (int i = 0; i < 7; i++) {
            result[i] = allDays[(startIndex + i) % 7];
        }
        
        return result;
    }

    /**
     * Debug method to force set user preferences (for testing)
     */
    private void debugForceSetUserPreferences() {
        Log.d(TAG, "=== DEBUG: Force Set User Preferences ===");
        
        // Force set preferences for testing
        Log.d(TAG, "FORCING PREFERENCES FOR TEST...");
        android.content.SharedPreferences prefs = getSharedPreferences("KuripotHubPreferences", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
               editor.putString("start_day", "Saturday");
        editor.putString("budget_reset", "Every week");
        editor.putString("spending_limit", "No Limit");
        editor.apply();
        Log.d(TAG, "Preferences forced!");
        
        // Reload analytics to apply new preferences
        loadAnalyticsData();
        
        Log.d(TAG, "=== END DEBUG ===");
    }
    
    /**
     * Test and debug the week start calculation for different scenarios
     */
    private void debugWeekStartCalculation() {
        Log.d(TAG, "=== DEBUG: Week Start Calculation ===");
        
        String currentUserStart = PreferenceActivity.getStartDay(this);
        Log.d(TAG, "Current user preference: " + currentUserStart);
        
        // Get today's info
        Calendar today = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        
        String todayDate = dateFormat.format(today.getTime());
        String todayDayName = dayFormat.format(today.getTime());
        int todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        
        Log.d(TAG, "Today: " + todayDayName + " (" + todayDate + ") - Calendar constant: " + todayDayOfWeek);
        
        // Test all possible start days and show the 7-day sequence
        String[] allDays = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        for (String testStartDay : allDays) {
            int startDayConstant = getDayOfWeekConstant(testStartDay);
            int daysBack = calculateDaysToGoBack(todayDayOfWeek, startDayConstant);
            
            Calendar testCalendar = Calendar.getInstance();
            testCalendar.add(Calendar.DAY_OF_YEAR, -daysBack);
            String calculatedStartDate = dateFormat.format(testCalendar.getTime());
            String calculatedStartDayName = dayFormat.format(testCalendar.getTime());
            
            // Show the 7-day sequence for this start day
            StringBuilder weekSequence = new StringBuilder();
            Calendar seqCalendar = Calendar.getInstance();
            seqCalendar.setTime(testCalendar.getTime());
            
            for (int i = 0; i < 7; i++) {
                String seqDate = dateFormat.format(seqCalendar.getTime());
                SimpleDateFormat shortDayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
                String shortDay = shortDayFormat.format(seqCalendar.getTime());
                
                // Check if this is today or future
                String indicator = "";
                if (seqDate.equals(todayDate)) {
                    indicator = " [TODAY]";
                } else if (seqDate.compareTo(todayDate) > 0) {
                    indicator = " [FUTURE - show ------]";
                } else {
                    indicator = " [PAST - show data if exists]";
                }
                
                weekSequence.append(shortDay).append("(").append(seqDate).append(")").append(indicator);
                if (i < 6) weekSequence.append(" → ");
                
                seqCalendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            Log.d(TAG, "Start day: " + testStartDay + " → Week starts: " + calculatedStartDayName + " (" + calculatedStartDate + ") [" + daysBack + " days back]");
            Log.d(TAG, "  Week sequence: " + weekSequence.toString());
        }
        
        Log.d(TAG, "=== Current calculation result ===");
        String actualStart = getCurrentWeekStartDate();
        String actualEnd = getCurrentWeekEndDate();
        Log.d(TAG, "Actual week: " + actualStart + " to " + actualEnd);
        
        Log.d(TAG, "=== END DEBUG ===");
    }

    /**
     * Test method to demonstrate week start logic for different preferences
     */
    private void testWeekStartLogicForDifferentDays() {
        Log.d(TAG, "=== TESTING WEEK START LOGIC ===");
        
        // Get today's info
        Calendar today = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        
        String todayDate = dateFormat.format(today.getTime());
        String todayDayName = dayFormat.format(today.getTime());
        
        Log.d(TAG, "TODAY: " + todayDayName + " (" + todayDate + ")");
        Log.d(TAG, "");
        
        // Test scenarios for different start days
        String[] testDays = {"Saturday", "Monday", "Tuesday"};
        
        for (String startDay : testDays) {
            Log.d(TAG, "=== IF USER CHOOSES " + startDay.toUpperCase() + " AS START DAY ===");
            
            // Temporarily save current preference
            String originalPref = PreferenceActivity.getStartDay(this);
            
            // Set test preference
            android.content.SharedPreferences prefs = getSharedPreferences("KuripotHubPreferences", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("start_day", startDay);
            editor.apply();
            
            // Calculate week start
            String weekStart = getCurrentWeekStartDate();
            
            // Show the 7-day sequence
            Calendar weekCal = Calendar.getInstance();
            try {
                weekCal.setTime(dateFormat.parse(weekStart));
                
                StringBuilder sequence = new StringBuilder();
                for (int i = 0; i < 7; i++) {
                    String date = dateFormat.format(weekCal.getTime());
                    String dayName = dayFormat.format(weekCal.getTime());
                    
                    String indicator = "";
                    if (date.equals(todayDate)) {
                        indicator = " [TODAY]";
                    } else if (date.compareTo(todayDate) > 0) {
                        indicator = " [FUTURE - show ------]";
                    } else {
                        indicator = " [PAST - show data if exists]";
                    }
                    
                    sequence.append("Day ").append(i + 1).append(": ").append(dayName).append(" (").append(date).append(")").append(indicator);
                    if (i < 6) sequence.append("\n");
                    
                    weekCal.add(Calendar.DAY_OF_MONTH, 1);
                }
                
                Log.d(TAG, "Week starts: " + weekStart);
                Log.d(TAG, "7-day sequence:\n" + sequence.toString());
                
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date", e);
            }
            
            // Restore original preference
            editor.putString("start_day", originalPref);
            editor.apply();
            
            Log.d(TAG, "");
        }
        
        Log.d(TAG, "=== END TESTING ===");
    }

    /**
     * Set up dynamic daily spending layout based on user's preferred start day
     */
    private void setupDynamicDailySpendingLayout() {
        LinearLayout container = findViewById(R.id.dailySpendingContainer);
        container.removeAllViews(); // Clear any existing views
        
        // Get user's preferred start day
        String userStartDay = PreferenceActivity.getStartDay(this);
        Log.d(TAG, "=== SETTING UP DYNAMIC LAYOUT ===");
        Log.d(TAG, "User's preferred start day: " + userStartDay);
        
        // Get the ordered list of day names starting from user's preference
        String[] orderedDayNames = getOrderedDayNames(userStartDay);
        Log.d(TAG, "Creating cards in order: " + java.util.Arrays.toString(orderedDayNames));
        
        // Initialize arrays for the dynamic TextViews
        dayLabelTextViews = new TextView[7];
        dayTotalTextViews = new TextView[7];
        
        // Create 7 day cards programmatically in the correct order
        for (int i = 0; i < 7; i++) {
            // Create the main card
            com.google.android.material.card.MaterialCardView dayCard = new com.google.android.material.card.MaterialCardView(this);
            
            // Set card properties
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * getResources().getDisplayMetrics().density) // 48dp in pixels
            );
            if (i < 6) { // Add margin bottom to all except last card
                cardParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp
            }
            dayCard.setLayoutParams(cardParams);
            
            // Set card styling
            dayCard.setCardBackgroundColor(getResources().getColor(R.color.dirty_white));
            dayCard.setStrokeColor(getResources().getColor(R.color.black));
            dayCard.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density)); // 2dp
            dayCard.setRadius(15 * getResources().getDisplayMetrics().density); // 15dp
            dayCard.setCardElevation(2 * getResources().getDisplayMetrics().density); // 2dp
            
            // Create inner LinearLayout
            LinearLayout innerLayout = new LinearLayout(this);
            innerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ));
            innerLayout.setOrientation(LinearLayout.HORIZONTAL);
            innerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int paddingHorizontal = (int) (12 * getResources().getDisplayMetrics().density); // 12dp
            innerLayout.setPadding(paddingHorizontal, 0, paddingHorizontal, 0);
            
            // Create day label TextView
            TextView dayLabel = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            dayLabel.setLayoutParams(labelParams);
            dayLabel.setTypeface(getResources().getFont(R.font.russo_one));
            dayLabel.setText(orderedDayNames[i]); // Use the correct day name from ordered list
            dayLabel.setTextColor(getResources().getColor(android.R.color.black));
            dayLabel.setTextSize(16);
            dayLabel.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            dayLabelTextViews[i] = dayLabel;
            
            // Create day total TextView
            TextView dayTotal = new TextView(this);
            LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            dayTotal.setLayoutParams(totalParams);
            dayTotal.setTypeface(getResources().getFont(R.font.russo_one));
            dayTotal.setText("Total: ₱0"); // Temporary text
            dayTotal.setTextColor(getResources().getColor(android.R.color.black));
            dayTotal.setTextSize(16);
            dayTotal.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            dayTotal.setMinWidth((int) (120 * getResources().getDisplayMetrics().density)); // 120dp
            dayTotal.setTextAlignment(TextView.TEXT_ALIGNMENT_TEXT_END);
            dayTotalTextViews[i] = dayTotal;
            
            // Add TextViews to inner layout
            innerLayout.addView(dayLabel);
            innerLayout.addView(dayTotal);
            
            // Add inner layout to card
            dayCard.addView(innerLayout);
            
            // Add card to container
            container.addView(dayCard);
        }
        
        Log.d(TAG, "Dynamic daily spending layout created with 7 cards in order: " + java.util.Arrays.toString(orderedDayNames));
        Log.d(TAG, "=== LAYOUT SETUP COMPLETE ===");
    }

    /**
     * Get ordered day names starting from the user's preferred start day
     */
    private String[] getOrderedDayNames(String startDay) {
        String[] allDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] orderedDays = new String[7];
        
        // Find the starting index
        int startIndex = 0;
        for (int i = 0; i < allDays.length; i++) {
            if (allDays[i].equals(startDay)) {
                startIndex = i;
                break;
            }
        }
        
        // Create ordered array starting from the preferred start day
        for (int i = 0; i < 7; i++) {
            orderedDays[i] = allDays[(startIndex + i) % 7];
        }
        
        Log.d(TAG, "Ordered days starting from " + startDay + ": " + java.util.Arrays.toString(orderedDays));
        return orderedDays;
    }
    
    /**
     * Fetch expenses for the current user and current week only (fixes: no data showing)
     */
    private void fetchCurrentUserWeeklyExpenses() {
        Log.d(TAG, "Fetching current user's expenses for the current week only...");

        String weekStartDate = getCurrentWeekStartDate();
        String weekEndDate = getCurrentWeekEndDate();
        Log.d(TAG, "Week range: " + weekStartDate + " to " + weekEndDate);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "No current user ID set. Cannot fetch expenses.");
            showEmptyState();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("expenses")
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThanOrEqualTo("date", weekStartDate)
                .whereLessThanOrEqualTo("date", weekEndDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " expenses for user in current week.");
                    List<Expense> weekExpenses = new ArrayList<>();
                    double totalAmount = 0.0;
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId());
                            weekExpenses.add(expense);
                            totalAmount += expense.getAmount();
                        }
                    }
                    if (!weekExpenses.isEmpty()) {
                        processRealExpenseDataWithWeeklyPeriod(weekExpenses);
                    } else {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch weekly expenses for user", e);
                    showEmptyState();
                });
    }
}
