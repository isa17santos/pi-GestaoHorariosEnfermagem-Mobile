package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class QualityBreakdown {

    @SerializedName("swaps_this_month")
    private Integer swapsThisMonth;

    @SerializedName("min_nurses_violations")
    private Integer minNursesViolations;

    @SerializedName("preference_type_violations")
    private Integer preferenceTypeViolations;

    @SerializedName("preference_weekend_violations")
    private Integer preferenceWeekendViolations;

    public Integer getSwapsThisMonth() { return swapsThisMonth; }
    public Integer getMinNursesViolations() { return minNursesViolations; }
    public Integer getPreferenceTypeViolations() { return preferenceTypeViolations; }
    public Integer getPreferenceWeekendViolations() { return preferenceWeekendViolations; }
}
