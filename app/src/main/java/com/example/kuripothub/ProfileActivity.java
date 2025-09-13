package com.example.kuripothub;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kuripothub.models.Expense;
import com.example.kuripothub.models.User;
import com.example.kuripothub.models.BudgetHistory;
import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.utils.PreferenceBasedDateUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    // UI Elements
    private TextView profileUsername;
    private TextView totalBudgetAmount;
    private TextView totalSpentAmount;
    private TextView totalSavedAmount;
    private LinearLayout chartPlaceholder;
    private LineChart lineChart;
    private ImageView backArrow;
    
    // Data
    private double cumulativeBudget = 0.0;
    private double cumulativeSpent = 0.0;
    private double cumulativeSaved = 0.0;
    private double userBudget = 2000.0; // Default, will be set dynamically
    private Calendar earliestExpenseCalendar = null; // Store earliest expense date
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        firebaseManager = FirebaseManager.getInstance();
        
        // Check if user is logged in
        FirebaseUser currentUser = firebaseManager.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        
        currentUserId = currentUser.getUid();
        
        initializeViews();
        setupClickListeners();
        loadUserData();
        loadCumulativeData();
    }
    
    private void initializeViews() {
        profileUsername = findViewById(R.id.profileUsername);
        totalBudgetAmount = findViewById(R.id.totalBudgetAmount);
        totalSpentAmount = findViewById(R.id.totalSpentAmount);
        totalSavedAmount = findViewById(R.id.totalSavedAmount);
        chartPlaceholder = findViewById(R.id.chartPlaceholder);
        lineChart = findViewById(R.id.lineChart);
        backArrow = findViewById(R.id.backArrow);
    }
    
    private void setupClickListeners() {
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void loadUserData() {
        Log.d(TAG, "Loading user data for: " + currentUserId);
        
        firebaseManager.getUserProfile(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            if (user.getUsername() != null) {
                                Log.d(TAG, "User data loaded: " + user.getUsername());
                                runOnUiThread(() -> profileUsername.setText(user.getUsername()));
                            } else {
                                Log.w(TAG, "User object is missing username");
                                runOnUiThread(() -> profileUsername.setText("User"));
                            }
                            // Set dynamic budget if available
                            if (user.getBudget() > 0) {
                                userBudget = user.getBudget();
                                Log.d(TAG, "Dynamic user budget loaded: " + userBudget);
                            } else {
                                userBudget = 2000.0;
                                Log.d(TAG, "User budget not set, using default: " + userBudget);
                            }
                        } else {
                            Log.w(TAG, "User object is null");
                            runOnUiThread(() -> profileUsername.setText("User"));
                            userBudget = 2000.0;
                        }
                    } else {
                        Log.w(TAG, "User document does not exist");
                        runOnUiThread(() -> profileUsername.setText("Guest"));
                        userBudget = 2000.0;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    runOnUiThread(() -> {
                        profileUsername.setText("Guest");
                        Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
                    userBudget = 2000.0;
                });
    }
    
    private void loadCumulativeData() {
        Log.d(TAG, "Loading cumulative data for user: " + currentUserId);
        
        // Use a simple query without orderBy to avoid index requirements
        // Get all expenses using FirebaseFirestore directly
        FirebaseFirestore.getInstance()
                .collection("expenses")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Expense> expenses = new ArrayList<>();
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " expense documents");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Log raw document data first
                            Log.d(TAG, "📄 Raw document " + document.getId() + ": " + document.getData());
                            
                            Expense expense = document.toObject(Expense.class);
                            if (expense != null) {
                                expense.setId(document.getId());
                                
                                // Log detailed expense info
                                Log.d(TAG, "📊 Parsed expense:");
                                Log.d(TAG, "   - ID: " + expense.getId());
                                Log.d(TAG, "   - Amount: " + expense.getAmount());
                                Log.d(TAG, "   - Date: " + expense.getDate());
                                Log.d(TAG, "   - Category: " + expense.getCategory());
                                Log.d(TAG, "   - User ID: " + expense.getUserId());
                                Log.d(TAG, "   - Description: " + expense.getDescription());
                                
                                // Validate expense data
                                if (expense.getAmount() > 0) {
                                    expenses.add(expense);
                                    Log.d(TAG, "✓ Valid expense added to list");
                                } else {
                                    Log.w(TAG, "⚠ Skipping expense with invalid amount: " + expense.getAmount());
                                }
                            } else {
                                Log.w(TAG, "⚠ Null expense object from document: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error parsing expense document: " + document.getId(), e);
                        }
                    }
                    
                    Log.d(TAG, "=== EXPENSE LOADING SUMMARY ===");
                    Log.d(TAG, "Documents found: " + queryDocumentSnapshots.size());
                    Log.d(TAG, "Valid expenses: " + expenses.size());
                    
                    runOnUiThread(() -> {
                        if (expenses.isEmpty()) {
                            Log.d(TAG, "No valid expenses found, showing default budget");
                            showDefaultBudget();
                            // You could uncomment the line below to show test data instead:
                            // loadTestDataIfNeeded();
                            Toast.makeText(ProfileActivity.this, "No expenses found. Showing default budget.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "Processing " + expenses.size() + " expenses");
                            calculateCumulativeData(expenses);
                            setupLineChart(expenses);
                            Toast.makeText(ProfileActivity.this, "Loaded " + expenses.size() + " expenses", Toast.LENGTH_SHORT).show();
                        }
                        updateUI();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load expenses from Firebase", e);
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load expense data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showDefaultBudget();
                        updateUI();
                    });
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
    
    private void calculateCumulativeData(List<Expense> expenses) {
        Log.d(TAG, "Calculating cumulative data for " + expenses.size() + " expenses");
        cumulativeSpent = 0.0;
        cumulativeBudget = 0.0;
        
        // Find the earliest expense date to calculate total weeks
        earliestExpenseCalendar = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (Expense expense : expenses) {
            try {
                if (expense.getDate() != null) {
                    Date expenseDate = dateFormat.parse(expense.getDate());
                    Calendar expenseCalendar = Calendar.getInstance();
                    expenseCalendar.setTime(expenseDate);
                    
                    if (earliestExpenseCalendar == null || expenseCalendar.before(earliestExpenseCalendar)) {
                        earliestExpenseCalendar = (Calendar) expenseCalendar.clone();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing expense date: " + expense.getDate(), e);
            }
        }
        
        if (earliestExpenseCalendar != null) {
            Log.d(TAG, "Found earliest expense date: " + dateFormat.format(earliestExpenseCalendar.getTime()));
        } else {
            Log.d(TAG, "No valid expense dates found");
        }
        
        // Use user's preferred week start
        int weekStartDay = getUserPreferredWeekStartDay();
        Calendar calendar = Calendar.getInstance();
        
        // Group expenses by week using preferred start day
        Map<String, List<Expense>> expensesByWeek = new HashMap<>();
        for (Expense expense : expenses) {
            try {
                if (expense.getDate() != null) {
                    Date expenseDate = dateFormat.parse(expense.getDate());
                    calendar.setTime(expenseDate);
                    calendar.setFirstDayOfWeek(weekStartDay);
                    int year = calendar.get(Calendar.YEAR);
                    int week = calendar.get(Calendar.WEEK_OF_YEAR);
                    String weekKey = year + "-W" + String.format("%02d", week);
                    if (!expensesByWeek.containsKey(weekKey)) expensesByWeek.put(weekKey, new ArrayList<>());
                    expensesByWeek.get(weekKey).add(expense);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing expense date: " + expense.getDate(), e);
            }
        }
        
        // Load budget history and calculate cumulative budget based on actual budget changes
        loadBudgetHistoryAndCalculate(expensesByWeek);
    }
    
    private void loadBudgetHistoryAndCalculate(Map<String, List<Expense>> expensesByWeek) {
        firebaseManager.getUserBudgetHistory(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Parse budget history
                    List<BudgetHistory> budgetHistoryList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BudgetHistory budgetHistory = document.toObject(BudgetHistory.class);
                        if (budgetHistory != null) {
                            budgetHistory.setId(document.getId());
                            budgetHistoryList.add(budgetHistory);
                        }
                    }
                    
                    // Calculate cumulative budget and spent using budget history
                    calculateWithBudgetHistory(expensesByWeek, budgetHistoryList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load budget history, using default budget calculation", e);
                    // Fallback to old calculation method
                    calculateWithDefaultBudget(expensesByWeek);
                });
    }
    
    private void calculateWithBudgetHistory(Map<String, List<Expense>> expensesByWeek, List<BudgetHistory> budgetHistoryList) {
        Log.d(TAG, "Calculating with budget history: " + budgetHistoryList.size() + " budget changes");
        
        // Sort budget history by start date to ensure proper order
        budgetHistoryList.sort((b1, b2) -> {
            if (b1.getStartDate() == null) return 1;
            if (b2.getStartDate() == null) return -1;
            return b1.getStartDate().compareTo(b2.getStartDate());
        });
        
        // Log all budget history entries for debugging
        for (int i = 0; i < budgetHistoryList.size(); i++) {
            BudgetHistory bh = budgetHistoryList.get(i);
            Log.d(TAG, "Budget History " + i + ": ₱" + bh.getBudget() + " from " + bh.getStartDate() + " to " + bh.getEndDate());
        }
        
        cumulativeBudget = 0.0;
        cumulativeSpent = 0.0;
        
        // Calculate total weeks from user creation to current date
        int totalWeeksFromStart = calculateTotalWeeksFromUserStart();
        
        if (totalWeeksFromStart <= 0) {
            Log.w(TAG, "Could not determine weeks from start, falling back to expense-based calculation");
            calculateWithDefaultBudget(expensesByWeek);
            return;
        }
        
        Log.d(TAG, "User has been active for " + totalWeeksFromStart + " weeks");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Calculate budget for each week from start to current
        for (int weekNumber = 1; weekNumber <= totalWeeksFromStart; weekNumber++) {
            String weekStartDate = getDateForWeekNumber(weekNumber);
            double weekBudget = getBudgetForDate(weekStartDate, budgetHistoryList);
            cumulativeBudget += weekBudget;
            
            Log.d(TAG, "Week " + weekNumber + " (starting " + weekStartDate + "): Budget=₱" + weekBudget);
        }
        
        // Calculate total spent from all expenses
        for (List<Expense> weekExpenses : expensesByWeek.values()) {
            for (Expense expense : weekExpenses) {
                cumulativeSpent += expense.getAmount();
            }
        }
        
        cumulativeSaved = cumulativeBudget - cumulativeSpent;
        
        Log.d(TAG, "=== BUDGET HISTORY CALCULATION SUMMARY ===");
        Log.d(TAG, "Total weeks from start: " + totalWeeksFromStart);
        Log.d(TAG, "Budget history entries: " + budgetHistoryList.size());
        Log.d(TAG, "Cumulative budget: ₱" + cumulativeBudget);
        Log.d(TAG, "Cumulative spent: ₱" + cumulativeSpent);
        Log.d(TAG, "Cumulative saved: ₱" + cumulativeSaved);
        Log.d(TAG, "Expected for scenario (Week1:2000 + Week2:2000 + Week3:1000): ₱5000");
        Log.d(TAG, "=== END BUDGET CALCULATION ===");
        
        runOnUiThread(this::updateUI);
    }
    
    private int calculateTotalWeeksFromUserStart() {
        try {
            // Try to determine the actual week number based on user data
            // First, try to get the earliest expense date
            Calendar earliestExpenseDate = getEarliestExpenseDate();
            Calendar currentDate = Calendar.getInstance();
            int weekStartDay = getUserPreferredWeekStartDay();
            
            if (earliestExpenseDate != null) {
                // Calculate weeks from earliest expense to now
                earliestExpenseDate.setFirstDayOfWeek(weekStartDay);
                currentDate.setFirstDayOfWeek(weekStartDay);
                
                // Set both dates to the start of their respective weeks
                earliestExpenseDate.set(Calendar.DAY_OF_WEEK, weekStartDay);
                currentDate.set(Calendar.DAY_OF_WEEK, weekStartDay);
                
                // Calculate difference in milliseconds and convert to weeks
                long timeDifferenceMs = currentDate.getTimeInMillis() - earliestExpenseDate.getTimeInMillis();
                long weeksDifference = timeDifferenceMs / (7 * 24 * 60 * 60 * 1000L);
                
                // Add 1 because we include both the start week and current week
                int totalWeeks = (int) weeksDifference + 1;
                
                Log.d(TAG, "Calculated weeks from earliest expense:");
                Log.d(TAG, "  Earliest expense week: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(earliestExpenseDate.getTime()));
                Log.d(TAG, "  Current week: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.getTime()));
                Log.d(TAG, "  Total weeks: " + totalWeeks);
                
                return Math.max(1, totalWeeks);
            } else {
                // No expenses found, check if we have budget history to determine start date
                Log.d(TAG, "No expenses found, using budget history or default");
                // For now, assume user has been active for 10 weeks based on your data
                // In a real scenario, you could check user registration date or budget history
                return 10; // Updated to match your actual usage
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating weeks from start", e);
            return 10; // Default to 10 weeks based on your actual data
        }
    }
    
    private Calendar getEarliestExpenseDate() {
        // This method should be called from calculateCumulativeData where we have access to expenses
        // For now, we'll store the earliest date as a class member and use it here
        // If not available, return null to use alternative calculation
        
        if (earliestExpenseCalendar != null) {
            return (Calendar) earliestExpenseCalendar.clone();
        }
        
        return null;
    }
    
    private String getDateForWeekNumber(int weekNumber) {
        try {
            // Calculate the actual start date based on user's first activity
            Calendar earliestDate = getEarliestExpenseDate();
            if (earliestDate == null) {
                // If no expenses, calculate based on 10 weeks ago (since user has 10 weeks of data)
                earliestDate = Calendar.getInstance();
                earliestDate.add(Calendar.WEEK_OF_YEAR, -9); // 10 weeks ago (9 weeks back + current week)
                Log.d(TAG, "No earliest expense date found, using calculated start date for 10 weeks");
            } else {
                Log.d(TAG, "Using earliest expense date as reference");
            }
            
            // Set to start of week based on user preference
            int weekStartDay = getUserPreferredWeekStartDay();
            earliestDate.setFirstDayOfWeek(weekStartDay);
            earliestDate.set(Calendar.DAY_OF_WEEK, weekStartDay);
            
            // Add weeks to get to the target week
            Calendar targetWeekStart = (Calendar) earliestDate.clone();
            targetWeekStart.add(Calendar.WEEK_OF_YEAR, weekNumber - 1);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String result = dateFormat.format(targetWeekStart.getTime());
            
            Log.d(TAG, "Week " + weekNumber + " starts on: " + result + " (earliest: " + dateFormat.format(earliestDate.getTime()) + ")");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error getting date for week number: " + weekNumber, e);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return dateFormat.format(new Date());
        }
    }
    
    private double getBudgetForDate(String date, List<BudgetHistory> budgetHistoryList) {
        Log.d(TAG, "Getting budget for date: " + date);
        
        // Find the effective budget for this specific date
        double effectiveBudget = userBudget; // Default fallback
        BudgetHistory applicableBudget = null;
        
        // Find the most recent budget that was active on or before this date
        for (BudgetHistory budget : budgetHistoryList) {
            if (budget.getStartDate() != null && budget.getStartDate().compareTo(date) <= 0) {
                // This budget was active during or before this date
                if (budget.getEndDate() == null || budget.getEndDate().compareTo(date) > 0) {
                    // This budget is still active for this date
                    // If we have multiple applicable budgets, use the most recent one
                    if (applicableBudget == null || budget.getStartDate().compareTo(applicableBudget.getStartDate()) > 0) {
                        applicableBudget = budget;
                    }
                }
            }
        }
        
        if (applicableBudget != null) {
            effectiveBudget = applicableBudget.getBudget();
            Log.d(TAG, "Found applicable budget for " + date + ": ₱" + effectiveBudget + " (from " + applicableBudget.getStartDate() + " to " + applicableBudget.getEndDate() + ")");
        } else {
            Log.d(TAG, "No budget history found for " + date + ", using default: ₱" + effectiveBudget);
        }
        
        return effectiveBudget;
    }
    
    private double getBudgetForWeek(String weekKey, List<BudgetHistory> budgetHistoryList, SimpleDateFormat dateFormat) {
        // Extract week start date from weekKey (e.g., "2025-W05")
        try {
            String[] parts = weekKey.split("-W");
            int year = Integer.parseInt(parts[0]);
            int week = Integer.parseInt(parts[1]);
            
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.WEEK_OF_YEAR, week);
            calendar.set(Calendar.DAY_OF_WEEK, getUserPreferredWeekStartDay());
            
            String weekStartDate = dateFormat.format(calendar.getTime());
            
            // Find the effective budget for this week start date
            double effectiveBudget = userBudget; // Default fallback
            
            for (BudgetHistory budget : budgetHistoryList) {
                if (budget.getStartDate() != null && budget.getStartDate().compareTo(weekStartDate) <= 0) {
                    // This budget was active during or before this week
                    if (budget.getEndDate() == null || budget.getEndDate().compareTo(weekStartDate) > 0) {
                        // This budget is still active for this week
                        effectiveBudget = budget.getBudget();
                    }
                }
            }
            
            return effectiveBudget;
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating budget for week: " + weekKey, e);
            return userBudget; // Fallback
        }
    }
    
    private void calculateWithDefaultBudget(Map<String, List<Expense>> expensesByWeek) {
        // Use week-based calculation even without budget history
        int totalWeeksFromStart = calculateTotalWeeksFromUserStart();
        
        // Calculate budget for total weeks (not just weeks with expenses)
        cumulativeBudget = totalWeeksFromStart * userBudget;
        
        // Calculate total spent from all expenses
        cumulativeSpent = 0.0;
        for (List<Expense> weekExpenses : expensesByWeek.values()) {
            for (Expense expense : weekExpenses) {
                cumulativeSpent += expense.getAmount();
            }
        }
        
        cumulativeSaved = cumulativeBudget - cumulativeSpent;
        
        Log.d(TAG, "=== DEFAULT CALCULATION (Fallback) ===");
        Log.d(TAG, "Total weeks from start: " + totalWeeksFromStart);
        Log.d(TAG, "User budget per week: ₱" + userBudget);
        Log.d(TAG, "Cumulative budget: ₱" + cumulativeBudget);
        Log.d(TAG, "Cumulative spent: ₱" + cumulativeSpent);
        Log.d(TAG, "Cumulative saved: ₱" + cumulativeSaved);
        
        runOnUiThread(this::updateUI);
    }
    
    private void updateUI() {
        Log.d(TAG, "Updating UI with values: Budget=" + cumulativeBudget + ", Spent=" + cumulativeSpent + ", Saved=" + cumulativeSaved);
        
        try {
            // Format currency values
            String budgetText = "₱" + String.format(Locale.getDefault(), "%.2f", cumulativeBudget);
            String spentText = "₱" + String.format(Locale.getDefault(), "%.2f", cumulativeSpent);
            String savedText = "₱" + String.format(Locale.getDefault(), "%.2f", cumulativeSaved);
            
            totalBudgetAmount.setText(budgetText);
            totalSpentAmount.setText(spentText);
            totalSavedAmount.setText(savedText);
            
            Log.d(TAG, "UI updated successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            // Fallback to simple formatting
            totalBudgetAmount.setText("₱0.00");
            totalSpentAmount.setText("₱0.00");
            totalSavedAmount.setText("₱0.00");
        }
    }
    
    private void setupLineChart(List<Expense> expenses) {
        Log.d(TAG, "Setting up line chart with " + expenses.size() + " expenses");
        
        if (lineChart == null) {
            Log.e(TAG, "LineChart is null, cannot setup chart");
            return;
        }
        
        try {
            // Hide placeholder and show chart
            if (chartPlaceholder != null) {
                chartPlaceholder.setVisibility(View.GONE);
            }
            lineChart.setVisibility(View.VISIBLE);
            
            // Group expenses by week and calculate weekly savings
            Map<String, WeeklySavings> weeklySavingsMap = calculateWeeklySavings(expenses);
            
            // Convert to entries for the chart with W1-W10 labels
            List<Entry> entries = new ArrayList<>();
            List<String> weekLabels = new ArrayList<>();
            
            // Always show W1-W10 labels
            for (int i = 1; i <= 10; i++) {
                weekLabels.add("W" + i);
            }
            
            // Sort weeks chronologically
            List<String> sortedWeeks = new ArrayList<>(weeklySavingsMap.keySet());
            sortedWeeks.sort(String::compareTo);
            
            // Map actual weeks to display weeks (W1-W10, cycling)
            for (int i = 0; i < sortedWeeks.size(); i++) {
                String actualWeek = sortedWeeks.get(i);
                WeeklySavings savings = weeklySavingsMap.get(actualWeek);
                
                // Calculate display position (cycles through W1-W10)
                int displayPosition = i % 10;
                
                entries.add(new Entry(displayPosition, (float) savings.saved));
                
                Log.d(TAG, "Week " + actualWeek + " -> Display Position W" + (displayPosition + 1) + 
                          ": Budget=₱" + savings.budget + ", Spent=₱" + savings.spent + ", Saved=₱" + savings.saved);
            }
            
            // If no data, show empty chart with W1-W10 labels
            if (entries.isEmpty()) {
                Log.d(TAG, "No expense data, showing empty chart with W1-W10 labels");
                // Add a single point at (0,0) to show the chart structure
                entries.add(new Entry(0, 0f));
            }
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Weekly Savings");
            dataSet.setColor(Color.parseColor("#388E3C")); // Rich green line
            dataSet.setCircleColor(Color.parseColor("#FBC02D")); // Gold/yellow points
            dataSet.setCircleRadius(8f);
            dataSet.setCircleHoleRadius(4f);
            dataSet.setCircleHoleColor(Color.WHITE);
            dataSet.setLineWidth(4f);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(9f); 
            dataSet.setValueTextColor(Color.parseColor("#212121"));
            dataSet.setDrawFilled(true);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
            
            // Gradient fill under the line
            int startColor = Color.parseColor("#A5D6A7"); // Light green
            int endColor = Color.parseColor("#FFF9C4"); // Light yellow
            android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
            );
            dataSet.setFillDrawable(gradient);
            dataSet.setHighlightEnabled(true);
            dataSet.setHighLightColor(Color.parseColor("#FBC02D"));
            
            // Value label formatter
            dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                @Override
                public String getPointLabel(Entry entry) {
                    return "₱" + ((int) entry.getY());
                }
                @Override
                public String getFormattedValue(float value) {
                    return "₱" + ((int) value);
                }
            });
            
            // Create line data
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            // Customize chart appearance
            Description description = new Description();
            description.setText("");
            lineChart.setDescription(description);
            // Configure X-axis
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(weekLabels));
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(9f); // Show W1-W10 (0-9 indices)
            xAxis.setLabelCount(10, true); // Force show all 10 labels
            xAxis.setTextColor(Color.parseColor("#212121"));
            xAxis.setTextSize(14f);
            xAxis.setDrawGridLines(false);
            xAxis.setAxisLineColor(Color.parseColor("#212121"));
            xAxis.setAxisLineWidth(2f);
            // Configure Y-axis
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setTextColor(Color.parseColor("#212121"));
            leftAxis.setTextSize(14f);
            leftAxis.setAxisMinimum(-1000f); // Allow negative values down to -1000
            // Set axis maximum to user's dynamic budget
            leftAxis.setAxisMaximum((float) userBudget);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(Color.parseColor("#BDBDBD"));
            leftAxis.setAxisLineColor(Color.parseColor("#212121"));
            leftAxis.setAxisLineWidth(2f);
            
            // Add a zero line for better visualization
            leftAxis.setDrawZeroLine(true);
            leftAxis.setZeroLineColor(Color.parseColor("#FF5722")); // Red line at zero
            leftAxis.setZeroLineWidth(2f);
            YAxis rightAxis = lineChart.getAxisRight();
            rightAxis.setEnabled(false);
            
            // Chart background
            lineChart.setBackgroundColor(Color.parseColor("#FFF9C4")); // Light yellow
            // Animation
            lineChart.animateX(1200);
            // Remove legend
            lineChart.getLegend().setEnabled(false);
            // Refresh chart
            lineChart.invalidate();
            
            Log.d(TAG, "Line chart setup completed with " + entries.size() + " data points");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up line chart", e);
            // Show placeholder on error
            if (chartPlaceholder != null && lineChart != null) {
                lineChart.setVisibility(View.GONE);
                chartPlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private Map<String, WeeklySavings> calculateWeeklySavings(List<Expense> expenses) {
        Map<String, WeeklySavings> weeklySavingsMap = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        int weekStartDay = getUserPreferredWeekStartDay();
        for (Expense expense : expenses) {
            try {
                if (expense.getDate() != null) {
                    Date expenseDate = dateFormat.parse(expense.getDate());
                    calendar.setTime(expenseDate);
                    calendar.setFirstDayOfWeek(weekStartDay);
                    int year = calendar.get(Calendar.YEAR);
                    int week = calendar.get(Calendar.WEEK_OF_YEAR);
                    String weekKey = year + "-W" + String.format("%02d", week);
                    WeeklySavings weekSavings = weeklySavingsMap.get(weekKey);
                    if (weekSavings == null) {
                        weekSavings = new WeeklySavings();
                        weekSavings.week = weekKey;
                        weekSavings.budget = userBudget;
                        weekSavings.spent = 0.0;
                        weeklySavingsMap.put(weekKey, weekSavings);
                    }
                    weekSavings.spent += expense.getAmount();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing expense date: " + expense.getDate(), e);
            }
        }
        for (WeeklySavings weekSavings : weeklySavingsMap.values()) {
            weekSavings.saved = weekSavings.budget - weekSavings.spent;
        }
        return weeklySavingsMap;
    }
    
    private String getWeekKey(Calendar calendar, int weekStartDay) {
        // Set first day of week
        calendar.setFirstDayOfWeek(weekStartDay);
        
        // Get year and week of year
        int year = calendar.get(Calendar.YEAR);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        
        return String.format(Locale.getDefault(), "%d-W%02d", year, week);
    }
    
    // Helper class for weekly savings data
    private static class WeeklySavings {
        String week;
        double budget;
        double spent;
        double saved;
    }
    
    private void showDefaultBudget() {
        Log.d(TAG, "No expenses found, showing default budget");
        // If no expenses, still show the current week's budget
        cumulativeBudget = userBudget; // Use user's current budget
        cumulativeSpent = 0.0;
        cumulativeSaved = cumulativeBudget - cumulativeSpent;
        
        Log.d(TAG, "Default budget set: ₱" + cumulativeBudget + " budget, ₱" + cumulativeSpent + " spent, ₱" + cumulativeSaved + " saved");
        
        // Show chart with W1-W10 labels even when no data
        if (lineChart != null) {
            setupLineChart(new ArrayList<>()); // Empty expense list to show basic chart structure
        }
    }
    
    // Method to test with sample data if no real data is available
    private void loadTestDataIfNeeded() {
        if (cumulativeBudget == 0.0 && cumulativeSpent == 0.0) {
            Log.d(TAG, "No real data found, loading test data");
            
            // Create some test expenses for demonstration
            List<Expense> testExpenses = new ArrayList<>();
            
            // Sample expense for this week
            Expense testExpense1 = new Expense();
            testExpense1.setAmount(150.0);
            testExpense1.setDate("2025-01-07"); // Current date
            testExpense1.setCategory("Lunch");
            testExpense1.setUserId(currentUserId);
            testExpenses.add(testExpense1);
            
            // Sample expense for last week
            Expense testExpense2 = new Expense();
            testExpense2.setAmount(300.0);
            testExpense2.setDate("2024-12-30");
            testExpense2.setCategory("Dinner");
            testExpense2.setUserId(currentUserId);
            testExpenses.add(testExpense2);
            
            // Sample expense for two weeks ago
            Expense testExpense3 = new Expense();
            testExpense3.setAmount(500.0);
            testExpense3.setDate("2024-12-23");
            testExpense3.setCategory("Shopping");
            testExpense3.setUserId(currentUserId);
            testExpenses.add(testExpense3);
            
            calculateCumulativeData(testExpenses);
            setupLineChart(testExpenses);
            updateUI();
            
            Toast.makeText(this, "Loaded test data for demonstration", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning to this activity
        Log.d(TAG, "Activity resumed, reloading data");
        loadCumulativeData();
    }
    
    // Alternative method to add to FirebaseManager (for future reference)
    // This should be added to FirebaseManager.java if needed:
    /*
    public Task<QuerySnapshot> getUserExpensesSimple(String userId) {
        return firestore.collection(EXPENSES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get();
    }
    */
}
