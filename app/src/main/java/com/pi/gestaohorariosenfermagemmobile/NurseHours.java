package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class NurseHours {

    @SerializedName("user_id")
    private Integer userId;

    private String name;

    private Double hours;

    public Integer getUserId() { return userId; }
    public String getName() { return name; }
    public Double getHours() { return hours; }
}
