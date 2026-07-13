package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NurseDashboardActivity extends BaseActivity {

    private TextView tvGreeting, tvNurseSubtitle;
    private TextView tvSwapBadge;
    private View sectionDashboardSummary;
    private TextView tvSummaryTitle;
    private TextView tvMonthlyHours, tvSwapsThisMonth;
    private TextView tvBreakdownMorning, tvBreakdownAfternoon, tvBreakdownNight, tvBreakdownDayoff;
    private View dotMorning, dotAfternoon, dotNight, dotDayoff;
    private View cardHours, cardSwapsCount;
    private TextView tvLabelMyShifts, tvLabelShiftsByType;
    private TextView tvLabelShiftMorning, tvLabelShiftAfternoon, tvLabelShiftNight, tvLabelShiftDayoff;
    private TextView tvLabelHours, tvLabelSwaps;
    private TextView tvTodayLabel, tvTodayShiftName, tvTodayShiftHours;
    private TextView tvTomorrowLabel, tvTomorrowShiftName, tvTomorrowShiftHours;
    private View viewTodayColorBar, viewTomorrowColorBar;
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
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
        loadPendingSwapsBadge();
        loadShiftTypeColors();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvNurseSubtitle = findViewById(R.id.tv_nurse_subtitle);
        tvSwapBadge = findViewById(R.id.tv_swap_badge);
        sectionDashboardSummary = findViewById(R.id.section_dashboard_summary);
        tvSummaryTitle = findViewById(R.id.tv_summary_title);
        tvMonthlyHours = findViewById(R.id.tv_monthly_hours);
        tvSwapsThisMonth = findViewById(R.id.tv_swaps_this_month);
        tvBreakdownMorning = findViewById(R.id.tv_breakdown_morning);
        tvBreakdownAfternoon = findViewById(R.id.tv_breakdown_afternoon);
        tvBreakdownNight = findViewById(R.id.tv_breakdown_night);
        tvBreakdownDayoff = findViewById(R.id.tv_breakdown_dayoff);
        dotMorning = findViewById(R.id.dot_morning);
        dotAfternoon = findViewById(R.id.dot_afternoon);
        dotNight = findViewById(R.id.dot_night);
        dotDayoff = findViewById(R.id.dot_dayoff);
        cardHours = findViewById(R.id.card_hours);
        cardSwapsCount = findViewById(R.id.card_swaps_count);
        tvLabelMyShifts = findViewById(R.id.tv_label_my_shifts);
        tvLabelShiftsByType = findViewById(R.id.tv_label_shifts_by_type);
        tvLabelShiftMorning = findViewById(R.id.tv_label_shift_morning);
        tvLabelShiftAfternoon = findViewById(R.id.tv_label_shift_afternoon);
        tvLabelShiftNight = findViewById(R.id.tv_label_shift_night);
        tvLabelShiftDayoff = findViewById(R.id.tv_label_shift_dayoff);
        tvLabelHours = findViewById(R.id.tv_label_hours);
        tvLabelSwaps = findViewById(R.id.tv_label_swaps);
        tvTodayLabel = findViewById(R.id.tv_today_label);
        tvTodayShiftName = findViewById(R.id.tv_today_shift_name);
        tvTodayShiftHours = findViewById(R.id.tv_today_shift_hours);
        viewTodayColorBar = findViewById(R.id.view_today_color_bar);
        tvTomorrowLabel = findViewById(R.id.tv_tomorrow_label);
        tvTomorrowShiftName = findViewById(R.id.tv_tomorrow_shift_name);
        tvTomorrowShiftHours = findViewById(R.id.tv_tomorrow_shift_hours);
        viewTomorrowColorBar = findViewById(R.id.view_tomorrow_color_bar);
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

    private void loadDashboardSummary() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getDashboard("Bearer " + token).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    sectionDashboardSummary.setVisibility(View.GONE);
                    return;
                }
                DashboardData data = response.body().getData();
                if (data == null) {
                    sectionDashboardSummary.setVisibility(View.GONE);
                    return;
                }

                Double hours = data.getMonthlyHours();
                tvMonthlyHours.setText(hours != null ? Math.round(hours) + "h" : "—");

                Integer swaps = data.getSwapsThisMonth();
                tvSwapsThisMonth.setText(swaps != null ? String.valueOf(swaps) : "—");

                Map<String, Integer> breakdown = data.getShiftTypeBreakdown();
                tvBreakdownMorning.setText(formatShiftCount(breakdown, "morning"));
                tvBreakdownAfternoon.setText(formatShiftCount(breakdown, "afternoon"));
                tvBreakdownNight.setText(formatShiftCount(breakdown, "night"));
                tvBreakdownDayoff.setText(formatShiftCount(breakdown, "dayOff"));

                sectionDashboardSummary.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                sectionDashboardSummary.setVisibility(View.GONE);
            }
        });
    }

    private void loadTodayTomorrowShifts() {
        Calendar cal = Calendar.getInstance();
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        // Pré-popular as labels de data já
        tvTodayLabel.setText(buildDayLabel(Calendar.getInstance()));
        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
        tvTomorrowLabel.setText(buildDayLabel(tomorrowCal));

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getWeeklySchedule("Bearer " + token, todayStr, "personal").enqueue(new Callback<WeeklyScheduleResponse>() {
            @Override
            public void onResponse(Call<WeeklyScheduleResponse> call, Response<WeeklyScheduleResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null) return;

                List<Shift> shifts = response.body().getData().getShifts();
                if (shifts == null) return;

                String userNameNorm = currentUserName.trim().toLowerCase();
                Shift todayShift = null;
                Shift tomorrowShift = null;

                for (Shift s : shifts) {
                    if (s.getUsers() == null) continue;
                    boolean mine = false;
                    for (User u : s.getUsers()) {
                        if (u.getName().trim().toLowerCase().equals(userNameNorm)) {
                            mine = true;
                            break;
                        }
                    }
                    if (!mine) continue;
                    if (todayStr.equals(s.getDate())) todayShift = s;
                    else if (tomorrowStr.equals(s.getDate())) tomorrowShift = s;
                }

                bindDayShift(viewTodayColorBar, tvTodayShiftName, tvTodayShiftHours, todayShift);
                if (tomorrowShift != null) {
                    bindDayShift(viewTomorrowColorBar, tvTomorrowShiftName, tvTomorrowShiftHours, tomorrowShift);
                } else {
                    // Amanhã pode estar noutra semana
                    fetchTomorrowFromNextWeek(tomorrowStr, userNameNorm);
                }
            }

            @Override
            public void onFailure(Call<WeeklyScheduleResponse> call, Throwable t) { }
        });
    }

    private void fetchTomorrowFromNextWeek(String tomorrowStr, String userNameNorm) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getWeeklySchedule("Bearer " + token, tomorrowStr, "personal").enqueue(new Callback<WeeklyScheduleResponse>() {
            @Override
            public void onResponse(Call<WeeklyScheduleResponse> call, Response<WeeklyScheduleResponse> response) {
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null) return;
                List<Shift> shifts = response.body().getData().getShifts();
                if (shifts == null) return;
                for (Shift s : shifts) {
                    if (!tomorrowStr.equals(s.getDate()) || s.getUsers() == null) continue;
                    for (User u : s.getUsers()) {
                        if (u.getName().trim().toLowerCase().equals(userNameNorm)) {
                            bindDayShift(viewTomorrowColorBar, tvTomorrowShiftName, tvTomorrowShiftHours, s);
                            return;
                        }
                    }
                }
                // Sem turno encontrado — mostrar estado vazio
                bindDayShift(viewTomorrowColorBar, tvTomorrowShiftName, tvTomorrowShiftHours, null);
            }
            @Override
            public void onFailure(Call<WeeklyScheduleResponse> call, Throwable t) { }
        });
    }

    private void loadShiftTypeColors() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftTypes("Bearer " + token).enqueue(new Callback<ShiftTypesResponse>() {
            @Override
            public void onResponse(Call<ShiftTypesResponse> call, Response<ShiftTypesResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    applyBreakdownDotColors(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ShiftTypesResponse> call, Throwable t) { }
        });
    }

    private void applyBreakdownDotColors(List<ShiftType> shiftTypes) {
        for (ShiftType st : shiftTypes) {
            if (st.getName() == null || st.getColor() == null) continue;
            String key = st.getName().toLowerCase().trim();
            switch (key) {
                case "morning":   case "manhã":   setDotColor(dotMorning, st.getColor());   break;
                case "afternoon": case "tarde":   setDotColor(dotAfternoon, st.getColor()); break;
                case "night":     case "noite":   setDotColor(dotNight, st.getColor());     break;
                case "dayoff":    case "folga":   setDotColor(dotDayoff, st.getColor());    break;
            }
        }
    }

    private void setDotColor(View dot, String hexColor) {
        if (dot == null) return;
        try {
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(hexColor));
            dot.setBackground(circle);
        } catch (Exception ignored) { }
    }

    private void bindDayShift(View colorBar, TextView tvName, TextView tvHours, Shift shift) {
        if (shift == null || shift.getShiftType() == null) {
            tvName.setText(getString(R.string.shift_dayoff));
            colorBar.setBackgroundColor(getColor(R.color.primary_soft));
            tvHours.setText("");
            return;
        }
        ShiftType st = shift.getShiftType();
        tvName.setText(translateShiftName(st.getName()));

        // Cor do turno aplicada à barra lateral; texto sempre em primary_strong
        try {
            colorBar.setBackgroundColor(Color.parseColor(st.getColor()));
        } catch (Exception e) {
            colorBar.setBackgroundColor(getColor(R.color.primary_soft));
        }

        String key = st.getName() != null ? st.getName().toLowerCase().trim() : "";
        if (key.equals("dayoff") || key.equals("folga")) {
            tvHours.setText(getString(R.string.all_day_label).replace("\n", " "));
            return;
        }
        String start = st.getStartTime();
        String end = st.getEndTime();
        if (start != null && end != null) {
            tvHours.setText(formatTime(start) + "h – " + formatTime(end) + "h");
        } else {
            tvHours.setText("");
        }
    }

    private static Locale getAppLocale() {
        androidx.core.os.LocaleListCompat list = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
        return list.isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(list.toLanguageTags());
    }

    private String buildDayLabel(Calendar cal) {
        Locale locale = getAppLocale();
        String weekday = new SimpleDateFormat("EEEE", locale).format(cal.getTime()).toUpperCase(locale);
        String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
        String month = new SimpleDateFormat("MMMM", locale).format(cal.getTime()).toUpperCase(locale);
        boolean isToday = isSameDay(cal, Calendar.getInstance());
        String prefix = isToday
                ? getString(R.string.dashboard_summary_today)
                : getString(R.string.dashboard_summary_tomorrow);
        return prefix + " – " + weekday + " – " + day + " " + getString(R.string.date_of_separator) + " " + month;
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String formatTime(String time) {
        if (time != null && time.length() >= 5) return time.substring(0, 5);
        return time != null ? time : "";
    }

    private String formatShiftCount(Map<String, Integer> breakdown, String key) {
        int count = (breakdown != null && breakdown.containsKey(key)) ? breakdown.get(key) : 0;
        return count + " " + getString(R.string.dashboard_summary_shifts_unit);
    }

    private String translateShiftName(String name) {
        if (name == null) return "";
        switch (name.toLowerCase().trim()) {
            case "morning":   case "manhã":   return getString(R.string.shift_morning);
            case "afternoon": case "tarde":   return getString(R.string.shift_afternoon);
            case "night":     case "noite":   return getString(R.string.shift_night);
            case "dayoff":    case "folga":   return getString(R.string.shift_dayoff);
            case "holidays":  case "férias":  return getString(R.string.shift_holidays);
            case "sick leave":case "baixa":   return getString(R.string.shift_sick_leave);
            case "parental leave": case "licença": return getString(R.string.shift_parental_leave);
            default: return name;
        }
    }

    @Override
    protected void updateUIStrings() {
        if (navbarManager != null) {
            navbarManager.updateRoleUI();
            navbarManager.updateLanguageUI();
        }
        tvGreeting.setText(getString(R.string.dashboard_nurse_greeting, currentUserName));
        tvNurseSubtitle.setText(R.string.dashboard_nurse_subtitle);
        updateCardText(R.id.card_schedule, R.string.schedule, R.string.schedule_subtitle);
        updateCardText(R.id.card_swaps, R.string.swaps, R.string.swaps_subtitle);
        if (tvLabelMyShifts != null) tvLabelMyShifts.setText(R.string.dashboard_summary_my_shifts);
        if (tvLabelShiftsByType != null) tvLabelShiftsByType.setText(R.string.dashboard_summary_shifts_by_type);
        if (tvLabelShiftMorning != null) tvLabelShiftMorning.setText(R.string.shift_morning);
        if (tvLabelShiftAfternoon != null) tvLabelShiftAfternoon.setText(R.string.shift_afternoon);
        if (tvLabelShiftNight != null) tvLabelShiftNight.setText(R.string.shift_night);
        if (tvLabelShiftDayoff != null) tvLabelShiftDayoff.setText(R.string.shift_dayoff);
        if (tvLabelHours != null) tvLabelHours.setText(R.string.dashboard_summary_hours_label);
        if (tvLabelSwaps != null) tvLabelSwaps.setText(R.string.dashboard_summary_swaps_label);
        if (tvSummaryTitle != null) tvSummaryTitle.setText(buildSummaryTitle());
        if (tvTodayLabel != null) tvTodayLabel.setText(buildDayLabel(Calendar.getInstance()));
        if (tvTomorrowLabel != null) {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            tvTomorrowLabel.setText(buildDayLabel(tomorrow));
        }
        // Limpa os textos dinâmicos dos turnos para evitar mostrar idioma antigo enquanto a API responde
        if (tvTodayShiftName != null) tvTodayShiftName.setText("—");
        if (tvTodayShiftHours != null) tvTodayShiftHours.setText("");
        if (tvTomorrowShiftName != null) tvTomorrowShiftName.setText("—");
        if (tvTomorrowShiftHours != null) tvTomorrowShiftHours.setText("");
        // Re-popula todos os dados dinâmicos com o idioma atualizado
        loadDashboardSummary();
        loadTodayTomorrowShifts();
    }

    private String buildSummaryTitle() {
        Locale locale = getAppLocale();
        Calendar cal = Calendar.getInstance();
        String month = new SimpleDateFormat("MMMM", locale).format(cal.getTime()).toUpperCase(locale);
        int year = cal.get(Calendar.YEAR);
        return getString(R.string.dashboard_summary_title) + " - " + month + " " + getString(R.string.date_of_separator) + " " + year;
    }

    private void updateCardText(int cardId, int titleRes, int subRes) {
        View card = findViewById(cardId);
        if (card != null) {
            ((TextView) card.findViewWithTag("title")).setText(titleRes);
            ((TextView) card.findViewWithTag("subtitle")).setText(subRes);
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.card_schedule).setOnClickListener(v ->
                startActivity(new Intent(this, ScheduleActivity.class)));
        findViewById(R.id.card_swaps).setOnClickListener(v ->
                startActivity(new Intent(this, SwapsActivity.class)));
    }
}
