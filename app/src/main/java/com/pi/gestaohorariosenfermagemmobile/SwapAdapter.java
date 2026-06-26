package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SwapAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_SWAP   = 1;

    public interface OnSwapActionListener {
        void onAccept(SwapRequest swap);
        void onReject(SwapRequest swap);
        void onCancel(SwapRequest swap);
    }

    // Lista mista: items são String (cabeçalho de secção) ou SwapRequest (card)
    private List<Object> items = new ArrayList<>();
    private final String currentUserName;
    private final OnSwapActionListener listener;
    private final Context context;

    public SwapAdapter(String currentUserName, OnSwapActionListener listener, Context context) {
        this.currentUserName = currentUserName;
        this.listener = listener;
        this.context = context;
    }

    public void setData(List<Object> data) {
        items = data;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_SWAP;
    }

    @Override
    public int getItemCount() { return items.size(); }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View v = inflater.inflate(R.layout.item_swap_header, parent, false);
            return new HeaderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_swap, parent, false);
        return new SwapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeader.setText((String) items.get(position));
        } else {
            SwapRequest swap = (SwapRequest) items.get(position);
            SwapViewHolder h = (SwapViewHolder) holder;
            boolean isSent = isRequester(swap);
            bindHeader(h, swap, isSent);
            bindShiftGrid(h, swap);
            bindActionButtons(h, swap, isSent);
        }
    }

    // Verifica se o utilizador autenticado é o requester (pedido enviado)
    private boolean isRequester(SwapRequest swap) {
        if (swap.getParticipants() == null) return false;
        for (SwapRequest.SwapParticipant p : swap.getParticipants()) {
            if ("requester".equals(p.getRole()) && currentUserName.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    private String getCounterpartyName(SwapRequest swap, boolean isSent) {
        String targetRole = isSent ? "target" : "requester";
        if (swap.getParticipants() == null) return "—";
        for (SwapRequest.SwapParticipant p : swap.getParticipants()) {
            if (targetRole.equals(p.getRole())) return p.getName();
        }
        return "—";
    }

    private void bindHeader(SwapViewHolder h, SwapRequest swap, boolean isSent) {
        h.tvCounterparty.setText(context.getString(R.string.swap_with, getCounterpartyName(swap, isSent)));
        applyStatusBadge(h.tvStatusBadge, swap.getStatus());
        h.tvDate.setText(formatCreatedAt(swap.getCreatedAt()));
    }

    // Aplica cor de fundo e texto do badge conforme o estado
    private void applyStatusBadge(TextView badge, String status) {
        String label;
        int bgColor;
        int textColor;

        switch (status != null ? status : "") {
            case "pending":
                label = context.getString(R.string.filter_pending).toUpperCase();
                bgColor = Color.parseColor("#FFF3CD");
                textColor = Color.parseColor("#856404");
                break;
            case "accepted":
                label = context.getString(R.string.filter_accepted).toUpperCase();
                bgColor = Color.parseColor("#D4EDDA");
                textColor = Color.parseColor("#155724");
                break;
            case "rejected":
                label = context.getString(R.string.filter_rejected).toUpperCase();
                bgColor = Color.parseColor("#F8D7DA");
                textColor = Color.parseColor("#721C24");
                break;
            case "cancelled":
                label = context.getString(R.string.filter_cancelled).toUpperCase();
                bgColor = Color.parseColor("#E2E3E5");
                textColor = Color.parseColor("#383D41");
                break;
            default:
                label = status != null ? status.toUpperCase() : "—";
                bgColor = Color.parseColor("#E2E3E5");
                textColor = Color.parseColor("#383D41");
        }

        badge.setText(label);
        badge.setBackground(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_badge_rounded));
        badge.getBackground().setTint(bgColor);
        badge.setTextColor(textColor);
    }

    private void bindShiftGrid(SwapViewHolder h, SwapRequest swap) {
        List<SwapRequest.SwapShift> offered = swap.getOfferedShifts();
        List<SwapRequest.SwapShift> requested = swap.getRequestedShifts();

        // Índice 0 das oferecidas: "TURNO OFERECIDO"
        if (offered != null && !offered.isEmpty()) {
            bindShiftSubCard(h.cardOffered0, h.tvLabelOffered0, h.tvDateOffered0, h.tvTypeOffered0, h.tvOwnerOffered0,
                    context.getString(R.string.label_offered_shift), offered.get(0));
        }

        // Índice 0 das pretendidas: "FOLGA PRETENDIDA"
        if (requested != null && !requested.isEmpty()) {
            bindShiftSubCard(h.cardRequested0, h.tvLabelRequested0, h.tvDateRequested0, h.tvTypeRequested0, h.tvOwnerRequested0,
                    context.getString(R.string.label_requested_dayoff), requested.get(0));
        }

        // Troca dupla: mostrar segundo par de cards em cada coluna
        boolean hasSecondRow = (offered != null && offered.size() > 1) || (requested != null && requested.size() > 1);
        h.cardOffered1.setVisibility(hasSecondRow ? View.VISIBLE : View.GONE);
        h.cardRequested1.setVisibility(hasSecondRow ? View.VISIBLE : View.GONE);

        if (offered != null && offered.size() > 1) {
            bindShiftSubCard(h.cardOffered1, h.tvLabelOffered1, h.tvDateOffered1, h.tvTypeOffered1, h.tvOwnerOffered1,
                    context.getString(R.string.label_offered_dayoff), offered.get(1));
        }
        if (requested != null && requested.size() > 1) {
            bindShiftSubCard(h.cardRequested1, h.tvLabelRequested1, h.tvDateRequested1, h.tvTypeRequested1, h.tvOwnerRequested1,
                    context.getString(R.string.label_requested_shift), requested.get(1));
        }
    }

    private void bindShiftSubCard(MaterialCardView card, TextView tvLabel, TextView tvDate,
                                  TextView tvType, TextView tvOwner,
                                  String label, SwapRequest.SwapShift shift) {
        card.setVisibility(View.VISIBLE);
        tvLabel.setText(label);
        tvDate.setText(formatShiftDate(shift.getDate()));
        String rawName = (shift.getShiftType() != null) ? shift.getShiftType().getName() : "—";
        tvType.setText(getLocalizedShiftTypeName(rawName));
        String ownerName = (shift.getOwner() != null) ? shift.getOwner().getName() : "—";
        tvOwner.setText(context.getString(R.string.swap_by, ownerName));
    }

    // Botões de ação: sent+pending → Cancelar; received+pending → Aceitar + Rejeitar
    private void bindActionButtons(SwapViewHolder h, SwapRequest swap, boolean isSent) {
        boolean isPending = "pending".equals(swap.getStatus());

        h.btnCancel.setVisibility(isSent && isPending ? View.VISIBLE : View.GONE);
        h.btnReject.setVisibility(!isSent && isPending ? View.VISIBLE : View.GONE);
        h.btnAccept.setVisibility(!isSent && isPending ? View.VISIBLE : View.GONE);
        h.llActions.setVisibility(isPending ? View.VISIBLE : View.GONE);

        h.btnCancel.setOnClickListener(v -> listener.onCancel(swap));
        h.btnReject.setOnClickListener(v -> listener.onReject(swap));
        h.btnAccept.setOnClickListener(v -> listener.onAccept(swap));
    }

    // Mapeia nomes de tipo de turno crus da API para nomes localizados em português
    private String getLocalizedShiftTypeName(String name) {
        if (name == null) return "—";
        switch (name.toLowerCase().trim()) {
            case "morning":        return "Manhã";
            case "afternoon":      return "Tarde";
            case "night":          return "Noite";
            case "dayoff":
            case "day off":        return "Folga";
            case "holidays":       return "Férias";
            case "sick leave":     return "Baixa Médica";
            case "parental leave": return "Licença Parental";
            default:               return name;
        }
    }

    // Formata ISO 8601 com microsegundos (ex: "2026-06-12T13:59:36.000000Z") → "Sex, 12/06/2026"
    private String formatCreatedAt(String isoDate) {
        if (isoDate == null) return "—";
        try {
            String normalized = isoDate.replaceAll("(\\.\\d{3})\\d+(Z)$", "$1$2");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(normalized);
            if (date == null) return isoDate;
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd/MM/yyyy", locale);
            String f = out.format(date);
            return f.substring(0, 1).toUpperCase(locale) + f.substring(1);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    private String formatShiftDate(String dateStr) {
        if (dateStr == null) return "—";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return dateStr;
            Locale locale = context.getResources().getConfiguration().getLocales().get(0);
            SimpleDateFormat out = new SimpleDateFormat("EEE, dd/MM/yyyy", locale);
            String f = out.format(date);
            return f.substring(0, 1).toUpperCase(locale) + f.substring(1);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    // --- ViewHolders ---

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;
        HeaderViewHolder(View v) {
            super(v);
            tvHeader = v.findViewById(R.id.tv_section_header);
        }
    }

    static class SwapViewHolder extends RecyclerView.ViewHolder {
        TextView tvCounterparty, tvStatusBadge, tvDate;
        MaterialCardView cardOffered0, cardRequested0;
        TextView tvLabelOffered0, tvDateOffered0, tvTypeOffered0, tvOwnerOffered0;
        TextView tvLabelRequested0, tvDateRequested0, tvTypeRequested0, tvOwnerRequested0;
        // Segundo par de cards (troca dupla)
        MaterialCardView cardOffered1, cardRequested1;
        TextView tvLabelOffered1, tvDateOffered1, tvTypeOffered1, tvOwnerOffered1;
        TextView tvLabelRequested1, tvDateRequested1, tvTypeRequested1, tvOwnerRequested1;
        LinearLayout llActions;
        MaterialButton btnCancel, btnReject, btnAccept;

        SwapViewHolder(View v) {
            super(v);
            tvCounterparty = v.findViewById(R.id.tv_counterparty);
            tvStatusBadge  = v.findViewById(R.id.tv_status_badge);
            tvDate         = v.findViewById(R.id.tv_date);

            cardOffered0    = v.findViewById(R.id.card_offered_0);
            tvLabelOffered0 = v.findViewById(R.id.tv_label_offered_0);
            tvDateOffered0  = v.findViewById(R.id.tv_date_offered_0);
            tvTypeOffered0  = v.findViewById(R.id.tv_type_offered_0);
            tvOwnerOffered0 = v.findViewById(R.id.tv_owner_offered_0);

            cardRequested0    = v.findViewById(R.id.card_requested_0);
            tvLabelRequested0 = v.findViewById(R.id.tv_label_requested_0);
            tvDateRequested0  = v.findViewById(R.id.tv_date_requested_0);
            tvTypeRequested0  = v.findViewById(R.id.tv_type_requested_0);
            tvOwnerRequested0 = v.findViewById(R.id.tv_owner_requested_0);

            cardOffered1    = v.findViewById(R.id.card_offered_1);
            tvLabelOffered1 = v.findViewById(R.id.tv_label_offered_1);
            tvDateOffered1  = v.findViewById(R.id.tv_date_offered_1);
            tvTypeOffered1  = v.findViewById(R.id.tv_type_offered_1);
            tvOwnerOffered1 = v.findViewById(R.id.tv_owner_offered_1);

            cardRequested1    = v.findViewById(R.id.card_requested_1);
            tvLabelRequested1 = v.findViewById(R.id.tv_label_requested_1);
            tvDateRequested1  = v.findViewById(R.id.tv_date_requested_1);
            tvTypeRequested1  = v.findViewById(R.id.tv_type_requested_1);
            tvOwnerRequested1 = v.findViewById(R.id.tv_owner_requested_1);

            llActions = v.findViewById(R.id.ll_actions);
            btnCancel = v.findViewById(R.id.btn_cancel_swap);
            btnAccept = v.findViewById(R.id.btn_accept_swap);
            btnReject = v.findViewById(R.id.btn_reject_swap);
        }
    }
}
