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
                        if (user != null && user.getUsername() != null) {
                            Log.d(TAG, "User data loaded: " + user.getUsername());
                            runOnUiThread(() -> {
                                profileUsername.setText(user.getUsername());
                            });
                        } else {
                            Log.w(TAG, "User object is null or missing username");
                            runOnUiThread(() -> {
                                profileUsername.setText("User");
                            });
                        }
                    } else {
                        Log.w(TAG, "User document does not exist");
                        runOnUiThread(() -> {
                            profileUsername.setText("Guest");
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    runOnUiThread(() -> {
                        profileUsername.setText("Guest");
                        Toast.makeText(ProfileActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
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
        
        // Use user's preferred week start
        int weekStartDay = getUserPreferredWeekStartDay();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
        
        // Calculate cumulative budget and spent
        int weekCount = expensesByWeek.size();
        for (List<Expense> weekExpenses : expensesByWeek.values()) {
            cumulativeBudget += 2000.0; // Only increment for actual budget resets
            for (Expense expense : weekExpenses) {
                cumulativeSpent += expense.getAmount();
            }
        }
        if (weekCount == 0) cumulativeBudget = 2000.0; // No expenses: just current week budget
        cumulativeSaved = cumulativeBudget - cumulativeSpent;
        Log.d(TAG, "=== FINAL CALCULATION ===");
        Log.d(TAG, "Total weeks: " + weekCount);
        Log.d(TAG, "Cumulative budget: ₱" + cumulativeBudget);
        Log.d(TAG, "Cumulative spent: ₱" + cumulativeSpent);
        Log.d(TAG, "Cumulative saved: ₱" + cumulativeSaved);
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
            
            // Convert to entries for the chart
            List<Entry> entries = new ArrayList<>();
            List<String> weekLabels = new ArrayList<>();
            
            // Sort weeks chronologically
            List<String> sortedWeeks = new ArrayList<>(weeklySavingsMap.keySet());
            sortedWeeks.sort((a, b) -> a.compareTo(b)); // String comparison works for "yyyy-Www" format
            
            int index = 0;
            for (String week : sortedWeeks) {
                WeeklySavings savings = weeklySavingsMap.get(week);
                entries.add(new Entry(index, (float) savings.saved));
                weekLabels.add("W" + (index + 1)); // Display as W1, W2, etc.
                
                Log.d(TAG, "Week " + week + " (W" + (index + 1) + "): Budget=₱" + savings.budget + 
                          ", Spent=₱" + savings.spent + ", Saved=₱" + savings.saved);
                index++;
            }
            
            if (entries.isEmpty()) {
                Log.w(TAG, "No entries to display in chart");
                if (chartPlaceholder != null) {
                    lineChart.setVisibility(View.GONE);
                    chartPlaceholder.setVisibility(View.VISIBLE);
                }
                return;
            }
            
            // Create dataset
            LineDataSet dataSet = new LineDataSet(entries, "Weekly Savings");
            dataSet.setColor(Color.parseColor("#2E7D32")); // Green color
            dataSet.setCircleColor(Color.parseColor("#1B5E20")); // Darker green for points
            dataSet.setLineWidth(3f);
            dataSet.setCircleRadius(6f);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawFilled(false);
            
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
            xAxis.setTextColor(Color.BLACK);
            xAxis.setTextSize(10f);
            
            // Configure Y-axis
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setTextColor(Color.BLACK);
            leftAxis.setTextSize(10f);
            leftAxis.setAxisMinimum(0f);
            
            YAxis rightAxis = lineChart.getAxisRight();
            rightAxis.setEnabled(false);
            
            // Disable interactions for a cleaner look
            lineChart.setTouchEnabled(false);
            lineChart.setDragEnabled(false);
            lineChart.setScaleEnabled(false);
            lineChart.setPinchZoom(false);
            
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
                        weekSavings.budget = 2000.0;
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
        cumulativeBudget = 2000.0; // Default weekly budget
        cumulativeSpent = 0.0;
        cumulativeSaved = cumulativeBudget - cumulativeSpent;
        
        Log.d(TAG, "Default budget set: ₱" + cumulativeBudget + " budget, ₱" + cumulativeSpent + " spent, ₱" + cumulativeSaved + " saved");
        
        // Show placeholder chart when no data
        if (lineChart != null && chartPlaceholder != null) {
            lineChart.setVisibility(View.GONE);
            chartPlaceholder.setVisibility(View.VISIBLE);
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
