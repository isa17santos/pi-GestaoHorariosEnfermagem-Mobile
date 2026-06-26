package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SwapCandidateAdapter extends RecyclerView.Adapter<SwapCandidateAdapter.ViewHolder> {

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int totalCount);
    }

    private static final List<String> DAYOFF_NAMES = Arrays.asList(
            "dayoff", "day off", "folga", "holidays", "férias",
            "sick leave", "baixa médica", "parental leave", "licença parental",
            "baixa", "licença"
    );

    private List<Shift> candidates = new ArrayList<>();
    private Shift ownDayoff = null; // nurse's selected day-off (dayoff mode only)
    // For dayoff mode: map from candidate shift id → target work shift on the dayoff date
    private Map<Integer, Shift> targetWorkShifts = new HashMap<>();
    private final Set<Integer> selectedShiftIds = new HashSet<>();
    // Validation warnings per candidate shift id
    private Map<Integer, List<String>> warningsMap = new HashMap<>();
    private final Context context;
    private OnSelectionChangedListener selectionListener;
    private final boolean isDayoffMode;

    public SwapCandidateAdapter(Context context, boolean isDayoffMode) {
        this.context = context;
        this.isDayoffMode = isDayoffMode;
    }

    public void setData(List<Shift> candidates) {
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        selectedShiftIds.clear();
        notifyDataSetChanged();
    }

    public void setOwnDayoff(Shift dayoff) {
        this.ownDayoff = dayoff;
        notifyDataSetChanged();
    }

    public void setTargetWorkShifts(Map<Integer, Shift> map) {
        this.targetWorkShifts = map;
        notifyDataSetChanged();
    }

    public void setWarnings(Map<Integer, List<String>> warnings) {
        this.warningsMap = warnings != null ? warnings : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public Set<Integer> getSelectedShiftIds() { return selectedShiftIds; }

    public void selectAll(boolean select) {
        selectedShiftIds.clear();
        if (select) {
            for (Shift s : candidates) {
                if (isSelectable(s)) selectedShiftIds.add(s.getId());
            }
        }
        notifyDataSetChanged();
        fireSelectionChanged();
    }

    public int getSelectableCount() {
        int count = 0;
        for (Shift s : candidates) { if (isSelectable(s)) count++; }
        return count;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_swap_candidate, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Shift shift = candidates.get(position);
        boolean selectable = isSelectable(shift);

        // Avatar: first letter of nurse name
        String name = getUserName(shift);
        h.tvAvatar.setText(name.isEmpty() || name.equals("—") ? "?" :
                String.valueOf(name.charAt(0)).toUpperCase());

        h.tvName.setText(name);

        // In dayoff mode show the nurse's selected day-off as the primary shift info.
        // In shift mode show the candidate's shift on the offered date.
        if (isDayoffMode && ownDayoff != null) {
            h.tvShiftInfo.setText(buildShiftInfo(ownDayoff));
            applyBorderColor(h.viewOfferedBorder, ownDayoff.getShiftType());
        } else {
            h.tvShiftInfo.setText(buildShiftInfo(shift));
            applyBorderColor(h.viewOfferedBorder, shift.getShiftType());
        }

        // Target shift row (dayoff mode only)
        if (isDayoffMode) {
            int userId = getUserId(shift);
            Shift targetWork = findTargetWorkShiftForUser(userId);

            if (targetWork != null) {
                h.llTargetRow.setVisibility(View.VISIBLE);
                h.tvAlsoDayoff.setVisibility(View.GONE);
                h.tvTargetLabel.setText(context.getString(R.string.swap_label_target_work));
                h.tvTargetShiftInfo.setText(buildShiftInfo(targetWork));
                applyBorderColor(h.viewTargetBorder, targetWork.getShiftType());
            } else {
                h.llTargetRow.setVisibility(View.GONE);
                h.tvAlsoDayoff.setVisibility(View.VISIBLE);
                h.tvAlsoDayoff.setText(context.getString(R.string.swap_also_dayoff));
            }
        } else {
            h.llTargetRow.setVisibility(View.GONE);
            h.tvAlsoDayoff.setVisibility(View.GONE);
        }

        // Validation warning
        List<String> warnings = warningsMap.get(shift.getId());
        if (warnings != null && !warnings.isEmpty()) {
            h.llWarning.setVisibility(View.VISIBLE);
            h.tvWarning.setText(warnings.get(0));
        } else {
            h.llWarning.setVisibility(View.GONE);
        }

        // Selection state
        boolean isSelected = selectedShiftIds.contains(shift.getId());
        h.cbSelect.setChecked(isSelected);
        h.cbSelect.setVisibility(selectable ? View.VISIBLE : View.GONE);
        h.itemView.setAlpha(selectable ? 1f : 0.6f);

        if (selectable) {
            h.itemView.setOnClickListener(v -> {
                if (selectedShiftIds.contains(shift.getId())) {
                    selectedShiftIds.remove(shift.getId());
                } else {
                    selectedShiftIds.add(shift.getId());
                }
                notifyItemChanged(h.getAdapterPosition());
                fireSelectionChanged();
            });
        } else {
            h.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() { return candidates.size(); }

    // In dayoff mode, a candidate is not selectable if they have no work shift on the dayoff date
    private boolean isSelectable(Shift shift) {
        if (!isDayoffMode) return true;
        return findTargetWorkShiftForUser(getUserId(shift)) != null;
    }

    private Shift findTargetWorkShiftForUser(int userId) {
        for (Shift s : targetWorkShifts.values()) {
            if (s.getUsers() != null) {
                for (User u : s.getUsers()) {
                    if (u.getId() == userId) return s;
                }
            }
        }
        return null;
    }

    /** Sets the color of a left-border View from the shift type's API color field. */
    private void applyBorderColor(View borderView, ShiftType shiftType) {
        if (shiftType == null || shiftType.getColor() == null) return;
        try {
            borderView.setBackgroundColor(Color.parseColor(shiftType.getColor()));
        } catch (IllegalArgumentException ignored) {
            // leave default purple if color string is invalid
        }
    }

    private String getUserName(Shift shift) {
        if (shift.getUsers() == null || shift.getUsers().isEmpty()) return "—";
        return shift.getUsers().get(0).getName();
    }

    private int getUserId(Shift shift) {
        if (shift.getUsers() == null || shift.getUsers().isEmpty()) return -1;
        return shift.getUsers().get(0).getId();
    }

    private String buildShiftInfo(Shift shift) {
        String date = formatDate(shift.getDate());
        if (shift.getShiftType() == null) return date;
        String type = getLocalizedName(shift.getShiftType().getName());
        String start = shift.getShiftType().getStartTime();
        String end = shift.getShiftType().getEndTime();
        boolean isAllDay = start != null && end != null && start.startsWith("00:00") && end.startsWith("00:00");
        if (isAllDay) return date + " · " + type + " · " + context.getString(R.string.all_day_label);
        return date + " · " + type + " · " + (start != null ? start.substring(0,5) : "") +
                " - " + (end != null ? end.substring(0,5) : "");
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            if (d == null) return dateStr;
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd/MM", locale);
            String f = out.format(d);
            return f.substring(0,1).toUpperCase(locale) + f.substring(1);
        } catch (ParseException e) { return dateStr; }
    }

    private String getLocalizedName(String name) {
        if (name == null) return "—";
        switch (name.toLowerCase().trim()) {
            case "morning":        return context.getString(R.string.shift_morning);
            case "afternoon":      return context.getString(R.string.shift_afternoon);
            case "night":          return context.getString(R.string.shift_night);
            case "dayoff": case "day off": return context.getString(R.string.shift_dayoff);
            case "holidays":       return context.getString(R.string.shift_holidays);
            case "sick leave":     return context.getString(R.string.shift_sick_leave);
            case "parental leave": return context.getString(R.string.shift_parental_leave);
            default:               return name;
        }
    }

    private void fireSelectionChanged() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedShiftIds.size(), getSelectableCount());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvShiftInfo, tvTargetLabel, tvTargetShiftInfo, tvAlsoDayoff, tvWarning;
        View viewOfferedBorder, viewTargetBorder;
        LinearLayout llTargetRow, llWarning;
        MaterialCheckBox cbSelect;

        ViewHolder(View v) {
            super(v);
            tvAvatar          = v.findViewById(R.id.tv_avatar);
            tvName            = v.findViewById(R.id.tv_candidate_name);
            tvShiftInfo       = v.findViewById(R.id.tv_candidate_shift_info);
            viewOfferedBorder = v.findViewById(R.id.view_offered_border);
            llTargetRow       = v.findViewById(R.id.ll_target_row);
            tvTargetLabel     = v.findViewById(R.id.tv_target_label);
            viewTargetBorder  = v.findViewById(R.id.view_target_border);
            tvTargetShiftInfo = v.findViewById(R.id.tv_target_shift_info);
            tvAlsoDayoff      = v.findViewById(R.id.tv_also_dayoff);
            llWarning         = v.findViewById(R.id.ll_warning);
            tvWarning         = v.findViewById(R.id.tv_warning_text);
            cbSelect          = v.findViewById(R.id.cb_select);
        }
    }
}
