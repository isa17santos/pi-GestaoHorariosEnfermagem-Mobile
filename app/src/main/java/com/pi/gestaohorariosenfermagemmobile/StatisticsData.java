package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatisticsData {

    private String role;

    @SerializedName("nurses_count")
    private Integer nursesCount;

    @SerializedName("head_nurses_count")
    private Integer headNursesCount;

    @SerializedName("medical_leaves_this_month")
    private Integer medicalLeavesThisMonth;

    @SerializedName("vacations_this_month")
    private Integer vacationsThisMonth;

    @SerializedName("inactive_users_count")
    private Integer inactiveUsersCount;

    @SerializedName("pending_password_change_count")
    private Integer pendingPasswordChangeCount;

    @SerializedName("acceptance_rate")
    private Double acceptanceRate;

    @SerializedName("swaps_accepted")
    private Integer swapsAccepted;

    @SerializedName("swaps_rejected")
    private Integer swapsRejected;

    @SerializedName("avg_hours_per_nurse")
    private List<NurseHours> avgHoursPerNurse;

    @SerializedName("quality_score")
    private Integer qualityScore;

    @SerializedName("quality_indicator")
    private String qualityIndicator;

    @SerializedName("quality_breakdown")
    private QualityBreakdown qualityBreakdown;

    public String getRole() { return role; }
    public Integer getNursesCount() { return nursesCount; }
    public Integer getHeadNursesCount() { return headNursesCount; }
    public Integer getMedicalLeavesThisMonth() { return medicalLeavesThisMonth; }
    public Integer getVacationsThisMonth() { return vacationsThisMonth; }
    public Integer getInactiveUsersCount() { return inactiveUsersCount; }
    public Integer getPendingPasswordChangeCount() { return pendingPasswordChangeCount; }
    public Double getAcceptanceRate() { return acceptanceRate; }
    public Integer getSwapsAccepted() { return swapsAccepted; }
    public Integer getSwapsRejected() { return swapsRejected; }
    public List<NurseHours> getAvgHoursPerNurse() { return avgHoursPerNurse; }
    public Integer getQualityScore() { return qualityScore; }
    public String getQualityIndicator() { return qualityIndicator; }
    public QualityBreakdown getQualityBreakdown() { return qualityBreakdown; }
}
