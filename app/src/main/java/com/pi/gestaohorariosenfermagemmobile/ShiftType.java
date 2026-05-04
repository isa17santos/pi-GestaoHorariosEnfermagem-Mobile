package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class ShiftType {
    // Shift type fields returned by the API
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("start_time")
    private String start_time;

    @SerializedName("end_time")
    private String end_time;

    @SerializedName("color")
    private String color;

    @SerializedName("min_nurses")
    private int min_nurses;

    // Get the shift type ID
    public int getId() { return id; }

    // Get the shift type name
    public String getName() { return name; }

    // Get the shift start time
    public String getStartTime() { return start_time; }

    // Get the shift end time
    public String getEndTime() { return end_time; }

    // Get the shift color
    public String getColor() { return color; }

    // Get the minimum number of nurses
    public int getMinNurses() { return min_nurses; }
}

