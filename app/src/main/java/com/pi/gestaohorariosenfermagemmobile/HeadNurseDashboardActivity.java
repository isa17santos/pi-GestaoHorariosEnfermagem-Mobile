package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.card.MaterialCardView;

public class HeadNurseDashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserRole, tvGreeting, tvHeadNurseSubtitle;
    private TextView tvLangFlag, tvLangLabel;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_nurse_dashboard);

        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Enfermeiro Chefe");

        tvUserName.setText(currentUserName);
        tvUserRole.setText(R.string.role_head_nurse);

        updateUIStrings();
        updateLanguageButton();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserRole = findViewById(R.id.tv_user_role);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvHeadNurseSubtitle = findViewById(R.id.tv_head_nurse_subtitle);

        MaterialCardView btnLang = findViewById(R.id.btn_language_switch_dashboard);
        tvLangFlag = findViewById(R.id.tv_language_flag_dashboard);
        tvLangLabel = findViewById(R.id.tv_language_label_dashboard);

        if (btnLang != null) btnLang.setOnClickListener(v -> toggleLanguage());
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIStrings();
        updateLanguageButton();
    }

    private void updateUIStrings() {
        tvGreeting.setText(getString(R.string.dashboard_head_nurse_greeting, currentUserName));
        tvHeadNurseSubtitle.setText(R.string.dashboard_head_nurse_subtitle);
        tvUserRole.setText(R.string.role_head_nurse);

        updateCardText(R.id.card_view_schedule, R.string.consult_schedule, R.string.consult_schedule_subtitle);
        updateCardText(R.id.card_stats, R.string.statistics, R.string.head_nurse_stats_subtitle);
    }

    private void updateCardText(int cardId, int titleRes, int subRes) {
        View card = findViewById(cardId);
        if (card != null) {
            ((TextView) card.findViewWithTag("title")).setText(titleRes);
            ((TextView) card.findViewWithTag("subtitle")).setText(subRes);
        }
    }

    private void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    private void setupClickListeners() {
        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.card_view_schedule).setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));

        findViewById(R.id.card_stats).setOnClickListener(v ->
                startActivity(new Intent(this, HeadNurseStatisticsActivity.class))); // Ou NurseStatistics se preferir

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            getSharedPreferences("AUTH", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}