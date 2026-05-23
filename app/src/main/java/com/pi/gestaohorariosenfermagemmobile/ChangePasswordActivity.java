package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.json.JSONObject;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends BaseActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private MaterialButton btnSave, toolbar;
    private TextView tvErrorMessage, tvInfoMessage;
    private TextView tvRecoveryLabel, tvResetTitle, tvResetSubtitle, tvRequirements;
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private ImageButton btnProfile, btnLogout;
    private MaterialCardView btnLangSwitch;

    private final Handler feedbackHandler = new Handler();
    private Runnable clearFeedbackRunnable;
    private Runnable clearInfoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Apply window insets for keyboard handling
        applyWindowInsets(findViewById(android.R.id.content));

        // Inicialização de Views
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilCurrentPassword = findViewById(R.id.til_current_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnSave = findViewById(R.id.btn_save);
        toolbar = findViewById(R.id.toolbar);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tvInfoMessage = findViewById(R.id.tv_info_message);
        tvRecoveryLabel = findViewById(R.id.tv_recovery_label);
        tvResetTitle = findViewById(R.id.tv_reset_title);
        tvResetSubtitle = findViewById(R.id.tv_reset_subtitle);
        tvRequirements = findViewById(R.id.tv_requirements);
        
        // Navbar and language selector
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        btnProfile = findViewById(R.id.btn_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnLangSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

        // Setup navbar
        setupNavbar();

        // Listeners
        toolbar.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> handleChangePassword());

        updateUIStrings();
        updateLanguageButton();
    }

    private void handleChangePassword() {
        if (etCurrentPassword.getText() == null || etNewPassword.getText() == null || etConfirmPassword.getText() == null) return;

        String currentPass = etCurrentPassword.getText().toString().trim();
        String pass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        hideError();
        hideInfo();

        // 1. Verificar campo vazio - palavra-passe actual
        if (currentPass.isEmpty()) {
            showError(getString(R.string.current_password_required));
            return;
        }

        // 2. Verificar campos vazios - nova palavra-passe
        if (pass.isEmpty()) {
            showError(getString(R.string.password_required));
            return;
        }
        if (confirm.isEmpty()) {
            showError(getString(R.string.password_confirmation_required));
            return;
        }

        // 3. Verificar se coincidem
        if (!pass.equals(confirm)) {
            showError(getString(R.string.password_mismatch));
            return;
        }

        // 4. Verificar requisitos de complexidade
        if (!validatePasswordFormat(pass)) {
            showError(getString(R.string.password_requirements));
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText(R.string.saving_password);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        ChangePasswordRequest request = new ChangePasswordRequest(currentPass, pass, confirm);

        apiService.changePassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                btnSave.setEnabled(true);
                btnSave.setText(R.string.save_changes);

                if (response.isSuccessful()) {
                    // Limpar os três campos
                    etCurrentPassword.setText("");
                    etNewPassword.setText("");
                    etConfirmPassword.setText("");

                    showInfo(getString(R.string.password_changed_profile_success));
                    // Não redirecionar - o utilizador mantém a sessão activa
                } else {
                    // Extrair mensagem de erro do response body (erro 422 ou outros)
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonError = new JSONObject(errorBody);
                            String errorMessage = jsonError.optString("message", "");
                            
                            // Mapear mensagens do backend para strings localizadas
                            if (errorMessage.contains("não corresponde à password actual")) {
                                showError(getString(R.string.error_current_password_incorrect));
                            } else if (errorMessage.contains("não pode ser igual")) {
                                showError(getString(R.string.error_new_password_same_as_current));
                            } else if (errorMessage.contains("pelo menos 8 caracteres")) {
                                showError(getString(R.string.error_password_complexity));
                            } else {
                                showError(getString(R.string.error_change_password_failed));
                            }
                        } else {
                            showError(getString(R.string.error_change_password_failed));
                        }
                    } catch (Exception e) {
                        showError(getString(R.string.error_change_password_failed));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText(R.string.save_changes);
                showError("Erro de rede: " + t.getMessage());
            }
        });
    }

    private boolean validatePasswordFormat(String password) {
        // Mínimo 8 chars, 1 maiúscula, 1 minúscula, 1 especial. Dígito opcional.
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }

    private void setupNavbar() {
        // Load user info from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String userName = prefs.getString("user_name", "Utilizador");
        String userRole = prefs.getString("user_role", "");
        
        // Set user name
        if (tvUserName != null) tvUserName.setText(userName);
        
        // Set user role (localized)
        if (tvUserRole != null) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            String roleLabel;
            switch (userRole) {
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
                    roleLabel = userRole;
            }
            tvUserRole.setText(roleLabel);
        }
        
        // Language switcher
        if (btnLangSwitch != null) {
            btnLangSwitch.setOnClickListener(v -> toggleLanguage());
        }
        
        // Profile button
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> 
                startActivity(new Intent(this, ProfileActivity.class)));
        }
        
        // Logout button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
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
        }
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang));
    }

    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    protected void updateUIStrings() {
        tvRecoveryLabel.setText(R.string.account_settings);
        tvResetTitle.setText(R.string.change_password_title_profile);
        tvResetSubtitle.setText(R.string.change_password_subtitle_profile);
        tilCurrentPassword.setHint(R.string.current_password_label);
        tilNewPassword.setHint(R.string.new_password_label);
        tilConfirmPassword.setHint(R.string.confirm_password_label);
        tvRequirements.setText(R.string.password_requirements);
        tvInfoMessage.setText(R.string.password_changed_profile_success);
        btnSave.setText(btnSave.isEnabled() ? R.string.save_changes : R.string.saving_password);
        toolbar.setText(R.string.back);
        
        // Update user role label when language changes
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        String userRole = prefs.getString("user_role", "");
        if (tvUserRole != null) {
            String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
            boolean isEn = currentLang.contains("en");
            String roleLabel;
            switch (userRole) {
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
                    roleLabel = userRole;
            }
            tvUserRole.setText(roleLabel);
        }
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        if (clearFeedbackRunnable != null) feedbackHandler.removeCallbacks(clearFeedbackRunnable);
        clearFeedbackRunnable = () -> tvErrorMessage.setVisibility(View.GONE);
        feedbackHandler.postDelayed(clearFeedbackRunnable, 4000);
    }

    private void showInfo(String message) {
        tvInfoMessage.setText(message);
        tvInfoMessage.setVisibility(View.VISIBLE);
        if (clearInfoRunnable != null) feedbackHandler.removeCallbacks(clearInfoRunnable);
        clearInfoRunnable = () -> tvInfoMessage.setVisibility(View.GONE);
        feedbackHandler.postDelayed(clearInfoRunnable, 5000);
    }

    private void hideError() {
        tvErrorMessage.setVisibility(View.GONE);
        if (clearFeedbackRunnable != null) feedbackHandler.removeCallbacks(clearFeedbackRunnable);
    }

    private void hideInfo() {
        tvInfoMessage.setVisibility(View.GONE);
        if (clearInfoRunnable != null) feedbackHandler.removeCallbacks(clearInfoRunnable);
    }
}




