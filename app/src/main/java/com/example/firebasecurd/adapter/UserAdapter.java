package com.example.firebasecurd.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebasecurd.R;
import com.example.firebasecurd.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public interface OnUserActionListener {
        void onEditUser(User user);
        void onDeleteUser(User user);
    }

    private final Context context;
    private List<User> userList;
    private final OnUserActionListener listener;
    private final String currentLoggedInUserId;

    public UserAdapter(Context context, List<User> userList, String currentLoggedInUserId, OnUserActionListener listener) {
        this.context = context;
        this.userList = userList != null ? userList : new ArrayList<>();
        this.currentLoggedInUserId = currentLoggedInUserId;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.userList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User u = userList.get(position);

        holder.tvFullName.setText(u.getFullName());
        String emailPart = (u.getEmail() != null && !u.getEmail().isEmpty()) ? " • " + u.getEmail() : "";
        holder.tvUsername.setText("@" + u.getUsername() + emailPart);

        String initial = u.getFullName() != null && !u.getFullName().isEmpty()
                ? u.getFullName().substring(0, 1).toUpperCase()
                : "U";
        holder.tvAvatarInitial.setText(initial);

        boolean isAdmin = u.isAdmin();
        holder.tvRoleBadge.setText(u.getRole());
        if (isAdmin) {
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_in_stock);
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.status_in_stock));
        } else {
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_badge_neutral);
            holder.tvRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_blue));
        }

        // Do not allow deleting self or primary admin
        if (u.getId() != null && u.getId().equals(currentLoggedInUserId) || "admin".equalsIgnoreCase(u.getUsername())) {
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditUser(u);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteUser(u);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitial, tvFullName, tvUsername, tvRoleBadge;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            tvFullName = itemView.findViewById(R.id.tv_user_fullname);
            tvUsername = itemView.findViewById(R.id.tv_user_username);
            tvRoleBadge = itemView.findViewById(R.id.tv_user_role_badge);
            btnEdit = itemView.findViewById(R.id.btn_edit_user);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
        }
    }
}
