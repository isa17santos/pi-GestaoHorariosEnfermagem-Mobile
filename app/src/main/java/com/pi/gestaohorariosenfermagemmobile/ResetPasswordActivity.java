package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.regex.Pattern;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends BaseActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private MaterialButton btnSave;
    private TextView tvBackToSignIn, tvErrorMessage, tvInfoMessage, tvLangFlag, tvLangLabel;
    private TextView tvRecoveryLabel, tvResetTitle, tvResetSubtitle, tvRequirements;
    private MaterialCardView btnLangSwitch;
    private String mToken, mEmail;

    private final Handler feedbackHandler = new Handler();
    private Runnable clearFeedbackRunnable;
    private Runnable clearInfoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Lógica para obter Token e Email de duas fontes possíveis
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            // CASO 1: Aberto via Link de Email (Deep Link)
            mToken = data.getQueryParameter("token");
            mEmail = data.getQueryParameter("email");
        } else {
            // CASO 2: Redirecionado do Login (must_change_password)
            mToken = intent.getStringExtra("token");
            mEmail = intent.getStringExtra("email");
        }

        // Inicialização de Views
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        btnSave = findViewById(R.id.btn_save);
        tvBackToSignIn = findViewById(R.id.tv_back_to_signin);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tvInfoMessage = findViewById(R.id.tv_info_message);
        tvRecoveryLabel = findViewById(R.id.tv_recovery_label);
        tvResetTitle = findViewById(R.id.tv_reset_title);
        tvResetSubtitle = findViewById(R.id.tv_reset_subtitle);
        tvRequirements = findViewById(R.id.tv_requirements);
        btnLangSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

        // Listeners
        btnLangSwitch.setOnClickListener(v -> toggleLanguage());
        tvBackToSignIn.setPaintFlags(tvBackToSignIn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvBackToSignIn.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> handleReset());

        updateUIStrings();
        updateLanguageButton();
    }

    private void handleReset() {
        if (etNewPassword.getText() == null || etConfirmPassword.getText() == null) return;

        String pass = etNewPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        hideError();
        hideInfo();

        // 1. Verificar campos vazios
        if (pass.isEmpty()) { showError(getString(R.string.password_required_field)); return; }
        if (confirm.isEmpty()) { showError(getString(R.string.password_confirmation_required)); return; }

        // 2. Verificar se coincidem (Prioridade visual idêntica à web)
        if (!pass.equals(confirm)) {
            showError(getString(R.string.password_mismatch));
            return;
        }

        // 3. Verificar requisitos de complexidade
        if (!validatePasswordFormat(pass)) {
            showError(getString(R.string.password_requirements));
            return;
        }

        // Validar se temos os dados de autenticação
        if (mToken == null || mEmail == null) {
            showError(getString(R.string.error_token_invalid));
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText(R.string.saving_password);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        ResetPasswordRequest request = new ResetPasswordRequest(mEmail, mToken, pass, confirm);

        apiService.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                btnSave.setEnabled(true);
                btnSave.setText(R.string.save_new_password);

                if (response.isSuccessful()) {
                    showInfo(getString(R.string.password_changed_success));
                    // Redireciona para o Login após 3 segundos
                    new Handler().postDelayed(() -> {
                        Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
                        // Estas flags limpam todas as atividades que estejam por cima da MainActivity
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish(); // Fecha a ResetPasswordActivity
                    }, 3000);
                } else {
                    showError(getString(R.string.error_token_invalid));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText(R.string.save_new_password);
                showError("Erro de rede: " + t.getMessage());
            }
        });
    }

    private boolean validatePasswordFormat(String password) {
        // Mínimo 8 chars, 1 maiúscula, 1 minúscula, 1 especial. Dígito opcional.
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
        return pattern.matcher(password).matches();
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    protected void updateUIStrings() {
        tvRecoveryLabel.setText(R.string.access_recovery);
        tvResetTitle.setText(R.string.change_password_title);
        tvResetSubtitle.setText(R.string.change_password_subtitle);
        tilNewPassword.setHint(R.string.new_password_label);
        tilConfirmPassword.setHint(R.string.confirm_password_label);
        tvRequirements.setText(R.string.password_requirements);
        tvBackToSignIn.setText(R.string.back_to_signin);
        tvInfoMessage.setText(R.string.password_changed_success);
        btnSave.setText(btnSave.isEnabled() ? R.string.save_new_password : R.string.saving_password);
    }

    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        tvLangFlag.setText(isEn ? "pt" : "en");
        tvLangLabel.setText(isEn ? "Português" : "English");
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