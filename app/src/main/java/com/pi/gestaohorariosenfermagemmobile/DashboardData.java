package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class DashboardData {

    private String role;

    @SerializedName("monthly_hours")
    private Double monthlyHours;

    @SerializedName("shift_type_breakdown")
    private Map<String, Integer> shiftTypeBreakdown;

    @SerializedName("pending_swaps_count")
    private Integer pendingSwapsCount;

    @SerializedName("swaps_this_month")
    private Integer swapsThisMonth;

    @SerializedName("team_pending_swaps_count")
    private Integer teamPendingSwapsCount;

    @SerializedName("team_swaps_this_month")
    private Integer teamSwapsThisMonth;

    public String getRole() { return role; }
    public Double getMonthlyHours() { return monthlyHours; }
    public Map<String, Integer> getShiftTypeBreakdown() { return shiftTypeBreakdown; }
    public Integer getPendingSwapsCount() { return pendingSwapsCount; }
    public Integer getSwapsThisMonth() { return swapsThisMonth; }
    public Integer getTeamPendingSwapsCount() { return teamPendingSwapsCount; }
    public Integer getTeamSwapsThisMonth() { return teamSwapsThisMonth; }
}
