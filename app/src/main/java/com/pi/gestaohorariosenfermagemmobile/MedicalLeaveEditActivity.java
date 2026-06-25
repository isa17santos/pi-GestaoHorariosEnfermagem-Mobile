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

public class MedicalLeaveEditActivity extends BaseActivity {

    private Spinner spinnerNurse;
    private EditText etReason, etStartDate, etEndDate;
    private MaterialButton btnBack, btnSave;
    private TextView tvTitle, tvSubtitle, tvLabelNurse, tvLabelReason, tvLabelStart, tvLabelEnd;

    private MaterialCardView cvErrorNotification;
    private TextView tvErrGeneral, tvErrNurse, tvErrStart, tvErrEnd;
    private View spacerNurse;
    private NestedScrollView nsvForm;

    private NavbarManager navbarManager;
    private String token;
    private int leaveId = -1;
    private int initialUserId = -1;
    private List<User> nurses = new ArrayList<>();
    private String selectedStartDate, selectedEndDate;

    private boolean isNurseErr, isStartErr, isEndErr, isGenErr;
    private final Handler errorHideHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_leave_edit);

        navbarManager = new NavbarManager(this);
        token = getSharedPreferences("AUTH", MODE_PRIVATE).getString("token", "");

        initViews();

        if (getIntent() != null) {
            leaveId = getIntent().getIntExtra("leave_id", -1);
            initialUserId = getIntent().getIntExtra("user_id", -1);

            // LIMPEZA DE DATAS: Garante que apenas YYYY-MM-DD é guardado e enviado
            String rawStart = getIntent().getStringExtra("start_date");
            String rawEnd = getIntent().getStringExtra("end_date");
            if (rawStart != null && rawStart.length() >= 10) selectedStartDate = rawStart.substring(0, 10);
            if (rawEnd != null && rawEnd.length() >= 10) selectedEndDate = rawEnd.substring(0, 10);

            String reason = getIntent().getStringExtra("reason");
            if (etReason != null && reason != null && !reason.equals("-")) {
                etReason.setText(reason);
            }
        }

        if (savedInstanceState != null) {
            selectedStartDate = savedInstanceState.getString("startDate");
            selectedEndDate = savedInstanceState.getString("endDate");
            isNurseErr = savedInstanceState.getBoolean("isNurseErr");
            isStartErr = savedInstanceState.getBoolean("isStartErr");
            isEndErr = savedInstanceState.getBoolean("isEndErr");
            isGenErr = savedInstanceState.getBoolean("isGenErr");
            restoreErrorTexts();
        }

        updateUIStrings();
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

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (etStartDate != null) etStartDate.setOnClickListener(v -> showDatePicker(true));
        if (etEndDate != null) etEndDate.setOnClickListener(v -> showDatePicker(false));
        if (btnSave != null) btnSave.setOnClickListener(v -> updateMedicalLeave());
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateFormatted = String.format(Locale.US, "%02d-%02d-%d", dayOfMonth, month + 1, year);
            String isoDate = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
            if (isStart) {
                if (etStartDate != null) etStartDate.setText(dateFormatted);
                selectedStartDate = isoDate;
                isStartErr = false;
            } else {
                if (etEndDate != null) etEndDate.setText(dateFormatted);
                selectedEndDate = isoDate;
                isEndErr = false;
            }
            restoreErrorTexts();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadNurses() {
        if (token == null || token.isEmpty()) return;

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.getUsers("Bearer " + token).enqueue(new Callback<UsersResponse>() {
            @Override
            public void onResponse(Call<UsersResponse> call, Response<UsersResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    nurses.clear();
                    List<String> nurseNames = new ArrayList<>();
                    nurseNames.add(getString(R.string.select_nurse));
                    int selectionPos = 0;

                    for (User u : response.body().getData()) {
                        if (u != null && !u.getFormattedRole().equals("admin")) {
                            nurses.add(u);
                            nurseNames.add(u.getName());
                            if (u.getId() == initialUserId) {
                                selectionPos = nurseNames.size() - 1;
                            }
                        }
                    }

                    if (spinnerNurse != null) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MedicalLeaveEditActivity.this, R.layout.spinner_item_selected, nurseNames);
                        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
                        spinnerNurse.setAdapter(adapter);
                        spinnerNurse.setSelection(selectionPos);
                    }
                }
            }
            @Override public void onFailure(Call<UsersResponse> call, Throwable t) {}
        });
    }

    private void updateMedicalLeave() {
        if (leaveId == -1 || spinnerNurse == null) return;

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
        body.put("reason", etReason != null ? etReason.getText().toString() : "");

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        api.updateMedicalLeave("Bearer " + token, leaveId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showSuccessNotification(getString(R.string.medical_leave_update_success));
                    new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 1500);
                } else {
                    btnSave.setEnabled(true);
                    isGenErr = true;
                    restoreErrorTexts();
                    startHideTimer();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
            }
        });
    }

    private void restoreErrorTexts() {
        if (tvErrNurse == null) return;
        tvErrNurse.setVisibility(isNurseErr ? View.VISIBLE : View.GONE);
        tvErrNurse.setText(R.string.medical_leave_select_required);
        if (spacerNurse != null) spacerNurse.setVisibility(isNurseErr ? View.GONE : View.VISIBLE);

        if (tvErrStart != null) {
            tvErrStart.setVisibility(isStartErr ? View.VISIBLE : View.GONE);
            tvErrStart.setText(R.string.medical_leave_select_required);
        }
        if (tvErrEnd != null) {
            tvErrEnd.setVisibility(isEndErr ? View.VISIBLE : View.GONE);
            tvErrEnd.setText(R.string.medical_leave_select_required);
        }
        if (cvErrorNotification != null) cvErrorNotification.setVisibility(isGenErr ? View.VISIBLE : View.GONE);
        if (tvErrGeneral != null) tvErrGeneral.setText(R.string.err_form_general);
    }

    private void startHideTimer() {
        errorHideHandler.removeCallbacksAndMessages(null);
        errorHideHandler.postDelayed(() -> {
            isGenErr = false;
            if(cvErrorNotification != null) cvErrorNotification.setVisibility(View.GONE);
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
        if (tvTitle != null) tvTitle.setText(R.string.edit_medical_leave_title);
        if (tvSubtitle != null) tvSubtitle.setText(R.string.edit_medical_leave_subtitle);
        if (btnBack != null) btnBack.setText(R.string.back);
        if (btnSave != null) btnSave.setText(R.string.update_medical_leave);
        if (tvLabelNurse != null) tvLabelNurse.setText(R.string.label_nurse);
        if (tvLabelReason != null) tvLabelReason.setText(R.string.label_reason_optional);
        if (tvLabelStart != null) tvLabelStart.setText(R.string.label_start_date);
        if (tvLabelEnd != null) tvLabelEnd.setText(R.string.label_end_date);
        if (etReason != null) etReason.setHint(R.string.reason_hint);

        if (selectedStartDate != null && etStartDate != null) etStartDate.setText(formatToDisplay(selectedStartDate));
        if (selectedEndDate != null && etEndDate != null) etEndDate.setText(formatToDisplay(selectedEndDate));

        restoreErrorTexts();
        loadNurses();
    }
}