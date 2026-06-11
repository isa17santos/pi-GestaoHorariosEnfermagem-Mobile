package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeeklyScheduleResponse {
    @SerializedName("data") private Data data;
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("shifts") private List<Shift> shifts;
        public List<Shift> getShifts() { return shifts; }

        @SerializedName("shift_types")
        private List<ShiftType> shiftTypes;

        public List<ShiftType> getShiftTypes() { return shiftTypes; }
    }
}