package com.pi.gestaohorariosenfermagemmobile;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    @PATCH("users/{id}")
    Call<Void> updateUser(@Header("Authorization") String token, @Path("id") int userId, @Body Map<String, Object> updates);

    @POST("users")
    Call<Void> createUser(@Header("Authorization") String token, @Body Map<String, Object> userData);

    @GET("shift-types")
    Call<ShiftTypesResponse> getShiftTypes(@Header("Authorization") String token);

    @POST("shift-types")
    Call<Void> createShiftType(@Header("Authorization") String token, @Body Map<String, Object> body);

    @PUT("shift-types/{id}")
    Call<Void> updateShiftType(@Header("Authorization") String token, @Path("id") int id, @Body Map<String, Object> body);

    @DELETE("shift-types/{id}")
    Call<Void> deleteShiftType(@Header("Authorization") String token, @Path("id") int id);

    // Profile endpoints

    // Get authenticated user's profile information
    @GET("profile")
    Call<ProfileResponse> getProfile(@Header("Authorization") String token);

    // Update authenticated user's profile information
    @PATCH("profile")
    Call<Void> updateProfile(@Header("Authorization") String token, @Body Map<String, Object> updates);

    // Get authenticated user's shift preferences
    @GET("profile/preferences")
    Call<NursePreferencesResponse> getProfilePreferences(@Header("Authorization") String token);

    // Save (create or update) the authenticated user's shift preferences
    @PATCH("profile/preferences")
    Call<Void> updateProfilePreferences(@Header("Authorization") String token, @Body Map<String, Object> updates);

    // Delete a specific preference by ID
    @DELETE("profile/preferences/{id}")
    Call<Void> deleteProfilePreference(@Header("Authorization") String token, @Path("id") int preferenceId);

    // Change authenticated user's password
    @POST("profile/change-password")
    Call<Void> changePassword(@Body ChangePasswordRequest request);


    @GET("schedules/weekly")
    Call<WeeklyScheduleResponse> getWeeklySchedule(
            @Header("Authorization") String token,
            @Query("date") String date,
            @Query("view") String view
    );


    // Shifts list
    @GET("shifts")
    Call<ShiftsResponse> getShifts(
            @Header("Authorization") String token,
            @Query("mine") Boolean mine,
            @Query("future") Boolean future
    );

    @GET("shifts")
    Call<ShiftsResponse> getShiftsFiltered(
            @Header("Authorization") String token,
            @Query("exclude_mine") Boolean excludeMine,
            @Query("date") String date,
            @Query("shift_type_id") Integer shiftTypeId
    );

    // Swap requests
    @POST("swaps")
    Call<Void> createSwap(@Header("Authorization") String token, @Body java.util.Map<String, Object> body);

    @GET("swaps/validate")
    Call<SwapValidationResponse> validateSwap(
            @Header("Authorization") String token,
            @Query("offered_shift_ids[]") List<Integer> offeredShiftIds,
            @Query("requested_shift_ids[]") List<Integer> requestedShiftIds
    );

    @GET("swaps")
    Call<SwapsResponse> getSwaps(
            @Header("Authorization") String token,
            @Query("direction") String direction,
            @Query("status") String status
    );

    @GET("swaps")
    Call<SwapsResponse> getSwapHistory(
            @Header("Authorization") String token,
            @Query("status") String status,
            @Query("user_id") Integer userId,
            @Query("month") Integer month,
            @Query("year") Integer year
    );

    @POST("swaps/{id}/accept")
    Call<Void> acceptSwap(@Header("Authorization") String token, @Path("id") int id);

    @POST("swaps/{id}/reject")
    Call<Void> rejectSwap(@Header("Authorization") String token, @Path("id") int id);

    @POST("swaps/{id}/cancel")
    Call<Void> cancelSwap(@Header("Authorization") String token, @Path("id") int id);

    // Notifications
    @GET("notifications")
    Call<NotificationsResponse> getNotifications();

    @GET("notifications/unread-count")
    Call<Map<String, Integer>> getUnreadCount();

    @PATCH("notifications/{id}/read")
    Call<Void> markAsRead(@Path("id") int id);

    @POST("notifications/read-all")
    Call<Void> markAllAsRead();

    @POST("medical-leaves")
    Call<Void> createMedicalLeave(@Header("Authorization") String token, @Body Map<String, Object> body);

    @GET("medical-leaves")
    Call<MedicalLeavesResponse> getMedicalLeaves(@Header("Authorization") String token);

    @PATCH("medical-leaves/{id}")
    Call<Void> updateMedicalLeave(
            @Header("Authorization") String token,
            @Path("id") int id,
            @Body Map<String, Object> body
    );

    @DELETE("medical-leaves/{id}")
    Call<Void> deleteMedicalLeave(@Header("Authorization") String token, @Path("id") int id);

    // Dashboard
    @GET("dashboard")
    Call<DashboardResponse> getDashboard(@Header("Authorization") String token);

    // Statistics
    @GET("statistics")
    Call<StatisticsResponse> getStatistics(@Header("Authorization") String token);
}
