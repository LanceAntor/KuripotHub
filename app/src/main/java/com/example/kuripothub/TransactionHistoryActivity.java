package com.example.kuripothub;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.WindowManager;
import android.graphics.Color;
import android.widget.ImageView;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.kuripothub.utils.FirebaseManager;
import com.example.kuripothub.models.Expense;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

public class TransactionHistoryActivity extends AppCompatActivity {

    private static final String TAG = "TransactionHistory";
    
    private TextView selectMonthButton;
    private GridLayout calendarGrid;
    private Calendar currentCalendar;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat monthFormat;
    private SimpleDateFormat dateFormat;
    private FirebaseManager firebaseManager;
    private String currentUserId;
    
    private String[] monthNames = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Initialize Firebase
        firebaseManager = FirebaseManager.getInstance();
        if (firebaseManager.getCurrentUser() != null) {
            currentUserId = firebaseManager.getCurrentUser().getUid();
        }

        initializeViews();
        setupCalendar();
        setupMonthSelector();
        setupBackButton();
    }

    private void initializeViews() {
        selectMonthButton = findViewById(R.id.selectMonthButton);
        calendarGrid = findViewById(R.id.calendarGrid);
        currentCalendar = Calendar.getInstance();
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Format for Firebase queries
    }

    private void setupCalendar() {
        // Set initial text to "Select Month"
        selectMonthButton.setText("Select Month");
        updateCalendarDisplay();
    }

    private void updateCalendarDisplay() {
        // Clear existing calendar
        calendarGrid.removeAllViews();
        
        // Get calendar information
        Calendar tempCalendar = (Calendar) currentCalendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add day headers
        String[] dayHeaders = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        for (String day : dayHeaders) {
            TextView dayHeader = new TextView(this);
            dayHeader.setText(day);
            dayHeader.setTextSize(14);
            dayHeader.setTextColor(Color.BLACK);
            dayHeader.setGravity(android.view.Gravity.CENTER);
            dayHeader.setPadding(8, 8, 8, 8);
            
            // Set font family
            try {
                dayHeader.setTypeface(getResources().getFont(R.font.russo_one));
            } catch (Exception e) {
                // Fallback if font not available
            }
            
            GridLayout.LayoutParams headerParams = new GridLayout.LayoutParams();
            headerParams.width = 0;
            headerParams.height = 60;
            headerParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            headerParams.setMargins(4, 4, 4, 4);
            dayHeader.setLayoutParams(headerParams);
            
            calendarGrid.addView(dayHeader);
        }
        
        // Add empty cells for days before the first day of month
        // Adjust for Monday start (subtract 1, but handle Sunday case)
        int emptyCells = (firstDayOfWeek == 0) ? 6 : firstDayOfWeek - 1;
        for (int i = 0; i < emptyCells; i++) {
            View emptyView = new View(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 100;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8); // Increased from 4 to 8 for consistency
            emptyView.setLayoutParams(params);
            calendarGrid.addView(emptyView);
        }
        
        // Add day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            CardView dayCard = createDayCard(day);
            calendarGrid.addView(dayCard);
        }
    }

    private CardView createDayCard(int day) {
        CardView cardView = new CardView(this);
        
        // Check if this day is today
        Calendar today = Calendar.getInstance();
        boolean isToday = (today.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                          today.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                          today.get(Calendar.DAY_OF_MONTH) == day);
        
        // Set CardView properties
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = 120;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.setMargins(16,16,16,16); // Increased from 4 to 8 for more spacing
        cardView.setLayoutParams(cardParams);
        
        // Set background color - special for today
        if (isToday) {
            cardView.setBackground(getResources().getDrawable(R.drawable.calendar_today_background, null));
        } else {
            cardView.setCardBackgroundColor(Color.parseColor("#FFDD00"));
            cardView.setRadius(50); // Make it more circular/rounded
            cardView.setCardElevation(2);
        }
        
        // Create TextView for day number
        TextView dayText = new TextView(this);
        dayText.setText(String.valueOf(day));
        dayText.setTextSize(16);
        dayText.setTextColor(Color.BLACK); // White text for today
        dayText.setGravity(android.view.Gravity.CENTER);
        
        // Set font family
        try {
            dayText.setTypeface(getResources().getFont(R.font.russo_one));
        } catch (Exception e) {
            // Fallback if font not available
        }
        
        // Make today's text bold
        if (isToday) {
            dayText.setTypeface(dayText.getTypeface(), android.graphics.Typeface.BOLD);
        }
        
        // Add click listener to show day details
        cardView.setOnClickListener(v -> {
            Calendar selectedDate = (Calendar) currentCalendar.clone();
            selectedDate.set(Calendar.DAY_OF_MONTH, day);
            showDayDetailsDialog(selectedDate);
        });
        
        cardView.addView(dayText);
        return cardView;
    }

    private void setupMonthSelector() {
        selectMonthButton.setOnClickListener(v -> showMonthPicker());
    }

    private void showMonthPicker() {
        final Dialog monthDialog = new Dialog(this);
        monthDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        monthDialog.setContentView(R.layout.month_picker_dialog);
        
        // Make dialog background transparent
        if (monthDialog.getWindow() != null) {
            monthDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Set the dialog width to match parent with margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(monthDialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            monthDialog.getWindow().setAttributes(layoutParams);
        }

        // Set up month buttons
        setupMonthButtons(monthDialog);
        
        monthDialog.show();
    }

    private void setupMonthButtons(Dialog dialog) {
        int[] monthButtonIds = {
            R.id.januaryButton, R.id.februaryButton, R.id.marchButton, R.id.aprilButton,
            R.id.mayButton, R.id.juneButton, R.id.julyButton, R.id.augustButton,
            R.id.septemberButton, R.id.octoberButton, R.id.novemberButton, R.id.decemberButton
        };

        for (int i = 0; i < monthButtonIds.length; i++) {
            final int monthIndex = i;
            CardView monthButton = dialog.findViewById(monthButtonIds[i]);
            if (monthButton != null) {
                monthButton.setOnClickListener(v -> {
                    currentCalendar.set(Calendar.MONTH, monthIndex);
                    // Update the button text to show selected month
                    selectMonthButton.setText(monthFormat.format(currentCalendar.getTime()));
                    updateCalendarDisplay();
                    dialog.dismiss();
                });
            }
        }
    }

    private void setupBackButton() {
        CardView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Show dialog with expenses for the selected day
     */
    private void showDayDetailsDialog(Calendar selectedDate) {
        if (currentUserId == null) {
            android.widget.Toast.makeText(this, "User not logged in", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dayDialog = new Dialog(this);
        dayDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dayDialog.setContentView(R.layout.day_details_dialog);
        
        // Make dialog background transparent
        if (dayDialog.getWindow() != null) {
            dayDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Set the dialog width to match parent with margins
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dayDialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dayDialog.getWindow().setAttributes(layoutParams);
        }

        // Format date for display
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String dateString = displayFormat.format(selectedDate.getTime());
        
        // Set date header
        TextView dateHeader = dayDialog.findViewById(R.id.dateHeader);
        dateHeader.setText(dateString);
        
        // Get references to UI elements
        LinearLayout expensesContainer = dayDialog.findViewById(R.id.expensesContainer);
        TextView totalAmount = dayDialog.findViewById(R.id.totalAmount);
        
        // Format date for Firebase query
        String queryDate = dateFormat.format(selectedDate.getTime());
        
        // Load expenses for this date
        loadExpensesForDate(queryDate, expensesContainer, totalAmount, dayDialog);
    }
    
    /**
     * Load expenses from Firebase for a specific date
     */
    private void loadExpensesForDate(String date, LinearLayout container, TextView totalView, Dialog dialog) {
        Log.d(TAG, "Loading expenses for date: " + date + ", user: " + currentUserId);
        
        firebaseManager.getUserExpensesByDateSimple(currentUserId, date)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "Query returned " + queryDocumentSnapshots.size() + " expenses");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No expenses for this date
                        showNoExpensesMessage(container, totalView, dialog);
                        return;
                    }
                    
                    List<Expense> expenses = new ArrayList<>();
                    double total = 0.0;
                    
                    // Process expenses from Firebase
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Expense expense = document.toObject(Expense.class);
                        if (expense != null) {
                            expense.setId(document.getId());
                            expenses.add(expense);
                            total += expense.getAmount();
                            Log.d(TAG, "Expense: " + expense.getCategory() + " - P" + expense.getAmount());
                        }
                    }
                    
                    // Populate the UI
                    populateExpensesList(container, expenses);
                    totalView.setText(String.format("%.0f", total));
                    
                    // Show the dialog
                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading expenses for date", e);
                    android.widget.Toast.makeText(this, "Error loading expenses", android.widget.Toast.LENGTH_SHORT).show();
                });
    }
    
    /**
     * Show message when no expenses found for the selected date
     */
    private void showNoExpensesMessage(LinearLayout container, TextView totalView, Dialog dialog) {
        container.removeAllViews();
        
        TextView noExpensesText = new TextView(this);
        noExpensesText.setText("No expenses recorded for this day");
        noExpensesText.setTextColor(Color.BLACK);
        noExpensesText.setGravity(android.view.Gravity.CENTER);
        noExpensesText.setPadding(0, 30, 0, 30);
        
        try {
            noExpensesText.setTypeface(getResources().getFont(R.font.russo_one));
        } catch (Exception e) {
            // Fallback if font not available
        }
        
        container.addView(noExpensesText);
        totalView.setText("0");
        
        dialog.show();
    }
    
    /**
     * Populate the expenses list in the dialog
     */
    private void populateExpensesList(LinearLayout container, List<Expense> expenses) {
        container.removeAllViews();
        
        for (Expense expense : expenses) {
            View expenseItem = getLayoutInflater().inflate(R.layout.expense_item_simple, container, false);
            
            TextView categoryText = expenseItem.findViewById(R.id.expenseCategory);
            TextView amountText = expenseItem.findViewById(R.id.expenseAmount);
            
            categoryText.setText(expense.getCategory().toUpperCase());
            amountText.setText("P" + String.format("%.0f", expense.getAmount()));
            
            container.addView(expenseItem);
        }
    }
}
