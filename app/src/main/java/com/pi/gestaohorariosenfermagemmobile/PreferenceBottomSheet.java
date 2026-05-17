package com.pi.gestaohorariosenfermagemmobile;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bottom Sheet Dialog for adding or editing monthly nurse preferences.
 * Supports bilingual (PT/EN) labels, past-month disabling, mutual
 * exclusivity for weekend switches, and pre-filling when editing.
 */
public class PreferenceBottomSheet extends BottomSheetDialogFragment {

    // ─── Callback interface ────────────────────────────────────────────────
    /** Receives the filled-in preference data Map when the user taps Save. */
    public interface OnPreferenceSavedListener {
        void onSaved(Map<String, Object> data);
    }

    // ─── Month name tables (PT & EN) ──────────────────────────────────────
    private static final String[] MONTHS_PT = {
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };
    private static final String[] MONTHS_EN = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    // ─── UI Views ──────────────────────────────────────────────────────────
    private TextView    tvTitle, tvLabelMonth, tvLabelYear, tvPrefsLabel, tvNotesLabel;
    private TextView    tvMorning, tvAfternoon, tvNight, tvPrefersWeekends, tvAvoidWeekends;
    private TextView    tvBsError;
    private MaterialCardView cvBsError;
    private Spinner     spMonth, spYear;
    private SwitchCompat swMorning, swAfternoon, swNight, swPrefersWeekends, swAvoidWeekends;
    private EditText    etNotes;
    private MaterialButton btnCancel, btnSave;

    // ─── Data ──────────────────────────────────────────────────────────────
    private final NursePreference preference;   // null when creating new
    private final boolean         isEdit;
    private final OnPreferenceSavedListener listener;

    // Spinner backing data
    private final List<Integer> yearList = new ArrayList<>();
    private int currentMonth; // 1-based
    private int currentYear;

    // ─── Constructor ──────────────────────────────────────────────────────
    /**
     * @param preference Existing preference to edit, or null to create new.
     * @param isEdit     True for edit mode; spinners are locked to existing month/year.
     * @param listener   Called with the form data Map when Save is confirmed.
     */
    public PreferenceBottomSheet(@Nullable NursePreference preference,
                                 boolean isEdit,
                                 OnPreferenceSavedListener listener) {
        this.preference = preference;
        this.isEdit     = isEdit;
        this.listener   = listener;
    }

    // ─── Fragment lifecycle ───────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_preference, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Expand the sheet to full height on open ───────────────────────
        // We grab the BottomSheetBehavior from the sheet's parent (the
        // design_bottom_sheet frame) and force it into the expanded state.
        // setSkipCollapsed(true) prevents a half-open peek state entirely.
        View parent = (View) view.getParent();
        if (parent != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        // Snapshot current date (1-based month)
        Calendar cal = Calendar.getInstance();
        currentMonth = cal.get(Calendar.MONTH) + 1;
        currentYear  = cal.get(Calendar.YEAR);

        initViews(view);
        applySwitchTints();   // thumb = white, track = purple/grey selector
        setupSpinners();

        // Pre-fill fields when editing an existing preference
        if (isEdit && preference != null) {
            prefillData();
        }

        setupWeekendSwitches(); // mutual exclusivity
        setupButtons();
        updateStrings();        // localise all labels
    }

