package com.example.kuripothub.utils;

import android.content.Context;
import android.util.Log;
import com.example.kuripothub.PreferenceActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class to handle date calculations based on user preferences
 */
public class PreferenceBasedDateUtils {
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TAG = "PreferenceDateUtils";
    
    /**
     * Get the start date for the current period based on user's "Day to start" preference
     */
    public static String getCurrentPeriodStartDate(Context context) {
        String startDay = PreferenceActivity.getStartDay(context);
        String budgetReset = PreferenceActivity.getBudgetReset(context);
        
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        Log.d(TAG, "Calculating period start date - Start Day: " + startDay + ", Budget Reset: " + budgetReset);
        
        String startDate;
        if (budgetReset.equals("Every week")) {
            startDate = getWeekStartDate(context, calendar);
        } else if (budgetReset.equals("Every month")) {
            startDate = getMonthStartDate(context, calendar);
        } else {
            // "Do not reset" - return a very old date to include all expenses
            calendar.set(2020, Calendar.JANUARY, 1);
            startDate = dateFormat.format(calendar.getTime());
        }
        
        Log.d(TAG, "Calculated period start date: " + startDate);
        return startDate;
    }
    
    /**
     * Get the end date for the current period
     */
    public static String getCurrentPeriodEndDate(Context context) {
        String budgetReset = PreferenceActivity.getBudgetReset(context);
        
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        if (budgetReset.equals("Every week")) {
            // Calculate week end directly to avoid any calendar state issues
            String startDate = getCurrentPeriodStartDate(context);
            try {
                Date start = dateFormat.parse(startDate);
                Calendar endCalendar = Calendar.getInstance();
                endCalendar.setTime(start);
                endCalendar.add(Calendar.DAY_OF_MONTH, 6); // Add 6 days for end of week
                String endDate = dateFormat.format(endCalendar.getTime());
                Log.d(TAG, "Week end calculation: start=" + startDate + ", end=" + endDate);
                return endDate;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating week end date", e);
                return dateFormat.format(calendar.getTime());
            }
        } else if (budgetReset.equals("Every month")) {
            return getMonthEndDate(calendar);
        } else {
            // "Do not reset" - return today's date
            return dateFormat.format(calendar.getTime());
        }
    }
    
    /**
     * Calculate week start date based on user's preferred start day
     */
    private static String getWeekStartDate(Context context, Calendar calendar) {
        String startDay = PreferenceActivity.getStartDay(context);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        // Convert start day string to Calendar day constant
        int targetDayOfWeek = getDayOfWeekConstant(startDay);
        
        // Get current day of week
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        Log.d(TAG, "Week calculation - Target day: " + startDay + " (" + targetDayOfWeek + "), Current day: " + currentDayOfWeek);
        Log.d(TAG, "Today is: " + dateFormat.format(calendar.getTime()));
        
        // Calculate days to subtract to get to the most recent occurrence of the target day
        int daysToSubtract;
        
        if (currentDayOfWeek == targetDayOfWeek) {
            // Today is the start day, so start from today
            daysToSubtract = 0;
        } else {
            // Calculate the difference, handling the wrap-around for Saturday -> Sunday
            daysToSubtract = (currentDayOfWeek - targetDayOfWeek + 7) % 7;
        }
        
        Log.d(TAG, "Days to subtract: " + daysToSubtract);
        
        // Go back to the start day
        calendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
        
        String result = dateFormat.format(calendar.getTime());
        Log.d(TAG, "Week start date: " + result);
        return result;
    }
    
