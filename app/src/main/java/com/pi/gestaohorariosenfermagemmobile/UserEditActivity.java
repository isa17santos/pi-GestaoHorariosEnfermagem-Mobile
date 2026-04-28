package com.pi.gestaohorariosenfermagemmobile;import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserEditActivity extends AppCompatActivity {
    private EditText etName, etEmail;
    private Spinner spinnerRole, spinnerStatus;
    private Button btnSubmit;
    private String token;
    private int userId;
    private NestedScrollView nsvForm;

    private TextView tvTitle, tvSubtitle;
    private TextView tvLabelName, tvLabelEmail, tvLabelRole, tvLabelStatus;
    private MaterialButton btnBack;

    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral, tvErrName, tvErrEmail, tvErrRole;
    private View spacerName, spacerEmail, spacerRole;

    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;

    // Flags de Persistência (Idêntico ao UserCreate)
    private boolean isNameErr, isEmailErr, isRoleErr, isGenErr;
    private String savedName, savedEmail;
    private int savedRolePos, savedStatusPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);

        // 1. Restaurar dados (Prioridade para o Intent que vem do toggleLanguage)
        restoreData(savedInstanceState);

        initViews();
        setupNavbar();
        updateUIStrings();
    }

    private void restoreData(Bundle savedInstanceState) {
        Intent i = getIntent();
        userId = i.getIntExtra("user_id", -1);

        // Se houver estado guardado pelo sistema
        if (savedInstanceState != null) {
            isNameErr = savedInstanceState.getBoolean("isNameErr", false);
            isEmailErr = savedInstanceState.getBoolean("isEmailErr", false);
            isRoleErr = savedInstanceState.getBoolean("isRoleErr", false);
            isGenErr = savedInstanceState.getBoolean("isGenErr", false);
            savedName = savedInstanceState.getString("filledName");
            savedEmail = savedInstanceState.getString("filledEmail");
            savedRolePos = savedInstanceState.getInt("filledRolePos", 0);
            savedStatusPos = savedInstanceState.getInt("filledStatusPos", 0);
        } else {
            // Se vier do Intent (Dashboard ou toggleLanguage)
            isNameErr = i.getBooleanExtra("isNameErr", false);
            isEmailErr = i.getBooleanExtra("isEmailErr", false);
            isRoleErr = i.getBooleanExtra("isRoleErr", false);
            isGenErr = i.getBooleanExtra("isGenErr", false);

            // Tenta pegar o que o utilizador já escreveu (filledName) ou o que veio da base de dados (user_name)
            savedName = i.hasExtra("filledName") ? i.getStringExtra("filledName") : i.getStringExtra("user_name");
            savedEmail = i.hasExtra("filledEmail") ? i.getStringExtra("filledEmail") : i.getStringExtra("user_email");

            // Para os spinners, decide se usa a posição guardada ou mapeia o slug vindo da BD
            if (i.hasExtra("filledRolePos")) {
                savedRolePos = i.getIntExtra("filledRolePos", 0);
                savedStatusPos = i.getIntExtra("filledStatusPos", 0);
            } else {
                String slug = i.getStringExtra("user_role_slug");
                savedRolePos = "nurse".equals(slug) ? 1 : ("head_nurse".equals(slug) ? 2 : 0);
                savedStatusPos = i.getBooleanExtra("user_active", true) ? 0 : 1;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("user_id", userId);
        outState.putBoolean("isNameErr", isNameErr);
        outState.putBoolean("isEmailErr", isEmailErr);
        outState.putBoolean("isRoleErr", isRoleErr);
        outState.putBoolean("isGenErr", isGenErr);
        outState.putString("filledName", etName.getText().toString());
        outState.putString("filledEmail", etEmail.getText().toString());
        outState.putInt("filledRolePos", spinnerRole.getSelectedItemPosition());
        outState.putInt("filledStatusPos", spinnerStatus.getSelectedItemPosition());
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        spinnerRole = findViewById(R.id.spinner_role);
        spinnerStatus = findViewById(R.id.spinner_status);
        btnSubmit = findViewById(R.id.btn_submit);
        nsvForm = findViewById(R.id.nsv_form);

        if (savedName != null) etName.setText(savedName);
        if (savedEmail != null) etEmail.setText(savedEmail);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnBack = findViewById(R.id.btn_back);
        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);
        tvErrName = findViewById(R.id.tv_error_name);
        tvErrEmail = findViewById(R.id.tv_error_email);
        tvErrRole = findViewById(R.id.tv_error_role);
        spacerName = findViewById(R.id.spacer_name);
        spacerEmail = findViewById(R.id.spacer_email);
        spacerRole = findViewById(R.id.spacer_role);
        tvLabelName = findViewById(R.id.tv_label_name);
        tvLabelEmail = findViewById(R.id.tv_label_email);
        tvLabelRole = findViewById(R.id.tv_label_role);
        tvLabelStatus = findViewById(R.id.tv_label_status);
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);
        btnSubmit.setOnClickListener(v -> handleSubmit());

        SharedPreferences prefs = getSharedPreferences("AUTH", MODE_PRIVATE);
        token = prefs.getString("token", "");
        tvUserName.setText(prefs.getString("user_name", "Utilizador"));

        updateUIStrings();
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

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang));

        Intent intent = getIntent();
        // Guardar o ID e os dados atuais
        intent.putExtra("user_id", userId);
        intent.putExtra("filledName", etName.getText().toString());
        intent.putExtra("filledEmail", etEmail.getText().toString());
        intent.putExtra("filledRolePos", spinnerRole.getSelectedItemPosition());
        intent.putExtra("filledStatusPos", spinnerStatus.getSelectedItemPosition());

        // Guardar erros
        intent.putExtra("isNameErr", tvErrName.getVisibility() == View.VISIBLE);
        intent.putExtra("isEmailErr", tvErrEmail.getVisibility() == View.VISIBLE);
        intent.putExtra("isRoleErr", tvErrRole.getVisibility() == View.VISIBLE);
        intent.putExtra("isGenErr", cvErrorNotification.getVisibility() == View.VISIBLE);

        // Ordem correta para evitar o "piscar" ou ecrã preto
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void updateUIStrings() {
        tvTitle.setText(R.string.edit_user_title);
        tvSubtitle.setText(R.string.edit_user_subtitle);
        btnBack.setText(R.string.back);
        btnSubmit.setText(R.string.btn_update_user_submit);
        tvErrGeneral.setText(R.string.err_form_general);
        tvLabelName.setText(R.string.label_name);
        tvLabelEmail.setText(R.string.label_email);
        tvLabelRole.setText(R.string.label_role);
        tvLabelStatus.setText(R.string.label_status);

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

        setupSpinners();
        updateLanguageButton();
        restoreErrors();
    }

    private void restoreErrors() {
        tvErrName.setVisibility(isNameErr ? View.VISIBLE : View.GONE);
        tvErrName.setText(R.string.err_name_required);
        spacerName.setVisibility(isNameErr ? View.GONE : View.VISIBLE);

        tvErrEmail.setVisibility(isEmailErr ? View.VISIBLE : View.GONE);
        String emailT = etEmail.getText().toString().trim();
        tvErrEmail.setText(emailT.isEmpty() ? R.string.err_email_required : R.string.err_email_invalid);
        spacerEmail.setVisibility(isEmailErr ? View.GONE : View.VISIBLE);

        tvErrRole.setVisibility(isRoleErr ? View.VISIBLE : View.GONE);
        tvErrRole.setText(R.string.err_role_required);
        spacerRole.setVisibility(isRoleErr ? View.GONE : View.VISIBLE);

        cvErrorNotification.setVisibility(isGenErr ? View.VISIBLE : View.GONE);
    }

    private void setupSpinners() {
        String[] roles = { getString(R.string.select_role_default), getString(R.string.role_nurse), getString(R.string.role_head_nurse) };
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, roles);
        roleAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setSelection(savedRolePos);

        String[] stats = { getString(R.string.status_active), getString(R.string.status_inactive) };
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, stats);
        sAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerStatus.setAdapter(sAdapter);
        spinnerStatus.setSelection(savedStatusPos);
    }

    private void updateLanguageButton() {
        boolean isEn = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("en");
        tvLangFlag.setText(isEn ? "pt" : "en");
        tvLangLabel.setText(isEn ? "Português" : "English");
    }

    private void handleSubmit() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Validação simples
        isNameErr = name.isEmpty();
        isEmailErr = email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        isRoleErr = spinnerRole.getSelectedItemPosition() == 0;

        if (isNameErr || isEmailErr || isRoleErr) {
            isGenErr = true;
            restoreErrors(); // Método que já tens para mostrar as mensagens de erro na UI
            nsvForm.smoothScrollTo(0, 0);
            return;
        }

        btnSubmit.setEnabled(false); // Evita cliques duplos

        // Preparar os dados para o PATCH
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("role", spinnerRole.getSelectedItemPosition() == 1 ? "nurse" : "head_nurse");
        updates.put("active", spinnerStatus.getSelectedItemPosition() == 0);

        // Chamar a API
        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateUser("Bearer " + token, userId, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccess(); // Mostra o brinde/toast de sucesso e fecha a activity
                } else {
                    btnSubmit.setEnabled(true);
                    isGenErr = true;
                    restoreErrors();
                    // Opcional: mostrar mensagem de erro vinda do servidor
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSubmit.setEnabled(true);
                // Opcional: mostrar erro de conexão
            }
        });
    }

    private void showSuccess() {
        TextView tvMsg = findViewById(R.id.tv_notification_msg);
        tvMsg.setText(R.string.update_user_success);
        findViewById(R.id.ll_notification_toast).setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
    }
}