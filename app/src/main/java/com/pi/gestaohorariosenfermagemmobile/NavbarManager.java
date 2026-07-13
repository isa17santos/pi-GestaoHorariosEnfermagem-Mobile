package com.pi.gestaohorariosenfermagemmobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavbarManager {
    private final Activity activity;
    private final SharedPreferences prefs;

    public NavbarManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("AUTH", Context.MODE_PRIVATE);
        init();
    }

    private void init() {
        // 1. Preencher nome do utilizador
        TextView tvName = activity.findViewById(R.id.nav_tv_user_name);
        if (tvName != null) {
            tvName.setText(prefs.getString("user_name", "User"));
        }

        // 2. Traduzir o cargo e preencher
        updateRoleUI();

        // 3. Clique no Logotipo para ir para a Dashboard correta
        View imgLogo = activity.findViewById(R.id.nav_img_logo);
        if (imgLogo != null) {
            imgLogo.setOnClickListener(v -> redirectToDashboard());
        }

        // 4. Outros Click Listeners (Perfil, Notificações, Logout)
        activity.findViewById(R.id.nav_btn_profile).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, ProfileActivity.class)));

        activity.findViewById(R.id.nav_btn_notifications).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, NotificationsActivity.class)));

        activity.findViewById(R.id.nav_btn_logout).setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(activity, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            activity.finish();
        });

        activity.findViewById(R.id.nav_btn_language).setOnClickListener(v -> toggleLanguage());

        updateLanguageUI();
        refreshUnreadCount();
    }

    // Função para redirecionar para a dashboard correta baseado no cargo
    private void redirectToDashboard() {
        String role = prefs.getString("user_role", "").toLowerCase().trim();
        Intent intent;

        if (role.equals("admin")) {
            intent = new Intent(activity, AdminDashboardActivity.class);
        } else if (role.equals("nurse")) {
            intent = new Intent(activity, NurseDashboardActivity.class);
        } else {
            intent = new Intent(activity, HeadNurseDashboardActivity.class);
        }

        // Limpa a pilha para não acumular atividades
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public void refreshUnreadCount() {
        RetrofitClient.getClient(activity).create(ApiService.class).getUnreadCount().enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(Call<Map<String, Integer>> call, Response<Map<String, Integer>> response) {
                TextView badge = activity.findViewById(R.id.nav_tv_badge);
                if (badge != null && response.isSuccessful() && response.body() != null) {
                    Integer count = response.body().get("unread_count");
                    badge.setVisibility(count != null && count > 0 ? View.VISIBLE : View.GONE);
                    badge.setText(String.valueOf(count));
                }
            }
            @Override public void onFailure(Call<Map<String, Integer>> call, Throwable t) {}
        });
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String next = current.contains("en") ? "pt" : "en";
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(next));
        // onConfigurationChanged na activity trata do resto
    }

    public void updateRoleUI() {
        TextView tvRole = activity.findViewById(R.id.nav_tv_user_role);
        if (tvRole != null) {
            String role = prefs.getString("user_role", "").toLowerCase().trim();
            String translatedRole;
            if (role.equals("admin")) {
                translatedRole = activity.getString(R.string.role_admin);
            } else if (role.equals("nurse")) {
                translatedRole = activity.getString(R.string.role_nurse);
            } else if (role.equals("head_nurse") || role.equals("head nurse")) {
                translatedRole = activity.getString(R.string.role_head_nurse);
            } else {
                translatedRole = role.isEmpty() ? "" :
                        role.substring(0, 1).toUpperCase() + role.substring(1);
            }
            tvRole.setText(translatedRole);
        }
    }

    public void updateLanguageUI() {
        boolean isEn = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("en");
        TextView tvFlag = activity.findViewById(R.id.nav_tv_lang_flag);
        TextView tvLabel = activity.findViewById(R.id.nav_tv_lang_label);
        if (tvFlag != null) tvFlag.setText(isEn ? "pt" : "en");
        if (tvLabel != null) tvLabel.setText(isEn ? "Português" : "English");
    }
}