    /**
     * Calculate week end date (6 days after start date)
     */
    private static String getWeekEndDate(Context context, Calendar calendar) {
        // Clone the calendar to avoid modifying the original
        Calendar clonedCalendar = (Calendar) calendar.clone();
        String startDateStr = getWeekStartDate(context, clonedCalendar);
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            Date startDate = dateFormat.parse(startDateStr);
            
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.setTime(startDate);
            endCalendar.add(Calendar.DAY_OF_MONTH, 6); // Add 6 days to get end of week
            
            String endDateStr = dateFormat.format(endCalendar.getTime());
            Log.d(TAG, "Week end date calculation: start=" + startDateStr + ", end=" + endDateStr);
            return endDateStr;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating week end date", e);
            // Fallback to current date
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            return dateFormat.format(calendar.getTime());
        }
    }
    
    /**
     * Calculate month start date based on user's preferred start day
     */
    private static String getMonthStartDate(Context context, Calendar calendar) {
        String startDay = PreferenceActivity.getStartDay(context);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        // Convert start day string to day of week number (1-7, where 1=Sunday)
        int targetDayOfWeek = getDayOfWeekConstant(startDay);
        
        // Get current date
        Calendar today = Calendar.getInstance();
        
        // Start from the first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        
        // Find the first occurrence of the target day in current month
        while (calendar.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // If this first occurrence is in the future, use the last occurrence from previous month
        if (calendar.after(today)) {
            // Go to previous month and find the last occurrence of target day
            calendar.add(Calendar.MONTH, -1);
            
            // Go to last day of previous month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            
            // Find the last occurrence of target day in previous month
            while (calendar.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
        }
        
        return dateFormat.format(calendar.getTime());
    }
    
    /**
     * Calculate month end date (day before next month's start day)
     */
    private static String getMonthEndDate(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        
        // For monthly reset, end date is the end of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        
        return dateFormat.format(calendar.getTime());
    }
    
    /**
     * Convert day name to Calendar day constant
     */
    private static int getDayOfWeekConstant(String dayName) {
        switch (dayName.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY; // Default to Monday
        }
    }
    
    /**
     * Check if we should enforce spending limit based on user preferences
     */
    public static boolean shouldEnforceSpendingLimit(Context context) {
        String spendingLimit = PreferenceActivity.getSpendingLimit(context);
        return !spendingLimit.equals("No Limit");
    }
    
    /**
     * Get the spending limit percentage as a decimal (e.g., 0.2 for 20%)
     */
    public static double getSpendingLimitPercentage(Context context) {
        String spendingLimit = PreferenceActivity.getSpendingLimit(context);
        
        switch (spendingLimit) {
            case "20%": return 0.20;
            case "40%": return 0.40;
            case "60%": return 0.60;
            default: return 1.0; // No limit means 100%
        }
    }
    
    /**
     * Get a user-friendly description of the current period
     */
    public static String getCurrentPeriodDescription(Context context) {
        String budgetReset = PreferenceActivity.getBudgetReset(context);
        String startDay = PreferenceActivity.getStartDay(context);
        
        if (budgetReset.equals("Every week")) {
            return "This week (starting " + startDay + ")";
        } else if (budgetReset.equals("Every month")) {
            return "This month (starting " + startDay + ")";
        } else {
            return "All time";
        }
    }
    
    /**
     * Check if an expense date falls within the current period
     */
    public static boolean isExpenseInCurrentPeriod(Context context, String expenseDate) {
        if (expenseDate == null || expenseDate.isEmpty()) {
            Log.w(TAG, "Empty expense date provided");
            return false;
        }
        
        String startDate = getCurrentPeriodStartDate(context);
        String endDate = getCurrentPeriodEndDate(context);
        
        Log.d(TAG, "Checking expense date: '" + expenseDate + "' against period: '" + startDate + "' to '" + endDate + "'");
        
        // Normalize the date format in case there are any formatting issues
        String normalizedExpenseDate = normalizeDate(expenseDate);
        String normalizedStartDate = normalizeDate(startDate);
        String normalizedEndDate = normalizeDate(endDate);
        
        Log.d(TAG, "After normalization:");
        Log.d(TAG, "  Expense date: '" + expenseDate + "' -> '" + normalizedExpenseDate + "'");
        Log.d(TAG, "  Start date: '" + startDate + "' -> '" + normalizedStartDate + "'");
        Log.d(TAG, "  End date: '" + endDate + "' -> '" + normalizedEndDate + "'");
        
        Log.d(TAG, "Raw comparison: '" + normalizedExpenseDate + "' >= '" + normalizedStartDate + "' = " + 
                  (normalizedExpenseDate.compareTo(normalizedStartDate) >= 0));
        Log.d(TAG, "Raw comparison: '" + normalizedExpenseDate + "' <= '" + normalizedEndDate + "' = " + 
                  (normalizedExpenseDate.compareTo(normalizedEndDate) <= 0));
        
        boolean inPeriod = normalizedExpenseDate.compareTo(normalizedStartDate) >= 0 && 
                          normalizedExpenseDate.compareTo(normalizedEndDate) <= 0;
        
        Log.d(TAG, "Final result: " + inPeriod);
        
        return inPeriod;
    }
    
    /**
     * Normalize date string to ensure consistent format
     */
    private static String normalizeDate(String dateString) {
        if (dateString == null) return "";
        
        // Remove any extra whitespace
        dateString = dateString.trim();
        
        // If date is already in YYYY-MM-DD format, return as is
        if (dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return dateString;
        }
        
        // Try to parse and reformat other common date formats
        String[] formats = {
            "yyyy/MM/dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "yyyy-M-d",
            "yyyy-MM-d",
            "yyyy-M-dd"
        };
        
        for (String format : formats) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(format, Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                return outputFormat.format(date);
            } catch (Exception e) {
                // Try next format
            }
        }
        
        Log.w(TAG, "Could not normalize date: " + dateString);
        return dateString; // Return original if no format matches
    }
    
    /**
     * Get the last N days from the current period for daily spending display
     */
    public static String[] getLastDaysInPeriod(Context context, int numberOfDays) {
        String startDate = getCurrentPeriodStartDate(context);
        String endDate = getCurrentPeriodEndDate(context);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        try {
            // Start from end date (today or period end, whichever is earlier)
            Date endDateObj = dateFormat.parse(endDate);
            Date today = calendar.getTime();
            
            if (today.before(endDateObj)) {
                calendar.setTime(today);
            } else {
                calendar.setTime(endDateObj);
            }
            
            String[] dates = new String[numberOfDays];
            for (int i = 0; i < numberOfDays; i++) {
                String currentDate = dateFormat.format(calendar.getTime());
                
                // Only include dates that are within the period
                if (currentDate.compareTo(startDate) >= 0) {
                    dates[i] = currentDate;
                } else {
                    dates[i] = null; // Outside period
                }
                
                // Move to previous day
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
            
            return dates;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating last days in period", e);
            // Fallback to regular last N days
            String[] dates = new String[numberOfDays];
            for (int i = 0; i < numberOfDays; i++) {
                dates[i] = dateFormat.format(calendar.getTime());
                calendar.add(Calendar.DAY_OF_MONTH, -1);
            }
            return dates;
        }
    }
    
    /**
     * Test method to verify week calculation logic
     */
    public static void testWeekCalculation(Context context) {
        Log.d(TAG, "=== TESTING WEEK CALCULATION ===");
        
        Calendar now = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        
        Log.d(TAG, "Today is: " + dateFormat.format(now.getTime()) + " (" + dayFormat.format(now.getTime()) + ")");
        Log.d(TAG, "Today's Calendar.DAY_OF_WEEK: " + now.get(Calendar.DAY_OF_WEEK));
        
        // Test specifically with current user preferences
        String userStartDay = PreferenceActivity.getStartDay(context);
        String userBudgetReset = PreferenceActivity.getBudgetReset(context);
        
        Log.d(TAG, "\n--- Testing with USER PREFERENCES: Start Day = " + userStartDay + ", Budget Reset = " + userBudgetReset + " ---");
        
        if (userBudgetReset.equals("Every week")) {
            Calendar testCalendar = (Calendar) now.clone();
            int targetDay = getDayOfWeekConstant(userStartDay);
            int currentDay = testCalendar.get(Calendar.DAY_OF_WEEK);
            
            Log.d(TAG, "Target day (" + userStartDay + ") constant: " + targetDay + ", Current day constant: " + currentDay);
            
            int daysToSubtract;
            if (currentDay == targetDay) {
                daysToSubtract = 0;
            } else {
                daysToSubtract = (currentDay - targetDay + 7) % 7;
            }
            
            Log.d(TAG, "Days to subtract: " + daysToSubtract);
            
            testCalendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract);
            String weekStart = dateFormat.format(testCalendar.getTime());
            
            testCalendar.add(Calendar.DAY_OF_MONTH, 6); // Add 6 days for week end
            String weekEnd = dateFormat.format(testCalendar.getTime());
            
            Log.d(TAG, "Calculated week: " + weekStart + " to " + weekEnd);
            
            // Test specific dates
            Log.d(TAG, "\n--- Testing specific dates ---");
            String[] testDates = {"2025-06-28", "2025-06-29", "2025-06-27", "2025-06-30", "2025-07-04"};
            for (String testDate : testDates) {
                boolean included = testDate.compareTo(weekStart) >= 0 && testDate.compareTo(weekEnd) <= 0;
                Log.d(TAG, "Date " + testDate + " in period [" + weekStart + " to " + weekEnd + "]: " + included);
            }
        }
        
        Log.d(TAG, "=== END WEEK CALCULATION TEST ===");
    }
}
