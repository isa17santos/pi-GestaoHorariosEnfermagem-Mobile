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

public class ShiftTypesActivity extends BaseActivity {
    private RecyclerView rvShiftTypes;
    private ShiftTypeAdapter adapter;
    private List<ShiftType> allShiftTypes = new ArrayList<>();
    private List<ShiftType> filteredShiftTypes = new ArrayList<>();
    private EditText etSearch;
    private LinearLayout llPaginationNumbers;
    private String token;

    // Pagination
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 8;

    // Navbar & UI Strings
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private TextView tvTitle, tvSubtitle, tvHeaderName, tvHeaderColor, tvHeaderSchedule, tvHeaderMinNurses, tvHeaderActions;
    private MaterialButton btnBack, btnCreate;

    private LinearLayout llNotification;

    private Spinner spinnerFilter;

    // Initialize the screen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_types);

        // Apply window insets for keyboard handling
        applyWindowInsets(findViewById(android.R.id.content));

        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();
        setupNavbar();
        setupRecyclerView();
        setupPaginationActions();

        btnCreate.setOnClickListener(v -> startActivity(new Intent(this, ShiftTypeCreateActivity.class)));

        // Load data and apply initial strings
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

    // Refresh data when returning to the screen
    @Override
    protected void onResume() {
        super.onResume();
        loadShiftTypes();
    }

    // Bind all views from the layout
    private void initViews() {
        rvShiftTypes = findViewById(R.id.rv_shift_types);
        etSearch = findViewById(R.id.et_search);
        llPaginationNumbers = findViewById(R.id.ll_pagination_numbers);
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

        // References for texts that need language updates
        btnBack = findViewById(R.id.btn_back);
        btnCreate = findViewById(R.id.btn_create);

        // Add title IDs from XML so we can reference them here
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        tvHeaderName = findViewById(R.id.tv_header_name);
        tvHeaderColor = findViewById(R.id.tv_header_color);
        tvHeaderSchedule = findViewById(R.id.tv_header_schedule);
        tvHeaderMinNurses = findViewById(R.id.tv_header_min_nurses);
        tvHeaderActions = findViewById(R.id.tv_header_actions);

        llNotification = findViewById(R.id.ll_notification_toast);

        // Load user data
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("user_name", "Utilizador"));

        spinnerFilter = findViewById(R.id.spinner_filter);

        updateUIStrings();

        setupFilterSpinner();

