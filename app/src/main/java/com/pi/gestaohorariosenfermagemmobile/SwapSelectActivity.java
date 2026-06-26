package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SwapSelectActivity extends BaseActivity {

    private static final int DP_PER_HOUR = 20;

    private Calendar currentCalendar = Calendar.getInstance();
    private List<Shift> allShifts = new ArrayList<>();

    private LinearLayout rowDaysHeader, rowAlldayContent, colHours, gridBackgroundLines, rowShiftColumns;
    private TextView tvWeekRange;
    private View loader;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_select);

        rowDaysHeader = findViewById(R.id.row_days_header);
        rowAlldayContent = findViewById(R.id.row_allday_content);
        colHours = findViewById(R.id.col_hours);
        gridBackgroundLines = findViewById(R.id.grid_background_lines);
        rowShiftColumns = findViewById(R.id.row_shift_columns);
        tvWeekRange = findViewById(R.id.tv_week_range);
        loader = findViewById(R.id.loader);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_today).setOnClickListener(v -> {
            currentCalendar = Calendar.getInstance();
            fetchData();
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            fetchData();
        });
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            fetchData();
        });

        navbarManager = new NavbarManager(this);
        renderBaseGrid();
        updateUIStrings();
        fetchData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void fetchData() {
        if (loader != null) loader.setVisibility(View.VISIBLE);

        Calendar start = (Calendar) currentCalendar.clone();
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }
        updateWeekLabel(start);

        String token = "Bearer " + getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShifts(token, true, null).enqueue(new Callback<ShiftsResponse>() {
            @Override
            public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                if (loader != null) loader.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allShifts = response.body().getData();
                } else {
                    allShifts = new ArrayList<>();
                }
                renderWeeklyData();
            }

            @Override
            public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                if (loader != null) loader.setVisibility(View.GONE);
                Toast.makeText(SwapSelectActivity.this, getString(R.string.err_loading_swaps), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderBaseGrid() {
        colHours.removeAllViews();
        gridBackgroundLines.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int hourHeight = (int) (DP_PER_HOUR * density);
        int lineHeight = Math.max(1, (int) (1 * density));

        for (int i = 0; i < 24; i++) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hourHeight));
            tv.setText(String.format(Locale.getDefault(), "%02d:00", i));
            tv.setTextSize(11);
            tv.setTextColor(Color.parseColor("#64748B"));
            tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP);
            tv.setPadding(0, (int) (2 * density), 0, 0);
            colHours.addView(tv);

            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, lineHeight));
            line.setBackgroundColor(Color.parseColor("#E2E8F0"));

            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hourHeight - lineHeight));

            gridBackgroundLines.addView(line);
            gridBackgroundLines.addView(spacer);
        }
    }

    private void renderWeeklyData() {
        rowDaysHeader.removeAllViews();
        rowAlldayContent.removeAllViews();
        rowShiftColumns.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int lineWidth = Math.max(1, (int) (1 * density));
        String gridColor = "#E2E8F0";

        LinearLayout gridContainer = findViewById(R.id.grid_container);
        if (gridContainer != null) {
            gridContainer.setDividerDrawable(new ColorDrawable(Color.parseColor(gridColor)));
            gridContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        }

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        for (int i = 0; i < 7; i++) {
            String dayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            addVerticalDivider(lineWidth, gridColor);
            addDayHeader((Calendar) cal.clone());

            LinearLayout alldayCol = new LinearLayout(this);
            alldayCol.setOrientation(LinearLayout.VERTICAL);
            alldayCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            renderAllDayShiftsForDay(dayStr, alldayCol);
            rowAlldayContent.addView(alldayCol);

            FrameLayout dayColumn = new FrameLayout(this);
            dayColumn.setLayoutParams(new LinearLayout.LayoutParams(0, (int) (24 * DP_PER_HOUR * density), 1));
            renderTimedShiftsForDay(dayStr, dayColumn, (Calendar) cal.clone());
            rowShiftColumns.addView(dayColumn);

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        addVerticalDivider(lineWidth, gridColor);
    }

    private void addVerticalDivider(int width, String color) {
        int c = Color.parseColor(color);
        View v1 = new View(this);
        v1.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        v1.setBackgroundColor(c);
        rowDaysHeader.addView(v1);

        View v2 = new View(this);
        v2.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        v2.setBackgroundColor(c);
        rowAlldayContent.addView(v2);

        View v3 = new View(this);
        v3.setLayoutParams(new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        v3.setBackgroundColor(c);
        rowShiftColumns.addView(v3);
    }

    private void addDayHeader(Calendar cal) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_schedule_day_header, rowDaysHeader, false);
        Locale locale = AppCompatDelegate.getApplicationLocales().isEmpty()
                ? Locale.getDefault()
                : Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags());

        TextView tvName = view.findViewById(R.id.tv_day_name);
        TextView tvDate = view.findViewById(R.id.tv_day_date);

        String dayName = new SimpleDateFormat("EEEE", locale).format(cal.getTime()).toUpperCase();
        if (locale.getLanguage().equals("pt")) {
            dayName = dayName.replace("-FEIRA", "");
        }
        tvName.setText(dayName);
        tvDate.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        if (isToday(cal)) {
            tvDate.setBackgroundResource(R.drawable.bg_circle_primary);
            tvDate.setTextColor(Color.WHITE);
            tvName.setTextColor(Color.parseColor("#7C3AED"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            tvDate.setBackground(null);
            tvDate.setTextColor(Color.parseColor("#1E293B"));
            tvName.setTextColor(Color.parseColor("#64748B"));
            tvName.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        view.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        rowDaysHeader.addView(view);
    }

    private void renderAllDayShiftsForDay(String date, LinearLayout container) {
        float density = getResources().getDisplayMetrics().density;
        for (Shift shift : allShifts) {
            if (shift.getDate().equals(date) && isAllDay(shift.getShiftType())) {
                ShiftType type = shift.getShiftType();
                int bgColor = Color.parseColor(type.getColor());

                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                card.setCardBackgroundColor(bgColor);
                card.setRadius(dpToPx(8));
                card.setCardElevation(0);
                card.setStrokeWidth(0);

                LinearLayout hLayout = new LinearLayout(this);
                hLayout.setOrientation(LinearLayout.HORIZONTAL);

                View detailBar = new View(this);
                detailBar.setLayoutParams(new LinearLayout.LayoutParams((int) dpToPx(4), ViewGroup.LayoutParams.MATCH_PARENT));
                detailBar.setBackgroundColor(manipulateColor(bgColor, 0.8f));

                View content = LayoutInflater.from(this).inflate(R.layout.item_schedule_shift, hLayout, false);
                ((com.google.android.material.card.MaterialCardView) content).setCardBackgroundColor(Color.TRANSPARENT);
                ((com.google.android.material.card.MaterialCardView) content).setCardElevation(0);
                content.findViewById(R.id.tv_shift_time).setVisibility(View.GONE);
                ((TextView) content.findViewById(R.id.tv_shift_name)).setText(translateShiftName(type.getName()));
                ((TextView) content.findViewById(R.id.tv_nurse_count)).setText(String.valueOf(shift.getUsers().size()));

                hLayout.addView(detailBar);
                hLayout.addView(content);
                card.addView(hLayout);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int) (4 * density), (int) (4 * density), (int) (4 * density), (int) (4 * density));
                card.setLayoutParams(lp);
                container.addView(card);
            }
        }
    }

    private void renderTimedShiftsForDay(String date, FrameLayout column, Calendar dayCal) {
        float density = getResources().getDisplayMetrics().density;

        for (Shift shift : allShifts) {
            if (shift.getDate().equals(date) && !isAllDay(shift.getShiftType())) {
                ShiftType type = shift.getShiftType();
                int bgColor = Color.parseColor(type.getColor());

                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                card.setCardBackgroundColor(bgColor);
                card.setRadius(dpToPx(12));
                card.setCardElevation(0);
                card.setStrokeWidth(0);

                LinearLayout hLayout = new LinearLayout(this);
                hLayout.setOrientation(LinearLayout.HORIZONTAL);

                View detailBar = new View(this);
                detailBar.setLayoutParams(new LinearLayout.LayoutParams((int) dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT));
                detailBar.setBackgroundColor(manipulateColor(bgColor, 0.8f));

                View content = LayoutInflater.from(this).inflate(R.layout.item_schedule_shift, hLayout, false);
                ((com.google.android.material.card.MaterialCardView) content).setCardBackgroundColor(Color.TRANSPARENT);
                ((com.google.android.material.card.MaterialCardView) content).setCardElevation(0);
                ((TextView) content.findViewById(R.id.tv_shift_name)).setText(translateShiftName(type.getName()));
                ((TextView) content.findViewById(R.id.tv_shift_time)).setText(
                        type.getStartTime().substring(0, 5) + " - " + type.getEndTime().substring(0, 5));
                ((TextView) content.findViewById(R.id.tv_nurse_count)).setText(String.valueOf(shift.getUsers().size()));

                hLayout.addView(detailBar);
                hLayout.addView(content);
                card.addView(hLayout);

                int startMin = getMinutes(type.getStartTime());
                int endMin = getMinutes(type.getEndTime());
                if (endMin <= startMin) endMin = 1440;

                int top = (int) (startMin * (DP_PER_HOUR / 60.0) * density);
                int height = (int) ((endMin - startMin) * (DP_PER_HOUR / 60.0) * density);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                lp.setMargins((int) (4 * density), top, (int) (4 * density), 0);
                card.setLayoutParams(lp);

                // Only future shifts are tappable (today and past are not)
                if (isFutureDate(date)) {
                    final int shiftId = shift.getId();
                    final int shiftTypeId = type.getId();
                    final String shiftInfo = translateShiftName(type.getName()) + " · " + date;
                    card.setOnClickListener(v -> openSwapIntentSheet(shiftId, shiftTypeId, shiftInfo));
                    card.setClickable(true);
                    card.setFocusable(true);
                } else {
                    card.setAlpha(0.5f);
                    card.setClickable(false);
                }

                column.addView(card);
            }
        }

        // Current time red line
        if (isToday(dayCal)) {
            Calendar now = Calendar.getInstance();
            int minutesPassed = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            float topPos = minutesPassed * (DP_PER_HOUR / 60.0f) * density;

            View timeLine = new View(this);
            timeLine.setBackgroundColor(Color.parseColor("#EF4444"));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (1.5f * density));
            lp.topMargin = (int) topPos;
            timeLine.setLayoutParams(lp);

            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(Color.parseColor("#EF4444"));

            View circle = new View(this);
            circle.setBackground(dot);
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams((int) (8 * density), (int) (8 * density));
            clp.topMargin = (int) topPos - (int) (3.5f * density);
            clp.leftMargin = -(int) (4 * density);
            circle.setLayoutParams(clp);

            column.addView(timeLine);
            column.addView(circle);
        }
    }

    private void openSwapIntentSheet(int shiftId, int shiftTypeId, String shiftInfo) {
        SwapIntentBottomSheet sheet = SwapIntentBottomSheet.newInstance(shiftId, shiftTypeId, shiftInfo);
        sheet.setOnIntentSelectedListener(new SwapIntentBottomSheet.OnIntentSelectedListener() {
            @Override
            public void onShiftForShift(int sId, int stId) {
                navigateToSwapCreate(sId, "shift", stId);
            }

            @Override
            public void onShiftForDayoff(int sId, int stId) {
                navigateToSwapCreate(sId, "shift_for_dayoff", stId);
            }
        });
        sheet.show(getSupportFragmentManager(), "swap_intent");
    }

    private void navigateToSwapCreate(int shiftId, String mode, int shiftTypeId) {
        Intent intent = new Intent(this, SwapCreateActivity.class);
        intent.putExtra("SHIFT_ID", shiftId);
        intent.putExtra("MODE", mode);
        intent.putExtra("SHIFT_TYPE_ID", shiftTypeId);
        startActivity(intent);
    }

    private void updateWeekLabel(Calendar start) {
        Calendar cal = (Calendar) start.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar end = (Calendar) cal.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        Locale locale = AppCompatDelegate.getApplicationLocales().isEmpty()
                ? Locale.getDefault()
                : Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags());

        SimpleDateFormat sdf = new SimpleDateFormat("d", locale);
        SimpleDateFormat sdfMY = new SimpleDateFormat("MMM. yyyy", locale);
        tvWeekRange.setText(sdf.format(cal.getTime()) + " - " + sdf.format(end.getTime()) + " " + sdfMY.format(end.getTime()));
    }

    private boolean isFutureDate(String dateStr) {
        try {
            java.util.Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            if (date == null) return false;
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            return date.after(today.getTime());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isToday(Calendar cal) {
        Calendar today = Calendar.getInstance();
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isAllDay(ShiftType type) {
        if (type == null) return false;
        String start = type.getStartTime();
        String end = type.getEndTime();
        boolean isTimeAllDay = start != null && end != null && start.startsWith("00:00") && end.startsWith("00:00");
        String name = type.getName().toLowerCase();
        boolean isNameAllDay = name.contains("folga") || name.contains("férias") || name.contains("baixa")
                || name.contains("licença") || name.contains("dayoff") || name.contains("holidays")
                || name.contains("day off") || name.contains("sick") || name.contains("parental");
        return isTimeAllDay || isNameAllDay;
    }

    private int getMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    private String translateShiftName(String name) {
        if (name == null) return "";
        switch (name.toLowerCase().trim()) {
            case "morning":  case "manhã":   return getString(R.string.shift_morning);
            case "afternoon": case "tarde":  return getString(R.string.shift_afternoon);
            case "night":    case "noite":   return getString(R.string.shift_night);
            case "dayoff":   case "folga":   return getString(R.string.shift_dayoff);
            case "holidays": case "férias":  return getString(R.string.shift_holidays);
            case "sick leave": case "baixa": return getString(R.string.shift_sick_leave);
            case "parental leave": case "licença": return getString(R.string.shift_parental_leave);
            default: return name;
        }
    }

    @Override
    protected void updateUIStrings() {
        TextView tvTitle = findViewById(R.id.tv_page_title);
        TextView tvSubtitle = findViewById(R.id.tv_page_subtitle);
        TextView tvBanner = findViewById(R.id.tv_banner_text);
        MaterialButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnToday = findViewById(R.id.btn_today);

        if (tvTitle != null) tvTitle.setText(R.string.swap_select_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.swap_select_subtitle);
        if (tvBanner != null) tvBanner.setText(R.string.swap_select_banner);
        if (btnBack != null) btnBack.setText(R.string.back);
        if (btnToday != null) btnToday.setText(R.string.today);

        updateWeekLabel(currentCalendar);

        if (!allShifts.isEmpty()) {
            renderWeeklyData();
        }
    }

}
