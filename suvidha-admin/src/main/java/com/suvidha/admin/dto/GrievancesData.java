package com.suvidha.admin.dto;

public class GrievancesData {

    private long total;
    private long open;
    private long resolved;
    private long pending;
    private double avgResolutionTimeHours;

    public GrievancesData() {}

    public GrievancesData(long total, long open, long resolved, long pending, double avgResolutionTimeHours) {
        this.total = total;
        this.open = open;
        this.resolved = resolved;
        this.pending = pending;
        this.avgResolutionTimeHours = avgResolutionTimeHours;
    }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getOpen() { return open; }
    public void setOpen(long open) { this.open = open; }
    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }
    public long getPending() { return pending; }
    public void setPending(long pending) { this.pending = pending; }
    public double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }
}
