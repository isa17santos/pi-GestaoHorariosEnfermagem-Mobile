package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
    private TextView tvLabelName, tvLabelEmail;   // section labels — localised in updateUIStrings()
    private TextView tvNameValue;
    private EditText etEmail;
    private MaterialButton btnChangePassword, btnSaveProfile;
    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral;

    // UI Components - Preferences Section
    private MaterialCardView cvPreferencesSection;
    private TextView tvPreferencesTitle;
    private MaterialButton btnAddPreference;
    private EditText etSearchPreferences;
    private LinearLayout llPreferencesContainer;
    private TextView tvNoPreferences;

    // UI Components - Toast
    private LinearLayout llNotificationToast;
    private TextView tvNotificationMsg;
    private LinearLayout llErrorToast;
    private TextView tvErrorToastMsg;

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
        tvLabelName = findViewById(R.id.tv_label_name);
        tvLabelEmail = findViewById(R.id.tv_label_email);
        tvNameValue = findViewById(R.id.tv_name_value);
        etEmail = findViewById(R.id.et_email);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);

        // Preferences Section
        cvPreferencesSection = findViewById(R.id.cv_preferences_section);
        tvPreferencesTitle = findViewById(R.id.tv_preferences_title);
        btnAddPreference = findViewById(R.id.btn_add_preference);
        etSearchPreferences = findViewById(R.id.et_search_preferences);
        llPreferencesContainer = findViewById(R.id.ll_preferences_container);
        tvNoPreferences = findViewById(R.id.tv_no_preferences);

        // Toast
        llNotificationToast = findViewById(R.id.ll_notification_toast);
        tvNotificationMsg = findViewById(R.id.tv_notification_msg);
        llErrorToast = findViewById(R.id.ll_error_toast);
        tvErrorToastMsg = findViewById(R.id.tv_error_toast_msg);

        // Load token and user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");

        // Pre-fill navbar name from the cached value so it shows before the API responds
        if (tvUserName != null) tvUserName.setText(prefs.getString("user_name", "Utilizador"));

        // Pre-fill Full Name from cache — loadProfile() will overwrite with the API value
        if (tvNameValue != null) tvNameValue.setText(prefs.getString("user_name", ""));

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

        // Add new preference — only reachable by nurses (button is inside the nurse-only section)
        btnAddPreference.setOnClickListener(v -> {
            // Guard: only nurses should be able to add preferences
            SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
            if (!"nurse".equals(prefs.getString("user_role", ""))) return;

            PreferenceBottomSheet bottomSheet = new PreferenceBottomSheet(
                null,  // No existing preference
                false, // Not editing
                this::savePreference // Save preference with callback data
            );
            bottomSheet.show(getSupportFragmentManager(), "add_preference");
        });

        // Search preferences — pattern mirrors HumanResourcesActivity:
        // filter is called in afterTextChanged so the full committed text is available.
        etSearchPreferences.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterPreferences(s.toString());
            }
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
            // Rebuild preference cards so pill labels, subtitle and notes
            // prefix are rendered in the newly selected language
            displayPreferences(allPreferences);
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

        // Personal data section labels (first form)
        if (tvLabelName  != null) tvLabelName.setText(isEn  ? "Full Name"  : "Nome Completo");
        if (tvLabelEmail != null) tvLabelEmail.setText(isEn ? "Email"      : "Email");
        if (etEmail      != null) etEmail.setHint(isEn      ? "email@example.com" : "email@exemplo.pt");

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
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    // Unwrap the nested "data" object from the API envelope
                    ProfileResponse.Data profile = response.body().getData();

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

    // Load user preferences from API and filter out past months.
    // Only nurses have monthly shift preferences — hide the entire section for other roles.
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String role = prefs.getString("user_role", "");

        // Non-nurse roles have no shift preferences — hide the section and return early
        if (!"nurse".equals(role)) {
            if (cvPreferencesSection != null) cvPreferencesSection.setVisibility(View.GONE);
            return;
        }

        // Nurse role: ensure section is visible before fetching
        if (cvPreferencesSection != null) cvPreferencesSection.setVisibility(View.VISIBLE);

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
            }
        });
    }

    // Display preferences in the UI
    /**
     * Display preferences in the UI.
     * Each preference card is clickable to edit via bottom sheet.
     * Delete button allows removing preferences with confirmation.
     */
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
            TextView tvMonthYear  = cardView.findViewById(R.id.tv_pref_month_year);
            TextView tvSubtitle   = cardView.findViewById(R.id.tv_pref_subtitle);
            ImageButton btnChevron = cardView.findViewById(R.id.btn_pref_chevron);
            ImageButton btnDelete  = cardView.findViewById(R.id.btn_pref_delete);
            TextView tvNotes       = cardView.findViewById(R.id.tv_pref_notes);
            ChipGroup cgPills      = cardView.findViewById(R.id.cg_pref_pills);

            // Get language for month names and all localised strings
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");

            // Set month/year title and section subtitle
            String monthName = getMonthName(preference.getMonth(), isEn);
            tvMonthYear.setText(monthName + " " + preference.getYear());
            tvSubtitle.setText(isEn ? "Monthly Preferences" : "Preferências Mensais");

            // Add only active pills — each is pill-shaped and sized to its content
            cgPills.removeAllViews();
            addPillChip(cgPills, preference.prefersMorning(),   isEn ? "Morning"        : "Manhã");
            addPillChip(cgPills, preference.prefersAfternoon(), isEn ? "Afternoon"       : "Tarde");
            addPillChip(cgPills, preference.prefersNight(),     isEn ? "Night"           : "Noite");
            addPillChip(cgPills, preference.prefersWeekends(),  isEn ? "Weekends"        : "Fim de Semana");
            addPillChip(cgPills, preference.avoidsWeekends(),   isEn ? "Avoid Weekends"  : "Evitar Fim de Semana");

            // Show notes if not empty
            if (preference.getNotes() != null && !preference.getNotes().isEmpty()) {
                tvNotes.setVisibility(View.VISIBLE);
                tvNotes.setText((isEn ? "Notes: " : "Notas: ") + preference.getNotes());
            } else {
                tvNotes.setVisibility(View.GONE);
            }

            // Hide chevron (no longer used for expand/collapse)
            if (btnChevron != null) {
                btnChevron.setVisibility(View.GONE);
            }

            // Make entire card clickable to edit preference
            cardView.setOnClickListener(v -> {
                PreferenceBottomSheet bottomSheet = new PreferenceBottomSheet(
                    preference, // Existing preference to edit
                    true,       // Edit mode
                    this::savePreference // Save preference with callback data
                );
                bottomSheet.show(getSupportFragmentManager(), "edit_preference");
            });

            // Delete button - show confirmation dialog
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(preference));

            llPreferencesContainer.addView(cardView);
        }
    }

    // Add an active preference chip to the group — only shown when the flag is true
    private void addPillChip(ChipGroup chipGroup, boolean isActive, String label) {
        if (!isActive) return;

        Chip chip = new Chip(this);
        chip.setText(label + " ✓");
        chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8DEFF")));
        chip.setTextColor(Color.parseColor("#7C3AED"));
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        chip.setChipStrokeWidth(0f);

        // Disable interactivity — pills are informational only
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setCheckedIconVisible(false);
        chip.setChipIconVisible(false);
        chip.setCloseIconVisible(false);

        chipGroup.addView(chip);
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

    // Handle save profile button.
    // Validates input, checks for actual changes, then fires the update API call.
    private void handleSaveProfile() {
        // Cancel any in-flight auto-hide timer from a previous error before we might show a new one
        errorHideHandler.removeCallbacksAndMessages(null);

        String email = etEmail.getText().toString().trim();

        // Validate email not empty
        if (email.isEmpty()) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            showFormError(isEn ? "Email cannot be empty" : "O email não pode estar vazio");
            return;
        }

        // Check if email changed from original
        if (email.equals(originalEmail)) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            showFormError(isEn ? "No changes to save" : "Não existem alterações para guardar");
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
                    // API returned a non-2xx status; surface a localised error via the standard
                    // toast helper so the message text is always set before the card is shown.
                    showErrorToast(isEn ? "Failed to update profile" : "Falha ao atualizar perfil");
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
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    /**
     * Shows a custom confirmation dialog (dialog_confirm_delete.xml) before
     * deleting a nurse preference. Follows the same pattern as
     * HumanResourcesActivity.onDeleteUser():
     *  - Inflate the shared layout
     *  - Transparent background + full-screen window + dim
     *  - btn_cancel   → dismiss
     *  - btn_confirm_delete → call DELETE API, reload on success
     * The confirm button is tinted purple (#7C3AED) to match the web profile page.
     */
    private void showDeleteConfirmation(NursePreference preference) {
        boolean isEn = AppCompatDelegate.getApplicationLocales()
                .toLanguageTags().contains("en");

        // Inflate the shared custom delete-confirmation layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        // Build dialog without any built-in title/message chrome
        final AlertDialog dialog = new AlertDialog.Builder(this).create();

        // Must call show() before configuring the Window
        dialog.show();

        if (dialog.getWindow() != null) {
            // Transparent card background so the rounded MaterialCardView shows cleanly
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // Centre the card on screen with natural wrap_content sizing
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);

            // Dim the activity content behind the dialog
            dialog.getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.4f);
        }

        // Attach the inflated view as the dialog content
        dialog.setContentView(dialogView);

        // Re-tint the confirm button to purple to match the web profile page style
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        btnConfirm.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#7C3AED")));

        // Cancel: simply close the dialog, no side-effects
        dialogView.findViewById(R.id.btn_cancel)
                .setOnClickListener(v -> dialog.dismiss());

        // Confirm: call DELETE /api/profile/preferences/{id}, reload on success
        btnConfirm.setOnClickListener(v -> {
            // Disable button to prevent double-tap
            btnConfirm.setEnabled(false);

            ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
            api.deleteProfilePreference("Bearer " + token, preference.getId())
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                boolean en = AppCompatDelegate.getApplicationLocales()
                                        .toLanguageTags().contains("en");
                                showSuccessToast(en
                                        ? "Preference deleted"
                                        : "Preferência eliminada");
                                dialog.dismiss();
                                // Refresh the preferences list
                                loadPreferences();
                            } else {
                                // Re-enable so the user can try again
                                btnConfirm.setEnabled(true);
                                boolean en = AppCompatDelegate.getApplicationLocales()
                                        .toLanguageTags().contains("en");
                                showErrorToast(en
                                        ? "Failed to delete"
                                        : "Falha ao eliminar");
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            btnConfirm.setEnabled(true);
                            boolean en = AppCompatDelegate.getApplicationLocales()
                                    .toLanguageTags().contains("en");
                            showErrorToast(en ? "Network error" : "Erro de rede");
                        }
                    });
        });
    }

    /**
     * Entry point called from PreferenceBottomSheet's save callback.
     * Checks whether a preference for the chosen month/year already exists.
     * If so, a confirmation dialog is shown before sending the request.
     */
    private void savePreference(Map<String, Object> data) {
        int month = (int) data.get("month");
        int year  = (int) data.get("year");

        // Check for an existing preference on the same month/year
        boolean monthExists = allPreferences.stream()
                .anyMatch(p -> p.getMonth() == month && p.getYear() == year);

        if (monthExists) {
            showOverwriteConfirmation(data);
            return;
        }

        performSavePreference(data);
    }

    /**
     * Shows a confirmation dialog when the selected month/year already has a
     * saved preference and the user is creating a new one (not editing).
     *
     * Reuses dialog_confirm_delete.xml exactly — only the title, message, and
     * confirm-button colour differ from the delete confirmation.
     */
    private void showOverwriteConfirmation(Map<String, Object> data) {
        boolean isEn = AppCompatDelegate.getApplicationLocales()
                .toLanguageTags().contains("en");

        // Inflate the same shared layout used for the delete confirmation
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);

        final AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.4f);
        }

        dialog.setContentView(dialogView);

        // Override title and message for overwrite context
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMsg   = dialogView.findViewById(R.id.tv_dialog_message);
        if (tvTitle != null)
            tvTitle.setText(isEn ? "Replace preference?" : "Substituir preferência?");
        if (tvMsg != null)
            tvMsg.setText(isEn
                    ? "A preference for this month already exists. Do you want to replace it?"
                    : "Já existe uma preferência para este mês. Deseja substitui-la?");

        // Re-tint confirm button purple to match the profile page style
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm_delete);
        btnConfirm.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#7C3AED")));

        // Override the button label — this is a replace action, not a delete
        btnConfirm.setText(isEn ? "Yes, Replace" : "Sim, Substituir");

        // Cancel: dismiss without saving
        dialogView.findViewById(R.id.btn_cancel)
                .setOnClickListener(v -> dialog.dismiss());

        // Confirm: proceed with the API call
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            performSavePreference(data);
        });
    }

    /**
     * Executes the actual save/update API call.
     * Called directly when there is no month conflict, or after the user
     * confirms the overwrite dialog.
     */
    private void performSavePreference(Map<String, Object> data) {
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);

        Log.d("PREF_DEBUG", "Saving preference: " + data.toString());

        api.updateProfilePreferences("Bearer " + token, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d("PREF_DEBUG", "Response code: " + response.code());

                if (response.isSuccessful()) {
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showSuccessToast(isEn ? "Preference saved" : "Preferência guardada");
                    loadPreferences();
                } else {
                    try {
                        Log.d("PREF_DEBUG", "Error body: " + response.errorBody().string());
                    } catch (Exception ignored) {}
                    String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                    boolean isEn = currentLang.contains("en");
                    showErrorToast(isEn ? "Failed to save" : "Falha ao guardar");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("PREF_DEBUG", "Failure: " + t.getMessage());
                String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
                boolean isEn = currentLang.contains("en");
                showErrorToast(isEn ? "Network error" : "Erro de rede");
            }
        });
    }

    // All success feedback should go through this method to ensure consistent UI behaviour.
    // Animates the toast banner in from the top and hides it after 2 seconds.
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

    // Form validation errors — shown inline inside the profile card via cvErrorNotification.
    // Auto-hides after 6 seconds.
    private void showFormError(String message) {
        if (tvErrGeneral != null) tvErrGeneral.setText(message);
        if (cvErrorNotification != null) {
            cvErrorNotification.setVisibility(View.VISIBLE);
            errorHideHandler.removeCallbacksAndMessages(null);
            errorHideHandler.postDelayed(() -> cvErrorNotification.setVisibility(View.GONE), 6000);
        }
    }

    // API errors — shown as an animated top-center toast banner.
    // Animates the error toast banner in from the top and hides it after 3 seconds.
    private void showErrorToast(String message) {
        if (tvErrorToastMsg != null) tvErrorToastMsg.setText(message);
        if (llErrorToast != null) {
            llErrorToast.setVisibility(View.VISIBLE);
            llErrorToast.setAlpha(0f);
            llErrorToast.setTranslationY(-100f);
            llErrorToast.animate().alpha(1f).translationY(0f).setDuration(400);

            errorHideHandler.removeCallbacksAndMessages(null);
            errorHideHandler.postDelayed(() -> {
                llErrorToast.animate().alpha(0f).translationY(-100f).setDuration(400)
                    .withEndAction(() -> llErrorToast.setVisibility(View.GONE));
            }, 3000);
        }
    }

    // Convert dp to px
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}