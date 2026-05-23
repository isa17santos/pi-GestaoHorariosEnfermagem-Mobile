package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {
    @SerializedName("current_password")
    private final String currentPassword;

    private final String password;

    @SerializedName("password_confirmation")
    private final String passwordConfirmation;

    public ChangePasswordRequest(String currentPassword, String password, String passwordConfirmation) {
        this.currentPassword = currentPassword;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getPassword() {
        return password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }
}


