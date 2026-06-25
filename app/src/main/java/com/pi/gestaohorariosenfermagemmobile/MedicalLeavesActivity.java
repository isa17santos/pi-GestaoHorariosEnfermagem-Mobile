package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import androidx.appcompat.app.AlertDialog;

public class MedicalLeavesActivity extends BaseActivity {

    private RecyclerView rvLeaves;
    private MedicalLeaveAdapter adapter;
    private List<MedicalLeave> allLeaves = new ArrayList<>();
    private List<MedicalLeave> filteredLeaves = new ArrayList<>();
    private EditText etSearch;
    private Spinner spinnerStatus;
    private LinearLayout llPaginationNumbers;
    private TextView tvTitle, tvSubtitle, tvHeaderName, tvHeaderStart, tvHeaderEnd, tvHeaderReason, tvHeaderStatus, tvHeaderActions;
    private MaterialButton btnBack, btnCreateLeave;
    private ImageButton btnPrev, btnNext;

    private NavbarManager navbarManager;
    private String token;
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;
    private boolean isUpdatingUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_leaves);

        // RESTAURAR PÁGINA APÓS MUDANÇA DE IDIOMA
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt("current_page", 1);
        }

        navbarManager = new NavbarManager(this);
        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();
        setupRecyclerView();
        updateUIStrings(); // Atualiza textos e cabeçalhos
    }

    // GUARDAR PÁGINA ATUAL
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_page", currentPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicalLeaves();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void initViews() {
        rvLeaves = findViewById(R.id.rv_leaves);
        etSearch = findViewById(R.id.et_search);
        spinnerStatus = findViewById(R.id.spinner_status);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnBack = findViewById(R.id.btn_back);
        btnCreateLeave = findViewById(R.id.btn_create_leave);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        // IDs dos cabeçalhos para tradução
        tvHeaderName = findViewById(R.id.tv_header_name);
        tvHeaderStart = findViewById(R.id.tv_header_start);
        tvHeaderEnd = findViewById(R.id.tv_header_end);
        tvHeaderReason = findViewById(R.id.tv_header_reason);
        tvHeaderStatus = findViewById(R.id.tv_header_status);
        tvHeaderActions = findViewById(R.id.tv_header_actions);

        btnCreateLeave = findViewById(R.id.btn_create_leave);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnCreateLeave != null) {
            btnCreateLeave.setOnClickListener(v -> {
                Intent intent = new Intent(MedicalLeavesActivity.this, MedicalLeaveCreateActivity.class);
                startActivity(intent);
            });
        }
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdatingUI) { currentPage = 1; applyFilters(); }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (!isUpdatingUI) { currentPage = 1; applyFilters(); }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                int maxPage = (int) Math.ceil((double) getFilteredList().size() / ITEMS_PER_PAGE);
                if (currentPage < maxPage) { currentPage++; applyFilters(); }
            });
        }
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (currentPage > 1) { currentPage--; applyFilters(); }
            });
        }
    }

    private void setupFilterSpinner() {
        int currentPos = spinnerStatus.getSelectedItemPosition();
        String[] options = {
                getString(R.string.filter_all_status),
                getString(R.string.status_ongoing),
                getString(R.string.status_future),
                getString(R.string.status_past)
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, options);
        statusAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setSelection(currentPos);
    }

    private void setupRecyclerView() {
        if (rvLeaves == null) return;
        rvLeaves.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicalLeaveAdapter(filteredLeaves, new MedicalLeaveAdapter.OnActionClickListener() {
            @Override public void onEdit(MedicalLeave leave) {
                Intent intent = new Intent(MedicalLeavesActivity.this, MedicalLeaveEditActivity.class);
                intent.putExtra("leave_id", leave.getId());
                intent.putExtra("user_id", leave.getUserId());
                intent.putExtra("start_date", leave.getStartDate());
                intent.putExtra("end_date", leave.getEndDate());
                intent.putExtra("reason", leave.getReason());
                startActivity(intent);
            }
            @Override public void onDelete(MedicalLeave leave) {
                onDeleteLeave(leave);
            }
        });
        rvLeaves.setAdapter(adapter);
    }

    private void onDeleteLeave(MedicalLeave leave) {
        // 1. Capturar ecrã e aplicar Blur
        View decorView = getWindow().getDecorView();
        Bitmap screenshot = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        decorView.draw(canvas);
        Bitmap blurredBitmap = blur(screenshot);

        // 2. Inflar o layout do diálogo de confirmação que já tens
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.BitmapDrawable(getResources(), blurredBitmap));
            dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.4f);
        }

        dialog.setContentView(dialogView);

        // 3. Configurar botões do Modal
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false); // Evitar cliques duplos

            // 4. Chamada à API para eliminar
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            api.deleteMedicalLeave("Bearer " + token, leave.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        allLeaves.remove(leave);
                        applyFilters();
                        showSuccessNotification(getString(R.string.delete_success));
                        dialog.dismiss();
                    } else {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(MedicalLeavesActivity.this, "Erro ao eliminar no servidor", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(MedicalLeavesActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Métodos auxiliares de UI (Iguais aos que tens na HumanResourcesActivity)
    private Bitmap blur(Bitmap image) {
        if (image == null) return null;
        Bitmap outputBitmap = Bitmap.createBitmap(image);
        RenderScript rs = RenderScript.create(this);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, image);
        Allocation allOut = Allocation.createFromBitmap(rs, outputBitmap);
        blurScript.setRadius(20f);
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outputBitmap);
        rs.destroy();
        return outputBitmap;
    }

    private void showSuccessNotification(String message) {
        LinearLayout llNotification = findViewById(R.id.ll_notification_toast);
        TextView tvMsg = findViewById(R.id.tv_notification_msg);
        if (llNotification == null || tvMsg == null) return;

        tvMsg.setText(message);
        llNotification.setVisibility(View.VISIBLE);
        llNotification.setAlpha(0f);
        llNotification.setTranslationY(-100f);

        llNotification.animate().alpha(1f).translationY(0f).setDuration(400).setListener(null);

        new android.os.Handler().postDelayed(() -> {
            llNotification.animate().alpha(0f).translationY(-100f).setDuration(400)
                    .withEndAction(() -> llNotification.setVisibility(View.GONE));
        }, 3000);
    }

    private void loadMedicalLeaves() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getMedicalLeaves("Bearer " + token).enqueue(new Callback<MedicalLeavesResponse>() {
            @Override
            public void onResponse(Call<MedicalLeavesResponse> call, Response<MedicalLeavesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allLeaves.clear();
                    allLeaves.addAll(response.body().getData());
                    applyFilters(); // Aplica com a currentPage restaurada
                }
            }
            @Override public void onFailure(Call<MedicalLeavesResponse> call, Throwable t) {}
        });
    }

    private List<MedicalLeave> getFilteredList() {
        String query = etSearch.getText().toString().toLowerCase();
        int statusPos = spinnerStatus.getSelectedItemPosition();
        List<MedicalLeave> temp = new ArrayList<>();

        for (MedicalLeave ml : allLeaves) {
            boolean matchesName = ml.getUser() != null && ml.getUser().getName() != null &&
                    ml.getUser().getName().toLowerCase().contains(query);

            String calcStatus = calculateInternalStatus(ml.getStartDate(), ml.getEndDate());

            boolean matchesStatus = statusPos == 0 ||
                    (statusPos == 1 && "ongoing".equals(calcStatus)) ||
                    (statusPos == 2 && "future".equals(calcStatus)) ||
                    (statusPos == 3 && "past".equals(calcStatus));

            if (matchesName && matchesStatus) temp.add(ml);
        }
        return temp;
    }

    private String calculateInternalStatus(String startIso, String endIso) {
        if (startIso == null || endIso == null) return "past";
        try {
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
            String start = startIso.substring(0, 10);
            String end = endIso.substring(0, 10);
            if (today.compareTo(start) < 0) return "future";
            if (today.compareTo(end) > 0) return "past";
            return "ongoing";
        } catch (Exception e) { return "past"; }
    }

    private void applyFilters() {
        if (adapter == null) return;

        List<MedicalLeave> filtered = getFilteredList();
        int total = filtered.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));

        // Só ajusta se a página atual for inválida para os novos filtros
        if (currentPage > maxPage && total > 0) currentPage = maxPage;

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, total);

        filteredLeaves.clear();
        if (start < total && start >= 0) {
            filteredLeaves.addAll(filtered.subList(start, end));
        }
        adapter.notifyDataSetChanged();
        renderPagination(maxPage);
    }

    private void renderPagination(int maxPage) {
        if (llPaginationNumbers == null) return;
        llPaginationNumbers.removeAllViews();
        for (int i = 1; i <= maxPage; i++) {
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
            tv.setOnClickListener(v -> { currentPage = pageNum; applyFilters(); });
            llPaginationNumbers.addView(tv);
        }
    }

    private int dpToPx(int dp) { return Math.round((float) dp * getResources().getDisplayMetrics().density); }

    @Override
    protected void updateUIStrings() {
        isUpdatingUI = true;
        if (tvTitle != null) tvTitle.setText(R.string.medical_leaves_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.medical_leaves_subtitle);
        if (btnBack != null) btnBack.setText(R.string.back);
        if (btnCreateLeave != null) btnCreateLeave.setText(R.string.assign_leave);
        if (etSearch != null) etSearch.setHint(R.string.search_hint);

        // Cabeçalhos Bilingues
        if (tvHeaderName != null) tvHeaderName.setText(R.string.table_name);
        if (tvHeaderStart != null) tvHeaderStart.setText(R.string.table_start_date);
        if (tvHeaderEnd != null) tvHeaderEnd.setText(R.string.table_end_date);
        if (tvHeaderReason != null) tvHeaderReason.setText(R.string.table_reason);
        if (tvHeaderStatus != null) tvHeaderStatus.setText(R.string.table_status);
        if (tvHeaderActions != null) tvHeaderActions.setText(R.string.table_actions);

        setupFilterSpinner();

        // Só chama applyFilters se já houver dados para evitar resetar a página prematuramente
        if (!allLeaves.isEmpty()) {
            applyFilters();
        }
        isUpdatingUI = false;
    }
}