package com.suvidha.admin.dto;

public class DashboardData {

    private long totalUsers;
    private long activeSessions;
    private long totalGrievances;
    private long grievancesOpen;
    private long grievancesResolved;
    private long grievancesPending;
    private double totalPayments;
    private long paymentCount;
    private double paymentSuccessRate;

    public DashboardData() {}

    public DashboardData(long totalUsers, long activeSessions, long totalGrievances,
                         long grievancesOpen, long grievancesResolved, long grievancesPending,
                         double totalPayments, long paymentCount, double paymentSuccessRate) {
        this.totalUsers = totalUsers;
        this.activeSessions = activeSessions;
        this.totalGrievances = totalGrievances;
        this.grievancesOpen = grievancesOpen;
        this.grievancesResolved = grievancesResolved;
        this.grievancesPending = grievancesPending;
        this.totalPayments = totalPayments;
        this.paymentCount = paymentCount;
        this.paymentSuccessRate = paymentSuccessRate;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    public long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }
    public long getTotalGrievances() { return totalGrievances; }
    public void setTotalGrievances(long totalGrievances) { this.totalGrievances = totalGrievances; }
    public long getGrievancesOpen() { return grievancesOpen; }
    public void setGrievancesOpen(long grievancesOpen) { this.grievancesOpen = grievancesOpen; }
    public long getGrievancesResolved() { return grievancesResolved; }
    public void setGrievancesResolved(long grievancesResolved) { this.grievancesResolved = grievancesResolved; }
    public long getGrievancesPending() { return grievancesPending; }
    public void setGrievancesPending(long grievancesPending) { this.grievancesPending = grievancesPending; }
    public double getTotalPayments() { return totalPayments; }
    public void setTotalPayments(double totalPayments) { this.totalPayments = totalPayments; }
    public long getPaymentCount() { return paymentCount; }
    public void setPaymentCount(long paymentCount) { this.paymentCount = paymentCount; }
    public double getPaymentSuccessRate() { return paymentSuccessRate; }
    public void setPaymentSuccessRate(double paymentSuccessRate) { this.paymentSuccessRate = paymentSuccessRate; }
}
