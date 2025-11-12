package com.example.fptcomicapp.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.fptcomicapp.R;
import com.example.fptcomicapp.model.Comic;
import java.util.List;

public class ComicAdapter extends RecyclerView.Adapter<ComicAdapter.ViewHolder> {

    private final List<Comic> list;
    private final boolean isHorizontal;

    public ComicAdapter(List<Comic> list, boolean isHorizontal) {
        this.list = list;
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = isHorizontal
                ? R.layout.item_comic_horizontal
                : R.layout.item_comic_grid;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comic comic = list.get(position);
        Log.d("ComicAdapter", "Position: " + position + ", Title: " + comic.getTitle());
        holder.tvComicTitle.setText(comic.getTitle());

        // Kiểm tra null
        if (comic.getTitle() != null) {
            holder.tvComicTitle.setText(comic.getTitle());
        } else {
            holder.tvComicTitle.setText("Không có tiêu đề");
        }
        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.home)
                .error(R.drawable.home)
                .into(holder.ivComicCover);
        if (holder.tvChapter != null) {
            holder.tvChapter.setText(comic.getChaptersCount() + " chapters");
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Glide.with(holder.itemView.getContext()).clear(holder.ivComicCover);
        super.onViewRecycled(holder);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivComicCover;
        TextView tvComicTitle;
        TextView tvChapter;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivComicCover = itemView.findViewById(R.id.ivComicCover);
            tvComicTitle = itemView.findViewById(R.id.tvComicTitle);
            tvChapter = itemView.findViewById(R.id.tvChapter); // Có thể null
        }
    }
}