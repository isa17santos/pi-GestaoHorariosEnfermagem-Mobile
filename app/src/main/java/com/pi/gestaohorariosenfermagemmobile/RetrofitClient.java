package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Configurar GSON para ser leniente (evita o erro de malformed JSON)
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                SharedPreferences prefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
                String token = prefs.getString("token", "");

                Request originalRequest = chain.request();
                Request.Builder builder = originalRequest.newBuilder()
                        .addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json");

                // SÓ ADICIONA O TOKEN SE:
                // 1. O token existir
                // 2. O pedido NÃO for para o login
                boolean isLoginRequest = originalRequest.url().encodedPath().contains("login");

                if (token != null && !token.isEmpty() && !isLoginRequest) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson)) // Usar o gson leniente
                    .build();
        }
        return retrofit;
    }
}