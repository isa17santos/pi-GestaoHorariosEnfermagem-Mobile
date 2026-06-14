package com.pi.gestaohorariosenfermagemmobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private final List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onReadClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification n = notifications.get(position);
        holder.tvTitle.setText(n.getSubject());
        holder.tvDesc.setText(n.getBody());

        // 1. Formatação de Data Bilingue
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(n.getCreatedAt());

            String currentLang = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
            java.text.SimpleDateFormat outputFormat;
            String formattedDate;

            if (currentLang.contains("pt")) {
                // Formato PT: 13 de junho de 2026 às 10:14
                outputFormat = new java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy 'às' HH:mm", new java.util.Locale("pt", "PT"));
            } else {
                // Formato EN: June 13, 2026 at 10:14 AM
                outputFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", java.util.Locale.US);
            }
            formattedDate = outputFormat.format(date);
            String prefix = holder.itemView.getContext().getString(R.string.notifications_received_at);
            holder.tvTime.setText(prefix + ": " + formattedDate);
        } catch (Exception e) {
            holder.tvTime.setText(n.getCreatedAt());
        }

        // 2. Lógica de Design (Lida vs Não Lida)
        boolean isUnread = !n.isRead();

        // Visibilidade dos elementos de destaque
        holder.indicator.setVisibility(isUnread ? View.VISIBLE : View.GONE);
        holder.dot.setVisibility(isUnread ? View.VISIBLE : View.GONE);
        holder.btnReadContainer.setVisibility(isUnread ? View.VISIBLE : View.GONE);

        if (isUnread) {
            // Estilo Não Lida (Roxo)
            holder.cvIconBg.setCardBackgroundColor(android.graphics.Color.parseColor("#F3E8FF"));
            holder.ivEnvelope.setColorFilter(android.graphics.Color.parseColor("#7C3AED"));

            holder.tvTitle.setAlpha(1.0f);
            holder.tvDesc.setAlpha(1.0f);
            holder.tvTime.setAlpha(1.0f);
        } else {
            // Estilo Lida (Cinza e Fundo Branco Total)
            holder.cvIconBg.setCardBackgroundColor(android.graphics.Color.parseColor("#F3F4F6"));
            holder.ivEnvelope.setColorFilter(android.graphics.Color.parseColor("#9CA3AF"));

            // Esmaecer apenas o texto, mantendo o card branco (Alpha 1.0 no itemView)
            holder.tvTitle.setAlpha(0.7f);
            holder.tvDesc.setAlpha(0.5f);
            holder.tvTime.setAlpha(0.5f);
        }

        // Garante que o fundo do card é branco e não tem transparência
        holder.itemView.setAlpha(1.0f);

        holder.btnRead.setOnClickListener(v -> listener.onReadClick(n));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc, tvTime;
        View indicator, dot;
        ImageButton btnRead;
        MaterialCardView cvIconBg, btnReadContainer;
        ImageView ivEnvelope;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_notif_title);
            tvDesc = v.findViewById(R.id.tv_notif_desc);
            tvTime = v.findViewById(R.id.tv_notif_time);
            indicator = v.findViewById(R.id.v_notif_indicator);
            dot = v.findViewById(R.id.v_notif_dot);
            btnRead = v.findViewById(R.id.btn_read_single);
            cvIconBg = v.findViewById(R.id.cv_icon_bg);
            btnReadContainer = v.findViewById(R.id.btn_read_container);
            ivEnvelope = v.findViewById(R.id.iv_notif_envelope);
        }
    }

    // Método auxiliar para o viewholder encontrar o container
    private View findViewById(ViewHolder holder, int id) {
        return holder.itemView.findViewById(id);
    }
}