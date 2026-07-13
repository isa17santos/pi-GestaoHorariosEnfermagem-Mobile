package com.pi.gestaohorariosenfermagemmobile;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
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
    private TextView tvQualityCardTitle, tvQualityBadge, tvQualityScore, tvQualityNote, tvQualityBasedOn;
    private View viewScoreMarker;
    private TextView tvBreakdownSwaps, tvBreakdownMinNurses, tvBreakdownPrefType, tvBreakdownPrefWeekend;
    private TextView tvLabelBreakdownSwaps, tvLabelBreakdownMinNurses, tvLabelBreakdownPrefType, tvLabelBreakdownPrefWeekend;
    // Card Aceitação
    private CircularProgressIndicator progressAcceptance;
    private TextView tvAcceptanceTitle, tvAcceptanceRate, tvAcceptanceLegend;
    // Card Horas
    private LinearLayout llNurseHours;
    private TextView tvHoursTitle, tvHoursSubtitle, tvHoursColName, tvHoursColHours;
    private TextView tvLegendAbove, tvLegendBelow, tvLegendThreshold, tvAvgHours;
    private View dotAboveAvg, dotBelowAvg;

    private String token;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_nurse_statistics);

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
        tvQualityCardTitle = findViewById(R.id.tv_quality_card_title);
        tvQualityBadge = findViewById(R.id.tv_quality_badge);
        tvQualityScore = findViewById(R.id.tv_quality_score);
        tvQualityNote = findViewById(R.id.tv_quality_note);
        tvQualityBasedOn = findViewById(R.id.tv_quality_based_on);
        viewScoreMarker = findViewById(R.id.view_score_marker);
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
        llNurseHours = findViewById(R.id.ll_nurse_hours);
        tvHoursTitle = findViewById(R.id.tv_hours_title);
        tvHoursSubtitle = findViewById(R.id.tv_hours_subtitle);
        tvHoursColName = findViewById(R.id.tv_hours_col_name);
        tvHoursColHours = findViewById(R.id.tv_hours_col_hours);
        tvLegendAbove = findViewById(R.id.tv_legend_above);
        tvLegendBelow = findViewById(R.id.tv_legend_below);
        tvLegendThreshold = findViewById(R.id.tv_legend_threshold);
        tvAvgHours = findViewById(R.id.tv_avg_hours);
        dotAboveAvg = findViewById(R.id.dot_above_avg);
        dotBelowAvg = findViewById(R.id.dot_below_avg);

        // Dots das legendas com as cores correctas
        setOvalColor(dotAboveAvg, getColor(R.color.quality_medium));
        setOvalColor(dotBelowAvg, getColor(R.color.primary_strong));
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

                bindQualityCard(data);
                bindAcceptanceCard(data);
                bindNurseHoursCard(data);
                sectionStats.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<StatisticsResponse> call, Throwable t) {
                sectionStats.setVisibility(View.GONE);
            }
        });
    }

    private void bindQualityCard(StatisticsData data) {
        Integer score = data.getQualityScore();
        String indicator = data.getQualityIndicator();

        int badgeTextColor, badgeBgColor;
        String badgeText;

        if (indicator != null && (indicator.equalsIgnoreCase("good") || indicator.equalsIgnoreCase("bom"))) {
            badgeText = getString(R.string.stat_quality_good);
            badgeTextColor = getColor(R.color.quality_good);
            badgeBgColor = getColor(R.color.quality_good_bg);
        } else if (indicator != null && (indicator.equalsIgnoreCase("medium") || indicator.equalsIgnoreCase("médio"))) {
            badgeText = getString(R.string.stat_quality_medium);
            badgeTextColor = getColor(R.color.quality_medium);
            badgeBgColor = getColor(R.color.quality_medium_bg);
        } else {
            badgeText = getString(R.string.stat_quality_bad);
            badgeTextColor = getColor(R.color.quality_bad);
            badgeBgColor = getColor(R.color.quality_bad_bg);
        }

        tvQualityBadge.setText(badgeText);
        tvQualityBadge.setTextColor(badgeTextColor);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setColor(badgeBgColor);
        badgeBg.setCornerRadius(dpToPx(24));
        tvQualityBadge.setBackground(badgeBg);

        tvQualityScore.setText(score != null
                ? getString(R.string.stat_quality_score_label, score)
                : getString(R.string.stat_quality_score_label_empty));

        if (score != null) {
            final int clampedScore = Math.max(0, Math.min(100, score));
            viewScoreMarker.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    viewScoreMarker.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    View bar = viewScoreMarker.getParent() instanceof FrameLayout
                            ? (View) viewScoreMarker.getParent() : null;
                    if (bar == null) return;
                    int barWidth = bar.getWidth();
                    int markerWidth = viewScoreMarker.getWidth();
                    int leftOffset = (int) (barWidth * clampedScore / 100f) - markerWidth / 2;
                    leftOffset = Math.max(0, Math.min(leftOffset, barWidth - markerWidth));
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) viewScoreMarker.getLayoutParams();
                    lp.leftMargin = leftOffset;
                    viewScoreMarker.setLayoutParams(lp);
                }
            });
        }

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
    }

    private void bindNurseHoursCard(StatisticsData data) {
        List<NurseHours> list = data.getAvgHoursPerNurse();
        if (list == null || list.isEmpty()) return;

        // Calcula média
        double sum = 0;
        int count = 0;
        for (NurseHours nh : list) {
            if (nh.getHours() != null) {
                sum += nh.getHours();
                count++;
            }
        }
        double avg = count > 0 ? sum / count : 0;
        double threshold = avg * 0.15;

        // Label da média
        String avgFormatted = avg == Math.floor(avg)
                ? String.valueOf((int) avg)
                : String.format(new Locale("pt", "PT"), "%.1f", avg);
        tvAvgHours.setText(getString(R.string.stat_hours_avg_label, avgFormatted));

        // Constrói as linhas programaticamente
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

            int color;
            if (hours > avg + threshold) {
                color = getColor(R.color.quality_medium);    // laranja — acima
            } else if (hours < avg - threshold) {
                color = getColor(R.color.primary_strong);    // roxo — abaixo
            } else {
                color = getColor(R.color.text_primary);
            }
            tvName.setTextColor(color);
            tvHours.setTextColor(color);

            divider.setVisibility(i < list.size() - 1 ? View.VISIBLE : View.GONE);

            llNurseHours.addView(row);
        }
    }

    private void setOvalColor(View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        v.setBackground(d);
    }

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
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
        if (tvLegendAbove != null) tvLegendAbove.setText(R.string.stat_hours_legend_above);
        if (tvLegendBelow != null) tvLegendBelow.setText(R.string.stat_hours_legend_below);
        if (tvLegendThreshold != null) tvLegendThreshold.setText(R.string.stat_hours_legend_threshold);
        loadStatistics();
    }
}
