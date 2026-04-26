package com.pi.gestaohorariosenfermagemmobile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserListener onEditListener;
    private OnUserListener onDeleteListener;

    public interface OnUserListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> users, OnUserListener editListener, OnUserListener deleteListener) {
        this.users = users;
        this.onEditListener = editListener;
        this.onDeleteListener = deleteListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(user.getName());
        if (user.getName() != null && !user.getName().isEmpty()) {
            holder.tvAvatar.setText(user.getName().substring(0, 1).toUpperCase());
        }

        // Tradução do Cargo (Bilingue)
        String role = user.getFormattedRole();
        boolean isHead = role.contains("head") || role.contains("chefe");
        int roleTextRes = isHead ? R.string.role_head_nurse : R.string.role_nurse;
        holder.tvRoleBadge.setText(context.getString(roleTextRes));

        holder.tvRoleBadge.setBackgroundResource(isHead ? R.drawable.bg_role_head_nurse : R.drawable.bg_role_nurse);
        holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, isHead ? R.color.bg_role_head_nurse : R.color.bg_role_nurse));
        holder.tvRoleBadge.setTypeface(null, android.graphics.Typeface.BOLD);

        // Tradução do Estado (Bilingue)
        boolean isActive = user.isActive();
        int statusTextRes = isActive ? R.string.status_active : R.string.status_inactive;
        holder.tvStatus.setText(context.getString(statusTextRes));
        holder.tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);

        holder.tvStatus.setBackgroundResource(isActive ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, isActive ? R.color.bg_status_active : R.color.bg_status_inactive));

        holder.btnEdit.setOnClickListener(v -> onEditListener.onUserClick(user));
        holder.btnDelete.setOnClickListener(v -> onDeleteListener.onUserClick(user));
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvRoleBadge, tvStatus;
        ImageButton btnEdit, btnDelete;
        UserViewHolder(View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tv_avatar); tvName = v.findViewById(R.id.tv_name);
            tvRoleBadge = v.findViewById(R.id.tv_role_badge); tvStatus = v.findViewById(R.id.tv_status);
            btnEdit = v.findViewById(R.id.btn_edit); btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}