    /** Expand the sheet automatically when it is shown. */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(di -> {
            View sheet = ((BottomSheetDialog) di)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> b = BottomSheetBehavior.from(sheet);
                b.setState(BottomSheetBehavior.STATE_EXPANDED);
                b.setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    // ─── View binding ─────────────────────────────────────────────────────

    private void initViews(View v) {
        tvTitle          = v.findViewById(R.id.tv_bs_title);
        tvLabelMonth     = v.findViewById(R.id.tv_bs_label_month);
        tvLabelYear      = v.findViewById(R.id.tv_bs_label_year);
        tvPrefsLabel     = v.findViewById(R.id.tv_bs_prefs_label);
        tvNotesLabel     = v.findViewById(R.id.tv_bs_notes_label);

        tvMorning        = v.findViewById(R.id.tv_morning);
        tvAfternoon      = v.findViewById(R.id.tv_afternoon);
        tvNight          = v.findViewById(R.id.tv_night);
        tvPrefersWeekends= v.findViewById(R.id.tv_prefers_weekends);
        tvAvoidWeekends  = v.findViewById(R.id.tv_avoid_weekends);

        spMonth          = v.findViewById(R.id.sp_month);
        spYear           = v.findViewById(R.id.sp_year);

        swMorning        = v.findViewById(R.id.sw_morning);
        swAfternoon      = v.findViewById(R.id.sw_afternoon);
        swNight          = v.findViewById(R.id.sw_night);
        swPrefersWeekends= v.findViewById(R.id.sw_prefers_weekends);
        swAvoidWeekends  = v.findViewById(R.id.sw_avoid_weekends);

        etNotes   = v.findViewById(R.id.et_notes);
        btnCancel = v.findViewById(R.id.btn_bs_cancel);
        btnSave   = v.findViewById(R.id.btn_bs_save);
        cvBsError = v.findViewById(R.id.cv_bs_error);
        tvBsError = v.findViewById(R.id.tv_bs_error);
    }

    // ─── Switch tints (programmatic) ──────────────────────────────────────

    /**
     * Sets thumb tint to white and track tint to a checked/unchecked
     * colour-state-list on every SwitchCompat in the form.
     * XML attributes alone are inconsistent across API levels; doing it here
     * guarantees the correct colours.
     */
    private void applySwitchTints() {
        // Track: purple when ON, grey when OFF
        ColorStateList trackTints = new ColorStateList(
            new int[][]{
                new int[]{ android.R.attr.state_checked },
                new int[]{}
            },
            new int[]{
                Color.parseColor("#7C3AED"),   // checked  — brand purple
                Color.parseColor("#CCCCCC")    // unchecked — light grey
            }
        );

        // Thumb: always white
        ColorStateList thumbTint = ColorStateList.valueOf(Color.WHITE);

        for (SwitchCompat sw : new SwitchCompat[]{
                swMorning, swAfternoon, swNight, swPrefersWeekends, swAvoidWeekends }) {
            if (sw != null) {
                sw.setTrackTintList(trackTints);
                sw.setThumbTintList(thumbTint);
            }
        }
    }

    // ─── Spinners ─────────────────────────────────────────────────────────

    /**
     * Builds the month and year spinners.
     *
     * Month adapter:
     *  - getView()         dims the currently-selected label if it is a past month
     *  - getDropDownView() dims past-month rows (alpha 0.4) so they appear disabled
     * An OnItemSelectedListener reverts any past-month selection to the nearest
     * available month (current month for the current year).
     *
     * IMPORTANT: the adapter is constructed with a *copy* of the month array so
     * that calling clear()/addAll() in updateStrings() never accidentally empties
     * the master MONTHS_PT / MONTHS_EN arrays.
     */
    private void setupSpinners() {
        boolean isEn = isEnglish();

        // ── Month spinner ──────────────────────────────────────────────────
        String[] months = isEn ? MONTHS_EN : MONTHS_PT;

        // Use a fresh ArrayList copy — avoids the clear()-wipes-backing-list bug
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(java.util.Arrays.asList(months))) {

            @Override
            public boolean isEnabled(int position) {
                // Past months are disabled only when creating a new preference
                return isEdit || (position + 1) >= currentMonth;
            }

            /** Closed-spinner view: dim the item if it represents a past month. */
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                view.setAlpha(isEnabled(position) ? 1f : 0.4f);
                return view;
            }

            /** Drop-down item view: dim past months to 0.4 alpha. */
            @Override
            public View getDropDownView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setAlpha(isEnabled(position) ? 1f : 0.4f);
                return view;
            }
        };
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(monthAdapter);

