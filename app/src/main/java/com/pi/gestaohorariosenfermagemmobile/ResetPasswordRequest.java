package com.pi.gestaohorariosenfermagemmobile;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {
    private String email;
    private String token;
    private String password;

    @SerializedName("password_confirmation")
    private String passwordConfirmation;

    public ResetPasswordRequest(String email, String token, String password, String passwordConfirmation) {
        this.email = email;
        this.token = token;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
    }
}