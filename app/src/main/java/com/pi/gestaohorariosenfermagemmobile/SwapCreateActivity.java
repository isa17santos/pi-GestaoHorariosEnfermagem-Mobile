package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import com.google.android.material.checkbox.MaterialCheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.google.android.material.button.MaterialButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SwapCreateActivity extends BaseActivity {

    private static final int ITEMS_PER_PAGE = 6;

    private static final List<String> DAYOFF_NAMES = Arrays.asList(
            "dayoff", "day off", "folga", "holidays", "férias",
            "sick leave", "baixa médica", "parental leave", "licença parental",
            "baixa", "licença"
    );

    // ─── Intent extras ───────────────────────────────────────────────────────
    private int offeredShiftId;
    private String mode; // "shift" or "shift_for_dayoff"
    private int targetShiftTypeId; // the shift type the nurse wants to receive

    // ─── Offered shift details (loaded from API) ──────────────────────────────
    private Shift offeredShift;

    // ─── Dayoff mode state ────────────────────────────────────────────────────
    private List<Shift> allOwnShifts = new ArrayList<>(); // all own shifts for agenda
    private List<Shift> ownDayoffShifts = new ArrayList<>(); // filtered dayoff shifts
    private Shift selectedOwnDayoff; // chosen in step 1
    private List<Shift> candidatesOnOfferedDate = new ArrayList<>(); // cached for step 2
    private Map<Integer, Shift> targetWorkShiftsMap = new HashMap<>(); // userId → work shift on dayoff date
    private List<Shift> preloadedCandidates = null; // permanent cache of candidates on offered date
    // date (yyyy-MM-dd) → (shiftTypeName → eligible candidate count)
    private Map<String, Map<String, Integer>> breakdownByDate = new HashMap<>();

    // ─── Week navigation ──────────────────────────────────────────────────────
    private Calendar currentWeekStart = Calendar.getInstance();
    private TextView tvWeekRange;

    // ─── Candidate list state ─────────────────────────────────────────────────
    private List<Shift> allCandidates = new ArrayList<>();
    private List<Shift> filteredCandidates = new ArrayList<>();
    private int currentPage = 1;

    // ─── Views ────────────────────────────────────────────────────────────────
    private NavbarManager navbarManager;
    private String token;
    private View loader;
    private LinearLayout llNotification;
    private TextView tvTitle;

    // Header card views
    private TextView tvLabelOffered, tvOfferedDate, tvOfferedType;
    private TextView tvLabelTarget, tvTargetDate, tvTargetType;
    private LinearLayout llHeaderBottomRow;
    private TextView tvLabelOwnDayoff, tvOwnDayoffDate, tvOwnDayoffType;
    private TextView tvLabelTargetWork, tvTargetWorkInfo;

    // Step 1 (dayoff agenda)
    private LinearLayout llStep1;
    private TextView tvStep1Title, tvStep1Desc;
    private RecyclerView rvAgenda;
    private AgendaShiftAdapter agendaAdapter;

    // Shift type filter spinner
    private FrameLayout flShiftTypeFilter;
    private Spinner spinnerShiftType;
    private String selectedShiftTypeFilter = null;
    private boolean spinnerShiftTypeReady = false;

    // Candidate section
    private LinearLayout llCandidateSection;
    private TextView tvCandidatesTitle, tvSelectAllLabel;
    private MaterialButton btnCreateSwap, btnBackToDayoffs;
    private MaterialCheckBox cbSelectAll;
    private EditText etSearch;
    private RecyclerView rvCandidates;
    private LinearLayout llPaginationNumbers;
    private SwapCandidateAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_create);

        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");
        navbarManager = new NavbarManager(this);

        offeredShiftId   = getIntent().getIntExtra("SHIFT_ID", -1);
        mode             = getIntent().getStringExtra("MODE");
        targetShiftTypeId = getIntent().getIntExtra("SHIFT_TYPE_ID", -1);

        bindViews();
        updateUIStrings();
        setupPagination();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if ("shift_for_dayoff".equals(mode)
                        && llCandidateSection != null
                        && llCandidateSection.getVisibility() == View.VISIBLE) {
                    showStep1();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Start by loading the offered shift to populate the header
        loadOfferedShift();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }

    // ─── View binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        loader           = findViewById(R.id.loader);
        llNotification   = findViewById(R.id.ll_notification_toast);
        tvTitle          = findViewById(R.id.tv_title);

        tvLabelOffered   = findViewById(R.id.tv_label_offered);
        tvOfferedDate    = findViewById(R.id.tv_offered_date);
        tvOfferedType    = findViewById(R.id.tv_offered_type);
        tvLabelTarget    = findViewById(R.id.tv_label_target);
        tvTargetDate     = findViewById(R.id.tv_target_date);
        tvTargetType     = findViewById(R.id.tv_target_type);
        llHeaderBottomRow = findViewById(R.id.ll_header_bottom_row);
        tvLabelOwnDayoff = findViewById(R.id.tv_label_own_dayoff);
        tvOwnDayoffDate  = findViewById(R.id.tv_own_dayoff_date);
        tvOwnDayoffType  = findViewById(R.id.tv_own_dayoff_type);
        tvLabelTargetWork = findViewById(R.id.tv_label_target_work);
        tvTargetWorkInfo = findViewById(R.id.tv_target_work_info);

        llStep1          = findViewById(R.id.ll_step1_dayoff);
        tvStep1Title     = findViewById(R.id.tv_step1_title);
        tvStep1Desc      = findViewById(R.id.tv_step1_desc);
        rvAgenda         = findViewById(R.id.rv_agenda);
        rvAgenda.setLayoutManager(new LinearLayoutManager(this));
        tvWeekRange      = findViewById(R.id.tv_week_range);
        findViewById(R.id.btn_prev_week).setOnClickListener(v -> {
            currentWeekStart.add(Calendar.DAY_OF_MONTH, -7);
            renderAgenda();
        });
        findViewById(R.id.btn_next_week).setOnClickListener(v -> {
            currentWeekStart.add(Calendar.DAY_OF_MONTH, 7);
            renderAgenda();
        });

        llCandidateSection = findViewById(R.id.ll_candidate_section);
        flShiftTypeFilter  = findViewById(R.id.fl_shift_type_filter);
        spinnerShiftType   = findViewById(R.id.spinner_shift_type);
        tvCandidatesTitle  = findViewById(R.id.tv_candidates_title);
        btnCreateSwap      = findViewById(R.id.btn_create_swap);
        btnBackToDayoffs   = findViewById(R.id.btn_back_to_dayoffs);
        cbSelectAll        = findViewById(R.id.cb_select_all);
        tvSelectAllLabel   = findViewById(R.id.tv_select_all_label);
        etSearch           = findViewById(R.id.et_search);
        rvCandidates       = findViewById(R.id.rv_candidates);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Back to dayoff picker from step 2
        btnBackToDayoffs.setOnClickListener(v -> showStep1());

        // "Criar pedido" — show blur confirmation before submitting
        btnCreateSwap.setOnClickListener(v -> confirmCreateSwaps());

        // Select-all checkbox
        cbSelectAll.setOnClickListener(v -> {
            adapter.selectAll(cbSelectAll.isChecked());
            updateSelectAllLabel();
        });

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                currentPage = 1;
                applySearch(s.toString());
            }
        });
    }

    // ─── Initial data load ────────────────────────────────────────────────────

    private void loadOfferedShift() {
        loader.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        // Load own shifts to find the offered shift details
        api.getShifts("Bearer " + token, true, null).enqueue(new Callback<ShiftsResponse>() {
            @Override
            public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                loader.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    for (Shift s : response.body().getData()) {
                        if (s.getId() == offeredShiftId) { offeredShift = s; break; }
                    }
                }
                if (offeredShift == null) {
                    Toast.makeText(SwapCreateActivity.this, getString(R.string.err_loading_swaps), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                populateOfferedShiftHeader();
                if ("shift_for_dayoff".equals(mode)) {
                    loadOwnDayoffShifts();
                } else {
                    loadCandidates();
                }
            }

            @Override
            public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                loader.setVisibility(View.GONE);
                Toast.makeText(SwapCreateActivity.this, getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // ─── Header population ────────────────────────────────────────────────────

    private void populateOfferedShiftHeader() {
        tvLabelOffered.setText(getString(R.string.swap_header_current_shift));
        tvOfferedDate.setText(formatDateFull(offeredShift.getDate()));
        tvOfferedType.setText(buildTypeTimeLine(offeredShift.getShiftType()));

        if ("shift_for_dayoff".equals(mode)) {
            // 4-panel header: show bottom row
            llHeaderBottomRow.setVisibility(View.VISIBLE);
            tvLabelTarget.setText(getString(R.string.swap_header_requested_dayoff));
            // The "FOLGA PRETENDIDA" is the day-off they want; we show the offered shift date
            // as the work shift date. The actual requested dayoff will be filled from step 1.
            tvTargetDate.setText(formatDateFull(offeredShift.getDate()));
            tvTargetType.setText(getString(R.string.swap_label_dayoff_allday));
            tvLabelOwnDayoff.setText(getString(R.string.swap_header_own_dayoff));
            tvOwnDayoffDate.setText("—");
            tvOwnDayoffType.setText("—");
            tvLabelTargetWork.setText(getString(R.string.swap_header_target_work));
            tvTargetWorkInfo.setText("—");
        } else {
            // 2-panel header: hide bottom row, show target shift type
            llHeaderBottomRow.setVisibility(View.GONE);
            tvLabelTarget.setText(getString(R.string.swap_header_requested_shift));
            // Find the shift type name from cached types is not available, use targetShiftTypeId
            // The name will be shown as "—" until we can resolve; the candidates list provides context
            tvTargetDate.setText("—");
            tvTargetType.setText("—");
        }
    }

    private void updateTargetShiftTypeInHeader(String typeName, String timeLine) {
        tvTargetDate.setText(typeName);
        tvTargetType.setText(timeLine);
    }

    // ─── MODE: shift — load candidates ───────────────────────────────────────

    private void loadCandidates() {
        if (offeredShift == null) return;
        loader.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftsFiltered("Bearer " + token, true, offeredShift.getDate(), targetShiftTypeId)
                .enqueue(new Callback<ShiftsResponse>() {
                    @Override
                    public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                        loader.setVisibility(View.GONE);
                        allCandidates = response.isSuccessful() && response.body() != null
                                ? response.body().getData() : new ArrayList<>();
                        buildShiftTypeFilterChips();
                        // Update header with resolved shift type info from first result
                        if (!allCandidates.isEmpty() && allCandidates.get(0).getShiftType() != null) {
                            ShiftType st = allCandidates.get(0).getShiftType();
                            updateTargetShiftTypeInHeader(
                                    getLocalizedName(st.getName()),
                                    st.getStartTime().substring(0,5) + " - " + st.getEndTime().substring(0,5));
                        }
                        showCandidateList(false);
                        applySearch("");
                    }

                    @Override
                    public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(SwapCreateActivity.this, getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─── MODE: shift_for_dayoff — step 1 ─────────────────────────────────────

    private void loadOwnDayoffShifts() {
        loader.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        // Fetch all own shifts (past + future) for agenda context
        api.getShifts("Bearer " + token, true, null).enqueue(new Callback<ShiftsResponse>() {
            @Override
            public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                loader.setVisibility(View.GONE);
                allOwnShifts = response.isSuccessful() && response.body() != null
                        ? response.body().getData() : new ArrayList<>();
                ownDayoffShifts = new ArrayList<>();
                for (Shift s : allOwnShifts) {
                    if (s.getShiftType() != null &&
                            DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim())) {
                        ownDayoffShifts.add(s);
                    }
                }
                loadCandidateBreakdownAndShowStep1();
            }

            @Override
            public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                loader.setVisibility(View.GONE);
                Toast.makeText(SwapCreateActivity.this, getString(R.string.err_network), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Step 1 of async load: get who works on the offered date (candidates). */
    private void loadCandidateBreakdownAndShowStep1() {
        if (offeredShift == null) { showStep1(); return; }
        loader.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftsFiltered("Bearer " + token, true, offeredShift.getDate(), null)
                .enqueue(new Callback<ShiftsResponse>() {
                    @Override
                    public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                        List<Shift> raw = response.isSuccessful() && response.body() != null
                                ? response.body().getData() : new ArrayList<>();
                        // Candidates = people who have a DAY OFF on the offered date
                        preloadedCandidates = new ArrayList<>();
                        for (Shift s : raw) {
                            if (s.getShiftType() != null &&
                                    DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim())) {
                                preloadedCandidates.add(s);
                            }
                        }
                        // Step 2: for each nurse's future day-off, find who among those candidates has a work shift that day
                        loadWorkShiftsPerDayoffAndShow();
                    }

                    @Override
                    public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                        loader.setVisibility(View.GONE);
                        preloadedCandidates = new ArrayList<>();
                        breakdownByDate = new HashMap<>();
                        showStep1();
                    }
                });
    }

    /** Step 2: for each future day-off date, fetch who else works that day and cross-reference candidates. */
    private void loadWorkShiftsPerDayoffAndShow() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Collect unique future day-off dates
        List<String> futureDayoffDates = new ArrayList<>();
        for (Shift s : ownDayoffShifts) {
            if (s.getDate() != null && s.getDate().compareTo(today) >= 0
                    && !futureDayoffDates.contains(s.getDate())) {
                futureDayoffDates.add(s.getDate());
            }
        }

        breakdownByDate = new HashMap<>();
        if (futureDayoffDates.isEmpty()) {
            loader.setVisibility(View.GONE);
            showStep1();
            return;
        }

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        AtomicInteger pending = new AtomicInteger(futureDayoffDates.size());

        for (String dayoffDate : futureDayoffDates) {
            api.getShiftsFiltered("Bearer " + token, true, dayoffDate, null)
                    .enqueue(new Callback<ShiftsResponse>() {
                        @Override
                        public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                            List<Shift> raw = response.isSuccessful() && response.body() != null
                                    ? response.body().getData() : new ArrayList<>();

                            // Build set of user IDs who have a day-off on the offered date (eligible candidates)
                            java.util.Set<Integer> dayoffCandidateIds = new java.util.HashSet<>();
                            for (Shift candidate : preloadedCandidates) {
                                if (candidate.getUsers() != null) {
                                    for (User u : candidate.getUsers()) dayoffCandidateIds.add(u.getId());
                                }
                            }

                            // Among work shifts on the day-off date, count those belonging to eligible candidates.
                            // Group by WORK shift type (what the candidate offers for the nurse's day-off).
                            Map<String, Integer> breakdown = new LinkedHashMap<>();
                            for (Shift s : raw) {
                                if (s.getShiftType() == null) continue;
                                if (DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim())) continue;
                                if (s.getUsers() == null) continue;
                                for (User u : s.getUsers()) {
                                    if (dayoffCandidateIds.contains(u.getId())) {
                                        String typeName = getLocalizedName(s.getShiftType().getName());
                                        breakdown.merge(typeName, 1, Integer::sum);
                                    }
                                }
                            }

                            synchronized (breakdownByDate) {
                                breakdownByDate.put(dayoffDate, breakdown);
                            }
                            if (pending.decrementAndGet() == 0) {
                                runOnUiThread(() -> {
                                    loader.setVisibility(View.GONE);
                                    showStep1();
                                });
                            }
                        }

                        @Override
                        public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                            synchronized (breakdownByDate) {
                                breakdownByDate.put(dayoffDate, new LinkedHashMap<>());
                            }
                            if (pending.decrementAndGet() == 0) {
                                runOnUiThread(() -> {
                                    loader.setVisibility(View.GONE);
                                    showStep1();
                                });
                            }
                        }
                    });
        }
    }

    private void showStep1() {
        // Anchor week to Monday of the current real week
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);

        llStep1.setVisibility(View.VISIBLE);
        llCandidateSection.setVisibility(View.GONE);
        tvOwnDayoffDate.setText("—");
        tvOwnDayoffType.setText("—");
        tvTargetWorkInfo.setText("—");
        renderAgenda();
    }

    // ─── Agenda view: all own shifts filtered to current week ─────────────────

    private void renderAgenda() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);
        String startStr = sdf.format(currentWeekStart.getTime());
        String endStr   = sdf.format(weekEnd.getTime());

        List<Shift> weekShifts = new ArrayList<>();
        for (Shift s : allOwnShifts) {
            if (s.getDate() != null
                    && s.getDate().compareTo(startStr) >= 0
                    && s.getDate().compareTo(endStr) < 0) {
                weekShifts.add(s);
            }
        }

        // Week range label: "2 Jun – 8 Jun 2026"
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        Calendar weekEndDisplay = (Calendar) currentWeekStart.clone();
        weekEndDisplay.add(Calendar.DAY_OF_MONTH, 6);
        SimpleDateFormat startFmt = new SimpleDateFormat("d MMM", locale);
        SimpleDateFormat endFmt   = new SimpleDateFormat("d MMM yyyy", locale);
        tvWeekRange.setText(startFmt.format(currentWeekStart.getTime())
                + " – " + endFmt.format(weekEndDisplay.getTime()));

        agendaAdapter = new AgendaShiftAdapter(this, weekShifts);
        agendaAdapter.setBreakdownByDate(breakdownByDate);
        agendaAdapter.setOnDayoffClickListener(this::onDayoffCellTapped);
        rvAgenda.setAdapter(agendaAdapter);
    }

    // ─── MODE: shift_for_dayoff — step 2 ─────────────────────────────────────

    private void onDayoffCellTapped(Shift ownDayoff) {
        selectedOwnDayoff = ownDayoff;

        // Update header bottom-left panel
        tvOwnDayoffDate.setText(formatDateFull(ownDayoff.getDate()));
        tvOwnDayoffType.setText(buildTypeTimeLine(ownDayoff.getShiftType()));
        tvTargetWorkInfo.setText("—");

        llStep1.setVisibility(View.GONE);
        btnBackToDayoffs.setVisibility(View.VISIBLE);
        showCandidateList(true, ownDayoff);

        loadDayoffModeCandidates();
    }

    private void loadDayoffModeCandidates() {
        if (offeredShift == null || selectedOwnDayoff == null) return;

        // Use the candidates pre-loaded during agenda setup (permanent cache)
        if (preloadedCandidates != null) {
            candidatesOnOfferedDate = preloadedCandidates;
            loadTargetWorkShiftsOnDayoffDate();
            return;
        }

        loader.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        api.getShiftsFiltered("Bearer " + token, true, offeredShift.getDate(), null)
                .enqueue(new Callback<ShiftsResponse>() {
                    @Override
                    public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                        List<Shift> raw = response.isSuccessful() && response.body() != null
                                ? response.body().getData() : new ArrayList<>();
                        // Fallback: keep only DAY OFF shifts on the offered date
                        candidatesOnOfferedDate = new ArrayList<>();
                        for (Shift s : raw) {
                            if (s.getShiftType() != null &&
                                    DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim())) {
                                candidatesOnOfferedDate.add(s);
                            }
                        }
                        loadTargetWorkShiftsOnDayoffDate();
                    }

                    @Override
                    public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(SwapCreateActivity.this, getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTargetWorkShiftsOnDayoffDate() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftsFiltered("Bearer " + token, true, selectedOwnDayoff.getDate(), null)
                .enqueue(new Callback<ShiftsResponse>() {
                    @Override
                    public void onResponse(Call<ShiftsResponse> call, Response<ShiftsResponse> response) {
                        loader.setVisibility(View.GONE);
                        List<Shift> raw = response.isSuccessful() && response.body() != null
                                ? response.body().getData() : new ArrayList<>();
                        // Index by shift id (adapter finds target work shift by user id)
                        targetWorkShiftsMap = new HashMap<>();
                        for (Shift s : raw) {
                            if (s.getShiftType() != null &&
                                    !DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim())) {
                                targetWorkShiftsMap.put(s.getId(), s);
                            }
                        }
                        adapter.setTargetWorkShifts(targetWorkShiftsMap);
                        allCandidates = candidatesOnOfferedDate;
                        buildShiftTypeFilterChips();
                        applySearch("");
                    }

                    @Override
                    public void onFailure(Call<ShiftsResponse> call, Throwable t) {
                        loader.setVisibility(View.GONE);
                        Toast.makeText(SwapCreateActivity.this, getString(R.string.err_network), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─── Candidate list display ───────────────────────────────────────────────

    private void showCandidateList(boolean isDayoffMode) {
        showCandidateList(isDayoffMode, null);
    }

    private void showCandidateList(boolean isDayoffMode, Shift ownDayoffShift) {
        llCandidateSection.setVisibility(View.VISIBLE);
        boolean isStep2 = "shift_for_dayoff".equals(mode) && isDayoffMode;
        btnBackToDayoffs.setVisibility(isStep2 ? View.VISIBLE : View.GONE);

        // Set up recycler + adapter
        boolean modeIsDayoff = "shift_for_dayoff".equals(mode) && isDayoffMode;
        adapter = new SwapCandidateAdapter(this, modeIsDayoff);
        if (modeIsDayoff && ownDayoffShift != null) {
            adapter.setOwnDayoff(ownDayoffShift);
        }
        adapter.setOnSelectionChangedListener((selected, total) -> {
            boolean anySelected = selected > 0;
            btnCreateSwap.setEnabled(anySelected);
            btnCreateSwap.setAlpha(anySelected ? 1f : 0.4f);
            updateSelectAllLabel();
            // Keep select-all checkbox in sync
            cbSelectAll.setChecked(selected == total && total > 0);
        });
        rvCandidates.setLayoutManager(new LinearLayoutManager(this));
        rvCandidates.setAdapter(adapter);
    }

    private void applySearch(String query) {
        String q = query.toLowerCase().trim();
        filteredCandidates = new ArrayList<>();
        for (Shift s : allCandidates) {
            String name = s.getUsers() != null && !s.getUsers().isEmpty()
                    ? s.getUsers().get(0).getName().toLowerCase() : "";
            if (!q.isEmpty() && !name.contains(q)) continue;
            if (selectedShiftTypeFilter != null
                    && !selectedShiftTypeFilter.equals(getWorkShiftTypeName(s))) continue;
            filteredCandidates.add(s);
        }
        currentPage = 1;
        renderPage();
    }

    private String getWorkShiftTypeName(Shift candidateShift) {
        if ("shift_for_dayoff".equals(mode)) {
            if (candidateShift.getUsers() != null && !candidateShift.getUsers().isEmpty()) {
                int userId = candidateShift.getUsers().get(0).getId();
                Shift targetWork = findTargetWorkShiftForUser(userId);
                if (targetWork != null && targetWork.getShiftType() != null) {
                    return getLocalizedName(targetWork.getShiftType().getName());
                }
            }
            return null;
        }
        return candidateShift.getShiftType() != null
                ? getLocalizedName(candidateShift.getShiftType().getName()) : null;
    }

    private void buildShiftTypeFilterChips() {
        if (spinnerShiftType == null) return;
        selectedShiftTypeFilter = null;
        spinnerShiftTypeReady = false;

        java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>();
        for (Shift s : allCandidates) {
            String typeName = getWorkShiftTypeName(s);
            if (typeName != null) types.add(typeName);
        }

        if (types.isEmpty()) {
            flShiftTypeFilter.setVisibility(View.GONE);
            return;
        }
        flShiftTypeFilter.setVisibility(View.VISIBLE);

        List<String> items = new ArrayList<>();
        items.add(getString(R.string.swap_filter_all_types));
        items.addAll(types);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, items);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerShiftType.setAdapter(adapter);
        spinnerShiftType.setSelection(0);
        spinnerShiftTypeReady = true;

        spinnerShiftType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerShiftTypeReady) return;
                selectedShiftTypeFilter = position == 0 ? null : items.get(position);
                currentPage = 1;
                applySearch(etSearch.getText().toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void renderPage() {
        int total = filteredCandidates.size();
        int maxPage = (int) Math.ceil((double) total / ITEMS_PER_PAGE);
        if (maxPage < 1) maxPage = 1;
        if (currentPage > maxPage) currentPage = maxPage;

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, total);
        List<Shift> page = start < total ? filteredCandidates.subList(start, end) : new ArrayList<>();

        adapter.setData(page);
        updateSelectAllLabel();
        renderPagination(maxPage);
    }

    private void updateSelectAllLabel() {
        int selected = adapter.getSelectedShiftIds().size();
        int total = adapter.getSelectableCount();
        tvSelectAllLabel.setText(getString(R.string.swap_select_all, selected, total));
    }

    // ─── Pagination ───────────────────────────────────────────────────────────

    private void setupPagination() {
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (currentPage > 1) { currentPage--; renderPage(); }
        });
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) filteredCandidates.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) { currentPage++; renderPage(); }
        });
    }

    private void renderPagination(int maxPage) {
        llPaginationNumbers.removeAllViews();
        for (int i = 1; i <= maxPage; i++) {
            final int pageNum = i;
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
            lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(String.valueOf(i));
            tv.setTextSize(16);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setBackgroundResource(i == currentPage ? R.drawable.bg_pagination : R.drawable.bg_action_card);
            tv.setTextColor(i == currentPage ? Color.WHITE : ContextCompat.getColor(this, R.color.primary_strong));
            tv.setOnClickListener(v -> { currentPage = pageNum; renderPage(); });
            llPaginationNumbers.addView(tv);
        }
        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton btnNext = findViewById(R.id.btn_next);
        btnPrev.setEnabled(currentPage > 1);
        btnPrev.setAlpha(currentPage > 1 ? 1f : 0.3f);
        btnNext.setEnabled(currentPage < maxPage);
        btnNext.setAlpha(currentPage < maxPage ? 1f : 0.3f);
    }

    // ─── Swap creation ────────────────────────────────────────────────────────

    private void confirmCreateSwaps() {
        int selectedCount = adapter.getSelectedShiftIds().size();

        View decorView = getWindow().getDecorView();
        Bitmap screenshot = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);
        decorView.draw(new Canvas(screenshot));
        Bitmap blurred = blur(screenshot);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_swap, null);
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.BitmapDrawable(getResources(), blurred));
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.4f);
        }
        dialog.setContentView(dialogView);

        ((TextView) dialogView.findViewById(R.id.tv_dialog_title)).setText(R.string.confirm_create_swap);
        ((TextView) dialogView.findViewById(R.id.tv_dialog_message))
                .setText(getString(R.string.confirm_create_swap_msg, selectedCount));
        ((TextView) dialogView.findViewById(R.id.tv_note_label)).setText(R.string.swap_note_label);
        EditText etNotes = dialogView.findViewById(R.id.et_notes);
        etNotes.setHint(R.string.swap_note_hint);

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm_swap);
        btnConfirm.setText(R.string.btn_confirm_swap);
        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            String notes = etNotes.getText().toString().trim();
            dialog.dismiss();
            executeCreateSwaps(notes.isEmpty() ? null : notes);
        });
    }

    private void executeCreateSwaps(String notes) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        List<Shift> selectedCandidates = new ArrayList<>();
        for (Shift s : filteredCandidates) {
            if (adapter.getSelectedShiftIds().contains(s.getId())) selectedCandidates.add(s);
        }

        if (selectedCandidates.isEmpty()) return;

        AtomicInteger pending = new AtomicInteger(selectedCandidates.size());
        AtomicInteger successCount = new AtomicInteger(0);

        for (Shift candidate : selectedCandidates) {
            if (candidate.getUsers() == null || candidate.getUsers().isEmpty()) {
                if (pending.decrementAndGet() == 0) onAllSwapsCreated(successCount.get(), selectedCandidates.size());
                continue;
            }
            int targetUserId = candidate.getUsers().get(0).getId();
            Map<String, Object> body = buildSwapBody(candidate, targetUserId);
            if (notes != null) body.put("notes", notes);

            api.createSwap("Bearer " + token, body).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) successCount.incrementAndGet();
                    if (pending.decrementAndGet() == 0)
                        runOnUiThread(() -> onAllSwapsCreated(successCount.get(), selectedCandidates.size()));
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (pending.decrementAndGet() == 0)
                        runOnUiThread(() -> onAllSwapsCreated(successCount.get(), selectedCandidates.size()));
                }
            });
        }
    }

    private Map<String, Object> buildSwapBody(Shift candidate, int targetUserId) {
        Map<String, Object> body = new HashMap<>();
        body.put("target_user_id", targetUserId);

        if ("shift_for_dayoff".equals(mode) && selectedOwnDayoff != null) {
            // Offered: our work shift + our dayoff
            List<Integer> offeredIds = Arrays.asList(offeredShiftId, selectedOwnDayoff.getId());
            body.put("offered_shift_ids", offeredIds);
            // Requested: candidate's dayoff (on offered date) + their work shift (on dayoff date)
            Shift targetWork = findTargetWorkShiftForUser(targetUserId);
            List<Integer> requestedIds = new ArrayList<>();
            requestedIds.add(candidate.getId()); // candidate's shift on our offered date
            if (targetWork != null) requestedIds.add(targetWork.getId());
            body.put("requested_shift_ids", requestedIds);
        } else {
            body.put("offered_shift_ids", Arrays.asList(offeredShiftId));
            body.put("requested_shift_ids", Arrays.asList(candidate.getId()));
        }
        return body;
    }

    private Shift findTargetWorkShiftForUser(int userId) {
        for (Shift s : targetWorkShiftsMap.values()) {
            if (s.getUsers() != null) {
                for (User u : s.getUsers()) {
                    if (u.getId() == userId) return s;
                }
            }
        }
        return null;
    }

    private void onAllSwapsCreated(int successCount, int total) {
        String msg = successCount == total
                ? getString(R.string.swap_created_success)
                : getString(R.string.swap_created_partial, successCount, total);
        Intent intent = new Intent(this, SwapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("SUCCESS_MSG", msg);
        startActivity(intent);
        finish();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String formatDateFull(String dateStr) {
        if (dateStr == null) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            if (d == null) return dateStr;
            Locale locale = getResources().getConfiguration().getLocales().get(0);
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd/MM/yyyy", locale);
            String f = out.format(d);
            return f.substring(0,1).toUpperCase(locale) + f.substring(1);
        } catch (ParseException e) { return dateStr; }
    }

    private String buildTypeTimeLine(ShiftType type) {
        if (type == null) return "—";
        String name = getLocalizedName(type.getName());
        String start = type.getStartTime() != null ? type.getStartTime().substring(0,5) : "";
        String end   = type.getEndTime() != null ? type.getEndTime().substring(0,5) : "";
        boolean allDay = "00:00".equals(start) && "00:00".equals(end);
        return allDay ? name + " · " + getString(R.string.all_day_label)
                      : name + " · " + start + " - " + end;
    }

    private String getLocalizedName(String name) {
        if (name == null) return "—";
        switch (name.toLowerCase().trim()) {
            case "morning":        return getString(R.string.shift_morning);
            case "afternoon":      return getString(R.string.shift_afternoon);
            case "night":          return getString(R.string.shift_night);
            case "dayoff": case "day off": return getString(R.string.shift_dayoff);
            case "holidays":       return getString(R.string.shift_holidays);
            case "sick leave":     return getString(R.string.shift_sick_leave);
            case "parental leave": return getString(R.string.shift_parental_leave);
            default:               return name;
        }
    }

    private Bitmap blur(Bitmap image) {
        Bitmap output = Bitmap.createBitmap(image);
        RenderScript rs = RenderScript.create(this);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, image);
        Allocation allOut = Allocation.createFromBitmap(rs, output);
        blurScript.setRadius(20f);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(output);
        rs.destroy();
        return output;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─── BaseActivity ─────────────────────────────────────────────────────────

    @Override
    protected void updateUIStrings() {
        if (tvTitle != null) tvTitle.setText(R.string.swap_create_title);
        if (tvStep1Title != null) tvStep1Title.setText(R.string.swap_step1_title);
        if (tvStep1Desc != null) tvStep1Desc.setText(R.string.swap_step1_desc);
        if (tvCandidatesTitle != null) tvCandidatesTitle.setText(R.string.swap_candidates_title);
        if (btnBackToDayoffs != null) btnBackToDayoffs.setText(R.string.swap_back_to_dayoffs);
        if (btnCreateSwap != null) btnCreateSwap.setText(R.string.btn_create_swap);
    }
}
