package com.suvidha.admin.dto;

public class UsersData {

    private long totalRegistered;
    private long newUsers;
    private long activeSessions;
    private long inactiveUsers;

    public UsersData() {}

    public UsersData(long totalRegistered, long newUsers, long activeSessions, long inactiveUsers) {
        this.totalRegistered = totalRegistered;
        this.newUsers = newUsers;
        this.activeSessions = activeSessions;
        this.inactiveUsers = inactiveUsers;
    }

    public long getTotalRegistered() { return totalRegistered; }
    public void setTotalRegistered(long totalRegistered) { this.totalRegistered = totalRegistered; }
    public long getNewUsers() { return newUsers; }
    public void setNewUsers(long newUsers) { this.newUsers = newUsers; }
    public long getActiveSessions() { return activeSessions; }
    public void setActiveSessions(long activeSessions) { this.activeSessions = activeSessions; }
    public long getInactiveUsers() { return inactiveUsers; }
    public void setInactiveUsers(long inactiveUsers) { this.inactiveUsers = inactiveUsers; }
}