        // Prevent the user from landing on a disabled (past) month
        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!isEdit && (position + 1) < currentMonth) {
                    // Revert to the current month (0-based index)
                    spMonth.setSelection(currentMonth - 1);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Year spinner ───────────────────────────────────────────────────
        yearList.clear();
        for (int i = 0; i < 3; i++) yearList.add(currentYear + i);

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                yearList);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);

        // ── Pre-select & lock when editing ────────────────────────────────
        if (isEdit && preference != null) {
            int monthIdx = preference.getMonth() - 1;           // 0-based
            int yearIdx  = yearList.indexOf(preference.getYear());

            if (monthIdx >= 0 && monthIdx < 12)  spMonth.setSelection(monthIdx);
            if (yearIdx  >= 0)                   spYear.setSelection(yearIdx);

            // Lock spinners — the month/year of an existing entry cannot change
            spMonth.setEnabled(false);
            spYear.setEnabled(false);
            spMonth.setAlpha(0.6f);
            spYear.setAlpha(0.6f);
        } else {
            // Default to current month / current year
            spMonth.setSelection(currentMonth - 1);
            spYear.setSelection(0);
            spMonth.setEnabled(true);
            spYear.setEnabled(true);
            spMonth.setAlpha(1f);
            spYear.setAlpha(1f);
        }
    }

    // ─── Pre-fill (edit mode) ─────────────────────────────────────────────

    /** Populates all form fields from the existing NursePreference object. */
    private void prefillData() {
        if (preference == null) return;

        swMorning.setChecked(preference.prefersMorning());
        swAfternoon.setChecked(preference.prefersAfternoon());
        swNight.setChecked(preference.prefersNight());
        swPrefersWeekends.setChecked(preference.prefersWeekends());
        swAvoidWeekends.setChecked(preference.avoidsWeekends());

        if (preference.getNotes() != null && !preference.getNotes().isEmpty()) {
            etNotes.setText(preference.getNotes());
        }
    }

    // ─── Weekend mutual exclusivity ───────────────────────────────────────

    /**
     * "Prefers weekends" and "Avoid weekends" are mutually exclusive.
     * Enabling one automatically unchecks the other.
     * Any switch change also clears a pending validation error.
     */
    private void setupWeekendSwitches() {
        swPrefersWeekends.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && swAvoidWeekends.isChecked()) swAvoidWeekends.setChecked(false);
            clearError();
        });
        swAvoidWeekends.setOnCheckedChangeListener((btn, checked) -> {
            if (checked && swPrefersWeekends.isChecked()) swPrefersWeekends.setChecked(false);
            clearError();
        });

        // Clear the error banner whenever any of the remaining switches change
        for (SwitchCompat sw : new SwitchCompat[]{ swMorning, swAfternoon, swNight }) {
            sw.setOnCheckedChangeListener((btn, checked) -> clearError());
        }
    }

    // ─── Buttons ──────────────────────────────────────────────────────────

    private void setupButtons() {
        // Cancel: simply close the sheet without any action
        btnCancel.setOnClickListener(v -> dismiss());

        // Save: validate → collect data → hand off to caller → close
        btnSave.setOnClickListener(v -> {
            if (!validateInput()) return;

            Map<String, Object> data = getData();
            if (listener != null) listener.onSaved(data);
            dismiss();
        });
    }

    // ─── Validation ───────────────────────────────────────────────────────

    /**
     * Accepts any combination where at least one of the five switches is ON
     * (shift or weekend preference counts equally).
     * Errors are shown inline using the cv_bs_error card, matching the
     * cv_error_notification pattern in ProfileActivity.
     */
    private boolean validateInput() {
        boolean en = isEnglish();

        // At least one of the five preference switches must be enabled
        boolean anySelected = swMorning.isChecked()
                || swAfternoon.isChecked()
                || swNight.isChecked()
                || swPrefersWeekends.isChecked()
                || swAvoidWeekends.isChecked();

        if (!anySelected) {
            showError(en ? "Please select at least one preference"
                         : "Selecione pelo menos uma preferência");
            return false;
        }

        // New preference must not reference a past month
        if (!isEdit) {
            int selMonth = spMonth.getSelectedItemPosition() + 1;
            int selYear  = yearList.get(spYear.getSelectedItemPosition());
            if (selYear == currentYear && selMonth < currentMonth) {
                showError(en ? "Cannot select a past month"
                             : "Não pode selecionar um mês passado");
                return false;
            }
        }

        return true;
    }

    // Show the inline error card with the given message
    private void showError(String message) {
        if (cvBsError != null && tvBsError != null) {
            tvBsError.setText(message);
            cvBsError.setVisibility(View.VISIBLE);
        }
    }

    // Hide the inline error card (called when the user changes any switch)
    private void clearError() {
        if (cvBsError != null) cvBsError.setVisibility(View.GONE);
    }

    // ─── Data collection ──────────────────────────────────────────────────

    /**
     * Collects all form values into a Map ready to be sent to the API.
     * The API identifies the preference by the authenticated user + month/year,
     * so no "id" field is included in the body.
     *
     * @return Map with keys: month, year, prefers_morning, prefers_afternoon,
     *         prefers_night, prefers_weekends, avoid_weekends, notes.
     */
    public Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();

        data.put("month", spMonth.getSelectedItemPosition() + 1);
        data.put("year",  yearList.get(spYear.getSelectedItemPosition()));

        data.put("prefers_morning",   swMorning.isChecked());
        data.put("prefers_afternoon", swAfternoon.isChecked());
        data.put("prefers_night",     swNight.isChecked());

        data.put("prefers_weekends",  swPrefersWeekends.isChecked());
        data.put("avoid_weekends",    swAvoidWeekends.isChecked());

        // Send null for empty notes so the API receives a proper absence value,
        // not an empty string that may fail server-side validation.
        String notes = etNotes.getText().toString().trim();
        data.put("notes", notes.isEmpty() ? null : notes);


        return data;
    }

    // ─── Localisation ─────────────────────────────────────────────────────

    /**
     * Updates every label, hint, and button text in the current language.
     * Also rebuilds the month spinner content so names reflect the language
     * without touching the master MONTHS_PT / MONTHS_EN arrays.
     */
    private void updateStrings() {
        boolean en = isEnglish();

        // Title changes depending on mode
        if (tvTitle != null)
            tvTitle.setText(en ? (isEdit ? "Edit Preference"  : "Add Preference")
                               : (isEdit ? "Editar Preferência" : "Adicionar Preferência"));

        if (tvLabelMonth  != null) tvLabelMonth.setText(en  ? "Month"        : "Mês");
        if (tvLabelYear   != null) tvLabelYear.setText(en   ? "Year"         : "Ano");
        if (tvPrefsLabel  != null) tvPrefsLabel.setText(en  ? "Preferences"  : "Preferências");

        if (tvMorning       != null) tvMorning.setText(en        ? "Morning Shift"        : "Turno da Manhã");
        if (tvAfternoon     != null) tvAfternoon.setText(en      ? "Afternoon Shift"      : "Turno da Tarde");
        if (tvNight         != null) tvNight.setText(en          ? "Night Shift"          : "Turno da Noite");
        if (tvPrefersWeekends != null) tvPrefersWeekends.setText(en ? "Prefer Weekends"   : "Preferir Fins de Semana");
        if (tvAvoidWeekends != null) tvAvoidWeekends.setText(en  ? "Avoid Weekends"       : "Evitar Fins de Semana");

        if (tvNotesLabel != null) tvNotesLabel.setText(en ? "Additional Notes" : "Notas Adicionais");
        if (etNotes      != null) etNotes.setHint(en      ? "Add any additional notes..." : "Adicione notas adicionais...");

        if (btnCancel != null) btnCancel.setText(en ? "Cancel"  : "Cancelar");
        if (btnSave   != null) btnSave.setText(en   ? "Save"    : "Guardar");

        // Rebuild month spinner with the correct language names.
        // We swap the adapter entirely to avoid clear()-on-backing-list pitfalls.
        if (spMonth != null) {
            int savedSelection = spMonth.getSelectedItemPosition();
            setupSpinners();          // rebuilds both spinners consistently
            // Restore selection (setupSpinners already handles edit-mode selection)
            if (!isEdit && savedSelection >= 0 && savedSelection < 12) {
                int validSel = Math.max(savedSelection, currentMonth - 1);
                spMonth.setSelection(validSel);
            }
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    /** Returns true when the app is running in English. */
    private boolean isEnglish() {
        return AppCompatDelegate.getApplicationLocales()
                .toLanguageTags()
                .contains("en");
    }
}
