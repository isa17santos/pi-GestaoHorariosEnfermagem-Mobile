package com.pi.gestaohorariosenfermagemmobile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class NotificationsActivity extends BaseActivity {
    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> allNotifications = new ArrayList<>();
    private String currentFilter = "all";
    private ApiService api;
    private NavbarManager navbarManager;

    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 4;
    private android.widget.LinearLayout llPaginationNumbers;
    private android.widget.ImageButton btnPrev, btnNext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        api = RetrofitClient.getClient(this).create(ApiService.class);
        navbarManager = new NavbarManager(this);

        initViews();
        updateUIStrings();
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rv_notifs);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        findViewById(R.id.btn_back_notif).setOnClickListener(v -> finish());
        findViewById(R.id.btn_all).setOnClickListener(v -> setFilter("all"));
        findViewById(R.id.btn_unread).setOnClickListener(v -> setFilter("unread"));
        findViewById(R.id.btn_read).setOnClickListener(v -> setFilter("read"));
        findViewById(R.id.btn_mark_all).setOnClickListener(v -> markAllRead());

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updateList();
            }
        });

        btnNext.setOnClickListener(v -> {
            List<Notification> filtered = getFilteredList();
            int maxPage = (int) Math.ceil((double) filtered.size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) {
                currentPage++;
                updateList();
            }
        });
    }

    private List<Notification> getFilteredList() {
        if (currentFilter.equals("unread")) {
            return allNotifications.stream().filter(n -> !n.isRead()).collect(java.util.stream.Collectors.toList());
        } else if (currentFilter.equals("read")) {
            return allNotifications.stream().filter(Notification::isRead).collect(java.util.stream.Collectors.toList());
        } else {
            return new ArrayList<>(allNotifications);
        }
    }

    @Override
    protected void updateUIStrings() {
        // 1. Atualizar textos estáticos da página
        MaterialButton btnBack = findViewById(R.id.btn_back_notif);
        if (btnBack != null) btnBack.setText(R.string.back);

        ((TextView)findViewById(R.id.tv_notif_page_title)).setText(R.string.notifications_title);
        ((TextView)findViewById(R.id.tv_notif_page_subtitle)).setText(R.string.notifications_subtitle);

        ((Button)findViewById(R.id.btn_all)).setText(R.string.notifications_filter_all);
        ((Button)findViewById(R.id.btn_unread)).setText(R.string.notifications_filter_unread);
        ((Button)findViewById(R.id.btn_read)).setText(R.string.notifications_filter_read);
        ((Button)findViewById(R.id.btn_mark_all)).setText(R.string.notifications_mark_all_read);

        TextView tvEmpty = findViewById(R.id.tv_empty_msg);
        if (tvEmpty != null) tvEmpty.setText(R.string.notifications_empty);

        // 2. FORÇAR ATUALIZAÇÃO DA LISTA (Data das notificações)
        if (adapter != null) {
            // notifyDataSetChanged() obriga o onBindViewHolder a ser chamado novamente,
            // relendo o idioma atual para formatar a data.
            adapter.notifyDataSetChanged();
        }


        updateTabStyles();
    }
    private void loadNotifications() {
        api.getNotifications().enqueue(new Callback<NotificationsResponse>() {
            @Override
            public void onResponse(Call<NotificationsResponse> call, Response<NotificationsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Ir buscar a lista dentro do .getData()
                    allNotifications = response.body().getData();
                    if (allNotifications == null) allNotifications = new ArrayList<>();

                    // Garantir que ao carregar do zero voltamos à primeira página
                    currentPage = 1;
                    updateList();
                }
            }
            @Override
            public void onFailure(Call<NotificationsResponse> call, Throwable t) {
                android.util.Log.e("API_ERROR", "Falha ao carregar: " + t.getMessage());
            }
        });
    }


    private void setFilter(String filter) {
        this.currentFilter = filter;
        this.currentPage = 1;
        updateTabStyles();
        updateList();
    }

    private void updateTabStyles() {
        // Lógica visual para destacar a tab ativa (estilo web)
        Button bAll = findViewById(R.id.btn_all);
        Button bUnread = findViewById(R.id.btn_unread);
        Button bRead = findViewById(R.id.btn_read);

        resetButtonStyle(bAll);
        resetButtonStyle(bUnread);
        resetButtonStyle(bRead);

        Button active = currentFilter.equals("all") ? bAll : (currentFilter.equals("unread") ? bUnread : bRead);
        active.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        active.setTextColor(Color.parseColor("#7C3AED"));
    }

    private void resetButtonStyle(Button b) {
        b.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.setTextColor(Color.parseColor("#8E87A3"));
    }

    private void updateList() {
        List<Notification> filtered = getFilteredList();

        View emptyState = findViewById(R.id.cv_empty_state);
        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            hidePagination();
        } else {
            emptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);

            int totalItems = filtered.size();
            int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
            if (currentPage > maxPage && maxPage > 0) currentPage = maxPage;
            if (currentPage < 1) currentPage = 1;

            int start = (currentPage - 1) * ITEMS_PER_PAGE;
            int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

            List<Notification> pagedList = filtered.subList(start, end);
            adapter = new NotificationAdapter(pagedList, this::markAsRead);
            rvNotifications.setAdapter(adapter);

            renderPagination(maxPage);
        }
    }

    private void renderPagination(int maxPage) {
        if (llPaginationNumbers == null) return;
        llPaginationNumbers.removeAllViews();
        findViewById(R.id.ll_pagination_container).setVisibility(View.VISIBLE);

        int startPage, endPage;
        if (maxPage <= 3) {
            startPage = 1; endPage = maxPage;
        } else {
            if (currentPage <= 2) {
                startPage = 1; endPage = 3;
            } else if (currentPage >= maxPage - 1) {
                startPage = maxPage - 2; endPage = maxPage;
            } else {
                startPage = currentPage - 1; endPage = currentPage + 1;
            }
        }

        for (int i = startPage; i <= endPage; i++) {
            final int pageNum = i;
            TextView tv = new TextView(this);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            tv.setLayoutParams(params);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setText(String.valueOf(i));
            tv.setTextSize(16);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);

            if (i == currentPage) {
                tv.setBackgroundResource(R.drawable.bg_pagination);
                tv.setTextColor(Color.WHITE);
            } else {
                tv.setBackgroundResource(R.drawable.bg_action_card);
                tv.setTextColor(Color.parseColor("#7C3AED"));
            }
            tv.setOnClickListener(v -> {
                currentPage = pageNum;
                updateList();
            });
            llPaginationNumbers.addView(tv);
        }

        btnPrev.setEnabled(currentPage > 1);
        btnPrev.setAlpha(currentPage > 1 ? 1.0f : 0.3f);
        btnNext.setEnabled(currentPage < maxPage);
        btnNext.setAlpha(currentPage < maxPage ? 1.0f : 0.3f);
    }

    private void hidePagination() {
        if (llPaginationNumbers != null) llPaginationNumbers.removeAllViews();
        View container = findViewById(R.id.ll_pagination_container);
        if (container != null) container.setVisibility(View.GONE);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void markAsRead(Notification notif) {
        if (notif.isRead()) return;
        api.markAsRead(notif.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    notif.setRead(true);
                    updateList();
                    // ATUALIZAÇÃO DINÂMICA:
                    if (navbarManager != null) {
                        navbarManager.refreshUnreadCount();
                    }
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
    private void markAllRead() {
        api.markAllAsRead().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 1. Atualizar o estado local das notificações
                    for (Notification n : allNotifications) {
                        n.setRead(true);
                    }

                    // 2. Atualizar a lista visual (RecyclerView)
                    updateList();

                    // 3. ATUALIZAÇÃO DINÂMICA DA NAVBAR:
                    // Força o Manager a ir buscar o novo contador (que será 0) à API
                    if (navbarManager != null) {
                        navbarManager.refreshUnreadCount();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Lógica de erro opcional
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navbarManager != null) navbarManager.refreshUnreadCount();
    }
}
