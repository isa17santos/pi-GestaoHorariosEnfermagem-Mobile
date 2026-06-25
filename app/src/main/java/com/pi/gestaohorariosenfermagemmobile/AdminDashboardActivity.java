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

public class AdminDashboardActivity extends BaseActivity {

    private TextView tvGreeting, tvOpsSubtitle;
    private String currentUserName;
    private NavbarManager navbarManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        navbarManager = new NavbarManager(this);

        // Inicializar Views
        initViews();

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Administrador");

        updateUIStrings();
        setupClickListeners();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(navbarManager != null) navbarManager.refreshUnreadCount();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvOpsSubtitle = findViewById(R.id.tv_ops_subtitle);
    }
    @Override
    protected void updateUIStrings() {
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

    private void setupClickListeners() {
        // Recursos Humanos
        findViewById(R.id.card_hr).setOnClickListener(v ->
                startActivity(new Intent(this, HumanResourcesActivity.class)));

        // Férias
        findViewById(R.id.card_vacations).setOnClickListener(v ->
                startActivity(new Intent(this, VacationsActivity.class)));

        // Baixas
        findViewById(R.id.card_sick).setOnClickListener(v ->
                startActivity(new Intent(this, MedicalLeavesActivity.class)));

        // Tipos de Turno
        findViewById(R.id.card_shifts).setOnClickListener(v ->
                startActivity(new Intent(this, ShiftTypesActivity.class)));

        // Estatísticas
        findViewById(R.id.card_stats).setOnClickListener(v ->
                startActivity(new Intent(this, AdminStatisticsActivity.class)));
    }
}