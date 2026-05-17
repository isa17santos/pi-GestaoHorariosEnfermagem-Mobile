package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;
    private TextView tvErrorMessage, tvLangFlag, tvLangLabel, tvForgotPass;
    private TextView tvBrandTitle, tvBrandText, tvBrandCardTitle, tvBrandCardText;
    private TextView tvWelcomeLabel, tvSigninLabel, tvLoginSubtitle;
    private MaterialCardView btnLangSwitch;

    private final Handler feedbackHandler = new Handler();
    private Runnable clearFeedbackRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getApplicationLocales().isEmpty()) {
            setLocale("pt");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle keyboard appearance correctly on Android API 30+ by adjusting bottom padding
        // to prevent the keyboard from covering input fields
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, Math.max(imeInsets.bottom, systemBarsInsets.bottom));
            return insets;
        });

        // Inicializar Views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        btnLogin = findViewById(R.id.btn_login);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        btnLangSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);
        tvForgotPass = findViewById(R.id.tv_forgot_password);

        tvBrandTitle = findViewById(R.id.tv_brand_title);
        tvBrandText = findViewById(R.id.tv_brand_text);
        tvBrandCardTitle = findViewById(R.id.tv_brand_card_title);
        tvBrandCardText = findViewById(R.id.tv_brand_card_text);
        tvWelcomeLabel = findViewById(R.id.tv_welcome_label);
        tvSigninLabel = findViewById(R.id.tv_signin_label);
        tvLoginSubtitle = findViewById(R.id.tv_login_subtitle);

        // Adicionar sublinhado ao link
        tvForgotPass.setPaintFlags(tvForgotPass.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        btnLangSwitch.setOnClickListener(v -> toggleLanguage());

        // Navegação para Forget Password
        tvForgotPass.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Limpa os campos sempre que a atividade volta a ficar visível
        if (etEmail != null) etEmail.setText("");
        if (etPassword != null) etPassword.setText("");
        if (tvErrorMessage != null) tvErrorMessage.setVisibility(View.GONE);
    }
    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        setLocale(current.contains("en") ? "pt" : "en");
    }

    private void setLocale(String langCode) {
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(langCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
        updateUIStrings();
        updateLanguageButton();
    }

    @Override
    protected void updateUIStrings() {
        if (tvBrandTitle == null) return;
        tvBrandTitle.setText(R.string.brand_title);
        tvBrandText.setText(R.string.brand_text);
        tvBrandCardTitle.setText(R.string.brand_card_title);
        tvBrandCardText.setText(R.string.brand_card_text);
        tvWelcomeLabel.setText(R.string.welcome);
        tvSigninLabel.setText(R.string.sign_in);
        tvLoginSubtitle.setText(R.string.login_subtitle);
        tilEmail.setHint(R.string.email_label);
        tilPassword.setHint(R.string.password_label);
        tvForgotPass.setText(R.string.forgot_password);
        btnLogin.setText(R.string.sign_in);
    }

    @Override
    protected void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (tvLangFlag != null && tvLangLabel != null) {
            boolean isEn = current.contains("en");
            tvLangFlag.setText(isEn ? "pt" : "en");
            tvLangLabel.setText(isEn ? "Português" : "English");
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateUIStrings();
        updateLanguageButton();
    }

    private void handleLogin() {
        if (etEmail.getText() == null || etPassword.getText() == null) return;

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        hideError();

        if (email.isEmpty()) {
            showError(getString(R.string.email_required));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.email_invalid));
            return;
        }

        if (password.isEmpty()) {
            showError(getString(R.string.password_required));
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.signing_in);

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(email, password);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.sign_in);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginRes = response.body();

                    if (loginRes.isMustChangePassword()) {
                        // Redireciona para Reset Password passando dados necessários
                        Intent intent = new Intent(MainActivity.this, ResetPasswordActivity.class);
                        intent.putExtra("email", loginRes.getEmail());
                        intent.putExtra("token", loginRes.getPasswordResetToken());
                        startActivity(intent);
                    } else {
                        // Sucesso total! Ir para o Dashboard e GUARDAR O TOKEN
                        getSharedPreferences("AUTH", MODE_PRIVATE)
                                .edit()
                                .putString("token", loginRes.getToken())
                                .putString("user_name", loginRes.getUser().getName())
                                .putString("user_role", loginRes.getUser().getRole())
                                .apply();

                        LoginResponse.UserData user = loginRes.getUser();
                        String role = (user != null && user.getRole() != null) ? user.getRole().toLowerCase().trim() : "";

                        Intent intent;

                        // Roteamento baseado no Role
                        if ("admin".equals(role)) {
                            intent = new Intent(MainActivity.this, AdminDashboardActivity.class);
                        } else if ("nurse".equals(role)) {
                            intent = new Intent(MainActivity.this, NurseDashboardActivity.class);
                        } else{
                            intent = new Intent(MainActivity.this, HeadNurseDashboardActivity.class);
                        }

                        // Passar os dados do utilizador para a Dashboard
                        if (user != null) {
                            intent.putExtra("USER_NAME", user.getName());
                            intent.putExtra("USER_ROLE", user.getRole());
                        }

                        startActivity(intent);
                        finish(); // Impede voltar ao login
                    }
                } else {
                    showError(getString(R.string.invalid_credentials));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText(R.string.sign_in);

                // Isto vai mostrar a causa real (ex: timeout, erro de rede, erro de URL) no ecrã
                String errorMsg = "Erro: " + (t.getMessage() != null ? t.getMessage() : "Desconhecido");
                showError(errorMsg);

                Log.e("API_ERROR", errorMsg, t);
            }
        });
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);

        if (clearFeedbackRunnable != null) {
            feedbackHandler.removeCallbacks(clearFeedbackRunnable);
        }

        clearFeedbackRunnable = () -> tvErrorMessage.setVisibility(View.GONE);
        feedbackHandler.postDelayed(clearFeedbackRunnable, 4000);
    }

    private void hideError() {
        tvErrorMessage.setVisibility(View.GONE);
    }
}
