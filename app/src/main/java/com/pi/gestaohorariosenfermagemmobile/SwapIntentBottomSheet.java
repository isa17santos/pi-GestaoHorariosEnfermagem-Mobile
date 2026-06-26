package com.pi.gestaohorariosenfermagemmobile;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SwapIntentBottomSheet extends BottomSheetDialogFragment {

    public interface OnIntentSelectedListener {
        void onShiftForShift(int shiftId, int targetShiftTypeId);
        void onShiftForDayoff(int shiftId, int shiftTypeId);
    }

    private static final String ARG_SHIFT_ID      = "shift_id";
    private static final String ARG_SHIFT_TYPE_ID = "shift_type_id";
    private static final String ARG_SHIFT_INFO    = "shift_info";

    private int shiftId;
    private int shiftTypeId;
    private String shiftInfo;
    private OnIntentSelectedListener listener;

    public static SwapIntentBottomSheet newInstance(int shiftId, int shiftTypeId, String shiftInfo) {
        SwapIntentBottomSheet sheet = new SwapIntentBottomSheet();
        Bundle args = new Bundle();
        args.putInt(ARG_SHIFT_ID, shiftId);
        args.putInt(ARG_SHIFT_TYPE_ID, shiftTypeId);
        args.putString(ARG_SHIFT_INFO, shiftInfo);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnIntentSelectedListener(OnIntentSelectedListener listener) {
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
        return inflater.inflate(R.layout.bottom_sheet_swap_intent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            shiftId     = getArguments().getInt(ARG_SHIFT_ID);
            shiftTypeId = getArguments().getInt(ARG_SHIFT_TYPE_ID);
            shiftInfo   = getArguments().getString(ARG_SHIFT_INFO, "");
        }

        ((TextView) view.findViewById(R.id.tv_bs_title)).setText(R.string.swap_intent_title);
        ((TextView) view.findViewById(R.id.tv_bs_shift_info)).setText(shiftInfo);
        ((TextView) view.findViewById(R.id.tv_shift_for_shift_title)).setText(R.string.swap_mode_shift_title);
        ((TextView) view.findViewById(R.id.tv_shift_for_shift_desc)).setText(R.string.swap_mode_shift_desc);
        ((TextView) view.findViewById(R.id.tv_shift_for_dayoff_title)).setText(R.string.swap_mode_dayoff_title);
        ((TextView) view.findViewById(R.id.tv_shift_for_dayoff_desc)).setText(R.string.swap_mode_dayoff_desc);

        view.findViewById(R.id.card_shift_for_shift).setOnClickListener(v -> {
            // Dismiss this sheet and open the shift-type picker as a second bottom sheet
            dismiss();
            showShiftTypePicker();
        });

        view.findViewById(R.id.card_shift_for_dayoff).setOnClickListener(v -> {
            dismiss();
            if (listener != null) listener.onShiftForDayoff(shiftId, shiftTypeId);
        });
    }

    private void showShiftTypePicker() {
        if (getActivity() == null) return;

        ShiftTypePickerBottomSheet picker = ShiftTypePickerBottomSheet.newInstance(shiftId, shiftTypeId);
        picker.setOnShiftTypePickedListener(selectedShiftTypeId -> {
            if (listener != null) listener.onShiftForShift(shiftId, selectedShiftTypeId);
        });
        picker.show(getActivity().getSupportFragmentManager(), "shift_type_picker");
    }
}
