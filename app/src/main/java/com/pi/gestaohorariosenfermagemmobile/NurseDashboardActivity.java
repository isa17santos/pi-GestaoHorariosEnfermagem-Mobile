package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.card.MaterialCardView;

public class NurseDashboardActivity extends BaseActivity {

    private TextView tvGreeting, tvNurseSubtitle;
    private String currentUserName;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_dashboard);

        navbarManager = new NavbarManager(this);

        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Enfermeiro");

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
        tvNurseSubtitle = findViewById(R.id.tv_nurse_subtitle);
    }


    @Override
    protected void updateUIStrings() {
        tvGreeting.setText(getString(R.string.dashboard_nurse_greeting, currentUserName));
        tvNurseSubtitle.setText(R.string.dashboard_nurse_subtitle);

        // Atualiza textos dos cartões
        updateCardText(R.id.card_schedule, R.string.schedule, R.string.schedule_subtitle);
        updateCardText(R.id.card_swaps, R.string.swaps, R.string.swaps_subtitle);
        updateCardText(R.id.card_stats, R.string.statistics, R.string.my_statistics_subtitle);
    }

    private void updateCardText(int cardId, int titleRes, int subRes) {
        View card = findViewById(cardId);
        if (card != null) {
            ((TextView) card.findViewWithTag("title")).setText(titleRes);
            ((TextView) card.findViewWithTag("subtitle")).setText(subRes);
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.card_schedule).setOnClickListener(v -> {
            startActivity(new Intent(this, ScheduleActivity.class));

        });

        findViewById(R.id.card_swaps).setOnClickListener(v -> {
            startActivity(new Intent(this, SwapsActivity.class));
        });

        findViewById(R.id.card_stats).setOnClickListener(v ->
                startActivity(new Intent(this, NurseStatisticsActivity.class)));

    }
}