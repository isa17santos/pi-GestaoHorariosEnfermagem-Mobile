package com.pi.gestaohorariosenfermagemmobile;

import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.Map;


public class ForgotPasswordActivity extends BaseActivity {

    private TextInputEditText etEmail;
    private TextInputLayout tilEmail;
    private MaterialButton btnSendEmail;
    private TextView tvBackToSignIn, tvErrorMessage, tvLangFlag, tvLangLabel;
    private TextView tvRecoveryLabel, tvResetTitle, tvResetSubtitle;
    private MaterialCardView btnLangSwitch;
    private TextView tvInfoMessage;

    private final Handler feedbackHandler = new Handler();
    private Runnable clearFeedbackRunnable;
    private Runnable clearInfoRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Apply window insets for keyboard handling
        applyWindowInsets(findViewById(android.R.id.content));

        // Inicializar Views
        etEmail = findViewById(R.id.et_email);
        tilEmail = findViewById(R.id.til_email);
        btnSendEmail = findViewById(R.id.btn_send_email);
        tvBackToSignIn = findViewById(R.id.tv_back_to_signin);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnLangSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);
        tvRecoveryLabel = findViewById(R.id.tv_recovery_label);
        tvResetTitle = findViewById(R.id.tv_reset_title);
        tvResetSubtitle = findViewById(R.id.tv_reset_subtitle);
        tvInfoMessage = findViewById(R.id.tv_info_message);

        // Adicionar sublinhado ao link
        tvBackToSignIn.setPaintFlags(tvBackToSignIn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Listeners
        btnSendEmail.setOnClickListener(v -> handleSendEmail());
        tvBackToSignIn.setOnClickListener(v -> finish());
        btnLangSwitch.setOnClickListener(v -> toggleLanguage());

        updateUIStrings();
        updateLanguageButton();
    }

    private void handleSendEmail() {
        if (etEmail.getText() == null) return;
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) { showError(getString(R.string.email_required)); return; }

        btnSendEmail.setEnabled(false);
        btnSendEmail.setText(R.string.sending_email);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        apiService.sendRecoveryEmail(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                btnSendEmail.setEnabled(true);
                btnSendEmail.setText(R.string.send_email);

                if (response.isSuccessful()) {
                    showInfo(); // Mostra a caixa rosa
                    etEmail.setText("");
                } else {
                    showError("Email não encontrado.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnSendEmail.setEnabled(true);
                btnSendEmail.setText(R.string.send_email);
                showError("Erro de ligação ao servidor.");
            }
        });
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(nextLang);
        AppCompatDelegate.setApplicationLocales(appLocales);
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
        tvResetTitle.setText(R.string.reset_password_title);
        tvResetSubtitle.setText(R.string.reset_password_subtitle);
        tilEmail.setHint(R.string.email_label);

        // Atualiza o texto do botão conforme o estado atual
        if (btnSendEmail.isEnabled()) {
            btnSendEmail.setText(R.string.send_email);
        } else {
            btnSendEmail.setText(R.string.sending_email);
        }

        tvBackToSignIn.setText(R.string.back_to_signin);
        tvInfoMessage.setText(R.string.recovery_email_sent_info);
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

    private void hideError() {
        tvErrorMessage.setVisibility(View.GONE);
        if (clearFeedbackRunnable != null) feedbackHandler.removeCallbacks(clearFeedbackRunnable);
    }

    private void showInfo() {
        tvInfoMessage.setVisibility(View.VISIBLE);
        if (clearInfoRunnable != null) feedbackHandler.removeCallbacks(clearInfoRunnable);
        clearInfoRunnable = () -> tvInfoMessage.setVisibility(View.GONE);
        feedbackHandler.postDelayed(clearInfoRunnable, 5000);
    }

    private void hideInfo() {
        tvInfoMessage.setVisibility(View.GONE);
        if (clearInfoRunnable != null) feedbackHandler.removeCallbacks(clearInfoRunnable);
    }
}