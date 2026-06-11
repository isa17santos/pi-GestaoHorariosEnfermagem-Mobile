package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Shift {
    @SerializedName("id") private int id;
    @SerializedName("date") private String date;
    @SerializedName("shift_type") private ShiftType shiftType;
    @SerializedName("users") private List<User> users;

    public int getId() { return id; }
    public String getDate() { return date; }
    public ShiftType getShiftType() { return shiftType; }
    public List<User> getUsers() { return users; }
}