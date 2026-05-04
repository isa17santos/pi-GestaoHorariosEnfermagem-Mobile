package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ShiftTypesResponse {
    // Shift type list returned by the API
    @SerializedName("data")
    private List<ShiftType> data;

    // Get the response data
    public List<ShiftType> getData() { return data; }
}