        // Listener for the filter spinner
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPage = 1;
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // Configure the shift type filter dropdown
    private void setupFilterSpinner() {
        // Guard to avoid an invalid position
        int filterPos = spinnerFilter != null ? spinnerFilter.getSelectedItemPosition() : 0;

        // Shift type dropdown configuration
        String[] filterOptions = {
                getString(R.string.filter_all_shift_types),
                getString(R.string.filter_work_shifts),
                getString(R.string.filter_non_work_shifts)
        };

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, filterOptions);
        filterAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setSelection(filterPos); // Restore selected position
    }

    // Configure pagination buttons
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

    // Configure the RecyclerView and adapter
    private void setupRecyclerView() {
        rvShiftTypes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShiftTypeAdapter(filteredShiftTypes, this::onEditShiftType, this::onDeleteShiftType);
        rvShiftTypes.setAdapter(adapter);
    }


    // Configure the navbar actions
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
            // Clear SharedPreferences
            getSharedPreferences("AUTH", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            // Redirect to Login and clear the activity stack
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // Toggle the application language
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

    // Update all translated texts on the screen
    @Override
    protected void updateUIStrings() {
        // Atualiza todos os textos da página usando as strings traduzidas
        if (btnBack != null) btnBack.setText(R.string.back);
        if (tvTitle != null) tvTitle.setText(R.string.shift_types);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.shift_types_subtitle);
        if (etSearch != null) etSearch.setHint(R.string.search_hint);
        if (btnCreate != null) btnCreate.setText(R.string.create_shift_type);
        if (tvHeaderName != null) tvHeaderName.setText(R.string.table_name);
        if (tvHeaderColor != null) tvHeaderColor.setText(R.string.col_color);
        if (tvHeaderSchedule != null) tvHeaderSchedule.setText(R.string.table_schedule);
        if (tvHeaderMinNurses != null) tvHeaderMinNurses.setText(R.string.table_min_nurses);
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

        setupFilterSpinner();
    }

    // Update the language switch button
    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    // Load shift types from the API
    private void loadShiftTypes() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getShiftTypes("Bearer " + token).enqueue(new Callback<ShiftTypesResponse>() {
            @Override
            public void onResponse(Call<ShiftTypesResponse> call, Response<ShiftTypesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ShiftType> data = response.body().getData();
                    if (data != null) {
                        allShiftTypes.clear();
                        allShiftTypes.addAll(data);

                        // Forçar a primeira página e aplicar os filtros/exibição
                        currentPage = 1;
                        applyFilters();
                    }
                } else {
                    Toast.makeText(ShiftTypesActivity.this, getString(R.string.err_loading_shift_types), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ShiftTypesResponse> call, Throwable t) {
                Toast.makeText(ShiftTypesActivity.this, getString(R.string.err_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Apply client-side search and type filters
    private List<ShiftType> getSearchFilteredList() {
        String query = etSearch.getText().toString().toLowerCase();
        int selectedFilterPos = spinnerFilter.getSelectedItemPosition();

        List<ShiftType> temp = new ArrayList<>();
        for (ShiftType st : allShiftTypes) {
            // Get localized name for filtering
            String localizedName = getLocalizedName(st.getName()).toLowerCase();

            // Filtro por Nome
            boolean matchesName = localizedName.contains(query);

            // Filtro por Tipo
            boolean matchesFilter = true;
            if (selectedFilterPos == 1) matchesFilter = st.getMinNurses() > 0; // Work shifts
            else if (selectedFilterPos == 2) matchesFilter = st.getMinNurses() == 0; // Non-work shifts

            if (matchesName && matchesFilter) {
                temp.add(st);
            }
        }
        return temp;
    }

    // Get the localized shift type name
    private String getLocalizedName(String apiName) {
        if (apiName == null || apiName.isEmpty()) return apiName;

        String language = getResources().getConfiguration().getLocales().get(0).getLanguage();
        boolean isPt = "pt".equals(language);

        switch (apiName) {
            case "morning":
                return isPt ? "Manhã" : "Morning";
            case "afternoon":
                return isPt ? "Tarde" : "Afternoon";
            case "night":
                return isPt ? "Noite" : "Night";
            case "dayOff":
                return isPt ? "Descanso" : "Day Off";
            case "sick leave":
                return isPt ? "Baixa Médica" : "Sick Leave";
            case "parental leave":
                return isPt ? "Licença Parental" : "Parental Leave";
            case "holidays":
                return isPt ? "Férias" : "Holidays";
            default:
                return apiName;
        }
    }

    // Apply filters and paginate the result list
    private void applyFilters() {
        List<ShiftType> searchFiltered = getSearchFilteredList();

        // Calcula o total de páginas para evitar erros
        int totalItems = searchFiltered.size();
        int maxPage = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        // Proteção: se a página atual for maior que o máximo, volta para a última válida
        if (currentPage > maxPage && maxPage > 0) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalItems);

        filteredShiftTypes.clear();
        if (start < totalItems) {
            // subList(start, end) pega exatamente os 8 itens da página
            filteredShiftTypes.addAll(searchFiltered.subList(start, end));
        }

        adapter.notifyDataSetChanged();

        renderPagination(maxPage);
    }

    // Render the pagination numbers
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

    // Convert dp to px
    private int dpToPx(int dp) {
        return Math.round((float) dp * getResources().getDisplayMetrics().density);
    }

    // Open the edit screen for the selected shift type
    private void onEditShiftType(ShiftType shiftType) {
        Intent intent = new Intent(this, ShiftTypeEditActivity.class);
        intent.putExtra("shiftTypeId", shiftType.getId());
        intent.putExtra("shiftTypeName", shiftType.getName());
        intent.putExtra("shiftTypeStartTime", shiftType.getStartTime());
        intent.putExtra("shiftTypeEndTime", shiftType.getEndTime());
        intent.putExtra("shiftTypeColor", shiftType.getColor());
        intent.putExtra("shiftTypeMinNurses", shiftType.getMinNurses());
        startActivity(intent);
    }

    // Show the delete confirmation flow
    private void onDeleteShiftType(ShiftType shiftType) {
        // 1. Capturar ecrã completo (DecorView) para o Blur cobrir tudo
        View decorView = getWindow().getDecorView();
        Bitmap screenshot = Bitmap.createBitmap(decorView.getWidth(), decorView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        decorView.draw(canvas);
        Bitmap blurredBitmap = blur(screenshot);

        // 2. Inflar layout do diálogo
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        // 3. Criar o diálogo (usando um tema básico, vamos customizar a janela manualmente)
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        // 4. Configurações críticas de Janela para FULLSCREEN e CENTRAMENTO
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

        // 5. Definir o conteúdo da vista
        dialog.setContentView(dialogView);

        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            // Desativar o botão para evitar cliques duplicados
            btnConfirm.setEnabled(false);

            // CHAMADA À API (Remoção Real na Base de Dados)
            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            api.deleteShiftType("Bearer " + token, shiftType.getId()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        // SUCESSO: Remover da lista local, atualizar filtros e mostrar notificação
                        allShiftTypes.remove(shiftType);
                        applyFilters();
                        showSuccessNotification(getString(R.string.success_shift_type_deleted));
                        dialog.dismiss();
                    } else {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(ShiftTypesActivity.this, getString(R.string.err_deleting_shift_type), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(ShiftTypesActivity.this, getString(R.string.err_connection), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Apply blur to a bitmap
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

    // Capture a screenshot from a view
    private Bitmap screenshot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    // Show the success notification toast
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
