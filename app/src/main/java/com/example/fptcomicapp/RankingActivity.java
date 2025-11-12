package com.example.fptcomicapp;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.adapter.RankingAdapter;
import com.example.fptcomicapp.model.Comic;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    RecyclerView rvRanking;
    RankingAdapter rankingAdapter;
    List<Comic> comicList = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    RadioGroup rgSort;
    ImageButton btnSortOrder;
    boolean isAscending = false; // false = cao → thấp, true = thấp → cao

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_ranking);

        rvRanking = findViewById(R.id.rvRanking);
        rgSort = findViewById(R.id.rgSort);
        btnSortOrder = findViewById(R.id.btnSortOrder);
        if (btnSortOrder == null) {
            Log.e("RankingActivity", "btnSortOrder is NULL! Check ID in XML");
            return;
        }

        rvRanking.setLayoutManager(new GridLayoutManager(this, 1));
        rvRanking.setHasFixedSize(true);
        rankingAdapter = new RankingAdapter(comicList);
        rvRanking.setAdapter(rankingAdapter);
        btnSortOrder.setOnClickListener(v -> {
            Log.d("Ranking", "Button clicked! isAscending = " + isAscending);
            isAscending = !isAscending;
            updateSortIcon();
            loadRanking(getCurrentSortField());
        });

        rgSort.setOnCheckedChangeListener((group, checkedId) -> loadRanking(getCurrentSortField()));

        updateSortIcon();
        loadRanking("likes");
    }

    private String getCurrentSortField() {
        return rgSort.getCheckedRadioButtonId() == R.id.rbLikes ? "likes" : "views";
    }


    private void updateSortIcon() {
        int icon = isAscending ? R.drawable.ic_sort_asc : R.drawable.ic_sort_desc;
        btnSortOrder.setImageResource(icon);
    }

    private void loadRanking(String sortBy) {
        db.collection("comics").get()
                .addOnSuccessListener(querySnapshot -> {
                    comicList.clear();
                    for (var doc : querySnapshot.getDocuments()) {
                        Comic comic = doc.toObject(Comic.class);
                        if (comic != null) {
                            comic.setId(doc.getId());
                            comicList.add(comic);
                        }
                    }
                    Comparator<Comic> comparator = "likes".equals(sortBy)
                            ? Comparator.comparingLong(Comic::getLikes)
                            : Comparator.comparingLong(Comic::getViews);

                    if (!isAscending) {
                        comparator = comparator.reversed();
                    }

                    comicList.sort(comparator);
                    rankingAdapter.notifyDataSetChanged();

                    Log.d("Ranking", "Sorted by " + sortBy + ", order: " + (isAscending ? "Low→High" : "High→Low"));
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error", e));
    }
}
