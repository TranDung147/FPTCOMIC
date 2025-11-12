package com.example.fptcomicapp.adapter;

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

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {

    private List<Comic> comicList;

    public RankingAdapter(List<Comic> comicList) {
        this.comicList = comicList;
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking_comic, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        Comic comic = comicList.get(position);

        Glide.with(holder.itemView.getContext())
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.home)
                .into(holder.ivComicImage);

        holder.tvComicTitle.setText(comic.getTitle());
        holder.tvLikes.setText("Likes: " + comic.getLikes());
        holder.tvViews.setText("Views: " + comic.getViews());
        holder.tvDescription.setText(comic.getDescription() != null ? comic.getDescription() : "No description");
        holder.tvRank.setText((position + 1) + "");
    }

    @Override
    public int getItemCount() {
        return comicList != null ? comicList.size() : 0;
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivComicImage;
        TextView tvRank, tvComicTitle, tvLikes, tvViews,tvDescription;

        public RankingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivComicImage = itemView.findViewById(R.id.ivComicImage);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvComicTitle = itemView.findViewById(R.id.tvComicTitle);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvViews = itemView.findViewById(R.id.tvViews);
            tvDescription=itemView.findViewById(R.id.tvDescription);
        }
    }
}