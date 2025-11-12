package com.example.fptcomicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.model.Chapter;

import java.util.ArrayList;
import java.util.List;

public class AdminChapterAdapter extends RecyclerView.Adapter<AdminChapterAdapter.AdminChapterViewHolder> {

    public interface AdminChapterListener {
        void onEditChapter(Chapter chapter);
        void onDeleteChapter(Chapter chapter);
        void onOpenChapter(Chapter chapter);
    }

    private final List<Chapter> chapters = new ArrayList<>();
    private final AdminChapterListener listener;

    public AdminChapterAdapter(AdminChapterListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Chapter> newChapters) {
        chapters.clear();
        if (newChapters != null) {
            chapters.addAll(newChapters);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_chapter, parent, false);
        return new AdminChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminChapterViewHolder holder, int position) {
        Chapter chapter = chapters.get(position);
        holder.tvTitle.setText(chapter.getTitle() != null ? chapter.getTitle() : "Chương " + (position + 1));
        holder.tvIndex.setText("#" + chapter.getIndex());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenChapter(chapter);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditChapter(chapter);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteChapter(chapter);
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class AdminChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvIndex;
        ImageButton btnEdit;
        ImageButton btnDelete;

        AdminChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAdminChapterTitle);
            tvIndex = itemView.findViewById(R.id.tvAdminChapterIndex);
            btnEdit = itemView.findViewById(R.id.btnEditChapter);
            btnDelete = itemView.findViewById(R.id.btnDeleteChapter);
        }
    }
}


