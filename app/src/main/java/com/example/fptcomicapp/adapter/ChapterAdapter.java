package com.example.fptcomicapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.model.Chapter;

import java.util.ArrayList;
import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ViewHolder> {

    public interface OnChapterClickListener {
        void onChapterClick(Chapter chapter);
        void onChapterLongClick(View view, Chapter chapter);
    }

    private final List<Chapter> chapters = new ArrayList<>();
    private final OnChapterClickListener listener;
    private boolean showIndex = true;

    public ChapterAdapter(OnChapterClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Chapter> newChapters) {
        chapters.clear();
        if (newChapters != null) {
            chapters.addAll(newChapters);
        }
        notifyDataSetChanged();
    }

    public void setShowIndex(boolean showIndex) {
        this.showIndex = showIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chapter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chapter chapter = chapters.get(position);
        String displayTitle = chapter.getTitle() != null ? chapter.getTitle() : "Chương " + (position + 1);
        String indexText = showIndex ? "#" + chapter.getIndex() : "";

        holder.title.setText(displayTitle);
        holder.index.setText(indexText);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChapterClick(chapter);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onChapterLongClick(v, chapter);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return chapters.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView index;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvChapterTitle);
            index = itemView.findViewById(R.id.tvChapterIndex);
        }
    }
}


