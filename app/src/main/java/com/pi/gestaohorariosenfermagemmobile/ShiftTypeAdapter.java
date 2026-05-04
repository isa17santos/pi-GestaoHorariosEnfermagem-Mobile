package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ShiftTypeAdapter extends RecyclerView.Adapter<ShiftTypeAdapter.ShiftTypeViewHolder> {

    private List<ShiftType> shiftTypes;
    private OnShiftTypeListener onEditListener;
    private OnShiftTypeListener onDeleteListener;

    // Listener for item actions
    public interface OnShiftTypeListener {
        void onShiftTypeClick(ShiftType shiftType);
    }

    // Initialize the adapter
    public ShiftTypeAdapter(List<ShiftType> shiftTypes, OnShiftTypeListener editListener, OnShiftTypeListener deleteListener) {
        this.shiftTypes = shiftTypes;
        this.onEditListener = editListener;
        this.onDeleteListener = deleteListener;
    }

    // Inflate the item layout
    @NonNull
    @Override
    public ShiftTypeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shift_type, parent, false);
        return new ShiftTypeViewHolder(view);
    }

    // Bind the shift type data to the item views
    @Override
    public void onBindViewHolder(@NonNull ShiftTypeViewHolder holder, int position) {
        ShiftType shiftType = shiftTypes.get(position);
        Context context = holder.itemView.getContext();

        String localizedName = getLocalizedName(context, shiftType.getName());
        holder.tvName.setText(localizedName);

        if (localizedName != null && !localizedName.isEmpty()) {
            holder.tvAvatar.setText(localizedName.substring(0, 1).toUpperCase());
        } else {
            holder.tvAvatar.setText("?");
        }

        // Color display
        String colorHex = shiftType.getColor() != null ? shiftType.getColor().trim() : "#A78BFA";
        holder.tvColor.setText(colorHex);

        try {
            int parsedColor = Color.parseColor(colorHex);
            holder.vColorDot.setBackgroundTintList(ColorStateList.valueOf(parsedColor));
        } catch (IllegalArgumentException e) {
            holder.vColorDot.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A78BFA")));
        }

        // Schedule
        holder.tvSchedule.setText(shiftType.getStartTime() + " - " + shiftType.getEndTime());

        // Min nurses
        holder.tvMinNurses.setText(String.valueOf(shiftType.getMinNurses()));

        holder.btnEdit.setOnClickListener(v -> onEditListener.onShiftTypeClick(shiftType));
        holder.btnDelete.setOnClickListener(v -> onDeleteListener.onShiftTypeClick(shiftType));
    }

    // Return the item count
    @Override
    public int getItemCount() { return shiftTypes.size(); }

    // Get the localized shift type name
    private String getLocalizedName(Context context, String apiName) {
        if (apiName == null || apiName.isEmpty()) return apiName;

        String language = context.getResources().getConfiguration().getLocales().get(0).getLanguage();
        boolean isPt = "pt".equals(language);

        switch (apiName) {
            case "morning":
                return isPt ? "Manhã" : "Morning";
            case "afternoon":
                return isPt ? "Tarde" : "Afternoon";
            case "night":
                return isPt ? "Noite" : "Night";
            case "dayOff":
                return isPt ? "Descanso" : "Day Off";
            case "sick leave":
                return isPt ? "Baixa Médica" : "Sick Leave";
            case "parental leave":
                return isPt ? "Licença Parental" : "Parental Leave";
            case "holidays":
                return isPt ? "Férias" : "Holidays";
            default:
                return apiName;
        }
    }

    // Cache the item views
    static class ShiftTypeViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvColor, tvSchedule, tvMinNurses;
        View vColorDot;
        ImageButton btnEdit, btnDelete;

        ShiftTypeViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_avatar);
            tvName = v.findViewById(R.id.tv_name);
            tvColor = v.findViewById(R.id.tv_color);
            vColorDot = v.findViewById(R.id.v_color_dot);
            tvSchedule = v.findViewById(R.id.tv_schedule);
            tvMinNurses = v.findViewById(R.id.tv_min_nurses);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}

