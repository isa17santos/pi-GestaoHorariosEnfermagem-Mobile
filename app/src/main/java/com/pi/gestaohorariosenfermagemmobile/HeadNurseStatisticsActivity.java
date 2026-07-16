package com.pi.gestaohorariosenfermagemmobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeadNurseStatisticsActivity extends BaseActivity {

    private TextView tvPageTitle, tvPageSubtitle, tvMonthLabel;
    private View sectionStats;
    // Card Qualidade
    private TextView tvQualityCardTitle, tvQualityNote, tvQualityBasedOn;
    private TextView tvBreakdownSwaps, tvBreakdownMinNurses, tvBreakdownPrefType, tvBreakdownPrefWeekend;
    private TextView tvLabelBreakdownSwaps, tvLabelBreakdownMinNurses, tvLabelBreakdownPrefType, tvLabelBreakdownPrefWeekend;
    // Card Aceitação
    private CircularProgressIndicator progressAcceptance;
    private TextView tvAcceptanceTitle, tvAcceptanceRate, tvAcceptanceLegend;
    private TextView tvSwapsAccepted, tvSwapsRejected;
    // Card Horas
    private LinearLayout llNurseHours;
    private TextView tvHoursTitle, tvHoursSubtitle, tvHoursColName, tvHoursColHours;

    private String token;
    private NavbarManager navbarManager;
    private int currentMonth;
    private int currentYear;
    private View btnNextMonth;
    private boolean hasEverLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_nurse_statistics);

        navbarManager = new NavbarManager(this);
        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");

        Calendar now = Calendar.getInstance();
        currentMonth = now.get(Calendar.MONTH) + 1;
        currentYear = now.get(Calendar.YEAR);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        View btnPrev = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);
        if (btnPrev != null) btnPrev.setOnClickListener(v -> navigateMonth(-1));
        if (btnNextMonth != null) btnNextMonth.setOnClickListener(v -> navigateMonth(1));
        updateNextMonthButton();
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
        tvQualityCardTitle = findViewById(R.id.tv_quality_card_title);
        tvQualityNote = findViewById(R.id.tv_quality_note);
        tvQualityBasedOn = findViewById(R.id.tv_quality_based_on);
        tvBreakdownSwaps = findViewById(R.id.tv_breakdown_swaps);
        tvBreakdownMinNurses = findViewById(R.id.tv_breakdown_min_nurses);
        tvBreakdownPrefType = findViewById(R.id.tv_breakdown_pref_type);
        tvBreakdownPrefWeekend = findViewById(R.id.tv_breakdown_pref_weekend);
        tvLabelBreakdownSwaps = findViewById(R.id.tv_label_breakdown_swaps);
        tvLabelBreakdownMinNurses = findViewById(R.id.tv_label_breakdown_min_nurses);
        tvLabelBreakdownPrefType = findViewById(R.id.tv_label_breakdown_pref_type);
        tvLabelBreakdownPrefWeekend = findViewById(R.id.tv_label_breakdown_pref_weekend);
        progressAcceptance = findViewById(R.id.progress_acceptance);
        tvAcceptanceTitle = findViewById(R.id.tv_acceptance_title);
        tvAcceptanceRate = findViewById(R.id.tv_acceptance_rate);
        tvAcceptanceLegend = findViewById(R.id.tv_acceptance_legend);
        tvSwapsAccepted = findViewById(R.id.tv_swaps_accepted);
        tvSwapsRejected = findViewById(R.id.tv_swaps_rejected);
        llNurseHours = findViewById(R.id.ll_nurse_hours);
        tvHoursTitle = findViewById(R.id.tv_hours_title);
        tvHoursSubtitle = findViewById(R.id.tv_hours_subtitle);
        tvHoursColName = findViewById(R.id.tv_hours_col_name);
        tvHoursColHours = findViewById(R.id.tv_hours_col_hours);
    }

    private void loadStatistics() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Calendar now = Calendar.getInstance();
        int nowMonth = now.get(Calendar.MONTH) + 1;
        int nowYear = now.get(Calendar.YEAR);
        Call<StatisticsResponse> call = (currentMonth == nowMonth && currentYear == nowYear)
                ? api.getStatistics("Bearer " + token)
                : api.getStatisticsForMonth("Bearer " + token, currentMonth, currentYear);
        call.enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(Call<StatisticsResponse> call, Response<StatisticsResponse> response) {
                StatisticsData data = (response.isSuccessful() && response.body() != null)
                        ? response.body().getData() : null;
                if (data == null) {
                    setStatsNoData();
                    return;
                }

                bindQualityCard(data);
                bindAcceptanceCard(data);
                bindNurseHoursCard(data);
                hasEverLoaded = true;
                sectionStats.setVisibility(View.VISIBLE);
                sectionStats.setAlpha(1f);
            }

            @Override
            public void onFailure(Call<StatisticsResponse> call, Throwable t) {
                setStatsNoData();
            }
        });
    }

    private void setStatsNoData() {
        if (!hasEverLoaded) {
            sectionStats.setVisibility(View.GONE);
            return;
        }
        sectionStats.setVisibility(View.VISIBLE);
        sectionStats.setAlpha(0.35f);
    }

    private void bindQualityCard(StatisticsData data) {
        QualityBreakdown bd = data.getQualityBreakdown();
        if (bd != null) {
            tvBreakdownSwaps.setText(bd.getSwapsThisMonth() != null ? String.valueOf(bd.getSwapsThisMonth()) : "—");
            tvBreakdownMinNurses.setText(bd.getMinNursesViolations() != null ? String.valueOf(bd.getMinNursesViolations()) : "—");
            tvBreakdownPrefType.setText(bd.getPreferenceTypeViolations() != null ? String.valueOf(bd.getPreferenceTypeViolations()) : "—");
            tvBreakdownPrefWeekend.setText(bd.getPreferenceWeekendViolations() != null ? String.valueOf(bd.getPreferenceWeekendViolations()) : "—");
        }
    }

    private void bindAcceptanceCard(StatisticsData data) {
        Double rate = data.getAcceptanceRate();
        if (rate == null) {
            progressAcceptance.setProgress(0);
            tvAcceptanceRate.setText(getString(R.string.stat_acceptance_no_data));
            tvAcceptanceRate.setTextColor(getColor(R.color.text_muted));
        } else {
            int progress = (int) Math.round(rate);
            progressAcceptance.setProgress(progress);
            String formatted = String.format(new Locale("pt", "PT"), "%.1f%%", rate);
            tvAcceptanceRate.setText(formatted);
            tvAcceptanceRate.setTextColor(getColor(R.color.primary_strong));
        }

        Integer accepted = data.getSwapsAccepted();
        Integer rejected = data.getSwapsRejected();
        tvSwapsAccepted.setText(accepted != null
                ? getString(R.string.stat_acceptance_accepted, accepted)
                : getString(R.string.stat_acceptance_no_data));
        tvSwapsRejected.setText(rejected != null
                ? getString(R.string.stat_acceptance_rejected, rejected)
                : getString(R.string.stat_acceptance_no_data));
    }

    private void bindNurseHoursCard(StatisticsData data) {
        List<NurseHours> list = data.getAvgHoursPerNurse();
        if (list == null || list.isEmpty()) return;

        llNurseHours.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < list.size(); i++) {
            NurseHours nh = list.get(i);
            View row = inflater.inflate(R.layout.item_nurse_hours, llNurseHours, false);

            TextView tvName = row.findViewById(R.id.tv_nurse_name);
            TextView tvHours = row.findViewById(R.id.tv_nurse_hours);
            View divider = row.findViewById(R.id.divider);

            tvName.setText(nh.getName() != null ? nh.getName() : "—");
            double hours = nh.getHours() != null ? nh.getHours() : 0;
            tvHours.setText((int) Math.round(hours) + "h");

            divider.setVisibility(i < list.size() - 1 ? View.VISIBLE : View.GONE);
            llNurseHours.addView(row);
        }
    }

    private static Locale getAppLocale() {
        androidx.core.os.LocaleListCompat list = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
        return list.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(list.toLanguageTags());
    }

    private String buildMonthLabel() {
        Locale locale = getAppLocale();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, currentMonth - 1);
        cal.set(Calendar.YEAR, currentYear);
        String month = new SimpleDateFormat("MMMM", locale).format(cal.getTime()).toUpperCase(locale);
        return month + " " + getString(R.string.date_of_separator) + " " + currentYear;
    }

    private void updateNextMonthButton() {
        if (btnNextMonth == null) return;
        Calendar now = Calendar.getInstance();
        boolean isCurrentMonth = currentMonth == now.get(Calendar.MONTH) + 1 && currentYear == now.get(Calendar.YEAR);
        btnNextMonth.setEnabled(!isCurrentMonth);
        btnNextMonth.setAlpha(isCurrentMonth ? 0.35f : 1f);
    }

    private void navigateMonth(int delta) {
        currentMonth += delta;
        if (currentMonth < 1) { currentMonth = 12; currentYear--; }
        else if (currentMonth > 12) { currentMonth = 1; currentYear++; }
        if (tvMonthLabel != null) tvMonthLabel.setText(buildMonthLabel());
        updateNextMonthButton();
        loadStatistics();
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
        if (tvQualityCardTitle != null) tvQualityCardTitle.setText(R.string.stat_quality_title);
        if (tvQualityNote != null) tvQualityNote.setText(R.string.stat_quality_note);
        if (tvQualityBasedOn != null) tvQualityBasedOn.setText(R.string.stat_quality_based_on);
        if (tvLabelBreakdownSwaps != null) tvLabelBreakdownSwaps.setText(R.string.stat_breakdown_swaps);
        if (tvLabelBreakdownMinNurses != null) tvLabelBreakdownMinNurses.setText(R.string.stat_breakdown_min_nurses);
        if (tvLabelBreakdownPrefType != null) tvLabelBreakdownPrefType.setText(R.string.stat_breakdown_pref_type);
        if (tvLabelBreakdownPrefWeekend != null) tvLabelBreakdownPrefWeekend.setText(R.string.stat_breakdown_pref_weekend);
        if (tvAcceptanceTitle != null) tvAcceptanceTitle.setText(R.string.stat_acceptance_title);
        if (tvAcceptanceLegend != null) tvAcceptanceLegend.setText(R.string.stat_acceptance_legend);
        if (tvHoursTitle != null) tvHoursTitle.setText(R.string.stat_hours_title);
        if (tvHoursSubtitle != null) tvHoursSubtitle.setText(R.string.stat_hours_subtitle);
        if (tvHoursColName != null) tvHoursColName.setText(R.string.stat_hours_col_name);
        if (tvHoursColHours != null) tvHoursColHours.setText(R.string.stat_hours_col_hours);
        loadStatistics();
    }
}
