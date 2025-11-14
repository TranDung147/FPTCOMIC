package com.example.fptcomicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fptcomicapp.R;
import com.example.fptcomicapp.model.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<User> userList;
    private final UserActionListener listener;

    public interface UserActionListener {
        void onRoleChange(User user, String newRole);
        void onBlockToggle(User user, boolean isBlocked);
    }

    public UserAdapter(List<User> userList, UserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        
        // Set display name
        holder.tvDisplayName.setText(user.getDisplayName() != null ? user.getDisplayName() : "No Name");
        
        // Set email
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email");
        
        // Set role badge
        String role = user.getRole() != null ? user.getRole() : "USER";
        holder.tvRole.setText(role);
        if ("ADMIN".equals(role)) {
            holder.tvRole.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
        } else {
            holder.tvRole.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_dark));
        }
        
        // Set status
        if (user.isBlocked()) {
            holder.tvStatus.setText("Blocked");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
        } else {
            holder.tvStatus.setText("Active");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
        }
        
        // Update block button text
        holder.btnBlockToggle.setText(user.isBlocked() ? "Unblock" : "Block");
        
        // Load avatar
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatar())
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.user);
        }
        
        // Role change button
        holder.btnChangeRole.setOnClickListener(v -> {
            String newRole = "ADMIN".equals(role) ? "USER" : "ADMIN";
            if (listener != null) {
                listener.onRoleChange(user, newRole);
            }
        });
        
        // Block/Unblock button
        holder.btnBlockToggle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBlockToggle(user, !user.isBlocked());
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvDisplayName;
        TextView tvEmail;
        TextView tvRole;
        TextView tvStatus;
        TextView btnChangeRole;
        TextView btnBlockToggle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnChangeRole = itemView.findViewById(R.id.btnChangeRole);
            btnBlockToggle = itemView.findViewById(R.id.btnBlockToggle);
        }
    }
}

