package com.example.kuripothub;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class PreferenceActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "KuripotHubPreferences";
    private static final String PREF_START_DAY = "start_day";
    private static final String PREF_BUDGET_RESET = "budget_reset";
    private static final String PREF_SPENDING_LIMIT = "spending_limit";

    // RadioGroups
    private RadioGroup budgetResetRadioGroup;
    private RadioGroup spendingLimitRadioGroup;
    
    // Individual RadioButtons for days (not in a RadioGroup due to layout constraints)
    private RadioButton mondayRadio, tuesdayRadio, wednesdayRadio, thursdayRadio;
    private RadioButton fridayRadio, saturdayRadio, sundayRadio;

    // Cards for click handling
    private CardView mondayCard, tuesdayCard, wednesdayCard, thursdayCard;
    private CardView fridayCard, saturdayCard, sundayCard;
    private CardView everyWeekCard, everyMonthCard, doNotResetCard;
    
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        initializeViews();
        loadSavedPreferences();
        setupClickListeners();
    }

    private void initializeViews() {
        // RadioGroups
        budgetResetRadioGroup = findViewById(R.id.budgetResetRadioGroup);
        spendingLimitRadioGroup = findViewById(R.id.spendingLimitRadioGroup);

        // Individual day RadioButtons
        mondayRadio = findViewById(R.id.mondayRadio);
        tuesdayRadio = findViewById(R.id.tuesdayRadio);
        wednesdayRadio = findViewById(R.id.wednesdayRadio);
        thursdayRadio = findViewById(R.id.thursdayRadio);
        fridayRadio = findViewById(R.id.fridayRadio);
        saturdayRadio = findViewById(R.id.saturdayRadio);
        sundayRadio = findViewById(R.id.sundayRadio);

        // Day Cards
        mondayCard = findViewById(R.id.mondayCard);
        tuesdayCard = findViewById(R.id.tuesdayCard);
        wednesdayCard = findViewById(R.id.wednesdayCard);
        thursdayCard = findViewById(R.id.thursdayCard);
        fridayCard = findViewById(R.id.fridayCard);
        saturdayCard = findViewById(R.id.saturdayCard);
        sundayCard = findViewById(R.id.sundayCard);

        // Budget Reset Cards
        everyWeekCard = findViewById(R.id.everyWeekCard);
        everyMonthCard = findViewById(R.id.everyMonthCard);
        doNotResetCard = findViewById(R.id.doNotResetCard);
        
        // Set default selection to Monday
        setDaySelection("Monday");
        
        // Set up radio button listeners for days
        setupDayRadioButtonListeners();
        
        // Set up RadioGroup change listeners to ensure exclusive selection
        setupRadioGroupListeners();
    }

    private void loadSavedPreferences() {
        // Load start day preference (default: Monday)
        String startDay = sharedPreferences.getString(PREF_START_DAY, "Monday");
        setDaySelection(startDay);

        // Load budget reset preference (default: Every week)
        String budgetReset = sharedPreferences.getString(PREF_BUDGET_RESET, "Every week");
        setBudgetResetSelection(budgetReset);

        // Load spending limit preference (default: No Limit)
        String spendingLimit = sharedPreferences.getString(PREF_SPENDING_LIMIT, "No Limit");
        setSpendingLimitSelection(spendingLimit);
    }

    private void setupClickListeners() {
        // Back button
        CardView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Day card click listeners
        mondayCard.setOnClickListener(v -> setDaySelection("Monday"));
        tuesdayCard.setOnClickListener(v -> setDaySelection("Tuesday"));
        wednesdayCard.setOnClickListener(v -> setDaySelection("Wednesday"));
        thursdayCard.setOnClickListener(v -> setDaySelection("Thursday"));
        fridayCard.setOnClickListener(v -> setDaySelection("Friday"));
        saturdayCard.setOnClickListener(v -> setDaySelection("Saturday"));
        sundayCard.setOnClickListener(v -> setDaySelection("Sunday"));

        // Budget reset card click listeners
        everyWeekCard.setOnClickListener(v -> setBudgetResetSelection("Every week"));
        everyMonthCard.setOnClickListener(v -> setBudgetResetSelection("Every month"));
        doNotResetCard.setOnClickListener(v -> setBudgetResetSelection("Do not reset"));

        // Spending limit radio button click listeners (for extra assurance)
        RadioButton noLimitRadio = findViewById(R.id.noLimitRadio);
        RadioButton twentyPercentRadio = findViewById(R.id.twentyPercentRadio);
        RadioButton fortyPercentRadio = findViewById(R.id.fortyPercentRadio);
        RadioButton sixtyPercentRadio = findViewById(R.id.sixtyPercentRadio);
        
        noLimitRadio.setOnClickListener(v -> setSpendingLimitSelection("No Limit"));
        twentyPercentRadio.setOnClickListener(v -> setSpendingLimitSelection("20%"));
        fortyPercentRadio.setOnClickListener(v -> setSpendingLimitSelection("40%"));
        sixtyPercentRadio.setOnClickListener(v -> setSpendingLimitSelection("60%"));

        // Save button
        CardView saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> savePreferences());
    }

    private void setupDayRadioButtonListeners() {
        // Set up click listeners for all day radio buttons
        mondayRadio.setOnClickListener(v -> setDaySelection("Monday"));
        tuesdayRadio.setOnClickListener(v -> setDaySelection("Tuesday"));
        wednesdayRadio.setOnClickListener(v -> setDaySelection("Wednesday"));
        thursdayRadio.setOnClickListener(v -> setDaySelection("Thursday"));
        fridayRadio.setOnClickListener(v -> setDaySelection("Friday"));
        saturdayRadio.setOnClickListener(v -> setDaySelection("Saturday"));
        sundayRadio.setOnClickListener(v -> setDaySelection("Sunday"));
    }

    private void setupRadioGroupListeners() {
        // Budget Reset RadioGroup listener
        budgetResetRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // RadioGroup automatically handles exclusive selection
            if (checkedId == R.id.everyWeekRadio) {
                // Optional: Add any additional logic here
            } else if (checkedId == R.id.everyMonthRadio) {
                // Optional: Add any additional logic here  
            } else if (checkedId == R.id.doNotResetRadio) {
                // Optional: Add any additional logic here
            }
        });

        // Spending Limit RadioGroup listener
        spendingLimitRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // RadioGroup automatically handles exclusive selection
            if (checkedId == R.id.noLimitRadio) {
                // Optional: Add any additional logic here
            } else if (checkedId == R.id.twentyPercentRadio) {
                // Optional: Add any additional logic here
            } else if (checkedId == R.id.fortyPercentRadio) {
                // Optional: Add any additional logic here
            } else if (checkedId == R.id.sixtyPercentRadio) {
                // Optional: Add any additional logic here
            }
        });
    }

    private void setDaySelection(String day) {
        // First, clear all day selections
        mondayRadio.setChecked(false);
        tuesdayRadio.setChecked(false);
        wednesdayRadio.setChecked(false);
        thursdayRadio.setChecked(false);
        fridayRadio.setChecked(false);
        saturdayRadio.setChecked(false);
        sundayRadio.setChecked(false);

        // Then set the selected day
        if (day.equals("Monday")) {
            mondayRadio.setChecked(true);
        } else if (day.equals("Tuesday")) {
            tuesdayRadio.setChecked(true);
        } else if (day.equals("Wednesday")) {
            wednesdayRadio.setChecked(true);
        } else if (day.equals("Thursday")) {
            thursdayRadio.setChecked(true);
        } else if (day.equals("Friday")) {
            fridayRadio.setChecked(true);
        } else if (day.equals("Saturday")) {
            saturdayRadio.setChecked(true);
        } else if (day.equals("Sunday")) {
            sundayRadio.setChecked(true);
        }
    }

    private void setBudgetResetSelection(String resetOption) {
        int radioButtonId = -1;
        if (resetOption.equals("Every week")) {
            radioButtonId = R.id.everyWeekRadio;
        } else if (resetOption.equals("Every month")) {
            radioButtonId = R.id.everyMonthRadio;
        } else if (resetOption.equals("Do not reset")) {
            radioButtonId = R.id.doNotResetRadio;
        }
        if (radioButtonId != -1) {
            budgetResetRadioGroup.check(radioButtonId);
        }
    }

    private void setSpendingLimitSelection(String limitOption) {
        int radioButtonId = -1;
        if (limitOption.equals("No Limit")) {
            radioButtonId = R.id.noLimitRadio;
        } else if (limitOption.equals("20%")) {
            radioButtonId = R.id.twentyPercentRadio;
        } else if (limitOption.equals("40%")) {
            radioButtonId = R.id.fortyPercentRadio;
        } else if (limitOption.equals("60%")) {
            radioButtonId = R.id.sixtyPercentRadio;
        }
        if (radioButtonId != -1) {
            spendingLimitRadioGroup.check(radioButtonId);
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save start day
        String startDay = getSelectedDay();
        editor.putString(PREF_START_DAY, startDay);

        // Save budget reset option
        String budgetReset = getSelectedBudgetReset();
        editor.putString(PREF_BUDGET_RESET, budgetReset);

        // Save spending limit
        String spendingLimit = getSelectedSpendingLimit();
        editor.putString(PREF_SPENDING_LIMIT, spendingLimit);

        editor.apply();

        Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getSelectedDay() {
        if (mondayRadio.isChecked()) {
            return "Monday";
        } else if (tuesdayRadio.isChecked()) {
            return "Tuesday";
        } else if (wednesdayRadio.isChecked()) {
            return "Wednesday";
        } else if (thursdayRadio.isChecked()) {
            return "Thursday";
        } else if (fridayRadio.isChecked()) {
            return "Friday";
        } else if (saturdayRadio.isChecked()) {
            return "Saturday";
        } else if (sundayRadio.isChecked()) {
            return "Sunday";
        } else {
            return "Monday"; // Default
        }
    }

    private String getSelectedBudgetReset() {
        int selectedId = budgetResetRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.everyWeekRadio) {
            return "Every week";
        } else if (selectedId == R.id.everyMonthRadio) {
            return "Every month";
        } else if (selectedId == R.id.doNotResetRadio) {
            return "Do not reset";
        } else {
            return "Every week"; // Default
        }
    }

    private String getSelectedSpendingLimit() {
        int selectedId = spendingLimitRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.noLimitRadio) {
            return "No Limit";
        } else if (selectedId == R.id.twentyPercentRadio) {
            return "20%";
        } else if (selectedId == R.id.fortyPercentRadio) {
            return "40%";
        } else if (selectedId == R.id.sixtyPercentRadio) {
            return "60%";
        } else {
            return "No Limit"; // Default
        }
    }

    // Static methods to get preferences from other activities
    public static String getStartDay(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(PREF_START_DAY, "Monday");
    }

    public static String getBudgetReset(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(PREF_BUDGET_RESET, "Every week");
    }

    public static String getSpendingLimit(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(PREF_SPENDING_LIMIT, "No Limit");
    }
}
