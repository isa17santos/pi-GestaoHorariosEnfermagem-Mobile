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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NurseDashboardActivity extends BaseActivity {

    private TextView tvGreeting, tvNurseSubtitle;
    private TextView tvSwapBadge;
    private String currentUserName;
    private String token;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nurse_dashboard);

        navbarManager = new NavbarManager(this);

        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Enfermeiro");
        token = prefs.getString("token", "");

        updateUIStrings();
        setupClickListeners();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(navbarManager != null) navbarManager.refreshUnreadCount();
        loadPendingSwapsBadge();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvNurseSubtitle = findViewById(R.id.tv_nurse_subtitle);
        tvSwapBadge = findViewById(R.id.tv_swap_badge);
    }


    private void loadPendingSwapsBadge() {
        if (tvSwapBadge == null) return;
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getSwaps("Bearer " + token, "received", "pending").enqueue(new Callback<SwapsResponse>() {
            @Override
            public void onResponse(Call<SwapsResponse> call, Response<SwapsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<SwapRequest> data = response.body().getData();
                    int count = data != null ? data.size() : 0;
                    if (count > 0) {
                        tvSwapBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        tvSwapBadge.setVisibility(View.VISIBLE);
                    } else {
                        tvSwapBadge.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(Call<SwapsResponse> call, Throwable t) {
                tvSwapBadge.setVisibility(View.GONE);
            }
        });
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