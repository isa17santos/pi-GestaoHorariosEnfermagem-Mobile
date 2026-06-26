package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class SwapValidationResponse {
    @SerializedName("data")
    private ValidationData data;

    public List<String> getWarnings() {
        return data != null ? data.getWarnings() : new ArrayList<>();
    }

    public static class ValidationData {
        @SerializedName("warnings")
        private List<String> warnings;
        public List<String> getWarnings() { return warnings != null ? warnings : new ArrayList<>(); }
    }
}
