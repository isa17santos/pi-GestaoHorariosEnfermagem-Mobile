package com.pi.gestaohorariosenfermagemmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShiftTypeEditActivity extends AppCompatActivity {
    private EditText etName, etStartTime, etEndTime, etColor, etMinNurses;
    private View vColorPreview;
    private MaterialButton btnPickColor;
    private Button btnSubmit;
    private String token;
    private NestedScrollView nsvForm;
    private int shiftTypeId;

    private TextView tvTitle, tvSubtitle;
    private TextView tvLabelName, tvLabelStartTime, tvLabelEndTime, tvLabelColor, tvLabelMinNurses;
    private MaterialButton btnBack;
    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral;
    private TextView tvUserName, tvUserRole, tvLangFlag, tvLangLabel;
    private ImageButton btnProfile, btnLogout;
    private MaterialCardView btnLanguageSwitch;
    private final Handler errorHideHandler = new Handler(Looper.getMainLooper());

    // Saved form state
    private String savedName, savedStartTime, savedEndTime, savedColor, savedMinNurses;

    // Initialize the screen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_type_edit);

        // Handle keyboard appearance correctly on Android API 30+ by adjusting bottom padding
        // to prevent the keyboard from covering form input fields (name, times, color, min nurses)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, Math.max(imeInsets.bottom, systemBarsInsets.bottom));
            return insets;
        });

        // Restore state
        if (savedInstanceState != null) {
            restoreFromBundle(savedInstanceState);
        }

        initViews();
        loadIntentData();

        // Restore saved values if available
        if (savedInstanceState != null) {
            restoreSavedValues();
        }

        setupNavbar();
        setupFormActions();
    }

    // Restore saved form values after recreation
    private void restoreSavedValues() {
        if (savedName != null) etName.setText(savedName);
        if (savedStartTime != null) etStartTime.setText(savedStartTime);
        if (savedEndTime != null) etEndTime.setText(savedEndTime);
        if (savedColor != null) { etColor.setText(savedColor); applyColorPreview(savedColor); }
        if (savedMinNurses != null) etMinNurses.setText(savedMinNurses);
    }

    // Restore values from the saved state bundle
    private void restoreFromBundle(Bundle b) {
        savedName = b.getString("filledName");
        savedStartTime = b.getString("filledStartTime");
        savedEndTime = b.getString("filledEndTime");
        savedColor = b.getString("filledColor");
        savedMinNurses = b.getString("filledMinNurses");
    }

    // Save the current form state
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("filledName", etName.getText().toString());
        outState.putString("filledStartTime", etStartTime.getText().toString());
        outState.putString("filledEndTime", etEndTime.getText().toString());
        outState.putString("filledColor", etColor.getText().toString());
        outState.putString("filledMinNurses", etMinNurses.getText().toString());
    }

    // Bind all views from the layout
    private void initViews() {
        etName = findViewById(R.id.et_name);
        etStartTime = findViewById(R.id.et_start_time);
        etEndTime = findViewById(R.id.et_end_time);
        etColor = findViewById(R.id.et_color);
        etMinNurses = findViewById(R.id.et_min_nurses);
        vColorPreview = findViewById(R.id.v_color_preview);
        btnPickColor = findViewById(R.id.btn_pick_color);
        btnSubmit = findViewById(R.id.btn_submit);
        nsvForm = findViewById(R.id.nsv_form);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvLabelName = findViewById(R.id.tv_label_name);
        tvLabelStartTime = findViewById(R.id.tv_label_start_time);
        tvLabelEndTime = findViewById(R.id.tv_label_end_time);
        tvLabelColor = findViewById(R.id.tv_label_color);
        tvLabelMinNurses = findViewById(R.id.tv_label_min_nurses);
        btnBack = findViewById(R.id.btn_back);
        tvUserName = findViewById(R.id.tv_user_name_nav);
        tvUserRole = findViewById(R.id.tv_user_role_nav);
        btnProfile = findViewById(R.id.btn_profile);
        btnLogout = findViewById(R.id.btn_logout);
        btnLanguageSwitch = findViewById(R.id.btn_language_switch);
        tvLangFlag = findViewById(R.id.tv_language_flag);
        tvLangLabel = findViewById(R.id.tv_language_label);

        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);

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

        updateUIStrings();
        updateLanguageButton();
    }

    // Load the shift type data received from the previous screen
    private void loadIntentData() {
        Intent intent = getIntent();
        shiftTypeId = intent.getIntExtra("shiftTypeId", -1);
        String name = intent.getStringExtra("shiftTypeName");
        String startTime = intent.getStringExtra("shiftTypeStartTime");
        String endTime = intent.getStringExtra("shiftTypeEndTime");
        String color = intent.getStringExtra("shiftTypeColor");
        int minNurses = intent.getIntExtra("shiftTypeMinNurses", 0);

        if (name != null) etName.setText(name);
        if (startTime != null) etStartTime.setText(startTime);
        if (endTime != null) etEndTime.setText(endTime);
        if (color != null) { etColor.setText(color); applyColorPreview(color); }
        if (minNurses > 0) etMinNurses.setText(String.valueOf(minNurses));
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

    // Configure the form actions
    private void setupFormActions() {
        btnPickColor.setOnClickListener(v -> showColorPickerDialog());
        btnSubmit.setOnClickListener(v -> handleSubmit());
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
    private void updateLanguageButton() {
        String current = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = current.contains("en");
        if (tvLangFlag != null) tvLangFlag.setText(isEn ? "pt" : "en");
        if (tvLangLabel != null) tvLangLabel.setText(isEn ? "Português" : "English");
    }

    // Update all translated texts on the screen
    private void updateUIStrings() {
        if (tvTitle != null) tvTitle.setText(R.string.edit_shift_type_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.edit_shift_type_subtitle);
        btnBack.setText(R.string.back);
        btnPickColor.setText(R.string.btn_pick_color);
        btnSubmit.setText(R.string.btn_edit_shift_type_submit);
        tvErrGeneral.setText(R.string.err_form_general);

        String role = getSharedPreferences("AUTH", MODE_PRIVATE).getString("user_role", "");
        String currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        boolean isEn = currentLang.contains("en");
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

        if (tvLabelName != null) tvLabelName.setText(isEn ? "Shift Type Name" : "Nome do Tipo de Turno");
        if (tvLabelStartTime != null) tvLabelStartTime.setText(isEn ? "Start Time (HH:mm)" : "Hora de Início (HH:mm)");
        if (tvLabelEndTime != null) tvLabelEndTime.setText(isEn ? "End Time (HH:mm)" : "Hora de Encerramento (HH:mm)");
        if (tvLabelColor != null) tvLabelColor.setText(isEn ? "Color (Hex)" : "Cor (Hex)");
        if (tvLabelMinNurses != null) tvLabelMinNurses.setText(isEn ? "Minimum Number of Nurses" : "Número Mínimo de Enfermeiros");

        etName.setHint(R.string.hint_shift_type_name);
        etStartTime.setHint(R.string.hint_start_time);
        etEndTime.setHint(R.string.hint_end_time);
        etMinNurses.setHint(R.string.hint_min_nurses);
    }

    // Validate and submit the form
    private void handleSubmit() {
        if (shiftTypeId == -1) {
            cvErrorNotification.setVisibility(View.VISIBLE);
            startHideTimer();
            return;
        }

        errorHideHandler.removeCallbacksAndMessages(null);

        String name = etName.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String color = etColor.getText().toString().trim();
        String minNursesStr = etMinNurses.getText().toString().trim();

        boolean hasError = false;
        if (name.isEmpty()) hasError = true;
        if (startTime.isEmpty()) hasError = true;
        if (endTime.isEmpty()) hasError = true;
        if (color.isEmpty()) hasError = true;
        if (minNursesStr.isEmpty()) hasError = true;

        if (hasError) {
            cvErrorNotification.setVisibility(View.VISIBLE);
            if (nsvForm != null) nsvForm.smoothScrollTo(0, 0);
            startHideTimer();
            return;
        }

        int minNurses;
        try {
            minNurses = Integer.parseInt(minNursesStr);
        } catch (NumberFormatException e) {
            cvErrorNotification.setVisibility(View.VISIBLE);
            if (nsvForm != null) nsvForm.smoothScrollTo(0, 0);
            startHideTimer();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("...");

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("start_time", startTime);
        data.put("end_time", endTime);
        data.put("color", color);
        data.put("min_nurses", minNurses);

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateShiftType("Bearer " + token, shiftTypeId, data).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    TextView tvSuccessMsg = findViewById(R.id.tv_notification_msg);
                    if (tvSuccessMsg != null) {
                        tvSuccessMsg.setText(R.string.edit_shift_type_success);
                    }

                    LinearLayout llToast = findViewById(R.id.ll_notification_toast);
                    if (llToast != null) {
                        llToast.setVisibility(View.VISIBLE);
                        llToast.setAlpha(0f);
                        llToast.setTranslationY(-100f);
                        llToast.animate().alpha(1f).translationY(0f).setDuration(400);
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1500);
                } else {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(R.string.btn_edit_shift_type_submit);
                    cvErrorNotification.setVisibility(View.VISIBLE);
                    startHideTimer();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSubmit.setEnabled(true);
                btnSubmit.setText(R.string.btn_edit_shift_type_submit);
            }
        });
    }

    // Hide the error notification after a delay
    private void startHideTimer() {
        errorHideHandler.removeCallbacksAndMessages(null);
        errorHideHandler.postDelayed(() -> {
            cvErrorNotification.setVisibility(View.GONE);
        }, 6000);
    }

    // Show the preset color picker dialog
    private void showColorPickerDialog() {
        final String[] presetColors = {
            "#A78BFA", "#dff7e8", "#fff3d6", "#f4e6ff",
            "#FF5733", "#e3efff", "#fce7d8", "#B5EAD7",
            "#FFDAC1", "#FF9AA2", "#C7CEEA", "#E2F0CB"
        };
        String currentColor = etColor.getText().toString().trim();

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        container.setPadding(pad, pad, pad, pad);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.btn_pick_color);
        builder.setView(container);
        AlertDialog dialog = builder.create();

        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (int col = 0; col < 4; col++) {
                int idx = row * 4 + col;
                String hex = presetColors[idx];

                FrameLayout swatchContainer = new FrameLayout(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(56));
                lp.setMargins(dp(8), dp(8), dp(8), dp(8));
                swatchContainer.setLayoutParams(lp);

                View swatch = new View(this);
                swatch.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                try { bg.setColor(Color.parseColor(hex)); }
                catch (IllegalArgumentException e) { bg.setColor(Color.LTGRAY); }
                if (hex.equalsIgnoreCase(currentColor)) {
                    bg.setStroke(dp(4), Color.parseColor("#6D28D9"));
                }
                swatch.setBackground(bg);
                swatchContainer.addView(swatch);

                final String finalHex = hex;
                swatchContainer.setOnClickListener(v -> {
                    applyColorPreview(finalHex);
                    dialog.dismiss();
                });
                rowLayout.addView(swatchContainer);
            }
            container.addView(rowLayout);
        }
        dialog.show();
    }

    // Apply the selected color to the preview and hidden field
    private void applyColorPreview(String hex) {
        etColor.setText(hex);
        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(8));
            bg.setColor(Color.parseColor(hex));
            vColorPreview.setBackground(bg);
        } catch (IllegalArgumentException ignored) {}
    }

    // Convert dp to px
    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
