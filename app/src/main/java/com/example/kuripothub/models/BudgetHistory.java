package com.example.kuripothub.models;

public class BudgetHistory {
    private String id;
    private String userId;
    private double budget;
    private String startDate; // When this budget became effective
    private String endDate;   // When this budget ended (null if current)
    private long timestamp;

    public BudgetHistory() {
        // Default constructor required for calls to DataSnapshot.getValue(BudgetHistory.class)
    }

    public BudgetHistory(String userId, double budget, String startDate) {
        this.userId = userId;
        this.budget = budget;
        this.startDate = startDate;
        this.endDate = null; // Current budget has no end date
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
