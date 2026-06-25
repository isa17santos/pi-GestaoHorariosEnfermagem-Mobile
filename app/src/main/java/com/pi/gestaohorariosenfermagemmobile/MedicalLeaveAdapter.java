package com.pi.gestaohorariosenfermagemmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicalLeaveAdapter extends RecyclerView.Adapter<MedicalLeaveAdapter.ViewHolder> {

    private List<MedicalLeave> leaves;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onEdit(MedicalLeave leave);
        void onDelete(MedicalLeave leave);
    }

    public MedicalLeaveAdapter(List<MedicalLeave> leaves, OnActionClickListener listener) {
        this.leaves = leaves;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medical_leave, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalLeave leave = leaves.get(position);
        User u = leave.getUser();

        holder.tvName.setText(u != null ? u.getName() : "---");
        holder.tvAvatar.setText(u != null && u.getName() != null && !u.getName().isEmpty()
                ? String.valueOf(u.getName().charAt(0)).toUpperCase() : "?");

        holder.tvStartDate.setText(formatDate(leave.getStartDate()));
        holder.tvEndDate.setText(formatDate(leave.getEndDate()));
        holder.tvReason.setText(leave.getReason() != null && !leave.getReason().isEmpty() ? leave.getReason() : "-");

        // CÁLCULO DINÂMICO DO ESTADO
        String status = calculateStatus(leave.getStartDate(), leave.getEndDate());
        int bgColor, textColor;
        String statusText;

        switch (status) {
            case "future":
                bgColor = 0x1AFF00E5; // Rosa clarinho
                textColor = 0xFFFF00E5;
                statusText = holder.itemView.getContext().getString(R.string.status_future);
                break;
            case "ongoing":
                bgColor = 0x1A7C3AED; // Roxo clarinho
                textColor = 0xFF7C3AED;
                statusText = holder.itemView.getContext().getString(R.string.status_ongoing);
                break;
            default: // past
                bgColor = 0x1AF79B32; // Laranja clarinho
                textColor = 0xFFF79B32;
                statusText = holder.itemView.getContext().getString(R.string.status_past);
                break;
        }

        holder.tvStatusBadge.setText(statusText);
        holder.tvStatusBadge.setTextColor(textColor);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(20f);
        holder.tvStatusBadge.setBackground(gd);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(leave));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(leave));
    }

    private String calculateStatus(String startIso, String endIso) {
        if (startIso == null || endIso == null) return "past";
        try {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String start = startIso.substring(0, 10);
            String end = endIso.substring(0, 10);

            if (today.compareTo(start) < 0) return "future";
            if (today.compareTo(end) > 0) return "past";
            return "ongoing";
        } catch (Exception e) {
            return "past";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return "-";
        try {
            String year = dateStr.substring(0, 4);
            String month = dateStr.substring(5, 7);
            String day = dateStr.substring(8, 10);
            return day + "-" + month + "-" + year;
        } catch (Exception e) { return dateStr; }
    }

    @Override
    public int getItemCount() { return leaves != null ? leaves.size() : 0; }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvStartDate, tvEndDate, tvReason, tvStatusBadge;
        ImageButton btnEdit, btnDelete;
        ViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            tvName = v.findViewById(R.id.tv_name);
            tvStartDate = v.findViewById(R.id.tv_start_date);
            tvEndDate = v.findViewById(R.id.tv_end_date);
            tvReason = v.findViewById(R.id.tv_reason);
            tvStatusBadge = v.findViewById(R.id.tv_status_badge);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}