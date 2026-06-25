package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class MedicalLeave {
    private int id;
    @SerializedName("user_id")
    private int userId;
    private User user;
    @SerializedName("start_date")
    private String startDate;
    @SerializedName("end_date")
    private String endDate;
    private String reason;
    private String status; // "ongoing", "future", "past"

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public User getUser() { return user; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
}