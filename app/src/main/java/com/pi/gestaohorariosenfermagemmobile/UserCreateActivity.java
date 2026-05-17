package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserCreateActivity extends BaseActivity {
    private EditText etName, etEmail, etPassword, etConfirm;
    private Spinner spinnerRole, spinnerStatus;
    private Button btnSubmit;
    private String token;
    private NestedScrollView nsvForm;

    private TextView tvTitle, tvSubtitle;
    private TextView tvLabelName, tvLabelEmail, tvLabelPass, tvLabelConfirm, tvLabelRole, tvLabelStatus;
    private MaterialButton btnBack;

    // Views de Erro e Spacers
    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral, tvErrName, tvErrEmail, tvErrPass, tvErrConfirm, tvErrRole;
    private View spacerName, spacerEmail, spacerPass, spacerConfirm, spacerRole;

    // Elementos da Navbar
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;

    // Flags de Persistência
    private boolean isNameErr, isEmailErr, isPassErr, isConfirmErr, isRoleErr, isGenErr;
    private String savedName, savedEmail, savedPass, savedConf;
    private int savedRolePos, savedStatusPos;

    // Handler para esconder o aviso geral
    private final Handler errorHideHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_create);

        // Handle keyboard appearance correctly on Android API 30+ by adjusting bottom padding
        // to prevent the keyboard from covering form input fields (name, email, password, confirmation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, Math.max(imeInsets.bottom, systemBarsInsets.bottom));
            return insets;
        });

        // 1. Restaurar estado
        if (savedInstanceState != null) {
            restoreFromBundle(savedInstanceState);
        } else {
            restoreFromIntent(getIntent());
        }

        initViews();
        setupNavbar();
        updateUIStrings();

        if (isGenErr) startHideTimer();
    }

    private void restoreFromBundle(Bundle b) {
        isNameErr = b.getBoolean("isNameErr", false);
        isEmailErr = b.getBoolean("isEmailErr", false);
        isPassErr = b.getBoolean("isPassErr", false);
        isConfirmErr = b.getBoolean("isConfirmErr", false);
        isRoleErr = b.getBoolean("isRoleErr", false);
        isGenErr = b.getBoolean("isGenErr", false);
        savedName = b.getString("filledName");
        savedEmail = b.getString("filledEmail");
        savedPass = b.getString("filledPass");
        savedConf = b.getString("filledConf");
        savedRolePos = b.getInt("filledRolePos", 0);
        savedStatusPos = b.getInt("filledStatusPos", 0);
    }

    private void restoreFromIntent(Intent i) {
        isNameErr = i.getBooleanExtra("isNameErr", false);
        isEmailErr = i.getBooleanExtra("isEmailErr", false);
        isPassErr = i.getBooleanExtra("isPassErr", false);
        isConfirmErr = i.getBooleanExtra("isConfirmErr", false);
        isRoleErr = i.getBooleanExtra("isRoleErr", false);
        isGenErr = i.getBooleanExtra("isGenErr", false);
        savedName = i.getStringExtra("filledName");
        savedEmail = i.getStringExtra("filledEmail");
        savedPass = i.getStringExtra("filledPass");
        savedConf = i.getStringExtra("filledConf");
        savedRolePos = i.getIntExtra("filledRolePos", 0);
        savedStatusPos = i.getIntExtra("filledStatusPos", 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isNameErr", isNameErr);
        outState.putBoolean("isEmailErr", isEmailErr);
        outState.putBoolean("isPassErr", isPassErr);
        outState.putBoolean("isConfirmErr", isConfirmErr);
        outState.putBoolean("isRoleErr", isRoleErr);
        outState.putBoolean("isGenErr", isGenErr);
        outState.putString("filledName", etName.getText().toString());
        outState.putString("filledEmail", etEmail.getText().toString());
        outState.putString("filledPass", etPassword.getText().toString());
        outState.putString("filledConf", etConfirm.getText().toString());
        outState.putInt("filledRolePos", spinnerRole.getSelectedItemPosition());
        outState.putInt("filledStatusPos", spinnerStatus.getSelectedItemPosition());
    }

    private void initViews() {
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirm = findViewById(R.id.et_password_confirmation);
        spinnerRole = findViewById(R.id.spinner_role);
        spinnerStatus = findViewById(R.id.spinner_status);
        btnSubmit = findViewById(R.id.btn_submit);
        nsvForm = findViewById(R.id.nsv_form);

        // Restaurar textos digitados
        if (savedName != null) etName.setText(savedName);
        if (savedEmail != null) etEmail.setText(savedEmail);
        if (savedPass != null) etPassword.setText(savedPass);
        if (savedConf != null) etConfirm.setText(savedConf);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnBack = findViewById(R.id.btn_back);

        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);
        tvErrName = findViewById(R.id.tv_error_name);
        tvErrEmail = findViewById(R.id.tv_error_email);
        tvErrPass = findViewById(R.id.tv_error_password);
        tvErrConfirm = findViewById(R.id.tv_error_confirm);
        tvErrRole = findViewById(R.id.tv_error_role);

        spacerName = findViewById(R.id.spacer_name);
        spacerEmail = findViewById(R.id.spacer_email);
        spacerPass = findViewById(R.id.spacer_password);
        spacerConfirm = findViewById(R.id.spacer_confirm);
        spacerRole = findViewById(R.id.spacer_role);

        tvLabelName = findViewById(R.id.tv_label_name);
        tvLabelEmail = findViewById(R.id.tv_label_email);
        tvLabelPass = findViewById(R.id.tv_label_password);
        tvLabelConfirm = findViewById(R.id.tv_label_confirm_password);
        tvLabelRole = findViewById(R.id.tv_label_role);
        tvLabelStatus = findViewById(R.id.tv_label_status);

        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

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

        findViewById(R.id.btn_language_switch).setOnClickListener(v -> toggleLanguage());
        findViewById(R.id.btn_profile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            getSharedPreferences("AUTH", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void toggleLanguage() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String nextLang = current.contains("en") ? "pt" : "en";

        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang));

        Intent intent = getIntent();
        // Guardar erros
        intent.putExtra("isNameErr", tvErrName.getVisibility() == View.VISIBLE);
        intent.putExtra("isEmailErr", tvErrEmail.getVisibility() == View.VISIBLE);
        intent.putExtra("isPassErr", tvErrPass.getVisibility() == View.VISIBLE);
        intent.putExtra("isConfirmErr", tvErrConfirm.getVisibility() == View.VISIBLE);
        intent.putExtra("isRoleErr", tvErrRole.getVisibility() == View.VISIBLE);
        intent.putExtra("isGenErr", cvErrorNotification.getVisibility() == View.VISIBLE);

        // Guardar dados inseridos
        intent.putExtra("filledName", etName.getText().toString());
        intent.putExtra("filledEmail", etEmail.getText().toString());
        intent.putExtra("filledPass", etPassword.getText().toString());
        intent.putExtra("filledConf", etConfirm.getText().toString());
        intent.putExtra("filledRolePos", spinnerRole.getSelectedItemPosition());
        intent.putExtra("filledStatusPos", spinnerStatus.getSelectedItemPosition());

        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void updateUIStrings() {
        if (tvTitle != null) tvTitle.setText(R.string.create_user_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.create_user_subtitle);
        btnBack.setText(R.string.back);
        btnSubmit.setText(R.string.btn_create_user_submit);
        tvErrGeneral.setText(R.string.err_form_general);

        tvLabelName.setText(R.string.label_name);
        tvLabelEmail.setText(R.string.label_email);
        tvLabelPass.setText(R.string.label_password);
        tvLabelConfirm.setText(R.string.label_confirm_password);
        tvLabelRole.setText(R.string.label_role);
        tvLabelStatus.setText(R.string.label_status);

        etName.setHint(R.string.hint_name);
        etEmail.setHint(R.string.hint_email);
        etPassword.setHint(R.string.hint_password);
        etConfirm.setHint(R.string.hint_confirm_password);

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


        restoreErrorTexts();
        setupSpinners();
        updateLanguageButton();
    }

    private void restoreErrorTexts() {
        tvErrName.setVisibility(isNameErr ? View.VISIBLE : View.GONE);
        tvErrName.setText(R.string.err_name_required);
        spacerName.setVisibility(isNameErr ? View.GONE : View.VISIBLE);

        tvErrEmail.setVisibility(isEmailErr ? View.VISIBLE : View.GONE);
        String emailT = etEmail.getText().toString().trim();
        tvErrEmail.setText(emailT.isEmpty() ? R.string.err_email_required : R.string.err_email_invalid);
        spacerEmail.setVisibility(isEmailErr ? View.GONE : View.VISIBLE);

        tvErrPass.setVisibility(isPassErr ? View.VISIBLE : View.GONE);
        String passT = etPassword.getText().toString();
        tvErrPass.setText(passT.isEmpty() ? R.string.err_password_required : R.string.err_password_weak);
        spacerPass.setVisibility(isPassErr ? View.GONE : View.VISIBLE);

        tvErrConfirm.setVisibility(isConfirmErr ? View.VISIBLE : View.GONE);
        String confT = etConfirm.getText().toString();
        tvErrConfirm.setText(confT.isEmpty() ? R.string.err_confirm_required : R.string.err_password_mismatch);
        spacerConfirm.setVisibility(isConfirmErr ? View.GONE : View.VISIBLE);

        tvErrRole.setVisibility(isRoleErr ? View.VISIBLE : View.GONE);
        tvErrRole.setText(R.string.err_role_required);
        spacerRole.setVisibility(isRoleErr ? View.GONE : View.VISIBLE);

        cvErrorNotification.setVisibility(isGenErr ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void updateLanguageButton() {
        boolean isEn = AppCompatDelegate.getApplicationLocales().toLanguageTags().contains("en");
        tvLangFlag.setText(isEn ? "pt" : "en");
        tvLangLabel.setText(isEn ? "Português" : "English");
    }

    private void setupSpinners() {
        int rPos = (savedRolePos != 0) ? savedRolePos : spinnerRole.getSelectedItemPosition();
        int sPos = (savedStatusPos != 0) ? savedStatusPos : spinnerStatus.getSelectedItemPosition();

        String[] roles = { getString(R.string.select_role_default), getString(R.string.role_nurse), getString(R.string.role_head_nurse) };
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item_selected, roles) {
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                if (position == 0) {
                    View v = new View(getContext()); v.setVisibility(View.GONE);
                    v.setLayoutParams(new AbsListView.LayoutParams(0, 0)); return v;
                }
                return super.getDropDownView(position, null, parent);
            }
        };
        roleAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerRole.setAdapter(roleAdapter);
        spinnerRole.setSelection(rPos);

        String[] stats = { getString(R.string.status_active), getString(R.string.status_inactive) };
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, stats);
        sAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerStatus.setAdapter(sAdapter);
        spinnerStatus.setSelection(sPos);
    }

    private void handleSubmit() {
        errorHideHandler.removeCallbacksAndMessages(null);

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();
        String conf = etConfirm.getText().toString();

        isNameErr = name.isEmpty();
        isEmailErr = email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        String passwordRegex = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
        isPassErr = pass.isEmpty() || !pass.matches(passwordRegex);
        isConfirmErr = conf.isEmpty() || !pass.equals(conf);
        isRoleErr = spinnerRole.getSelectedItemPosition() == 0;

        if (isNameErr || isEmailErr || isPassErr || isConfirmErr || isRoleErr) {
            isGenErr = true;
            restoreErrorTexts();
            if (nsvForm != null) nsvForm.smoothScrollTo(0, 0);
            startHideTimer();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("...");

        Map<String, Object> data = new HashMap<>();
        data.put("name", name); data.put("email", email); data.put("password", pass);
        data.put("password_confirmation", conf);
        data.put("role", spinnerRole.getSelectedItemPosition() == 1 ? "nurse" : "head_nurse");
        data.put("active", spinnerStatus.getSelectedItemPosition() == 0);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.createUser("Bearer " + token, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Encontra o TextView da mensagem de sucesso
                    TextView tvSuccessMsg = findViewById(R.id.tv_notification_msg);
                    if (tvSuccessMsg != null) {
                        // Define o texto usando a string bilingue do sistema
                        tvSuccessMsg.setText(R.string.create_user_success);
                    }

                    // Mostra a notificação
                    LinearLayout llToast = findViewById(R.id.ll_notification_toast);
                    if (llToast != null) {
                        llToast.setVisibility(View.VISIBLE);
                        llToast.setAlpha(0f);
                        llToast.setTranslationY(-100f);
                        llToast.animate().alpha(1f).translationY(0f).setDuration(400);
                    }

                    // Aguarda 1.5s e volta para a página anterior
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1500);
                } else {
                    // Lógica de erro já existente...
                    btnSubmit.setEnabled(true);
                    isGenErr = true;
                    restoreErrorTexts();
                    startHideTimer();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.btn_create_user_submit);
            }
        });
    }

    private void startHideTimer() {
        errorHideHandler.removeCallbacksAndMessages(null);
        errorHideHandler.postDelayed(() -> {
            isGenErr = false;
            cvErrorNotification.setVisibility(View.GONE);
        }, 6000);
    }
}