package com.pi.gestaohorariosenfermagemmobile;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("password-recovery/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @POST("password-recovery/email")
    Call<Void> sendRecoveryEmail(@Body Map<String, String> body);
}
