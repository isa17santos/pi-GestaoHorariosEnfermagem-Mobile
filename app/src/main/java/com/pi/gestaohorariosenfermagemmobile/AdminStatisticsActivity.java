package com.pi.gestaohorariosenfermagemmobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatisticsActivity extends BaseActivity {

    private TextView tvPageTitle, tvPageSubtitle, tvMonthLabel;
    private View sectionStats;
    private TextView tvNursesCount, tvHeadNursesCount;
    private TextView tvMedicalLeaves, tvVacations;
    private TextView tvInactiveUsers, tvPendingPassword;
    private TextView tvLabelNurses, tvLabelHeadNurses, tvLabelMedicalLeaves, tvLabelVacations;
    private TextView tvLabelInactiveUsers, tvLabelPendingPassword;
    private String token;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        navbarManager = new NavbarManager(this);
        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        updateUIStrings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void initViews() {
        tvPageTitle = findViewById(R.id.tv_page_title);
        tvPageSubtitle = findViewById(R.id.tv_page_subtitle);
        tvMonthLabel = findViewById(R.id.tv_month_label);
        sectionStats = findViewById(R.id.section_stats);
        tvNursesCount = findViewById(R.id.tv_nurses_count);
        tvHeadNursesCount = findViewById(R.id.tv_head_nurses_count);
        tvMedicalLeaves = findViewById(R.id.tv_medical_leaves);
        tvVacations = findViewById(R.id.tv_vacations);
        tvInactiveUsers = findViewById(R.id.tv_inactive_users);
        tvPendingPassword = findViewById(R.id.tv_pending_password);
        tvLabelNurses = findViewById(R.id.tv_label_nurses);
        tvLabelHeadNurses = findViewById(R.id.tv_label_head_nurses);
        tvLabelMedicalLeaves = findViewById(R.id.tv_label_medical_leaves);
        tvLabelVacations = findViewById(R.id.tv_label_vacations);
        tvLabelInactiveUsers = findViewById(R.id.tv_label_inactive_users);
        tvLabelPendingPassword = findViewById(R.id.tv_label_pending_password);
    }

    private void loadStatistics() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getStatistics("Bearer " + token).enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(Call<StatisticsResponse> call, Response<StatisticsResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    sectionStats.setVisibility(View.GONE);
                    return;
                }
                StatisticsData data = response.body().getData();
                if (data == null) {
                    sectionStats.setVisibility(View.GONE);
                    return;
                }

                tvNursesCount.setText(data.getNursesCount() != null ? String.valueOf(data.getNursesCount()) : "—");
                tvHeadNursesCount.setText(data.getHeadNursesCount() != null ? String.valueOf(data.getHeadNursesCount()) : "—");
                tvMedicalLeaves.setText(data.getMedicalLeavesThisMonth() != null ? String.valueOf(data.getMedicalLeavesThisMonth()) : "—");
                tvVacations.setText(data.getVacationsThisMonth() != null ? String.valueOf(data.getVacationsThisMonth()) : "—");
                tvInactiveUsers.setText(data.getInactiveUsersCount() != null ? String.valueOf(data.getInactiveUsersCount()) : "—");
                tvPendingPassword.setText(data.getPendingPasswordChangeCount() != null ? String.valueOf(data.getPendingPasswordChangeCount()) : "—");

                sectionStats.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<StatisticsResponse> call, Throwable t) {
                sectionStats.setVisibility(View.GONE);
            }
        });
    }

    private static Locale getAppLocale() {
        androidx.core.os.LocaleListCompat list = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
        return list.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(list.toLanguageTags());
    }

    private String buildMonthLabel() {
        Locale locale = getAppLocale();
        Calendar cal = Calendar.getInstance();
        String month = new SimpleDateFormat("MMMM", locale).format(cal.getTime()).toUpperCase(locale);
        int year = cal.get(Calendar.YEAR);
        return month + " " + getString(R.string.date_of_separator) + " " + year;
    }

    @Override
    protected void updateUIStrings() {
        if (navbarManager != null) {
            navbarManager.updateRoleUI();
            navbarManager.updateLanguageUI();
        }
        tvPageTitle.setText(R.string.statistics);
        tvPageSubtitle.setText(R.string.admin_statistics_subtitle);
        tvMonthLabel.setText(buildMonthLabel());
        if (tvLabelNurses != null) tvLabelNurses.setText(R.string.stat_nurses_label);
        if (tvLabelHeadNurses != null) tvLabelHeadNurses.setText(R.string.stat_head_nurses_label);
        if (tvLabelMedicalLeaves != null) tvLabelMedicalLeaves.setText(R.string.stat_medical_leaves_label);
        if (tvLabelVacations != null) tvLabelVacations.setText(R.string.stat_vacations_label);
        if (tvLabelInactiveUsers != null) tvLabelInactiveUsers.setText(R.string.stat_inactive_users_label);
        if (tvLabelPendingPassword != null) tvLabelPendingPassword.setText(R.string.stat_pending_password_label);
        loadStatistics();
    }
}
