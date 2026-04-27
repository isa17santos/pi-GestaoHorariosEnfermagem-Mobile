package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserRole, tvGreeting, tvOpsSubtitle;
    private TextView tvLangFlag, tvLangLabel;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Inicializar Views
        initViews();

        // Receber dados
        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Utilizador");
        String role = prefs.getString("user_role", "Admin");

        // Configurar UI Inicial
        if (currentUserName != null) tvUserName.setText(currentUserName);
        if (role != null) tvUserRole.setText(role);

        updateUIStrings();
        updateLanguageButton();
        setupClickListeners();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserRole = findViewById(R.id.tv_user_role);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvOpsSubtitle = findViewById(R.id.tv_ops_subtitle);

        MaterialCardView btnLang = findViewById(R.id.btn_language_switch_dashboard);
        tvLangFlag = findViewById(R.id.tv_language_flag_dashboard);
        tvLangLabel = findViewById(R.id.tv_language_label_dashboard);

        if (btnLang != null) btnLang.setOnClickListener(v -> toggleLanguage());
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
        // Atualiza a saudação dinâmica
        tvGreeting.setText(getString(R.string.dashboard_admin_greeting, currentUserName));
        tvOpsSubtitle.setText(R.string.dashboard_ops_management);

        // Atualiza textos dos cartões
        updateCardText(R.id.card_hr, R.string.hr_management, R.string.hr_subtitle);
        updateCardText(R.id.card_vacations, R.string.vacations, R.string.vacations_subtitle);
        updateCardText(R.id.card_sick, R.string.absences, R.string.absences_subtitle);
        updateCardText(R.id.card_shifts, R.string.shift_types, R.string.shift_types_subtitle);
        updateCardText(R.id.card_stats, R.string.statistics, R.string.statistics_subtitle);
    }

    private void updateCardText(int cardId, int titleRes, int subRes) {
        View card = findViewById(cardId);
        if (card != null) {
            ((TextView) card.findViewWithTag("title")).setText(titleRes);
            ((TextView) card.findViewWithTag("subtitle")).setText(subRes);
        }
    }

    private void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    private void setupClickListeners() {
        // Perfil
        findViewById(R.id.btn_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Recursos Humanos
        findViewById(R.id.card_hr).setOnClickListener(v ->
                startActivity(new Intent(this, HumanResourcesActivity.class)));

        // Férias
        findViewById(R.id.card_vacations).setOnClickListener(v ->
                startActivity(new Intent(this, VacationsActivity.class)));

        // Baixas
        findViewById(R.id.card_sick).setOnClickListener(v ->
                startActivity(new Intent(this, AbsencesActivity.class)));

        // Tipos de Turno
        findViewById(R.id.card_shifts).setOnClickListener(v ->
                startActivity(new Intent(this, ShiftTypesActivity.class)));

        // Estatísticas
        findViewById(R.id.card_stats).setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class)));

        // Logout
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
    }
}