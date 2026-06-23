package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;

import android.content.res.ColorStateList;

public class ScheduleActivity extends BaseActivity {

    private static final int DP_PER_HOUR = 20;
    private String currentViewMode = "personal";
    private Calendar currentCalendar = Calendar.getInstance();
    private List<Shift> allShifts = new ArrayList<>();

    private LinearLayout rowDaysHeader, rowAlldayContent, colHours, gridBackgroundLines, rowShiftColumns;
    private TextView tvWeekRange, tvLangFlag, tvLangLabel;
    private View loader;

    private List<ShiftType> cachedShiftTypes = new ArrayList<>();
    private Handler timeHandler = new Handler(Looper.getMainLooper());

    private NavbarManager navbarManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);
        initViews();
        setupListeners();
        renderBaseGrid();
        fetchData();

        navbarManager = new NavbarManager(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(navbarManager != null) navbarManager.refreshUnreadCount();
    }
    private void initViews() {
        rowDaysHeader = findViewById(R.id.row_days_header);
        rowAlldayContent = findViewById(R.id.row_allday_content);
        colHours = findViewById(R.id.col_hours);
        gridBackgroundLines = findViewById(R.id.grid_background_lines);
        rowShiftColumns = findViewById(R.id.row_shift_columns);
        tvWeekRange = findViewById(R.id.tv_week_range);

        loader = findViewById(R.id.loader);

        findViewById(R.id.btn_ical).setOnClickListener(v -> handleIcalExport());


        Button btnPrev = findViewById(R.id.btn_prev);


        // Carregar dados do utilizador das SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);

        // Configurar botões de navegação
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        MaterialButtonToggleGroup toggleGroup = findViewById(R.id.toggle_view);
        MaterialButton btnPersonal = findViewById(R.id.btn_personal_view);
        MaterialButton btnGlobal = findViewById(R.id.btn_global_view);

        // Listener para mudar os estilos dinamicamente
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateToggleStyles(checkedId, btnPersonal, btnGlobal);
                fetchData();
            }
        });

        // Aplicar estilo inicial (Vista Pessoal selecionada por defeito)
        updateToggleStyles(R.id.btn_personal_view, btnPersonal, btnGlobal);


        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        View banner = findViewById(R.id.banner_ical);
        if (banner != null) {
            findViewById(R.id.btn_close_banner).setOnClickListener(v -> banner.setVisibility(View.GONE));
        }

        updateUIStrings();
        setupBannerLink();
    }

    private Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            renderWeeklyData(); // Atualiza a posição da linha vermelha
            timeHandler.postDelayed(this, 60000); // Atualiza a cada minuto
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeRunnable); // Para o marcador ao sair
    }

    // Helper para criar o fundo arredondado com o detalhe lateral escuro
    private android.graphics.drawable.Drawable createShiftDrawable(String colorHex) {
        int color = Color.parseColor(colorHex);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dpToPx(12));
        // Simula o detalhe lateral mais escuro
        gd.setStroke((int)dpToPx(6), manipulateColor(color, 0.8f));
        return gd;
    }


    private void updateToggleStyles(int checkedId, MaterialButton btnPersonal, MaterialButton btnGlobal) {
        boolean isPersonal = checkedId == R.id.btn_personal_view;

        int colorActive = Color.parseColor("#7C3AED");   // Roxo
        int colorInactive = Color.parseColor("#64748B"); // Cinzento

        // Estilo para o botão Pessoal
        btnPersonal.setBackgroundTintList(ColorStateList.valueOf(isPersonal ? Color.WHITE : Color.TRANSPARENT));
        btnPersonal.setTextColor(isPersonal ? colorActive : colorInactive);
        btnPersonal.setIconTint(ColorStateList.valueOf(isPersonal ? colorActive : colorInactive));
        btnPersonal.setElevation(isPersonal ? 4f : 0f);

        // Estilo para o botão Global
        btnGlobal.setBackgroundTintList(ColorStateList.valueOf(!isPersonal ? Color.WHITE : Color.TRANSPARENT));
        btnGlobal.setTextColor(!isPersonal ? colorActive : colorInactive);
        btnGlobal.setIconTint(ColorStateList.valueOf(!isPersonal ? colorActive : colorInactive));
        btnGlobal.setElevation(!isPersonal ? 4f : 0f);
    }


    private void setupListeners() {
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
        findViewById(R.id.btn_personal_view).setOnClickListener(v -> {
            currentViewMode = "personal";
            fetchData();
        });
        findViewById(R.id.btn_global_view).setOnClickListener(v -> {
            currentViewMode = "global";
            fetchData();
        });
    }

    private void setupShiftLegend(List<ShiftType> shiftTypes) {
        ChipGroup chipGroup = findViewById(R.id.cg_shift_legend);
        if (chipGroup == null || shiftTypes == null) return;
        chipGroup.removeAllViews();

        for (ShiftType type : shiftTypes) {
            // TRADUÇÃO E CAPITALIZAÇÃO
            String translatedName = translateShiftName(type.getName());
            String label = translatedName.substring(0, 1).toUpperCase() + translatedName.substring(1).toLowerCase();

            String start = type.getStartTime();
            String end = type.getEndTime();
            if (start != null && end != null && !(start.startsWith("00:00") && end.startsWith("00:00"))) {
                label += String.format(" (%s - %s)", start.substring(0, 5), end.substring(0, 5));
            }

            int bgColor = Color.parseColor(type.getColor());
            com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
            card.setCardBackgroundColor(bgColor);
            card.setRadius(dpToPx(10));
            card.setCardElevation(dpToPx(2));
            card.setStrokeWidth(0);

            LinearLayout layout = new LinearLayout(this);
            View detailBar = new View(this);
            detailBar.setLayoutParams(new LinearLayout.LayoutParams((int)dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT));
            detailBar.setBackgroundColor(manipulateColor(bgColor, 0.8f));

            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(Color.parseColor("#2D2054"));
            tv.setPadding((int)dpToPx(12), (int)dpToPx(8), (int)dpToPx(16), (int)dpToPx(8));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);

            layout.addView(detailBar);
            layout.addView(tv);
            card.addView(layout);
            chipGroup.addView(card);
        }
    }

    // Helper para escurecer a cor da borda
    private int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    // Helper para converter dp em pixels
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void fetchData() {
        if (loader != null) loader.setVisibility(View.VISIBLE);

        Calendar start = (Calendar) currentCalendar.clone();
        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_YEAR, -1);
        }

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(start.getTime());
        updateWeekLabel(start);

        // Busca o token do SharedPreferences "AUTH" (conforme definido no teu RetrofitClient)
        String rawToken = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");
        String token = "Bearer " + rawToken;

        // 1. Carregar os Turnos da Semana (Grelha)
        fetchWeeklyData(token, dateStr);

        // 2. Carregar TODOS os tipos de turno (Legenda)
        fetchShiftTypes(token);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getWeeklySchedule(token, dateStr, currentViewMode)
                .enqueue(new Callback<WeeklyScheduleResponse>() {
                    @Override
                    public void onResponse(Call<WeeklyScheduleResponse> call, Response<WeeklyScheduleResponse> response) {
                        if (loader != null) loader.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allShifts = response.body().getData().getShifts();
                            setupShiftLegend(response.body().getData().getShiftTypes());

                            renderWeeklyData();
                        }
                    }

                    @Override
                    public void onFailure(Call<WeeklyScheduleResponse> call, Throwable t) {
                        if (loader != null) loader.setVisibility(View.GONE);
                        Toast.makeText(ScheduleActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchWeeklyData(String token, String dateStr) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getWeeklySchedule(token, dateStr, currentViewMode)
                .enqueue(new Callback<WeeklyScheduleResponse>() {
                    @Override
                    public void onResponse(Call<WeeklyScheduleResponse> call, Response<WeeklyScheduleResponse> response) {
                        if (loader != null) loader.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allShifts = response.body().getData().getShifts();
                            renderWeeklyData();
                            renderDistribution();
                        }
                    }

                    @Override
                    public void onFailure(Call<WeeklyScheduleResponse> call, Throwable t) {
                        if (loader != null) loader.setVisibility(View.GONE);
                        Toast.makeText(ScheduleActivity.this, "Erro ao carregar horário", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchShiftTypes(String token) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftTypes(token).enqueue(new Callback<ShiftTypesResponse>() {
            @Override
            public void onResponse(Call<ShiftTypesResponse> call, Response<ShiftTypesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedShiftTypes = response.body().getData();
                    setupShiftLegend(cachedShiftTypes);
                }
            }

            @Override
            public void onFailure(Call<ShiftTypesResponse> call, Throwable t) {
                // Falha silenciosa para a legenda
            }
        });
    }
    private void renderBaseGrid() {
        colHours.removeAllViews();
        gridBackgroundLines.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int hourHeight = (int) (DP_PER_HOUR * density);
        int lineHeight = Math.max(1, (int)(1 * density)); // Garante pelo menos 1px

        for (int i = 0; i < 24; i++) {
            // Texto da hora
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hourHeight));
            tv.setText(String.format(Locale.getDefault(), "%02d:00", i));
            tv.setTextSize(11);
            tv.setTextColor(Color.parseColor("#64748B"));
            tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP);
            tv.setPadding(0, (int)(2*density), 0, 0);
            colHours.addView(tv);

            // Linha Horizontal
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, lineHeight));
            line.setBackgroundColor(Color.parseColor("#E2E8F0"));

            // Espaço para completar a altura da hora
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
        int lineWidth = Math.max(1, (int)(1 * density));
        String gridColor = "#E2E8F0";

        // Ativar linhas horizontais entre as secções da tabela
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

            // Adicionar Linhas Verticais Sincronizadas (Cabeçalho, Dia Inteiro e Grelha)
            addVerticalDivider(lineWidth, gridColor);

            // Renderizar Conteúdos
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

        // Adicionar as últimas linhas verticais à direita para fechar a tabela
        addClosingLines(lineWidth, gridColor);
    }

    // Método auxiliar para criar as linhas verticais entre colunas
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

    private void addClosingLines(int width, String color) {
        addVerticalDivider(width, color);
    }

    private void addDayHeader(Calendar cal) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_schedule_day_header, rowDaysHeader, false);
        Locale locale = AppCompatDelegate.getApplicationLocales().isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags());

        TextView tvName = view.findViewById(R.id.tv_day_name);
        TextView tvDate = view.findViewById(R.id.tv_day_date);

        String dayName = new SimpleDateFormat("EEEE", locale).format(cal.getTime()).toUpperCase();

        if (locale.getLanguage().equals("pt")) {
            dayName = dayName.replace("-FEIRA", ""); // Transforma "QUARTA-FEIRA" em "QUARTA"
        }

        tvName.setText(dayName);
        tvDate.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

        if (isToday(cal)) {
            tvDate.setBackgroundResource(R.drawable.bg_circle_primary);
            tvDate.setTextColor(Color.WHITE);
            tvName.setTextColor(Color.parseColor("#7C3AED"));
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        else {
            // Estilo normal (cinza/escuro)
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

                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                int bgColor = Color.parseColor(type.getColor());
                card.setCardBackgroundColor(bgColor);
                card.setRadius(dpToPx(8));
                card.setCardElevation(0);
                card.setStrokeWidth(0);

                LinearLayout hLayout = new LinearLayout(this);
                hLayout.setOrientation(LinearLayout.HORIZONTAL);

                View detailBar = new View(this);
                detailBar.setLayoutParams(new LinearLayout.LayoutParams((int)dpToPx(4), ViewGroup.LayoutParams.MATCH_PARENT));
                detailBar.setBackgroundColor(manipulateColor(bgColor, 0.8f));

                View content = LayoutInflater.from(this).inflate(R.layout.item_schedule_shift, hLayout, false);
                ((com.google.android.material.card.MaterialCardView)content).setCardBackgroundColor(Color.TRANSPARENT);
                ((com.google.android.material.card.MaterialCardView)content).setCardElevation(0);

                // Esconder o horário para turnos de dia inteiro
                content.findViewById(R.id.tv_shift_time).setVisibility(View.GONE);

                ((TextView) content.findViewById(R.id.tv_shift_name)).setText(translateShiftName(type.getName()));
                ((TextView) content.findViewById(R.id.tv_nurse_count)).setText(String.valueOf(shift.getUsers().size()));

                hLayout.addView(detailBar);
                hLayout.addView(content);
                card.addView(hLayout);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins((int)(4*density), (int)(4*density), (int)(4*density), (int)(4*density));
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

                // Criar o card como o da legenda (pormenor escuro lateral)
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                int bgColor = Color.parseColor(type.getColor());
                card.setCardBackgroundColor(bgColor);
                card.setRadius(dpToPx(12));
                card.setCardElevation(0); // Remove contorno cinzento/sombra
                card.setStrokeWidth(0);   // Remove borda cinzenta

                LinearLayout hLayout = new LinearLayout(this);
                hLayout.setOrientation(LinearLayout.HORIZONTAL);

                // A Barra Lateral Escura (como na legenda)
                View detailBar = new View(this);
                detailBar.setLayoutParams(new LinearLayout.LayoutParams((int)dpToPx(6), ViewGroup.LayoutParams.MATCH_PARENT));
                detailBar.setBackgroundColor(manipulateColor(bgColor, 0.8f));

                // Inflar o conteúdo (Texto e Ícones)
                View content = LayoutInflater.from(this).inflate(R.layout.item_schedule_shift, hLayout, false);
                // Limpar o fundo e elevação do XML inflado para não chocar
                ((com.google.android.material.card.MaterialCardView)content).setCardBackgroundColor(Color.TRANSPARENT);
                ((com.google.android.material.card.MaterialCardView)content).setCardElevation(0);

                ((TextView) content.findViewById(R.id.tv_shift_name)).setText(translateShiftName(type.getName()));
                ((TextView) content.findViewById(R.id.tv_shift_time)).setText(type.getStartTime().substring(0, 5) + " - " + type.getEndTime().substring(0, 5));
                ((TextView) content.findViewById(R.id.tv_nurse_count)).setText(String.valueOf(shift.getUsers().size()));

                hLayout.addView(detailBar);
                hLayout.addView(content);
                card.addView(hLayout);

                int start = getMinutes(type.getStartTime());
                int end = getMinutes(type.getEndTime());
                if (end <= start) end = 1440;

                int top = (int) (start * (DP_PER_HOUR / 60.0) * density);
                int height = (int) ((end - start) * (DP_PER_HOUR / 60.0) * density);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                lp.setMargins((int)(4*density), top, (int)(4*density), 0);
                card.setLayoutParams(lp);
                column.addView(card);
            }
        }

        // 2. INDICADOR VERMELHO DEPOIS (Fica por cima de tudo)
        if (isToday(dayCal)) {
            Calendar now = Calendar.getInstance();
            int minutesPassed = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            float topPos = minutesPassed * (DP_PER_HOUR / 60.0f) * density;

            View timeLine = new View(this);
            timeLine.setBackgroundColor(Color.parseColor("#EF4444"));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(1.5f * density));
            lp.topMargin = (int) topPos;
            timeLine.setLayoutParams(lp);

            android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
            dot.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dot.setColor(Color.parseColor("#EF4444"));

            View circle = new View(this);
            circle.setBackground(dot);
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams((int)(8 * density), (int)(8 * density));
            clp.topMargin = (int) topPos - (int)(3.5f * density);
            clp.leftMargin = -(int)(4 * density);
            circle.setLayoutParams(clp);

            column.addView(timeLine);
            column.addView(circle);
        }
    }

    private int getMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private boolean isAllDay(ShiftType type) {
        if (type == null) return false;
        String start = type.getStartTime();
        String end = type.getEndTime();

        // Verifica se o tempo é 00:00 até 00:00
        boolean isTimeAllDay = start != null && end != null && start.startsWith("00:00") && end.startsWith("00:00");

        String name = type.getName().toLowerCase();
        boolean isNameAllDay = name.contains("folga") || name.contains("férias") || name.contains("baixa") || name.contains("licença") || name.contains("dayoff") || name.contains("holidays");

        return isTimeAllDay || isNameAllDay;
    }

    private boolean isToday(Calendar cal) {
        Calendar today = Calendar.getInstance();
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private void updateWeekLabel(Calendar start) {
        Calendar cal = (Calendar) start.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }

        Calendar end = (Calendar) cal.clone();
        end.add(Calendar.DAY_OF_MONTH, 6);

        // Usar o Locale atual para a tradução do mês (Jun / jun.)
        Locale locale = AppCompatDelegate.getApplicationLocales().isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags());

        SimpleDateFormat sdf = new SimpleDateFormat("d", locale);
        SimpleDateFormat sdfMY = new SimpleDateFormat("MMM. yyyy", locale);

        tvWeekRange.setText(sdf.format(cal.getTime()) + " - " + sdf.format(end.getTime()) + " " + sdfMY.format(end.getTime()));
    }

    private void setLocale(String langCode) {
        androidx.core.os.LocaleListCompat appLocales = androidx.core.os.LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
        // O sistema irá recriar a atividade ou chamar o onConfigurationChanged automaticamente
    }

    // Método Auxiliar para Traduzir Cargos e Turnos
    private String translateRole(String role) {
        if (role == null) return "";
        String key = role.toLowerCase().trim();
        if (key.contains("head")) return getString(R.string.role_head_nurse);
        if (key.contains("admin")) return getString(R.string.role_admin);
        return getString(R.string.role_nurse);
    }

    private String translateShiftName(String name) {
        if (name == null) return "";
        String key = name.toLowerCase().trim();
        switch (key) {
            case "morning": case "manhã": return getString(R.string.shift_morning);
            case "afternoon": case "tarde": return getString(R.string.shift_afternoon);
            case "night": case "noite": return getString(R.string.shift_night);
            case "dayoff": case "folga": return getString(R.string.shift_dayoff);
            case "holidays": case "férias": return getString(R.string.shift_holidays);
            case "sick leave": case "baixa": return getString(R.string.shift_sick_leave);
            case "parental leave": case "licença": return getString(R.string.shift_parental_leave);
            default: return name;
        }
    }


    @Override
    protected void updateUIStrings() {
        // Role do Utilizador
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);

        // Títulos e Legend
        TextView tvTitle = findViewById(R.id.tv_page_title);
        TextView tvSubtitle = findViewById(R.id.tv_page_subtitle);
        TextView tvLegend = findViewById(R.id.tv_legend_title);
        TextView tvAllDay = findViewById(R.id.tv_allday_label);

        if (tvTitle != null) tvTitle.setText(R.string.schedule);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.schedule_page_subtitle);
        if (tvLegend != null) tvLegend.setText(R.string.shifts_legend_label);
        if (tvAllDay != null) tvAllDay.setText(R.string.all_day_label);

        // Botões
        MaterialButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnToday = findViewById(R.id.btn_today);
        MaterialButton btnPersonal = findViewById(R.id.btn_personal_view);
        MaterialButton btnGlobal = findViewById(R.id.btn_global_view);
        if (btnBack != null) btnBack.setText(R.string.back);
        if (btnToday != null) btnToday.setText(R.string.today);
        if (btnPersonal != null) btnPersonal.setText(R.string.personal_view);
        if (btnGlobal != null) btnGlobal.setText(R.string.global_view);

        TextView tvDistTitle = findViewById(R.id.tv_distribution_title);
        if (tvDistTitle != null) tvDistTitle.setText(R.string.distribution_title);

        renderDistribution();

        if (!cachedShiftTypes.isEmpty()) {
            setupShiftLegend(cachedShiftTypes);
        }

        if (!allShifts.isEmpty()) {
            renderWeeklyData();
        }
        // Banner e Data
        updateWeekLabel(currentCalendar);
        setupBannerLink();
    }


    // 3. Tradução do iCal (Banner e Modal)
    private void setupBannerLink() {
        TextView tvBanner = findViewById(R.id.tv_banner_text);
        if (tvBanner == null) return;

        String fullText = getString(R.string.banner_ical_text);
        String linkPart = getString(R.string.banner_ical_link_text);
        SpannableString ss = new SpannableString(fullText);
        int start = fullText.indexOf(linkPart);
        if (start != -1) {
            // O link vai desde o 'start' até ao fim do 'fullText' (incluindo o ponto final)
            int end = fullText.length();

            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    showInstructionsDialog();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(Color.parseColor("#7C3AED")); // Roxo
                    ds.setUnderlineText(true);               // Sublinhado
                    ds.setFakeBoldText(true);                // Negrito
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvBanner.setText(ss);
        tvBanner.setMovementMethod(LinkMovementMethod.getInstance());
        tvBanner.setHighlightColor(Color.TRANSPARENT);
    }

    private void showInstructionsDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_instructions_ical, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = v.findViewById(R.id.tv_instructions_title);
        TextView tvContent = v.findViewById(R.id.tv_instructions_content);
        Button btnGotIt = v.findViewById(R.id.btn_dismiss_dialog);

        if (tvTitle != null) tvTitle.setText(R.string.ical_sync_title);
        if (tvContent != null) tvContent.setText(Html.fromHtml(getString(R.string.ical_instructions_html), Html.FROM_HTML_MODE_COMPACT));
        if (btnGotIt != null) {
            btnGotIt.setText(R.string.ical_btn_got_it);
            btnGotIt.setOnClickListener(view -> dialog.dismiss());
        }

        dialog.show();
    }

    private void renderDistribution() {
        ChipGroup cgDistribution = findViewById(R.id.cg_daily_distribution);
        if (cgDistribution == null) return;
        cgDistribution.removeAllViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String currentUserName = prefs.getString("user_name", "").trim().toLowerCase();

        Calendar cal = (Calendar) currentCalendar.clone();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { cal.add(Calendar.DAY_OF_YEAR, -1); }

        Locale locale = AppCompatDelegate.getApplicationLocales().isEmpty() ? Locale.getDefault() : Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags());

        for (int i = 0; i < 7; i++) {
            String dayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding((int)dpToPx(16), (int)dpToPx(16), (int)dpToPx(16), (int)dpToPx(16));

            boolean hasContent = false;
            for (Shift shift : allShifts) {
                if (shift.getDate().equals(dayStr)) {
                    boolean userInThisShift = false;
                    for (User u : shift.getUsers()) {
                        if (u.getName().trim().toLowerCase().equals(currentUserName)) {
                            userInThisShift = true;
                            break;
                        }
                    }

                    // Lógica: Global mostra tudo | Pessoal só mostra o turno onde o user está
                    if (currentViewMode.equals("global") || (currentViewMode.equals("personal") && userInThisShift)) {
                        hasContent = true;
                        cardContent.addView(createDistributionRow(shift));
                    }
                }
            }

            if (hasContent) {
                com.google.android.material.card.MaterialCardView dayCard = new com.google.android.material.card.MaterialCardView(this);

                ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dayCard.setLayoutParams(lp);

                dayCard.setRadius(dpToPx(16));
                dayCard.setCardElevation(dpToPx(4));
                dayCard.setCardBackgroundColor(Color.WHITE);
                dayCard.setStrokeWidth(0);

                TextView tvDay = new TextView(this);
                String headerLabel = new SimpleDateFormat("EEEE, d", locale).format(cal.getTime());
                tvDay.setText(headerLabel.substring(0, 1).toUpperCase() + headerLabel.substring(1));
                tvDay.setTextColor(Color.parseColor("#2D2054"));
                tvDay.setTextSize(15);
                tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
                tvDay.setPadding(0, 0, 0, (int)dpToPx(12));
                cardContent.addView(tvDay, 0);

                dayCard.addView(cardContent);
                cgDistribution.addView(dayCard);
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private View createDistributionRow(Shift shift) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, (int)dpToPx(4), 0, (int)dpToPx(4));

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams((int)dpToPx(8), (int)dpToPx(8));
        dotParams.setMargins(0, (int)dpToPx(6), (int)dpToPx(8), 0);
        dot.setLayoutParams(dotParams);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(Color.parseColor(shift.getShiftType().getColor()));
        dot.setBackground(shape);

        TextView tvInfo = new TextView(this);
        String shiftName = translateShiftName(shift.getShiftType().getName());
        StringBuilder users = new StringBuilder();
        for (int j = 0; j < shift.getUsers().size(); j++) {
            users.append(shift.getUsers().get(j).getName()).append(j < shift.getUsers().size() - 1 ? ", " : "");
        }

        String fullText = "<b>" + shiftName + ":</b> " + users.toString();
        tvInfo.setText(Html.fromHtml(fullText, Html.FROM_HTML_MODE_COMPACT));
        tvInfo.setTextColor(Color.parseColor("#475569"));
        tvInfo.setTextSize(13);

        row.addView(dot);
        row.addView(tvInfo);
        return row;
    }

    private void handleIcalExport() {
        // 1. Obter o Token das SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String token = prefs.getString("token", "");

        // 2. Construir o URL dinâmico (Usa o BuildConfig.BASE_URL do teu RetrofitClient)
        // O token é passado via query string para que o browser/calendário consiga autenticar o download
        String icalUrl = BuildConfig.BASE_URL + "schedules/ical?token=" + token;

        // 3. Copiar para a Área de Transferência (Clipboard)
        // Isto permite que o utilizador siga o passo "Cole o link" se o download automático falhar
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("iCal Link", icalUrl);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        // 4. Abrir no Browser / Aplicação de Calendário
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(icalUrl));
            startActivity(intent);

            Toast.makeText(this, "Link copiado e a abrir calendário...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir o calendário. O link foi copiado para a área de transferência.", Toast.LENGTH_LONG).show();
        }
    }

}