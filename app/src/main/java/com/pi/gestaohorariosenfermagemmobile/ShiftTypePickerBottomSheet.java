package com.pi.gestaohorariosenfermagemmobile;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShiftTypePickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnShiftTypePickedListener {
        void onPicked(int shiftTypeId);
    }

    private static final String ARG_SHIFT_ID              = "shift_id";
    private static final String ARG_OFFERED_SHIFT_TYPE_ID = "offered_shift_type_id";

    private static final List<String> DAYOFF_NAMES = Arrays.asList(
            "dayoff", "day off", "folga", "holidays", "férias",
            "sick leave", "baixa médica", "parental leave", "licença parental",
            "baixa", "licença"
    );

    private int shiftId;
    private int offeredShiftTypeId;
    private OnShiftTypePickedListener listener;

    public static ShiftTypePickerBottomSheet newInstance(int shiftId, int offeredShiftTypeId) {
        ShiftTypePickerBottomSheet sheet = new ShiftTypePickerBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_SHIFT_ID, shiftId);
        args.putInt(ARG_OFFERED_SHIFT_TYPE_ID, offeredShiftTypeId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnShiftTypePickedListener(OnShiftTypePickedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            View bottomSheet = ((BottomSheetDialog) d)
                    .findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                BottomSheetBehavior.from(bottomSheet).setSkipCollapsed(true);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_shift_type_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            shiftId             = getArguments().getInt(ARG_SHIFT_ID);
            offeredShiftTypeId  = getArguments().getInt(ARG_OFFERED_SHIFT_TYPE_ID, -1);
        }

        ((TextView) view.findViewById(R.id.tv_picker_title)).setText(R.string.picker_shift_type_title);
        ((TextView) view.findViewById(R.id.tv_picker_subtitle)).setText(R.string.picker_shift_type_subtitle);

        LinearLayout  llCards    = view.findViewById(R.id.ll_shift_type_cards);
        ProgressBar   loader     = view.findViewById(R.id.picker_loader);
        MaterialButton btnCancel  = view.findViewById(R.id.btn_picker_cancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_picker_confirm);

        final int[] selectedShiftTypeId = {-1};

        btnCancel.setOnClickListener(v -> dismiss());
        btnConfirm.setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onPicked(selectedShiftTypeId[0]);
        });

        fetchAndBuildCards(llCards, loader, selectedShiftTypeId, btnConfirm, offeredShiftTypeId);
    }

    private void fetchAndBuildCards(LinearLayout llCards, ProgressBar loader,
                                    int[] selectedId, MaterialButton btnConfirm,
                                    int excludeShiftTypeId) {
        loader.setVisibility(View.VISIBLE);

        String token = "Bearer " + requireContext()
                .getSharedPreferences("AUTH", AppCompatActivity.MODE_PRIVATE)
                .getString("token", "");

        RetrofitClient.getClient(requireContext())
                .create(ApiService.class)
                .getShiftTypes(token)
                .enqueue(new Callback<ShiftTypesResponse>() {
                    @Override
                    public void onResponse(Call<ShiftTypesResponse> call,
                                           Response<ShiftTypesResponse> response) {
                        if (!isAdded()) return;
                        loader.setVisibility(View.GONE);
                        if (!response.isSuccessful() || response.body() == null) return;

                        List<ShiftType> workTypes = filterWorkTypes(response.body().getData(), excludeShiftTypeId);
                        buildCards(llCards, workTypes, selectedId, btnConfirm);
                    }

                    @Override
                    public void onFailure(Call<ShiftTypesResponse> call, Throwable t) {
                        if (isAdded()) loader.setVisibility(View.GONE);
                    }
                });
    }

    private List<ShiftType> filterWorkTypes(List<ShiftType> all, int excludeId) {
        List<ShiftType> result = new ArrayList<>();
        if (all == null) return result;
        for (ShiftType st : all) {
            // Exclude day-off types and the shift type the nurse is offering
            if (!DAYOFF_NAMES.contains(st.getName().toLowerCase().trim()) && st.getId() != excludeId) {
                result.add(st);
            }
        }
        return result;
    }

    private void buildCards(LinearLayout container, List<ShiftType> types,
                            int[] selectedId, MaterialButton btnConfirm) {
        container.removeAllViews();
        float dp = requireContext().getResources().getDisplayMetrics().density;

        for (ShiftType type : types) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setRadius(12 * dp);
            card.setCardElevation(2 * dp);
            card.setStrokeWidth((int) dp);
            card.setClickable(true);
            card.setFocusable(true);
            applyCardStyle(card, false);

            LinearLayout content = new LinearLayout(requireContext());
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding((int)(14*dp), (int)(12*dp), (int)(14*dp), (int)(12*dp));

            TextView tvName = new TextView(requireContext());
            tvName.setText(getLocalizedShiftTypeName(type.getName()));
            tvName.setTextSize(15);
            tvName.setTextColor(Color.parseColor("#2D2054"));
            tvName.setTypeface(null, Typeface.BOLD);

            TextView tvTime = new TextView(requireContext());
            String start = type.getStartTime() != null ? type.getStartTime().substring(0, 5) : "";
            String end   = type.getEndTime()   != null ? type.getEndTime().substring(0, 5)   : "";
            tvTime.setText(start + " – " + end);
            tvTime.setTextSize(13);
            tvTime.setTextColor(Color.parseColor("#64748B"));
            tvTime.setPadding(0, (int)(2*dp), 0, 0);

            content.addView(tvName);
            content.addView(tvTime);
            card.addView(content);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, (int)(8*dp));
            card.setLayoutParams(lp);

            final int typeId = type.getId();
            card.setOnClickListener(v -> {
                // Deselect all, then mark this one selected
                for (int i = 0; i < container.getChildCount(); i++) {
                    applyCardStyle((MaterialCardView) container.getChildAt(i), false);
                }
                applyCardStyle(card, true);
                selectedId[0] = typeId;
                btnConfirm.setEnabled(true);
                btnConfirm.setAlpha(1f);
            });

            container.addView(card);
        }
    }

    private void applyCardStyle(MaterialCardView card, boolean selected) {
        card.setCardBackgroundColor(selected
                ? Color.parseColor("#F8F5FF")
                : Color.WHITE);
        card.setStrokeColor(selected
                ? Color.parseColor("#7C3AED")
                : Color.parseColor("#E2E8F0"));
    }

    private String getLocalizedShiftTypeName(String name) {
        if (name == null) return "—";
        switch (name.toLowerCase().trim()) {
            case "morning":        return getString(R.string.shift_morning);
            case "afternoon":      return getString(R.string.shift_afternoon);
            case "night":          return getString(R.string.shift_night);
            case "dayoff":
            case "day off":        return getString(R.string.shift_dayoff);
            case "holidays":       return getString(R.string.shift_holidays);
            case "sick leave":     return getString(R.string.shift_sick_leave);
            case "parental leave": return getString(R.string.shift_parental_leave);
            default:               return name;
        }
    }
}
