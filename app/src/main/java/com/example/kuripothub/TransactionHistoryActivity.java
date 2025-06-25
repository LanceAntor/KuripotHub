package com.example.kuripothub;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.Button;
import android.view.WindowManager;
import android.graphics.Color;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {

    private TextView selectMonthButton;
    private GridLayout calendarGrid;
    private Calendar currentCalendar;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat monthFormat;
    private String[] monthNames = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

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
        
        // Set CardView properties
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = 120;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.setMargins(16,16,16,16); // Increased from 4 to 8 for more spacing
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(Color.parseColor("#FFDD00"));
        cardView.setRadius(50); // Make it more circular/rounded
        cardView.setCardElevation(2);
        
        // Create TextView for day number
        TextView dayText = new TextView(this);
        dayText.setText(String.valueOf(day));
        dayText.setTextSize(16); 
        dayText.setTextColor(Color.BLACK);
        dayText.setGravity(android.view.Gravity.CENTER);
        
        // Set font family
        try {
            dayText.setTypeface(getResources().getFont(R.font.russo_one));
        } catch (Exception e) {
            // Fallback if font not available
        }
        
        // Add click listener for future transaction details
        cardView.setOnClickListener(v -> {
            // TODO: Show transactions for this day
            String selectedDate = monthFormat.format(currentCalendar.getTime()) + " " + day + ", " + currentCalendar.get(Calendar.YEAR);
            android.widget.Toast.makeText(this, "Transactions for " + selectedDate, android.widget.Toast.LENGTH_SHORT).show();
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
}
