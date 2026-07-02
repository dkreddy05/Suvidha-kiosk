package com.suvidha.admin.dto;

public class PaymentsData {

    private double totalAmount;
    private long transactionCount;
    private long successCount;
    private long failedCount;
    private double successRate;
    private double avgTransactionAmount;

    public PaymentsData() {}

    public PaymentsData(double totalAmount, long transactionCount, long successCount,
                        long failedCount, double successRate, double avgTransactionAmount) {
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.successRate = successRate;
        this.avgTransactionAmount = avgTransactionAmount;
    }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long transactionCount) { this.transactionCount = transactionCount; }
    public long getSuccessCount() { return successCount; }
    public void setSuccessCount(long successCount) { this.successCount = successCount; }
    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public double getAvgTransactionAmount() { return avgTransactionAmount; }
    public void setAvgTransactionAmount(double avgTransactionAmount) { this.avgTransactionAmount = avgTransactionAmount; }
}
