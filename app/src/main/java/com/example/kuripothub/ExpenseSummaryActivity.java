package com.example.kuripothub;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.models.Expense;
import com.example.kuripothub.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ExpenseSummaryActivity extends AppCompatActivity {

    private static final String TAG = "ExpenseSummary";
    
    private FirebaseManager firebaseManager;
    private String currentUserId;
    private double currentBudget = 0.0;
    
    // UI Components
    private PieChart categoryPieChart;
    private LineChart spendingLineChart;
    private BarChart savingsBarChart;
    private TextView savedAmountText;
    private TextView day1Total, day2Total, day3Total, day4Total, day5Total;

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
        loadUserDataAndAnalytics();
    }

    private void initializeViews() {
        categoryPieChart = findViewById(R.id.categoryPieChart);
        spendingLineChart = findViewById(R.id.spendingLineChart);
        savingsBarChart = findViewById(R.id.savingsBarChart);
        savedAmountText = findViewById(R.id.savedAmountText);
        
        // Daily spending TextViews
        day1Total = findViewById(R.id.day1Total);
        day2Total = findViewById(R.id.day2Total);
        day3Total = findViewById(R.id.day3Total);
        day4Total = findViewById(R.id.day4Total);
        day5Total = findViewById(R.id.day5Total);
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
        // Get current month's expenses
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String currentMonth = monthFormat.format(calendar.getTime());

        firebaseManager.getUserExpenses(currentUserId)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Expense> allExpenses = new ArrayList<>();
                    
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null && expense.getDate().startsWith(currentMonth)) {
                            expense.setId(document.getId());
                            allExpenses.add(expense);
                        }
                    }

                    // Update all charts with the loaded data
                    updateCategoryPieChart(allExpenses);
                    updateSpendingLineChart(allExpenses);
                    updateSavingsBarChart(allExpenses);
                    updateDailySpending(allExpenses);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading analytics data", e);
                });
    }

    private void updateCategoryPieChart(List<Expense> expenses) {
        Map<String, Float> categoryTotals = new HashMap<>();
        
        // Calculate category totals
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            float amount = (float) expense.getAmount();
            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0f) + amount);
        }

        // If no expenses, show empty state
        if (categoryTotals.isEmpty()) {
            categoryPieChart.clear();
            categoryPieChart.setNoDataText("No expenses to display");
            categoryPieChart.invalidate();
            return;
        }

        // Create pie chart entries
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        // Create dataset
        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Set colors for different categories
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(255, 99, 132));   // Red
        colors.add(Color.rgb(54, 162, 235));   // Blue
        colors.add(Color.rgb(255, 205, 86));   // Yellow
        colors.add(Color.rgb(75, 192, 192));   // Teal
        colors.add(Color.rgb(153, 102, 255));  // Purple
        colors.add(Color.rgb(255, 159, 64));   // Orange
        
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        // Create chart data
        PieData data = new PieData(dataSet);
        categoryPieChart.setData(data);
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.setDrawHoleEnabled(true);
        categoryPieChart.setHoleColor(Color.TRANSPARENT);
        categoryPieChart.setHoleRadius(30f);
        categoryPieChart.setTransparentCircleRadius(35f);
        categoryPieChart.getLegend().setEnabled(false);
        categoryPieChart.invalidate();
    }

    private void updateSpendingLineChart(List<Expense> expenses) {
        Map<String, Float> dailyTotals = new HashMap<>();
        
        // Calculate daily totals for the month
        for (Expense expense : expenses) {
            String date = expense.getDate();
            float amount = (float) expense.getAmount();
            dailyTotals.put(date, dailyTotals.getOrDefault(date, 0f) + amount);
        }

        // If no expenses, show empty state
        if (dailyTotals.isEmpty()) {
            spendingLineChart.clear();
            spendingLineChart.setNoDataText("No spending data");
            spendingLineChart.invalidate();
            return;
        }

        // Sort dates and create entries
        List<String> sortedDates = new ArrayList<>(dailyTotals.keySet());
        Collections.sort(sortedDates);
        
        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();
        
        for (int i = 0; i < sortedDates.size(); i++) {
            String fullDate = sortedDates.get(i);
            entries.add(new Entry(i, dailyTotals.get(fullDate)));
            
            // Format date for display (show only day)
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd", Locale.getDefault());
                Date date = inputFormat.parse(fullDate);
                dateLabels.add(outputFormat.format(date));
            } catch (Exception e) {
                dateLabels.add(String.valueOf(i + 1));
            }
        }

        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(Color.rgb(75, 192, 192));
        dataSet.setCircleColor(Color.rgb(75, 192, 192));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);
        dataSet.setValueTextSize(8f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Create chart data
        LineData data = new LineData(dataSet);
        spendingLineChart.setData(data);
        spendingLineChart.getDescription().setEnabled(false);
        spendingLineChart.getLegend().setEnabled(false);
        
        // Configure X-axis
        XAxis xAxis = spendingLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(8f);
        
        // Configure Y-axis
        YAxis leftAxis = spendingLineChart.getAxisLeft();
        leftAxis.setTextSize(8f);
        leftAxis.setAxisMinimum(0f);
        
        YAxis rightAxis = spendingLineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        spendingLineChart.invalidate();
    }

    private void updateSavingsBarChart(List<Expense> expenses) {
        // Calculate total expenses
        float totalExpenses = 0f;
        for (Expense expense : expenses) {
            totalExpenses += expense.getAmount();
        }

        float totalSavings = (float) (currentBudget - totalExpenses);
        if (totalSavings < 0) totalSavings = 0; // No negative savings

        // Create bar chart entries - stacked bar showing expenses and savings
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, new float[]{totalExpenses, totalSavings}));

        // Create dataset
        BarDataSet dataSet = new BarDataSet(entries, "");
        
        // Set colors - red for expenses, green for savings
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(255, 99, 132));  // Red for expenses
        colors.add(Color.rgb(75, 192, 192));  // Green for savings
        
        dataSet.setColors(colors);
        dataSet.setStackLabels(new String[]{"Expenses", "Savings"});
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.WHITE);

        // Create chart data
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        
        savingsBarChart.setData(data);
        savingsBarChart.getDescription().setEnabled(false);
        savingsBarChart.getLegend().setEnabled(false);
        
        // Configure X-axis
        XAxis xAxis = savingsBarChart.getXAxis();
        xAxis.setEnabled(false);
        
        // Configure Y-axis
        YAxis leftAxis = savingsBarChart.getAxisLeft();
        leftAxis.setTextSize(8f);
        leftAxis.setAxisMinimum(0f);
        
        YAxis rightAxis = savingsBarChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        savingsBarChart.invalidate();

        // Update saved amount text
        savedAmountText.setText("P " + String.format("%.0f", totalSavings));
    }

    private void updateDailySpending(List<Expense> expenses) {
        // Get last 5 days
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        
        TextView[] dayTotals = {day1Total, day2Total, day3Total, day4Total, day5Total};
        
        // Find corresponding day labels
        TextView day1Label = findViewById(R.id.day1Label);
        TextView day2Label = findViewById(R.id.day2Label);
        TextView day3Label = findViewById(R.id.day3Label);
        TextView day4Label = findViewById(R.id.day4Label);
        TextView day5Label = findViewById(R.id.day5Label);
        TextView[] dayLabels = {day1Label, day2Label, day3Label, day4Label, day5Label};
        
        for (int i = 0; i < 5; i++) {
            String date = dateFormat.format(calendar.getTime());
            String dayName = dayFormat.format(calendar.getTime());
            
            // Calculate total for this date
            double dayTotal = 0.0;
            for (Expense expense : expenses) {
                if (expense.getDate().equals(date)) {
                    dayTotal += expense.getAmount();
                }
            }
            
            // Update day label if it exists
            if (i < dayLabels.length && dayLabels[i] != null) {
                dayLabels[i].setText(dayName);
            }
            
            dayTotals[i].setText("Total: P" + String.format("%.0f", dayTotal));
            
            // Move to previous day
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
    }
}
