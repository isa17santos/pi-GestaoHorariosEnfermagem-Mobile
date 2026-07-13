package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class SwapsActivity extends BaseActivity {

    private RecyclerView rvSwaps;
    private SwapAdapter adapter;
    private NavbarManager navbarManager;

    private List<SwapRequest> allSwaps = new ArrayList<>();

    private LinearLayout llPaginationNumbers;
    private LinearLayout llNotification;
    private LinearLayout llErrorToast;

    private Spinner spinnerDirection, spinnerStatus;

    private String token;
    private String currentUserName;

    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 4;

    // Garante que setupFilterSpinners define statusPos=1 apenas na primeira chamada
    private boolean spinnersInitialized = false;

    private TextView tvTitle, tvSubtitle;
    private MaterialButton btnBack, btnNewSwap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swaps);

        navbarManager = new NavbarManager(this);
        applyWindowInsets(findViewById(android.R.id.content));

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");
        currentUserName = prefs.getString("user_name", "");

        initViews();
        setupRecyclerView();
        setupPaginationActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSwaps();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
        String successMsg = getIntent().getStringExtra("SUCCESS_MSG");
        if (successMsg != null) {
            getIntent().removeExtra("SUCCESS_MSG");
            new android.os.Handler().postDelayed(() -> showSuccessNotification(successMsg), 400);
        }
    }

    private void initViews() {
        rvSwaps = findViewById(R.id.rv_swaps);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
        llNotification = findViewById(R.id.ll_notification_toast);
        llErrorToast = findViewById(R.id.ll_error_toast);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnBack = findViewById(R.id.btn_back);
        btnNewSwap = findViewById(R.id.btn_new_swap);

        spinnerDirection = findViewById(R.id.spinner_direction);
        spinnerStatus = findViewById(R.id.spinner_status);

        btnBack.setOnClickListener(v -> finish());

        btnNewSwap.setOnClickListener(v ->
                startActivity(new Intent(this, SwapSelectActivity.class))
        );

        // updateUIStrings chama setupFilterSpinners uma única vez (primeira inicialização)
        updateUIStrings();

        spinnerDirection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                loadSwaps();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                loadSwaps();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterSpinners() {
        int dirPos = spinnersInitialized ? spinnerDirection.getSelectedItemPosition() : 0;
        int statusPos = spinnersInitialized ? spinnerStatus.getSelectedItemPosition() : 0;
        spinnersInitialized = true;

        String[] directions = {
                getString(R.string.filter_all_directions),
                getString(R.string.filter_sent),
                getString(R.string.filter_received)
        };
        ArrayAdapter<String> dirAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, directions);
        dirAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerDirection.setAdapter(dirAdapter);
        spinnerDirection.setSelection(dirPos);

        String[] statuses = {
                getString(R.string.filter_all_statuses),
                getString(R.string.filter_pending),
                getString(R.string.filter_accepted),
                getString(R.string.filter_rejected),
                getString(R.string.filter_cancelled)
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, statuses);
        statusAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setSelection(statusPos);
    }

    private void setupRecyclerView() {
        rvSwaps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SwapAdapter(currentUserName, new SwapAdapter.OnSwapActionListener() {
            @Override public void onAccept(SwapRequest swap) { confirmAction(swap, "accept"); }
            @Override public void onReject(SwapRequest swap) { confirmAction(swap, "reject"); }
            @Override public void onCancel(SwapRequest swap) { executeAction(swap, "cancel"); }
        }, this);
        rvSwaps.setAdapter(adapter);
    }

    private void setupPaginationActions() {
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) allSwaps.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) { currentPage++; applyFilters(); }
        });
        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (currentPage > 1) { currentPage--; applyFilters(); }
        });
    }

    // Carrega os pedidos da API com os filtros actuais dos spinners
    private void loadSwaps() {
        String direction = directionQueryParam();
        String status = statusQueryParam();

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getSwaps("Bearer " + token, direction, status).enqueue(new Callback<SwapsResponse>() {
            @Override
            public void onResponse(Call<SwapsResponse> call, Response<SwapsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allSwaps.clear();
                    List<SwapRequest> data = response.body().getData();
                    if (data != null) allSwaps.addAll(data);
                    currentPage = 1;
                    applyFilters();
                }
            }
            @Override
            public void onFailure(Call<SwapsResponse> call, Throwable t) {
                showErrorNotification(getString(R.string.err_loading_swaps));
            }
        });
    }

    // Posição 0 = sem filtro (null); 1 = "sent"; 2 = "received"
    private String directionQueryParam() {
        switch (spinnerDirection.getSelectedItemPosition()) {
            case 1: return "sent";
            case 2: return "received";
            default: return null;
        }
    }

    // Posição 0 = sem filtro (null); 1-4 mapeiam para estados
    private String statusQueryParam() {
        switch (spinnerStatus.getSelectedItemPosition()) {
            case 1: return "pending";
            case 2: return "accepted";
            case 3: return "rejected";
            case 4: return "cancelled";
            default: return null;
        }
    }

    private void applyFilters() {
        boolean isAllDirections = spinnerDirection.getSelectedItemPosition() == 0;

        if (isAllDirections) {
            // Modo secções: mostrar todos sem paginação, com cabeçalhos "Enviados"/"Recebidos"
            adapter.setData(buildSectionedList(allSwaps));
            hidePagination();
        } else {
            // Modo lista simples: paginar normalmente
            int totalItems = allSwaps.size();
            int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
            if (currentPage > maxPage && maxPage > 0) currentPage = maxPage;
            if (currentPage < 1) currentPage = 1;

            int start = (currentPage - 1) * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

            List<Object> flat = new ArrayList<>();
            if (start < totalItems) flat.addAll(allSwaps.subList(start, end));
            adapter.setData(flat);
            renderPagination(maxPage);
        }
    }

    // Constrói lista mista com cabeçalhos de secção quando direction = "Todas"
    // Usa o nome do utilizador autenticado para separar enviados (requester) de recebidos (target)
    private List<Object> buildSectionedList(List<SwapRequest> swaps) {
        List<SwapRequest> sent     = new ArrayList<>();
        List<SwapRequest> received = new ArrayList<>();

        for (SwapRequest swap : swaps) {
            if (swap.getParticipants() == null) continue;
            boolean isSender = false;
            for (SwapRequest.SwapParticipant p : swap.getParticipants()) {
                if ("requester".equals(p.getRole()) && currentUserName.equals(p.getName())) {
                    isSender = true;
                    break;
                }
            }
            if (isSender) sent.add(swap);
            else received.add(swap);
        }

        // Pendentes primeiro em cada secção
        sortPendingFirst(sent);
        sortPendingFirst(received);

        long sentPending     = countPending(sent);
        long receivedPending = countPending(received);

        // A secção com mais pendentes aparece primeiro
        List<Object> mixed = new ArrayList<>();
        boolean receivedFirst = receivedPending > sentPending;

        if (receivedFirst) {
            addSection(mixed, getString(R.string.section_received), received);
            addSection(mixed, getString(R.string.section_sent), sent);
        } else {
            addSection(mixed, getString(R.string.section_sent), sent);
            addSection(mixed, getString(R.string.section_received), received);
        }
        return mixed;
    }

    private void sortPendingFirst(List<SwapRequest> list) {
        list.sort((a, b) -> {
            boolean aPending = "pending".equals(a.getStatus());
            boolean bPending = "pending".equals(b.getStatus());
            if (aPending == bPending) return 0;
            return aPending ? -1 : 1;
        });
    }

    private long countPending(List<SwapRequest> list) {
        long count = 0;
        for (SwapRequest s : list) if ("pending".equals(s.getStatus())) count++;
        return count;
    }

    private void addSection(List<Object> mixed, String header, List<SwapRequest> items) {
        if (!items.isEmpty()) {
            mixed.add(header);
            mixed.addAll(items);
        }
    }

    private void hidePagination() {
        if (llPaginationNumbers != null) llPaginationNumbers.removeAllViews();
        findViewById(R.id.btn_prev).setEnabled(false);
        findViewById(R.id.btn_prev).setAlpha(0f);
        findViewById(R.id.btn_next).setEnabled(false);
        findViewById(R.id.btn_next).setAlpha(0f);
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

    // Mostra diálogo de confirmação com efeito blur antes de executar a acção
    private void confirmAction(SwapRequest swap, String action) {
        View decorView = getWindow().getDecorView();
        Bitmap screenshot = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);
        decorView.draw(new Canvas(screenshot));
        Bitmap blurred = blur(screenshot);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_swap_action, null);
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

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_message);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btn_confirm_action);

        switch (action) {
            case "accept":
                tvTitle.setText(R.string.confirm_accept_swap_title);
                tvMsg.setText(R.string.confirm_accept_swap);
                btnConfirm.setText(R.string.btn_accept_swap);
                break;
            case "reject":
                tvTitle.setText(R.string.confirm_reject_swap_title);
                tvMsg.setText(R.string.confirm_reject_swap);
                btnConfirm.setText(R.string.btn_reject_swap);
                break;
            case "cancel":
                tvTitle.setText(R.string.confirm_cancel_swap_title);
                tvMsg.setText(R.string.confirm_cancel_swap);
                btnConfirm.setText(R.string.btn_cancel_swap);
                break;
        }

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setEnabled(false);
            dialog.dismiss();
            executeAction(swap, action);
        });
    }

    // Executa a chamada à API para aceitar, rejeitar ou cancelar
    private void executeAction(SwapRequest swap, String action) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        Call<Void> call;

        switch (action) {
            case "accept": call = api.acceptSwap("Bearer " + token, swap.getId()); break;
            case "reject": call = api.rejectSwap("Bearer " + token, swap.getId()); break;
            case "cancel": call = api.cancelSwap("Bearer " + token, swap.getId()); break;
            default: return;
        }

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> c, Response<Void> response) {
                if (response.isSuccessful()) {
                    String msg;
                    switch (action) {
                        case "accept": msg = getString(R.string.swap_accepted_success); break;
                        case "reject": msg = getString(R.string.swap_rejected_success); break;
                        default:       msg = getString(R.string.swap_cancelled_success);
                    }
                    showSuccessNotification(msg);
                    loadSwaps();
                } else {
                    showErrorNotification(getString(R.string.err_network));
                }
            }
            @Override
            public void onFailure(Call<Void> c, Throwable t) {
                showErrorNotification(getString(R.string.err_network));
            }
        });
    }

    private Bitmap blur(Bitmap image) {
        if (image == null) return null;
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

    private void showSuccessNotification(String message) {
        TextView tvMsg = findViewById(R.id.tv_notification_msg);
        tvMsg.setText(message);
        llNotification.setVisibility(View.VISIBLE);
        llNotification.setAlpha(0f);
        llNotification.setTranslationY(-100f);
        llNotification.animate().alpha(1f).translationY(0f).setDuration(400).setListener(null);
        new android.os.Handler().postDelayed(() ->
                llNotification.animate().alpha(0f).translationY(-100f).setDuration(400)
                        .withEndAction(() -> llNotification.setVisibility(View.GONE)), 3000);
    }

    private void showErrorNotification(String message) {
        if (llErrorToast == null) return;
        TextView tvMsg = llErrorToast.findViewById(R.id.tv_error_toast_msg);
        if (tvMsg != null) tvMsg.setText(message);
        llErrorToast.setVisibility(View.VISIBLE);
        llErrorToast.setAlpha(0f);
        llErrorToast.setTranslationY(-100f);
        llErrorToast.animate().alpha(1f).translationY(0f).setDuration(400).setListener(null);
        new android.os.Handler().postDelayed(() ->
                llErrorToast.animate().alpha(0f).translationY(-100f).setDuration(400)
                        .withEndAction(() -> llErrorToast.setVisibility(View.GONE)), 3000);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void updateUIStrings() {
        if (btnBack != null) btnBack.setText(R.string.back);
        if (tvTitle != null) tvTitle.setText(R.string.swaps_page_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.swaps_page_subtitle);
        if (btnNewSwap != null) btnNewSwap.setText(R.string.btn_new_swap);
        setupFilterSpinners();
    }

    @Override
    protected void updateLanguageButton() {
        // O NavbarManager já gere o botão de idioma da navbar
    }
}
