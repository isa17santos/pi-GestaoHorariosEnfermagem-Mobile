package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NurseHoursAdapter extends RecyclerView.Adapter<NurseHoursAdapter.ViewHolder> {

    private final List<NurseHours> items;
    private final double avg;
    private final double threshold;

    public NurseHoursAdapter(List<NurseHours> items, double avg) {
        this.items = items;
        this.avg = avg;
        this.threshold = avg * 0.15;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nurse_hours, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NurseHours item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(item.getName() != null ? item.getName() : "—");

        double hours = item.getHours() != null ? item.getHours() : 0;
        int hoursInt = (int) Math.round(hours);
        holder.tvHours.setText(hoursInt + "h");

        // Cor consoante desvio em relação à média
        int nameColor, hoursColor;
        if (hours > avg + threshold) {
            // Acima do limiar — laranja
            nameColor = ctx.getColor(R.color.quality_medium);
            hoursColor = ctx.getColor(R.color.quality_medium);
        } else if (hours < avg - threshold) {
            // Abaixo do limiar — azul/roxo (primary_strong da paleta)
            nameColor = ctx.getColor(R.color.primary_strong);
            hoursColor = ctx.getColor(R.color.primary_strong);
        } else {
            nameColor = ctx.getColor(R.color.text_primary);
            hoursColor = ctx.getColor(R.color.text_primary);
        }

        holder.tvName.setTextColor(nameColor);
        holder.tvHours.setTextColor(hoursColor);

        // Separador: linha fina entre items (excepto no último)
        holder.divider.setVisibility(position < items.size() - 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvHours;
        View divider;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_nurse_name);
            tvHours = v.findViewById(R.id.tv_nurse_hours);
            divider = v.findViewById(R.id.divider);
        }
    }
}
