package com.pi.gestaohorariosenfermagemmobile;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("password-recovery/reset")
    Call<Void> resetPassword(@Body ResetPasswordRequest request);

    @POST("password-recovery/email")
    Call<Void> sendRecoveryEmail(@Body Map<String, String> body);

    @GET("users")
    Call<UsersResponse> getUsers(@Header("Authorization") String token);

    @DELETE("users/{id}")
    Call<Void> deleteUser(@Header("Authorization") String token, @Path("id") int userId);
}
