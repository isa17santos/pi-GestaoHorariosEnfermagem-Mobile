package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SwapRequest {

    private int id;
    private String status;
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    private List<SwapParticipant> participants;

    @SerializedName("offered_shifts")
    private List<SwapShift> offeredShifts;

    @SerializedName("requested_shifts")
    private List<SwapShift> requestedShifts;

    // --- Getters ---

    public int getId() { return id; }
    public String getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getCreatedAt() { return createdAt; }
    public List<SwapParticipant> getParticipants() { return participants; }
    public List<SwapShift> getOfferedShifts() { return offeredShifts; }
    public List<SwapShift> getRequestedShifts() { return requestedShifts; }

    // --- Inner classes ---

    public static class SwapParticipant {
        private int id;
        private String name;
        private String role; // "requester" or "target"

        public int getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
    }

    public static class SwapShift {
        private int id;
        private String date;

        @SerializedName("shift_type")
        private SwapShiftType shiftType;

        private SwapOwner owner;

        public int getId() { return id; }
        public String getDate() { return date; }
        public SwapShiftType getShiftType() { return shiftType; }
        public SwapOwner getOwner() { return owner; }
    }

    public static class SwapShiftType {
        private String name;
        private String color;

        public String getName() { return name; }
        public String getColor() { return color; }
    }

    public static class SwapOwner {
        private String name;

        public String getName() { return name; }
    }
}
