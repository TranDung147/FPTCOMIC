package com.example.fptcomicapp.adapter;

import android.annotation.SuppressLint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.model.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submitList(List<Comment> items) {
        comments.clear();
        if (items != null) {
            comments.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.tvUserName.setText(comment.getUserName() != null ? comment.getUserName() : "Người dùng");
        holder.tvContent.setText(comment.getContent());
        holder.tvTimestamp.setText(formatTimestamp(comment.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    private String formatTimestamp(long createdAt) {
        if (createdAt <= 0) return "";
        Date date = new Date(createdAt);
        return DateFormat.format("dd/MM/yyyy HH:mm", date).toString();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName;
        TextView tvContent;
        TextView tvTimestamp;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvCommentUser);
            tvContent = itemView.findViewById(R.id.tvCommentContent);
            tvTimestamp = itemView.findViewById(R.id.tvCommentTime);
        }
    }
}


