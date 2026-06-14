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

        findViewById(R.id.btn_back_notif).setOnClickListener(v -> finish());
        findViewById(R.id.btn_all).setOnClickListener(v -> setFilter("all"));
        findViewById(R.id.btn_unread).setOnClickListener(v -> setFilter("unread"));
        findViewById(R.id.btn_read).setOnClickListener(v -> setFilter("read"));
        findViewById(R.id.btn_mark_all).setOnClickListener(v -> markAllRead());
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
                    // Aqui está o segredo: ir buscar a lista dentro do .getData()
                    allNotifications = response.body().getData();
                    if (allNotifications == null) allNotifications = new ArrayList<>();
                    updateList();
                }
            }
            @Override public void onFailure(Call<NotificationsResponse> call, Throwable t) {
                android.util.Log.e("API_ERROR", "Falha ao carregar: " + t.getMessage());
            }
        });
    }


    private void setFilter(String filter) {
        this.currentFilter = filter;
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
        List<Notification> filtered;
        if (currentFilter.equals("unread")) {
            filtered = allNotifications.stream().filter(n -> !n.isRead()).collect(Collectors.toList());
        } else if (currentFilter.equals("read")) {
            filtered = allNotifications.stream().filter(Notification::isRead).collect(Collectors.toList());
        } else {
            filtered = allNotifications;
        }

        // Lógica de Visibilidade do Estado Vazio
        View emptyState = findViewById(R.id.cv_empty_state);
        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }

        adapter = new NotificationAdapter(filtered, this::markAsRead);
        rvNotifications.setAdapter(adapter);
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
