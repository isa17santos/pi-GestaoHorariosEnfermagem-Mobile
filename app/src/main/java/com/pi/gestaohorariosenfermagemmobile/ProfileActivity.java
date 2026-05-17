package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends BaseActivity {
    // UI Components - Navbar
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private ImageButton btnProfile, btnLogout;
    private MaterialCardView btnLanguageSwitch;
    private MaterialButton btnBack;

    // UI Components - Profile Section
    private TextView tvTitle, tvSubtitle;
    private TextView tvNameValue;
    private EditText etEmail;
    private MaterialButton btnChangePassword, btnSaveProfile;
    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral;

    // UI Components - Preferences Section
    private TextView tvPreferencesTitle;
    private MaterialButton btnAddPreference;
    private EditText etSearchPreferences;
    private LinearLayout llPreferencesContainer;
    private TextView tvNoPreferences;

    // UI Components - Toast
    private LinearLayout llNotificationToast;
    private TextView tvNotificationMsg;

    // Data
    private String token;
    private String originalEmail;
    private List<NursePreference> allPreferences = new ArrayList<>();
    private final Handler errorHideHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Apply window insets for keyboard handling
        applyWindowInsets(findViewById(android.R.id.content));

        initViews();
        setupNavbar();
        setupActions();
        updateUIStrings();

        // Load profile and preferences data
        loadProfile();
        loadPreferences();
    }

    // Bind all views from the layout
    private void initViews() {
        // Navbar
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        btnProfile = findViewById(R.id.btn_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnLanguageSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);
        btnBack = findViewById(R.id.btn_back);

        // Profile Section
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvNameValue = findViewById(R.id.tv_name_value);
        etEmail = findViewById(R.id.et_email);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);

        // Preferences Section
        tvPreferencesTitle = findViewById(R.id.tv_preferences_title);
        btnAddPreference = findViewById(R.id.btn_add_preference);
        etSearchPreferences = findViewById(R.id.et_search_preferences);
        llPreferencesContainer = findViewById(R.id.ll_preferences_container);
        tvNoPreferences = findViewById(R.id.tv_no_preferences);

        // Toast
        llNotificationToast = findViewById(R.id.ll_notification_toast);
        tvNotificationMsg = findViewById(R.id.tv_notification_msg);

        // Load token and user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");
        if (tvUserName != null) tvUserName.setText(prefs.getString("user_name", "Utilizador"));

        String role = prefs.getString("user_role", "");
        if (tvUserRole != null) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            String roleLabel;
            switch (role) {
                case "admin":
                    roleLabel = isEn ? "Admin" : "Administrador";
                    break;
                case "head_nurse":
                    roleLabel = isEn ? "Head Nurse" : "Enfermeiro Chefe";
                    break;
                case "nurse":
                    roleLabel = isEn ? "Nurse" : "Enfermeiro";
                    break;
                default:
                    roleLabel = role;
            }
            tvUserRole.setText(roleLabel);
        }

        updateLanguageButton();
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

    // Setup button actions
    private void setupActions() {
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());
        btnChangePassword.setOnClickListener(v -> handleChangePassword());
        btnAddPreference.setOnClickListener(v -> showAddPreferenceDialog());

        // Search functionality
        etSearchPreferences.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPreferences(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Toggle the application language
    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";

        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(nextLang);
        AppCompatDelegate.setApplicationLocales(appLocales);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateUIStrings();
            updateLanguageButton();
        }, 100);
    }

    // Update the language switch button
    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    // Update all translated texts on the screen
    @Override
    protected void updateUIStrings() {
        String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = currentLang.contains("en");

        // Title and subtitle
        if (tvTitle != null) tvTitle.setText(isEn ? "My Profile" : "Meu Perfil");
        if (tvSubtitle != null) tvSubtitle.setText(isEn ? "Manage account information and preferences" : "Gerir informações da conta e preferências");

        // Buttons
        if (btnBack != null) btnBack.setText(isEn ? "Back" : "Voltar");
        if (btnSaveProfile != null) btnSaveProfile.setText(isEn ? "Save" : "Guardar");
        if (btnChangePassword != null) btnChangePassword.setText(isEn ? "Change Password" : "Alterar Senha");
        if (btnAddPreference != null) btnAddPreference.setText(isEn ? "Add" : "Adicionar");

        // Preferences section
        if (tvPreferencesTitle != null) tvPreferencesTitle.setText(isEn ? "Monthly Preferences" : "Preferências Mensais");
        if (etSearchPreferences != null) etSearchPreferences.setHint(isEn ? "Search preferences..." : "Procurar preferências...");
        if (tvNoPreferences != null) tvNoPreferences.setText(isEn ? "No preferences defined" : "Nenhuma preferência definida");

        // Error message
        if (tvErrGeneral != null) tvErrGeneral.setText(isEn ? "Please fill all required fields correctly" : "Por favor preencha todos os campos corretamente");

        // Update user role
        String role = getSharedPreferences("AUTH", MODE_PRIVATE).getString("user_role", "");
        switch (role) {
            case "admin":
                tvUserRole.setText(isEn ? "Admin" : "Administrador");
                break;
            case "head_nurse":
                tvUserRole.setText(isEn ? "Head Nurse" : "Enfermeiro Chefe");
                break;
            case "nurse":
                tvUserRole.setText(isEn ? "Nurse" : "Enfermeiro");
                break;
            default:
                tvUserRole.setText(role);
        }
    }

    // Load user profile data from API
    private void loadProfile() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getProfile("Bearer " + token).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse profile = response.body();

                    // Populate UI with profile data
                    tvNameValue.setText(profile.getName());
                    etEmail.setText(profile.getEmail());

                    // Store original email to detect changes
                    originalEmail = profile.getEmail();
                } else {
                    showErrorToast("Failed to load profile");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                showErrorToast("Network error");
            }
        });
    }

    // Load user preferences from API and filter out past months
    private void loadPreferences() {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getProfilePreferences("Bearer " + token).enqueue(new Callback<NursePreferencesResponse>() {
            @Override
            public void onResponse(Call<NursePreferencesResponse> call, Response<NursePreferencesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allPreferences.clear();

                    // Get current date
                    Calendar calendar = Calendar.getInstance();
                    int currentYear = calendar.get(Calendar.YEAR);
                    int currentMonth = calendar.get(Calendar.MONTH) + 1; // 0-based, so add 1

                    // Filter out past months
                    List<NursePreference> preferences = response.body().getData();
                    if (preferences != null) {
                        for (NursePreference pref : preferences) {
                            // Keep only current and future months
                            if (pref.getYear() > currentYear ||
                                (pref.getYear() == currentYear && pref.getMonth() >= currentMonth)) {
                                allPreferences.add(pref);
                            }
                        }
                    }

                    // Display preferences
                    displayPreferences(allPreferences);
                } else {
                    showErrorToast("Failed to load preferences");
                }
            }

            @Override
            public void onFailure(Call<NursePreferencesResponse> call, Throwable t) {
                showErrorToast("Network error");
            }
        });
    }

    // Display preferences in the UI
    private void displayPreferences(List<NursePreference> preferences) {
        llPreferencesContainer.removeAllViews();

        if (preferences.isEmpty()) {
            tvNoPreferences.setVisibility(View.VISIBLE);
            return;
        }

        tvNoPreferences.setVisibility(View.GONE);

        for (NursePreference preference : preferences) {
            View cardView = LayoutInflater.from(this).inflate(R.layout.item_preference_card, llPreferencesContainer, false);

            // Bind data to card
            TextView tvMonthYear = cardView.findViewById(R.id.tv_pref_month_year);
            ImageButton btnChevron = cardView.findViewById(R.id.btn_pref_chevron);
            ImageButton btnDelete = cardView.findViewById(R.id.btn_pref_delete);
            TextView tvNotes = cardView.findViewById(R.id.tv_pref_notes);

            // Pills
            TextView tvPillMorning = cardView.findViewById(R.id.tv_pill_morning);
            TextView tvPillAfternoon = cardView.findViewById(R.id.tv_pill_afternoon);
            TextView tvPillNight = cardView.findViewById(R.id.tv_pill_night);
            TextView tvPillWeekends = cardView.findViewById(R.id.tv_pill_weekends);
            TextView tvPillAvoidWeekends = cardView.findViewById(R.id.tv_pill_avoid_weekends);

            // Get language for month names
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");

            // Set month/year
            String monthName = getMonthName(preference.getMonth(), isEn);
            tvMonthYear.setText(monthName + " " + preference.getYear());

            // Configure pills
            configurePill(tvPillMorning, preference.prefersMorning(), isEn ? "Morning" : "Manhã");
            configurePill(tvPillAfternoon, preference.prefersAfternoon(), isEn ? "Afternoon" : "Tarde");
            configurePill(tvPillNight, preference.prefersNight(), isEn ? "Night" : "Noite");
            configurePill(tvPillWeekends, preference.prefersWeekends(), isEn ? "Weekends" : "Fim de Semana");
            configurePill(tvPillAvoidWeekends, preference.avoidsWeekends(), isEn ? "Avoid Weekends" : "Evitar Fim de Semana");

            // Show notes if not empty
            if (preference.getNotes() != null && !preference.getNotes().isEmpty()) {
                tvNotes.setVisibility(View.VISIBLE);
                tvNotes.setText((isEn ? "Notes: " : "Notas: ") + preference.getNotes());
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            // Chevron toggle (for future expansion - currently no edit form)
            btnChevron.setOnClickListener(v -> {
                // Toggle expanded/collapsed state (placeholder for future edit functionality)
            });

            // Delete button
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(preference));

            llPreferencesContainer.addView(cardView);
        }
    }

    // Configure a preference pill (active or inactive)
    private void configurePill(TextView pill, boolean isActive, String label) {
        if (isActive) {
            pill.setVisibility(View.VISIBLE);
            pill.setText(label + " ✓");
            pill.setBackgroundColor(Color.parseColor("#E8DEFF"));
            pill.setTextColor(Color.parseColor("#7C3AED"));
            pill.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            pill.setVisibility(View.GONE);
        }
    }

    // Get month name in current language
    private String getMonthName(int month, boolean isEn) {
        String[] monthsPt = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                             "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        String[] monthsEn = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};

        if (month < 1 || month > 12) return "";
        return isEn ? monthsEn[month - 1] : monthsPt[month - 1];
    }

    // Filter preferences based on search query
    private void filterPreferences(String query) {
        if (query.trim().isEmpty()) {
            displayPreferences(allPreferences);
            return;
        }

        List<NursePreference> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (NursePreference pref : allPreferences) {
            String monthYear = getMonthName(pref.getMonth(), false) + " " + pref.getYear();
            if (monthYear.toLowerCase().contains(lowerQuery)) {
                filtered.add(pref);
            }
        }

        displayPreferences(filtered);
    }

    // Handle save profile button
    private void handleSaveProfile() {
        errorHideHandler.removeCallbacksAndMessages(null);

        String email = etEmail.getText().toString().trim();

        // Validate email not empty
        if (email.isEmpty()) {
            cvErrorNotification.setVisibility(View.VISIBLE);
            startHideTimer();
            return;
        }

        // Check if email changed from original
        if (email.equals(originalEmail)) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            showErrorToast(isEn ? "No changes to save" : "Não existem alterações para guardar");
            return;
        }

        // Disable button during request
        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("...");

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);

        // Call API
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateProfile("Bearer " + token, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSaveProfile.setEnabled(true);
                String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                boolean isEn = currentLang.contains("en");
                btnSaveProfile.setText(isEn ? "Save" : "Guardar");

                if (response.isSuccessful()) {
                    // Update original email
                    originalEmail = email;

                    // Show success toast
                    showSuccessToast(isEn ? "Profile updated successfully" : "Perfil atualizado com sucesso");
                } else {
                    cvErrorNotification.setVisibility(View.VISIBLE);
                    startHideTimer();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSaveProfile.setEnabled(true);
                String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                boolean isEn = currentLang.contains("en");
                btnSaveProfile.setText(isEn ? "Save" : "Guardar");
                showErrorToast(isEn ? "Network error" : "Erro de rede");
            }
        });
    }

    // Handle change password button
    private void handleChangePassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    // Show delete confirmation dialog
    private void showDeleteConfirmation(NursePreference preference) {
        String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = currentLang.contains("en");

        String monthName = getMonthName(preference.getMonth(), isEn);
        String title = isEn ? "Delete Preference" : "Eliminar Preferência";
        String message = isEn ?
            "Are you sure you want to delete preferences for " + monthName + " " + preference.getYear() + "?" :
            "Tem certeza que deseja eliminar as preferências de " + monthName + " " + preference.getYear() + "?";

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(isEn ? "Delete" : "Eliminar", (dialog, which) -> deletePreference(preference))
            .setNegativeButton(isEn ? "Cancel" : "Cancelar", null)
            .show();
    }

    // Delete preference via API
    private void deletePreference(NursePreference preference) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.deleteProfilePreference("Bearer " + token, preference.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showSuccessToast(isEn ? "Preference deleted" : "Preferência eliminada");

                    // Reload preferences
                    loadPreferences();
                } else {
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showErrorToast(isEn ? "Failed to delete" : "Falha ao eliminar");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                boolean isEn = currentLang.contains("en");
                showErrorToast(isEn ? "Network error" : "Erro de rede");
            }
        });
    }

    // Show add preference dialog
    private void showAddPreferenceDialog() {
        String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = currentLang.contains("en");

        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.select_dialog_multichoice, null);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(24), dp(24), dp(24));

        // Month spinner
        TextView tvMonthLabel = new TextView(this);
        tvMonthLabel.setText(isEn ? "Month" : "Mês");
        tvMonthLabel.setTextSize(16);
        tvMonthLabel.setPadding(0, 0, 0, dp(8));
        container.addView(tvMonthLabel);

        Spinner spMonth = new Spinner(this);
        String[] monthsPt = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                             "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        String[] monthsEn = {"January", "February", "March", "April", "May", "June",
                             "July", "August", "September", "October", "November", "December"};

        // Get current date to disable past months
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;

        List<String> availableMonths = new ArrayList<>();
        List<Integer> monthNumbers = new ArrayList<>();
        String[] monthNames = isEn ? monthsEn : monthsPt;

        for (int i = 0; i < 12; i++) {
            availableMonths.add(monthNames[i]);
            monthNumbers.add(i + 1);
        }

        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableMonths);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(monthAdapter);
        spMonth.setSelection(currentMonth - 1); // Select current month by default
        container.addView(spMonth);

        // Add spacing
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        container.addView(spacer1);

        // Year spinner
        TextView tvYearLabel = new TextView(this);
        tvYearLabel.setText(isEn ? "Year" : "Ano");
        tvYearLabel.setTextSize(16);
        tvYearLabel.setPadding(0, 0, 0, dp(8));
        container.addView(tvYearLabel);

        Spinner spYear = new Spinner(this);
        List<String> years = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            years.add(String.valueOf(currentYear + i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);
        container.addView(spYear);

        // Add spacing
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        container.addView(spacer2);

        // Preference switches
        TextView tvPrefsLabel = new TextView(this);
        tvPrefsLabel.setText(isEn ? "Preferences" : "Preferências");
        tvPrefsLabel.setTextSize(16);
        tvPrefsLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(tvPrefsLabel);

        Switch swMorning = new Switch(this);
        swMorning.setText(isEn ? "Prefers Morning" : "Prefere Manhã");
        container.addView(swMorning);

        Switch swAfternoon = new Switch(this);
        swAfternoon.setText(isEn ? "Prefers Afternoon" : "Prefere Tarde");
        container.addView(swAfternoon);

        Switch swNight = new Switch(this);
        swNight.setText(isEn ? "Prefers Night" : "Prefere Noite");
        container.addView(swNight);

        Switch swPrefersWeekends = new Switch(this);
        swPrefersWeekends.setText(isEn ? "Prefers Weekends" : "Prefere Fim de Semana");
        container.addView(swPrefersWeekends);

        Switch swAvoidWeekends = new Switch(this);
        swAvoidWeekends.setText(isEn ? "Avoid Weekends" : "Evitar Fim de Semana");
        container.addView(swAvoidWeekends);

        // Mutual exclusivity for weekend preferences
        swPrefersWeekends.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) swAvoidWeekends.setChecked(false);
        });
        swAvoidWeekends.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) swPrefersWeekends.setChecked(false);
        });

        // Add spacing
        View spacer3 = new View(this);
        spacer3.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)));
        container.addView(spacer3);

        // Notes field
        TextView tvNotesLabel = new TextView(this);
        tvNotesLabel.setText(isEn ? "Notes (optional)" : "Notas (opcional)");
        tvNotesLabel.setTextSize(16);
        tvNotesLabel.setPadding(0, 0, 0, dp(8));
        container.addView(tvNotesLabel);

        EditText etNotes = new EditText(this);
        etNotes.setHint(isEn ? "Additional notes..." : "Notas adicionais...");
        etNotes.setMinLines(3);
        container.addView(etNotes);

        // Show dialog
        new AlertDialog.Builder(this)
            .setTitle(isEn ? "Add Preference" : "Adicionar Preferência")
            .setView(container)
            .setPositiveButton(isEn ? "Save" : "Guardar", (dialog, which) -> {
                int selectedMonth = monthNumbers.get(spMonth.getSelectedItemPosition());
                int selectedYear = Integer.parseInt(years.get(spYear.getSelectedItemPosition()));

                // Check for duplicate
                for (NursePreference pref : allPreferences) {
                    if (pref.getMonth() == selectedMonth && pref.getYear() == selectedYear) {
                        showOverwriteConfirmation(selectedMonth, selectedYear, swMorning.isChecked(),
                            swAfternoon.isChecked(), swNight.isChecked(), swAvoidWeekends.isChecked(),
                            swPrefersWeekends.isChecked(), etNotes.getText().toString());
                        return;
                    }
                }

                // No duplicate, save directly
                savePreference(selectedMonth, selectedYear, swMorning.isChecked(),
                    swAfternoon.isChecked(), swNight.isChecked(), swAvoidWeekends.isChecked(),
                    swPrefersWeekends.isChecked(), etNotes.getText().toString());
            })
            .setNegativeButton(isEn ? "Cancel" : "Cancelar", null)
            .show();
    }

    // Show overwrite confirmation dialog
    private void showOverwriteConfirmation(int month, int year, boolean morning, boolean afternoon,
                                          boolean night, boolean avoidWeekends, boolean prefersWeekends, String notes) {
        String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = currentLang.contains("en");

        String monthName = getMonthName(month, isEn);
        String message = isEn ?
            "Preferences for " + monthName + " " + year + " already exist. Overwrite?" :
            "Preferências para " + monthName + " " + year + " já existem. Sobrescrever?";

        new AlertDialog.Builder(this)
            .setTitle(isEn ? "Overwrite Preference?" : "Sobrescrever Preferência?")
            .setMessage(message)
            .setPositiveButton(isEn ? "Overwrite" : "Sobrescrever", (dialog, which) ->
                savePreference(month, year, morning, afternoon, night, avoidWeekends, prefersWeekends, notes))
            .setNegativeButton(isEn ? "Cancel" : "Cancelar", null)
            .show();
    }

    // Save preference to API
    private void savePreference(int month, int year, boolean morning, boolean afternoon,
                               boolean night, boolean avoidWeekends, boolean prefersWeekends, String notes) {
        Map<String, Object> data = new HashMap<>();
        data.put("month", month);
        data.put("year", year);
        data.put("prefers_morning", morning);
        data.put("prefers_afternoon", afternoon);
        data.put("prefers_night", night);
        data.put("avoid_weekends", avoidWeekends);
        data.put("prefers_weekends", prefersWeekends);
        data.put("notes", notes);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateProfilePreferences("Bearer " + token, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showSuccessToast(isEn ? "Preference saved" : "Preferência guardada");

                    // Reload preferences
                    loadPreferences();
                } else {
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showErrorToast(isEn ? "Failed to save" : "Falha ao guardar");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                boolean isEn = currentLang.contains("en");
                showErrorToast(isEn ? "Network error" : "Erro de rede");
            }
        });
    }

    // Show success toast notification
    private void showSuccessToast(String message) {
        if (tvNotificationMsg != null) tvNotificationMsg.setText(message);
        if (llNotificationToast != null) {
            llNotificationToast.setVisibility(View.VISIBLE);
            llNotificationToast.setAlpha(0f);
            llNotificationToast.setTranslationY(-100f);
            llNotificationToast.animate().alpha(1f).translationY(0f).setDuration(400);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                llNotificationToast.animate().alpha(0f).translationY(-100f).setDuration(400)
                    .withEndAction(() -> llNotificationToast.setVisibility(View.GONE));
            }, 2000);
        }
    }

    // Show error toast notification
    private void showErrorToast(String message) {
        if (tvErrGeneral != null) tvErrGeneral.setText(message);
        cvErrorNotification.setVisibility(View.VISIBLE);
        startHideTimer();
    }

    // Hide the error notification after a delay
    private void startHideTimer() {
        errorHideHandler.removeCallbacksAndMessages(null);
        errorHideHandler.postDelayed(() -> {
            cvErrorNotification.setVisibility(View.GONE);
        }, 6000);
    }

    // Convert dp to px
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}