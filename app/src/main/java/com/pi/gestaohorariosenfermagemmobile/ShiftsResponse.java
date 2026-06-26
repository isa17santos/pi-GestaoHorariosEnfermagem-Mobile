package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShiftsResponse {
    @SerializedName("data")
    private List<Shift> data;

    public List<Shift> getData() { return data; }
}
