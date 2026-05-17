package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

public class HumanResourcesActivity extends BaseActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private EditText etSearch;
    private LinearLayout llPaginationNumbers;
    private String token;

    // Paginação
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 6;

    // Navbar & UI Strings
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private TextView tvTitle, tvSubtitle, tvHeaderName, tvHeaderRole, tvHeaderStatus, tvHeaderActions;
    private MaterialButton btnBack, btnCreateUser;

    private LinearLayout llNotification;

    private Spinner spinnerRole, spinnerStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_resources);

        // Apply window insets for keyboard handling
        applyWindowInsets(findViewById(android.R.id.content));

        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();
        setupNavbar();
        setupRecyclerView();
        setupPaginationActions();

        btnCreateUser.setOnClickListener(v -> startActivity(new Intent(this, UserCreateActivity.class)));

        // Carregar dados e aplicar strings iniciais
        updateUIStrings();
        updateLanguageButton();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) { applyFilters(); }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }
    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);
        etSearch = findViewById(R.id.et_search);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
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

        llNotification = findViewById(R.id.ll_notification_toast);

        // Carregar dados do utilizador
        android.content.SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("user_name", "Utilizador"));

        spinnerRole = findViewById(R.id.spinner_role);
        spinnerStatus = findViewById(R.id.spinner_status);

        updateUIStrings();

        setupFilterSpinners();

        // Listener para o Spinner de Cargo
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Listener para o Spinner de Estado
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupFilterSpinners() {
        // Guardar as seleções atuais para não as perder ao mudar o idioma
        int rolePos = spinnerRole != null ? spinnerRole.getSelectedItemPosition() : 0;
        int statusPos = spinnerStatus != null ? spinnerStatus.getSelectedItemPosition() : 0;

        // 1. Configuração do Dropdown de Cargo
        String[] roles = {
                getString(R.string.filter_all_roles),
                getString(R.string.filter_nurse),
                getString(R.string.filter_head_nurse)
        };

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, roles);
        roleAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setSelection(rolePos); // Restaura a posição

        // 2. Configuração do Dropdown de Estado
        String[] statusOptions = {
                getString(R.string.filter_all_status),
                getString(R.string.filter_active),
                getString(R.string.filter_inactive)
        };

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, statusOptions);
        statusAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setSelection(statusPos); // Restaura a posição
    }

    private void setupPaginationActions() {
        findViewById(R.id.btn_next).setOnClickListener(v -> {
            int maxPage = (int) Math.ceil((double) getSearchFilteredList().size() / ITEMS_PER_PAGE);
            if (currentPage < maxPage) {
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
        findViewById(R.id.img_logo).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

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
            finish();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";

        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(nextLang);
        AppCompatDelegate.setApplicationLocales(appLocales);

        // Atualiza os textos fixos da página
        updateUIStrings();

        // Atualiza o botão de idioma na Navbar
        updateLanguageButton();

        // Notifica o adapter para redesenhar a lista com o novo idioma
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void updateUIStrings() {
        // Atualiza todos os textos da página usando as strings traduzidas
        if (btnBack != null) btnBack.setText(R.string.back);
        if (tvTitle != null) tvTitle.setText(R.string.hr_management);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.hr_page_subtitle);
        if (etSearch != null) etSearch.setHint(R.string.search_hint);
        if (btnCreateUser != null) btnCreateUser.setText(R.string.create_user);
        if (tvHeaderName != null) tvHeaderName.setText(R.string.table_name);
        if (tvHeaderRole != null) tvHeaderRole.setText(R.string.table_role);
        if (tvHeaderStatus != null) tvHeaderStatus.setText(R.string.table_status);
        if (tvHeaderActions != null) tvHeaderActions.setText(R.string.table_actions);

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String role = prefs.getString("user_role", "").toLowerCase();

        if (role.equals("nurse")) {
            tvUserRole.setText(R.string.role_nurse);
        } else if (role.equals("head_nurse")) {
            tvUserRole.setText(R.string.role_head_nurse);
        } else if (role.equals("admin")) {
            tvUserRole.setText(R.string.role_admin);
        } else {
            tvUserRole.setText(role);
        }

        setupFilterSpinners();
    }

    @Override
    protected void updateLanguageButton() {
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
        int selectedRolePos = spinnerRole.getSelectedItemPosition();
        int selectedStatusPos = spinnerStatus.getSelectedItemPosition();

        List<User> temp = new ArrayList<>();
        for (User u : allUsers) {
            // Filtro por Nome
            boolean matchesName = u.getName().toLowerCase().contains(query);

            // Filtro por Cargo
            boolean matchesRole = true;
            if (selectedRolePos == 1) matchesRole = u.getFormattedRole().equals("nurse");
            else if (selectedRolePos == 2) matchesRole = u.getFormattedRole().equals("head_nurse");

            // Filtro por Estado
            boolean matchesStatus = true;
            if (selectedStatusPos == 1) matchesStatus = u.isActive();
            else if (selectedStatusPos == 2) matchesStatus = !u.isActive();

            if (matchesName && matchesRole && matchesStatus) {
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

            tv.setOnClickListener(v -> {
                currentPage = pageNum;
                applyFilters();
            });
            llPaginationNumbers.addView(tv);
        }

        findViewById(R.id.btn_prev).setEnabled(currentPage > 1);
        findViewById(R.id.btn_prev).setAlpha(currentPage > 1 ? 1.0f : 0.3f);
        findViewById(R.id.btn_next).setEnabled(currentPage < maxPage);
        findViewById(R.id.btn_next).setAlpha(currentPage < maxPage ? 1.0f : 0.3f);
    }

    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }


    private void onEditUser(User user) {
        Intent intent = new Intent(this, UserEditActivity.class);
        intent.putExtra("user_id", user.getId());
        intent.putExtra("user_name", user.getName());
        intent.putExtra("user_email", user.getEmail());
        intent.putExtra("user_role_slug", user.getFormattedRole());
        intent.putExtra("user_active", user.isActive());
        startActivity(intent);
    }

    private void onDeleteUser(User user) {
        // Capturar ecrã completo (DecorView) para o Blur cobrir tudo
        View decorView = getWindow().getDecorView();
        Bitmap screenshot = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        decorView.draw(canvas);
        Bitmap blurredBitmap = blur(screenshot);

        // Inflar layout do diálogo
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        // Criar o diálogo (usando um tema básico, vamos customizar a janela manualmente)
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        // Configurações críticas de Janela para FULLSCREEN e CENTRAMENTO
        dialog.show(); // Tem de ser chamado antes para configurar a Window

        if (dialog.getWindow() != null) {
            // Aplicar o blur como fundo total da janela
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.BitmapDrawable(getResources(), blurredBitmap));

            // Forçar a janela a ocupar o ecrã todo sem margens de sistema
            dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT);

            // Adicionar escurecimento (Dim) para destacar o pop-up branco
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.4f);
        }

        // Definir o conteúdo da vista
        dialog.setContentView(dialogView);

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        btnConfirm.setOnClickListener(v -> {
            // Desativar o botão para evitar cliques duplicados
            btnConfirm.setEnabled(false);

            // CHAMADA À API (Remoção Real na Base de Dados)
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            api.deleteUser("Bearer " + token, user.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // SUCESSO: Remover da lista local, atualizar filtros e mostrar notificação
                        allUsers.remove(user);
                        applyFilters();
                        showSuccessNotification(getString(R.string.delete_success));
                        dialog.dismiss();
                    } else {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(HumanResourcesActivity.this, "Erro no servidor ao remover", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(HumanResourcesActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show();
                }
            });
        });

        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> {
            // Fecha o pop-up e volta para a página anterior
            dialog.dismiss();
        });
    }

    private Bitmap blur(Bitmap image) {
        if (image == null) return null;
        Bitmap outputBitmap = Bitmap.createBitmap(image);
        RenderScript rs = RenderScript.create(this);
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation allIn = Allocation.createFromBitmap(rs, image);
        Allocation allOut = Allocation.createFromBitmap(rs, outputBitmap);
        blurScript.setRadius(20f); // Intensidade do blur (0-25)
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        allOut.copyTo(outputBitmap);
        rs.destroy();
        return outputBitmap;
    }

    private Bitmap screenshot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void showSuccessNotification(String message) {
        TextView tvMsg = findViewById(R.id.tv_notification_msg);
        tvMsg.setText(message);

        llNotification.setVisibility(View.VISIBLE);
        llNotification.setAlpha(0f);
        llNotification.setTranslationY(-100f); // Começa um pouco acima do ecrã

        // Animação de entrada
        llNotification.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setListener(null);

        // Esconder automaticamente após 3 segundos
        new android.os.Handler().postDelayed(() -> {
            llNotification.animate()
                    .alpha(0f)
                    .translationY(-100f)
                    .setDuration(400)
                    .withEndAction(() -> llNotification.setVisibility(View.GONE));
        }, 3000);
    }
}