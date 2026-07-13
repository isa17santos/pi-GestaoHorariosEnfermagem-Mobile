package com.pi.gestaohorariosenfermagemmobile;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SwapHistoryActivity extends BaseActivity {

    private static final int ITEMS_PER_PAGE = 4;

    private RecyclerView rvHistory;
    private SwapAdapter adapter;
    private Spinner spinnerNurse, spinnerMonth;
    private LinearLayout llPaginationNumbers;
    private LinearLayout llErrorToast;
    private TextView tvErrorMsg;

    private List<SwapRequest> allSwaps = new ArrayList<>();
    private int currentPage = 1;
    private String token;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swap_history);

        navbarManager = new NavbarManager(this);

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");

        initViews();
        updateUIStrings();
        setupAdapter();
        setupPaginationButtons();
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void initViews() {
        rvHistory = findViewById(R.id.rv_history);
        spinnerNurse = findViewById(R.id.spinner_nurse);
        spinnerMonth = findViewById(R.id.spinner_month);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
        llErrorToast = findViewById(R.id.ll_error_toast);
        tvErrorMsg = findViewById(R.id.tv_error_toast_msg);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void updateUIStrings() {
        if (navbarManager != null) {
            navbarManager.updateRoleUI();
            navbarManager.updateLanguageUI();
        }
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.swap_history_title);
        ((TextView) findViewById(R.id.tv_subtitle)).setText(R.string.swap_history_subtitle);
        // Re-monta os spinners com o idioma atualizado e força re-bind dos cards
        if (spinnerNurse != null && spinnerMonth != null && !allSwaps.isEmpty()) {
            setupNurseSpinner();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupAdapter() {
        adapter = new SwapAdapter("", null, this, true);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void setupPaginationButtons() {
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            int maxPage = getMaxPage();
            if (currentPage < maxPage) { currentPage++; applyFilters(); }
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (currentPage > 1) { currentPage--; applyFilters(); }
        });
    }

    private void loadHistory() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getSwapHistory("Bearer " + token, "accepted", null, null, null)
                .enqueue(new Callback<SwapsResponse>() {
                    @Override
                    public void onResponse(Call<SwapsResponse> call, Response<SwapsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allSwaps.clear();
                            List<SwapRequest> data = response.body().getData();
                            if (data != null) allSwaps.addAll(data);
                            currentPage = 1;
                            setupNurseSpinner();
                            setupMonthSpinner();
                        } else {
                            showError(getString(R.string.err_loading_swaps));
                        }
                    }

                    @Override
                    public void onFailure(Call<SwapsResponse> call, Throwable t) {
                        showError(getString(R.string.err_loading_swaps));
                    }
                });
    }

    private void setupNurseSpinner() {
        LinkedHashSet<String> namesSet = new LinkedHashSet<>();
        for (SwapRequest swap : allSwaps) {
            if (swap.getParticipants() == null) continue;
            for (SwapRequest.SwapParticipant p : swap.getParticipants()) {
                if (p.getName() != null) namesSet.add(p.getName());
            }
        }
        List<String> nurseItems = new ArrayList<>();
        nurseItems.add(getString(R.string.filter_all_nurses));
        nurseItems.addAll(namesSet);

        ArrayAdapter<String> nurseAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, nurseItems);
        nurseAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerNurse.setAdapter(nurseAdapter);
        spinnerNurse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentPage = 1;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        setupMonthSpinner();
    }

    private void setupMonthSpinner() {
        String[] monthNames = getResources().getStringArray(R.array.months_array);
        List<String> monthItems = new ArrayList<>();
        monthItems.add(getString(R.string.filter_all_months));
        for (String m : monthNames) monthItems.add(m);

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item_selected, monthItems);
        monthAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentPage = 1;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        applyFilters();
    }

    private void applyFilters() {
        List<SwapRequest> filtered = getFiltered();
        int totalItems = filtered.size();
        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (maxPage < 1) maxPage = 1;
        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        List<Object> page = new ArrayList<>();
        if (start < totalItems) page.addAll(filtered.subList(start, end));
        adapter.setData(page);
        renderPagination(maxPage);
    }

    private List<SwapRequest> getFiltered() {
        int nursePos = spinnerNurse.getSelectedItemPosition();
        int monthPos = spinnerMonth.getSelectedItemPosition();

        String selectedNurse = nursePos > 0
                ? (String) spinnerNurse.getSelectedItem()
                : null;
        int selectedMonth = monthPos > 0 ? monthPos : 0; // 1-12, 0 = all

        List<SwapRequest> result = new ArrayList<>();
        for (SwapRequest swap : allSwaps) {
            if (selectedNurse != null && !participantHasName(swap, selectedNurse)) continue;
            if (selectedMonth > 0 && getSwapMonth(swap) != selectedMonth) continue;
            result.add(swap);
        }
        return result;
    }

    private boolean participantHasName(SwapRequest swap, String name) {
        if (swap.getParticipants() == null) return false;
        for (SwapRequest.SwapParticipant p : swap.getParticipants()) {
            if (name.equals(p.getName())) return true;
        }
        return false;
    }

    private int getSwapMonth(SwapRequest swap) {
        String iso = swap.getCreatedAt();
        if (iso == null) return 0;
        try {
            String normalized = iso.replaceAll("(\\.\\d{3})\\d+(Z)$", "$1$2");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(normalized);
            if (date == null) return 0;
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal.get(Calendar.MONTH) + 1; // 1-12
        } catch (ParseException e) {
            return 0;
        }
    }

    private int getMaxPage() {
        int total = getFiltered().size();
        return Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
    }

    private void renderPagination(int maxPage) {
        if (llPaginationNumbers == null) return;
        llPaginationNumbers.removeAllViews();

        // Lógica para mostrar apenas 3 números (janela deslizante)
        int startPage, endPage;
        if (maxPage <= 3) {
            startPage = 1;
            endPage = maxPage;
        } else {
            if (currentPage <= 2) {
                startPage = 1;
                endPage = 3;
            } else if (currentPage >= maxPage - 1) {
                startPage = maxPage - 2;
                endPage = maxPage;
            } else {
                startPage = currentPage - 1;
                endPage = currentPage + 1;
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            final int pageNum = i;
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setText(String.valueOf(i));
            tv.setTextSize(16);
            tv.setTypeface(null, Typeface.BOLD);

            if (i == currentPage) {
                tv.setBackgroundResource(R.drawable.bg_pagination);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setBackgroundResource(R.drawable.bg_action_card);
                tv.setTextColor(ContextCompat.getColor(this, R.color.primary_strong));
            }
            tv.setOnClickListener(v -> {
                currentPage = pageNum;
                applyFilters();
            });
            llPaginationNumbers.addView(tv);
        }

        ImageButton btnPrev = findViewById(R.id.btn_prev);
        ImageButton btnNext = findViewById(R.id.btn_next);
        btnPrev.setEnabled(currentPage > 1);
        btnPrev.setAlpha(currentPage > 1 ? 1.0f : 0.3f);
        btnNext.setEnabled(currentPage < maxPage);
        btnNext.setAlpha(currentPage < maxPage ? 1.0f : 0.3f);
    }

    private void showError(String msg) {
        if (tvErrorMsg == null || llErrorToast == null) return;
        tvErrorMsg.setText(msg);
        llErrorToast.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> llErrorToast.setVisibility(View.GONE), 3000);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
