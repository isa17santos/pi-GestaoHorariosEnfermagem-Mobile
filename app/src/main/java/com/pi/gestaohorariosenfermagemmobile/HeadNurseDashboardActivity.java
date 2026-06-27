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

public class HeadNurseDashboardActivity extends BaseActivity {

    private TextView tvGreeting, tvHeadNurseSubtitle;
    private String currentUserName;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_nurse_dashboard);

        navbarManager = new NavbarManager(this);

        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Enfermeiro Chefe");

        updateUIStrings();
        setupClickListeners();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvHeadNurseSubtitle = findViewById(R.id.tv_head_nurse_subtitle);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    protected void updateUIStrings() {
        tvGreeting.setText(getString(R.string.dashboard_head_nurse_greeting, currentUserName));
        tvHeadNurseSubtitle.setText(R.string.dashboard_head_nurse_subtitle);

        updateCardText(R.id.card_view_schedule, R.string.consult_schedule, R.string.consult_schedule_subtitle);
        updateCardText(R.id.card_swap_history, R.string.swap_history_title, R.string.swap_history_card_subtitle);
        updateCardText(R.id.card_stats, R.string.statistics, R.string.head_nurse_stats_subtitle);
    }

    private void updateCardText(int cardId, int titleRes, int subRes) {
        View card = findViewById(cardId);
        if (card != null) {
            ((TextView) card.findViewWithTag("title")).setText(titleRes);
            ((TextView) card.findViewWithTag("subtitle")).setText(subRes);
        }
    }


    private void setupClickListeners() {
        findViewById(R.id.card_view_schedule).setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));

        findViewById(R.id.card_swap_history).setOnClickListener(v ->
                startActivity(new Intent(this, SwapHistoryActivity.class)));

        findViewById(R.id.card_stats).setOnClickListener(v ->
                startActivity(new Intent(this, HeadNurseStatisticsActivity.class)));
    }
}