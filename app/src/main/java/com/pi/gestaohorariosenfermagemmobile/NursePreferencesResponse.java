package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NursePreferencesResponse {
    // Nurse preference list returned by the API
    @SerializedName("data")
    private List<NursePreference> data;

    // Get the response data
    public List<NursePreference> getData() { return data; }
}


