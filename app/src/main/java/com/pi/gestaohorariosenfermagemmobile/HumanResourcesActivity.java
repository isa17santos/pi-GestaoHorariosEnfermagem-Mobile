package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HumanResourcesActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private EditText etSearch;
    private String token;

    // Paginação
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;
    private TextView tvPageInfo;

    // Navbar & UI Strings
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private TextView tvTitle, tvSubtitle, tvHeaderName, tvHeaderRole, tvHeaderStatus, tvHeaderActions;
    private MaterialButton btnBack, btnCreateUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_resources);

        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();
        setupNavbar();
        setupRecyclerView();
        setupPagination();

        // Carregar dados e aplicar strings iniciais
        updateUIStrings();
        updateLanguageButton();

        loadUsers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { applyFilters(); }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);
        etSearch = findViewById(R.id.et_search);
        tvPageInfo = findViewById(R.id.tv_page_info);
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

        // Referências para textos que precisam de mudar de idioma
        btnBack = findViewById(R.id.btn_back);
        btnCreateUser = findViewById(R.id.btn_create_user);

        // Adicionar IDs aos títulos no XML para que possamos referenciá-los aqui
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        tvHeaderName = findViewById(R.id.tv_header_name);
        tvHeaderRole = findViewById(R.id.tv_header_role);
        tvHeaderStatus = findViewById(R.id.tv_header_status);
        tvHeaderActions = findViewById(R.id.tv_header_actions);

        // Carregar dados do utilizador
        android.content.SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("user_name", "Utilizador"));
        tvUserRole.setText(prefs.getString("user_role", "Admin"));
    }

    private void setupPagination() {
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            List<User> searchFiltered = getSearchFilteredList();
            if (currentPage * ITEMS_PER_PAGE < searchFiltered.size()) {
                currentPage++;
                applyFilters();
            }
        });

        findViewById(R.id.btn_prev).setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                applyFilters();
            }
        });
    }

    private void setupNavbar() {
        MaterialCardView btnLang = findViewById(R.id.btn_language_switch);
        if (btnLang != null) {
            btnLang.setOnClickListener(v -> toggleLanguage());
        }

        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            // Limpar SharedPreferences (Token Morre)
            getSharedPreferences("AUTH", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            // Redirecionar para o Login limpando a pilha de atividades
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);

            // Terminar esta atividade
            finish();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";

        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(nextLang);
        AppCompatDelegate.setApplicationLocales(appLocales);

        updateUIStrings();
        updateLanguageButton();
    }

    private void updateUIStrings() {
        // Atualiza todos os textos da página usando as strings traduzidas
        if (btnBack != null) btnBack.setText(R.string.back);
        if (tvTitle != null) tvTitle.setText(R.string.hr_management);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.hr_page_subtitle);
        if (etSearch != null) etSearch.setHint(R.string.search_hint);
        if (btnCreateUser != null) btnCreateUser.setText(R.string.create_user);
    }

    private void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(filteredUsers, this::onEditUser, this::onDeleteUser);
        rvUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getUsers("Bearer " + token).enqueue(new Callback<UsersResponse>() {
            @Override
            public void onResponse(Call<UsersResponse> call, Response<UsersResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    List<User> data = response.body().getData();

                    if (data != null) {
                        for (User u : data) {
                            String role = u.getFormattedRole();
                            // Se NÃO for admin, adicionamos (nurse ou head_nurse)
                            if (!role.equals("admin")) {
                                allUsers.add(u);
                            }
                        }
                    }

                    // Forçar a primeira página e aplicar os filtros/exibição
                    currentPage = 1;
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<UsersResponse> call, Throwable t) {
                Toast.makeText(HumanResourcesActivity.this, "Erro ao carregar utilizadores", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<User> getSearchFilteredList() {
        String query = etSearch.getText().toString().toLowerCase();
        List<User> temp = new ArrayList<>();
        for (User u : allUsers) {
            if (u.getName().toLowerCase().contains(query)) {
                temp.add(u);
            }
        }
        return temp;
    }

    private void applyFilters() {
        List<User> searchFiltered = getSearchFilteredList();

        // Calcula o total de páginas para evitar erros
        int totalItems = searchFiltered.size();
        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        // Proteção: se a página atual for maior que o máximo, volta para a última válida
        if (currentPage > maxPage && maxPage > 0) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        filteredUsers.clear();
        if (start < totalItems) {
            // subList(start, end) pega exatamente os 6 itens da página
            filteredUsers.addAll(searchFiltered.subList(start, end));
        }

        adapter.notifyDataSetChanged();

        // Atualiza o texto da página (ex: "1")
        tvPageInfo.setText(String.valueOf(currentPage));

        // Opcional: Desativar botões se não houver mais páginas
        findViewById(R.id.btn_prev).setAlpha(currentPage > 1 ? 1.0f : 0.3f);
        findViewById(R.id.btn_prev).setEnabled(currentPage > 1);

        findViewById(R.id.btn_next).setAlpha(currentPage < maxPage ? 1.0f : 0.3f);
        findViewById(R.id.btn_next).setEnabled(currentPage < maxPage);
    }

    private void onEditUser(User user) { }

    private void onDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_removal)
                .setMessage(R.string.delete_confirmation_msg)
                .setPositiveButton(R.string.yes_remove, (dialog, which) -> {
                    allUsers.remove(user);
                    applyFilters();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}