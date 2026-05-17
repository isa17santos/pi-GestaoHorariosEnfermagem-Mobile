package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class NursePreference {
    // Nurse preference fields returned by the API
    @SerializedName("id")
    private Integer id;

    @SerializedName("month")
    private int month;

    @SerializedName("year")
    private int year;

    @SerializedName("prefers_morning")
    private boolean prefers_morning;

    @SerializedName("prefers_afternoon")
    private boolean prefers_afternoon;

    @SerializedName("prefers_night")
    private boolean prefers_night;

    @SerializedName("avoid_weekends")
    private boolean avoid_weekends;

    @SerializedName("prefers_weekends")
    private boolean prefers_weekends;

    @SerializedName("notes")
    private String notes;

    // Get the preference ID
    public Integer getId() { return id; }

    // Get the month
    public int getMonth() { return month; }

    // Get the year
    public int getYear() { return year; }

    // Check if prefers morning shifts
    public boolean prefersMorning() { return prefers_morning; }

    // Check if prefers afternoon shifts
    public boolean prefersAfternoon() { return prefers_afternoon; }

    // Check if prefers night shifts
    public boolean prefersNight() { return prefers_night; }

    // Check if avoids weekends
    public boolean avoidsWeekends() { return avoid_weekends; }

    // Check if prefers weekends
    public boolean prefersWeekends() { return prefers_weekends; }

    // Get additional notes
    public String getNotes() { return notes; }
}


