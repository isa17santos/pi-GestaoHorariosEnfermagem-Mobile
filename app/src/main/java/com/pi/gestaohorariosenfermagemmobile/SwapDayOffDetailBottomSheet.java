package com.pi.gestaohorariosenfermagemmobile;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SwapDayOffDetailBottomSheet extends BottomSheetDialogFragment {

    public interface OnConfirmListener {
        void onConfirm(int ownDayoffShiftId);
    }

    private static final String ARG_DATE              = "date";
    private static final String ARG_SHIFT_TYPE_NAME   = "shift_type_name";
    private static final String ARG_SHIFT_TIME_RANGE  = "shift_time_range";
    private static final String ARG_CANDIDATE_COUNT   = "candidate_count";
    private static final String ARG_OWN_DAYOFF_SHIFT_ID = "own_dayoff_shift_id";

    private String date;
    private String shiftTypeName;
    private String shiftTimeRange;
    private int candidateCount;
    private int ownDayoffShiftId;
    private OnConfirmListener confirmListener;

    public static SwapDayOffDetailBottomSheet newInstance(
            String date, String shiftTypeName, String shiftTimeRange,
            int candidateCount, int ownDayoffShiftId) {

        SwapDayOffDetailBottomSheet sheet = new SwapDayOffDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        args.putString(ARG_SHIFT_TYPE_NAME, shiftTypeName);
        args.putString(ARG_SHIFT_TIME_RANGE, shiftTimeRange);
        args.putInt(ARG_CANDIDATE_COUNT, candidateCount);
        args.putInt(ARG_OWN_DAYOFF_SHIFT_ID, ownDayoffShiftId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.confirmListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // Expand immediately, skip collapsed state
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
        return inflater.inflate(R.layout.bottom_sheet_swap_dayoff_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            date             = getArguments().getString(ARG_DATE, "");
            shiftTypeName    = getArguments().getString(ARG_SHIFT_TYPE_NAME, "");
            shiftTimeRange   = getArguments().getString(ARG_SHIFT_TIME_RANGE, "");
            candidateCount   = getArguments().getInt(ARG_CANDIDATE_COUNT, 0);
            ownDayoffShiftId = getArguments().getInt(ARG_OWN_DAYOFF_SHIFT_ID, -1);
        }

        // Header
        ((TextView) view.findViewById(R.id.tv_bs_date)).setText(date);
        ((TextView) view.findViewById(R.id.tv_bs_subtitle))
                .setText(getString(R.string.dayoff_detail_subtitle));

        // Shift-to-receive row
        ((TextView) view.findViewById(R.id.tv_label_shift_to_receive))
                .setText(getString(R.string.dayoff_detail_label_shift_to_receive));
        ((TextView) view.findViewById(R.id.tv_shift_type_name)).setText(shiftTypeName);
        ((TextView) view.findViewById(R.id.tv_shift_time_range)).setText(shiftTimeRange);

        // Candidate count row
        ((TextView) view.findViewById(R.id.tv_label_candidates))
                .setText(getString(R.string.dayoff_detail_label_candidates));
        ((TextView) view.findViewById(R.id.tv_candidate_count))
                .setText(getString(R.string.dayoff_detail_candidate_count, candidateCount));

        // "Ver candidatos" — dismiss and notify activity to advance to step 2
        Button btnSee = view.findViewById(R.id.btn_see_candidates);
        btnSee.setText(R.string.dayoff_detail_btn_see_candidates);
        btnSee.setOnClickListener(v -> {
            dismiss();
            if (confirmListener != null) confirmListener.onConfirm(ownDayoffShiftId);
        });

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
    }
}
