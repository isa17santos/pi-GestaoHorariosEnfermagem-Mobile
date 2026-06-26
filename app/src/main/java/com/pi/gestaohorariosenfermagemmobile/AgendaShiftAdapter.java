package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AgendaShiftAdapter extends RecyclerView.Adapter<AgendaShiftAdapter.ViewHolder> {

    public interface OnDayoffClickListener {
        void onDayoffClick(Shift shift);
    }

    private static final List<String> DAYOFF_NAMES = Arrays.asList(
            "dayoff", "day off", "folga", "holidays", "férias",
            "sick leave", "baixa médica", "parental leave", "licença parental",
            "baixa", "licença"
    );

    // date (yyyy-MM-dd) → (shiftTypeName → eligible candidate count)
    private Map<String, Map<String, Integer>> breakdownByDate = new HashMap<>();

    private final Context context;
    private final List<Shift> shifts;
    private OnDayoffClickListener listener;
    private final String today;

    public AgendaShiftAdapter(Context context, List<Shift> shifts) {
        this.context = context;
        this.shifts = shifts;
        this.today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    public void setOnDayoffClickListener(OnDayoffClickListener l) {
        this.listener = l;
    }

    /** Replaces the entire breakdown map; keyed by each day-off's date string. */
    public void setBreakdownByDate(Map<String, Map<String, Integer>> breakdown) {
        this.breakdownByDate = breakdown != null ? breakdown : new HashMap<>();
        notifyDataSetChanged();
    }

    private boolean isDayoff(Shift s) {
        if (s.getDate() != null && s.getDate().compareTo(today) <= 0) return false; // past or today
        return s.getShiftType() != null &&
                DAYOFF_NAMES.contains(s.getShiftType().getName().toLowerCase().trim());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_agenda_shift, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Shift shift = shifts.get(position);
        boolean dayoff = isDayoff(shift);

        // ── Card styling ──────────────────────────────────────────────────────
        if (dayoff) {
            h.card.setCardBackgroundColor(Color.parseColor("#F0EBFF"));
            h.card.setStrokeColor(Color.parseColor("#C4B5FD"));
            h.card.setStrokeWidth(dpToPx(1.5f));
            h.card.setRippleColor(ColorStateList.valueOf(Color.parseColor("#337C3AED")));
            h.card.setClickable(true);
            h.card.setFocusable(true);
            h.card.setOnClickListener(v -> { if (listener != null) listener.onDayoffClick(shift); });
        } else {
            h.card.setCardBackgroundColor(Color.WHITE);
            h.card.setStrokeColor(Color.parseColor("#E2E8F0"));
            h.card.setStrokeWidth(dpToPx(1f));
            h.card.setClickable(false);
            h.card.setFocusable(false);
            h.card.setOnClickListener(null);
        }

        // ── Color bar ─────────────────────────────────────────────────────────
        h.colorBar.setBackgroundColor(Color.parseColor(resolveBarColor(shift, dayoff)));

        // ── Day label ─────────────────────────────────────────────────────────
        h.tvDayLabel.setText(formatDayAbbrev(shift.getDate()));
        h.tvDayLabel.setTextColor(dayoff
                ? Color.parseColor("#7C3AED") : Color.parseColor("#94A3B8"));

        // ── Full date ─────────────────────────────────────────────────────────
        h.tvDate.setText(formatDateFull(shift.getDate()));
        h.tvDate.setTextColor(dayoff
                ? Color.parseColor("#2D2054") : Color.parseColor("#1E293B"));

        // ── Shift info line ───────────────────────────────────────────────────
        h.tvShiftInfo.setText(buildShiftInfo(shift));
        h.tvShiftInfo.setTextColor(dayoff
                ? Color.parseColor("#6D28D9") : Color.parseColor("#64748B"));

        // ── Badge ─────────────────────────────────────────────────────────────
        if (shift.getShiftType() != null) {
            h.tvBadge.setVisibility(View.VISIBLE);
            String badgeText = getLocalizedName(shift.getShiftType().getName())
                    .toUpperCase(Locale.getDefault());
            h.tvBadge.setText(badgeText);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dpToPx(12));
            if (dayoff) {
                bg.setColor(Color.parseColor("#7C3AED"));
            } else {
                bg.setColor(Color.parseColor(resolveShiftColor(shift)));
            }
            h.tvBadge.setBackground(bg);
            h.tvBadge.setTextColor(Color.WHITE);
        } else {
            h.tvBadge.setVisibility(View.GONE);
        }

        // ── Chevron ───────────────────────────────────────────────────────────
        h.ivChevron.setVisibility(dayoff ? View.VISIBLE : View.GONE);

        // ── Candidate breakdown (dayoff cards only) ───────────────────────────
        Map<String, Integer> breakdown = dayoff ? breakdownByDate.get(shift.getDate()) : null;
        if (breakdown != null && !breakdown.isEmpty()) {
            h.llBreakdown.setVisibility(View.VISIBLE);
            h.tvBreakdownLabel.setText(context.getString(R.string.swap_breakdown_label));
            h.llBreakdownRows.removeAllViews();

            for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = dpToPx(4);
                row.setLayoutParams(rowLp);

                TextView tvType = new TextView(context);
                tvType.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvType.setText(entry.getKey());
                tvType.setTextSize(12);
                tvType.setTextColor(Color.parseColor("#5B21B6"));

                TextView tvCount = new TextView(context);
                tvCount.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvCount.setText(context.getString(
                        R.string.swap_breakdown_count, entry.getValue()));
                tvCount.setTextSize(12);
                tvCount.setTypeface(null, android.graphics.Typeface.BOLD);
                tvCount.setTextColor(Color.parseColor("#7C3AED"));

                row.addView(tvType);
                row.addView(tvCount);
                h.llBreakdownRows.addView(row);
            }
        } else {
            h.llBreakdown.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return shifts.size(); }

    private String resolveBarColor(Shift shift, boolean dayoff) {
        if (dayoff) return "#7C3AED";
        return resolveShiftColor(shift);
    }

    private String resolveShiftColor(Shift shift) {
        if (shift.getShiftType() != null && shift.getShiftType().getColor() != null) {
            try {
                Color.parseColor(shift.getShiftType().getColor());
                return shift.getShiftType().getColor();
            } catch (Exception ignored) {}
        }
        return "#94A3B8";
    }

    private String formatDayAbbrev(String dateStr) {
        if (dateStr == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            if (d == null) return "";
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            return new SimpleDateFormat("EEE", locale).format(d)
                    .toUpperCase(locale).replace(".", "");
        } catch (ParseException e) { return ""; }
    }

    private String formatDateFull(String dateStr) {
        if (dateStr == null) return "";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
            if (d == null) return dateStr;
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            return new SimpleDateFormat("dd 'de' MMMM yyyy", locale).format(d);
        } catch (ParseException e) { return dateStr; }
    }

    private String buildShiftInfo(Shift shift) {
        if (shift.getShiftType() == null) return "—";
        String name = getLocalizedName(shift.getShiftType().getName());
        String start = shift.getShiftType().getStartTime() != null
                ? shift.getShiftType().getStartTime().substring(0, 5) : "";
        String end = shift.getShiftType().getEndTime() != null
                ? shift.getShiftType().getEndTime().substring(0, 5) : "";
        boolean allDay = "00:00".equals(start) && "00:00".equals(end);
        return allDay
                ? name + " · " + context.getString(R.string.all_day_label)
                : name + " · " + start + " – " + end;
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

    private int dpToPx(float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        View colorBar;
        TextView tvDayLabel, tvDate, tvShiftInfo, tvBadge, tvBreakdownLabel;
        ImageView ivChevron;
        LinearLayout llBreakdown, llBreakdownRows;

        ViewHolder(View v) {
            super(v);
            card = (MaterialCardView) v;
            colorBar = v.findViewById(R.id.view_color_bar);
            tvDayLabel = v.findViewById(R.id.tv_day_label);
            tvDate = v.findViewById(R.id.tv_date);
            tvShiftInfo = v.findViewById(R.id.tv_shift_info);
            tvBadge = v.findViewById(R.id.tv_badge);
            ivChevron = v.findViewById(R.id.iv_chevron);
            llBreakdown = v.findViewById(R.id.ll_breakdown);
            tvBreakdownLabel = v.findViewById(R.id.tv_breakdown_label);
            llBreakdownRows = v.findViewById(R.id.ll_breakdown_rows);
        }
    }
}
