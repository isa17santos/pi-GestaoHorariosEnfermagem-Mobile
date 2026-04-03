package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Criar um interceptor que adiciona o Bearer Token e o Accept Header
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                SharedPreferences prefs = context.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
                String token = prefs.getString("token", "");

                Request.Builder builder = chain.request().newBuilder()
                        .addHeader("Accept", "application/json");

                // Só adiciona o token se ele existir
                if (token != null && !token.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
