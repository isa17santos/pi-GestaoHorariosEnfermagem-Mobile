package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String token;
    private String message;
    
    @SerializedName("must_change_password")
    private boolean mustChangePassword;
    
    private String email;
    
    @SerializedName("password_reset_token")
    private String passwordResetToken;
    
    private UserData user;

    // Getters
    public String getToken() { return token; }
    public String getMessage() { return message; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getEmail() { return email; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public UserData getUser() { return user; }

    public static class UserData {
        private String name;
        private String email;
        private String role;
        
        @SerializedName("must_change_password")
        private boolean mustChangePassword;

        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isMustChangePassword() { return mustChangePassword; }
    }
}
