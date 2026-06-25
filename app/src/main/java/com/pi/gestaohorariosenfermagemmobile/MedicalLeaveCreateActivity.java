package com.pi.gestaohorariosenfermagemmobile;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MedicalLeaveCreateActivity extends BaseActivity {

    private Spinner spinnerNurse;
    private EditText etReason, etStartDate, etEndDate;
    private MaterialButton btnBack, btnSave;
    private TextView tvTitle, tvSubtitle, tvLabelNurse, tvLabelReason, tvLabelStart, tvLabelEnd;
    private String token;
    private List<User> nurses = new ArrayList<>();
    private String selectedStartDate, selectedEndDate;

    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral, tvErrNurse, tvErrStart, tvErrEnd;
    private View spacerNurse;
    private NestedScrollView nsvForm;
    private NavbarManager navbarManager;

    private boolean isNurseErr, isStartErr, isEndErr, isGenErr;

    private int savedNursePos = 0;
    private final Handler errorHideHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_leave_create);

        if (savedInstanceState != null) {
            selectedStartDate = savedInstanceState.getString("startDate");
            selectedEndDate = savedInstanceState.getString("endDate");
            isNurseErr = savedInstanceState.getBoolean("isNurseErr");
            isStartErr = savedInstanceState.getBoolean("isStartErr");
            isEndErr = savedInstanceState.getBoolean("isEndErr");
            isGenErr = savedInstanceState.getBoolean("isGenErr");
            savedNursePos = savedInstanceState.getInt("nursePos", 0);
        }


        navbarManager = new NavbarManager(this);
        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("startDate", selectedStartDate);
        outState.putString("endDate", selectedEndDate);
        outState.putBoolean("isNurseErr", isNurseErr);
        outState.putBoolean("isStartErr", isStartErr);
        outState.putBoolean("isEndErr", isEndErr);
        outState.putBoolean("isGenErr", isGenErr);
        outState.putInt("nursePos", spinnerNurse.getSelectedItemPosition());
    }

    private void initViews() {
        spinnerNurse = findViewById(R.id.spinner_nurse);
        etReason = findViewById(R.id.et_reason);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        nsvForm = findViewById(R.id.nsv_form);

        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tvLabelNurse = findViewById(R.id.tv_label_nurse);
        tvLabelReason = findViewById(R.id.tv_label_reason);
        tvLabelStart = findViewById(R.id.tv_label_start);
        tvLabelEnd = findViewById(R.id.tv_label_end);

        cvErrorNotification = findViewById(R.id.cv_error_notification);
        tvErrGeneral = findViewById(R.id.tv_error_general);
        tvErrNurse = findViewById(R.id.tv_error_nurse);
        tvErrStart = findViewById(R.id.tv_error_start);
        tvErrEnd = findViewById(R.id.tv_error_end);
        spacerNurse = findViewById(R.id.spacer_nurse);

        btnBack.setOnClickListener(v -> finish());
        etStartDate.setOnClickListener(v -> showDatePicker(true));
        etEndDate.setOnClickListener(v -> showDatePicker(false));
        btnSave.setOnClickListener(v -> saveMedicalLeave());

        updateUIStrings();
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateFormatted = String.format(Locale.US, "%02d-%02d-%d", dayOfMonth, month + 1, year);
            String isoDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isStart) {
                etStartDate.setText(dateFormatted);
                selectedStartDate = isoDate;
                tvErrStart.setVisibility(View.GONE);
            } else {
                etEndDate.setText(dateFormatted);
                selectedEndDate = isoDate;
                isEndErr = false;
            }
            restoreErrorTexts();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadNurses() {
        final int currentSelection = (savedNursePos != 0) ? savedNursePos : spinnerNurse.getSelectedItemPosition();

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getUsers("Bearer " + token).enqueue(new Callback<UsersResponse>() {
            @Override
            public void onResponse(Call<UsersResponse> call, Response<UsersResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    nurses.clear();
                    List<String> nurseNames = new ArrayList<>();

                    // Texto inicial traduzido
                    nurseNames.add(getString(R.string.select_nurse));

                    for (User u : response.body().getData()) {
                        if (!u.getFormattedRole().equals("admin")) {
                            nurses.add(u);
                            nurseNames.add(u.getName());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MedicalLeaveCreateActivity.this,
                            R.layout.spinner_item_selected, nurseNames);
                    adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
                    spinnerNurse.setAdapter(adapter);

                    if (currentSelection < nurseNames.size()) {
                        spinnerNurse.setSelection(currentSelection);
                        savedNursePos = 0; // Reset após usar
                    }
                }
            }
            @Override public void onFailure(Call<UsersResponse> call, Throwable t) {}
        });
    }

    private void saveMedicalLeave() {
        errorHideHandler.removeCallbacksAndMessages(null);
        int pos = spinnerNurse.getSelectedItemPosition();

        isNurseErr = pos <= 0;
        isStartErr = selectedStartDate == null;
        isEndErr = selectedEndDate == null;

        if (isNurseErr || isStartErr || isEndErr) {
            isGenErr = true;
            restoreErrorTexts();
            if (nsvForm != null) nsvForm.smoothScrollTo(0, 0);
            startHideTimer();
            return;
        }

        btnSave.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", nurses.get(pos - 1).getId());
        body.put("start_date", selectedStartDate);
        body.put("end_date", selectedEndDate);
        body.put("reason", etReason.getText().toString());

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.createMedicalLeave("Bearer " + token, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccessNotification(getString(R.string.medical_leave_save_success));
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1500);
                } else {
                    btnSave.setEnabled(true);
                    isGenErr = true;
                    restoreErrorTexts();
                    startHideTimer();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) { btnSave.setEnabled(true); }
        });
    }

    private void restoreErrorTexts() {
        tvErrNurse.setVisibility(isNurseErr ? View.VISIBLE : View.GONE);
        tvErrNurse.setText(R.string.medical_leave_select_required);
        spacerNurse.setVisibility(isNurseErr ? View.GONE : View.VISIBLE);

        tvErrStart.setVisibility(isStartErr ? View.VISIBLE : View.GONE);
        tvErrStart.setText(R.string.medical_leave_select_required);

        tvErrEnd.setVisibility(isEndErr ? View.VISIBLE : View.GONE);
        tvErrEnd.setText(R.string.medical_leave_select_required);

        cvErrorNotification.setVisibility(isGenErr ? View.VISIBLE : View.GONE);
        tvErrGeneral.setText(R.string.err_form_general);
    }

    private void startHideTimer() {
        errorHideHandler.removeCallbacksAndMessages(null);
        errorHideHandler.postDelayed(() -> {
            isGenErr = false;
            cvErrorNotification.setVisibility(View.GONE);
        }, 6000);
    }

    private void showSuccessNotification(String message) {
        TextView tvMsg = findViewById(R.id.tv_notification_msg);
        LinearLayout llToast = findViewById(R.id.ll_notification_toast);
        if (tvMsg != null && llToast != null) {
            tvMsg.setText(message);
            llToast.setVisibility(View.VISIBLE);
            llToast.setAlpha(0f);
            llToast.setTranslationY(-100f);
            llToast.animate().alpha(1f).translationY(0f).setDuration(400);
        }
    }

    private String formatToDisplay(String isoDate) {
        if (isoDate == null || isoDate.length() < 10) return "";
        try {
            String year = isoDate.substring(0, 4);
            String month = isoDate.substring(5, 7);
            String day = isoDate.substring(8, 10);
            return day + "-" + month + "-" + year;
        } catch (Exception e) { return ""; }
    }


    @Override
    protected void updateUIStrings() {
        if (tvTitle != null) tvTitle.setText(R.string.assign_leave_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.assign_leave_subtitle);
        if (btnBack != null) btnBack.setText(R.string.back);
        if (btnSave != null) btnSave.setText(R.string.save_medical_leave);
        if (tvLabelNurse != null) tvLabelNurse.setText(R.string.label_nurse);
        if (tvLabelReason != null) tvLabelReason.setText(R.string.label_reason_optional);
        if (tvLabelStart != null) tvLabelStart.setText(R.string.label_start_date);
        if (tvLabelEnd != null) tvLabelEnd.setText(R.string.label_end_date);
        if (etReason != null) etReason.setHint(R.string.reason_hint);

        if (etStartDate != null) etStartDate.setHint("DD-MM-YYYY");
        if (etEndDate != null) etEndDate.setHint("DD-MM-YYYY");

        // Restaurar Valores das Datas
        if (selectedStartDate != null) etStartDate.setText(formatToDisplay(selectedStartDate));
        if (selectedEndDate != null) etEndDate.setText(formatToDisplay(selectedEndDate));

        restoreErrorTexts();
        // Recarregar o Spinner para traduzir o "Selecione um enfermeiro"
        loadNurses();
    }
}