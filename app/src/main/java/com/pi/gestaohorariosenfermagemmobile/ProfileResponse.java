package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class ProfileResponse {

    // Top-level wrapper matching the API envelope: { "data": { ... } }
    @SerializedName("data")
    private Data data;

    /** Returns the nested profile payload. */
    public Data getData() { return data; }

    // -------------------------------------------------------------------------
    // Inner class representing the actual profile fields inside "data"
    // -------------------------------------------------------------------------
    public static class Data {

        // Profile fields returned by the API
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("email")
        private String email;

        @SerializedName("role")
        private String role;

        @SerializedName("active")
        private boolean active;

        // Get the user ID
        public int getId() { return id; }

        // Get the user name
        public String getName() { return name; }

        // Get the user email
        public String getEmail() { return email; }

        // Get the user role
        public String getRole() { return role; }

        // Check if user is active
        public boolean isActive() { return active; }
    }
}


