# Week Start Day Preference Implementation

## Overview
The KuripotHub analytics feature now supports user-configurable week start days. Users can choose any day of the week (Monday through Sunday) as their preferred week start day through the preferences screen.

## What Was Implemented

### 1. User Preference Integration
- **PreferenceActivity.java**: Already had the static getter method `getStartDay(Context)` to retrieve user's preferred start day
- **Default**: Monday (if no preference is set)
- **Options**: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday

### 2. Dynamic Week Calculation
Updated `ExpenseSummaryActivity.java` with the following key changes:

#### New Methods:
- `getDayOfWeekConstant(String dayName)`: Converts day names to Calendar constants
- `calculateDaysToGoBack(int currentDay, int startDay)`: Calculates how many days to go back to reach the week start
- `getFallbackDayNames()`: Generates day labels based on user preference for error cases

#### Updated Methods:
- `getCurrentWeekStartDate()`: Now uses user's preferred start day instead of hardcoded Saturday
- `getCurrentWeekEndDate()`: Automatically calculates end date based on user's start day
- `updateWeeklyDailySpending()`: Already dynamic - uses week start date to generate correct day labels

### 3. Week Period Examples
- **Monday start**: Week runs Monday → Sunday
- **Tuesday start**: Week runs Tuesday → Monday (next week)
- **Wednesday start**: Week runs Wednesday → Tuesday (next week)
- **Thursday start**: Week runs Thursday → Wednesday (next week)
- **Friday start**: Week runs Friday → Thursday (next week)
- **Saturday start**: Week runs Saturday → Friday (next week)
- **Sunday start**: Week runs Sunday → Saturday (next week)

## Technical Details

### Week Calculation Logic
```java
// Get user's preferred start day
String userStartDay = PreferenceActivity.getStartDay(this);
int preferredStartDay = getDayOfWeekConstant(userStartDay);

// Calculate days to go back from current day to reach start day
int daysToGoBack = calculateDaysToGoBack(currentDayOfWeek, preferredStartDay);
```

### Day Label Generation
The daily spending section automatically shows the correct day labels:
- Day 1: User's selected start day
- Day 2: Next day
- ...
- Day 7: Day before start day (completes the week)

### Budget Reset Integration
The budget reset logic already respects the weekly period calculations, so when users select "Every week" for budget reset, it will reset based on their chosen start day.

## User Experience

1. **Preferences Screen**: User selects their preferred week start day
2. **Analytics Screen**: Automatically calculates and displays weekly data based on preference
3. **Dynamic Updates**: If user changes preference, analytics will recalculate on next app load
4. **Fallback Handling**: If any errors occur, the system gracefully falls back to showing appropriate day labels

## Testing

The implementation includes debug logging that shows:
- User's selected preferences
- Calculated week start and end dates
- Day-by-day expense filtering
- Which expenses are included/excluded from current week

## Files Modified

1. **ExpenseSummaryActivity.java**:
   - Updated week calculation methods
   - Added preference integration
   - Updated comments and documentation

2. **PreferenceActivity.java**: 
   - Already complete with static getter methods

3. **Layout files**: 
   - No changes needed - day labels are set dynamically in code

## Backward Compatibility

- Default preference is Monday (sensible default)
- All existing functionality preserved
- No changes to database schema required
- Graceful fallback to Monday if preferences are corrupted
