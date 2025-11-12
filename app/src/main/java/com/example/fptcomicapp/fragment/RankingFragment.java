package com.example.fptcomicapp.fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.adapter.RankingAdapter;
import com.example.fptcomicapp.model.Comic;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RankingFragment extends Fragment {

    private RecyclerView rvRanking;
    private RankingAdapter rankingAdapter;
    private List<Comic> comicList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private RadioGroup rgSort;
    private ImageButton btnSortOrder;
    private boolean isAscending = false; // false = cao → thấp

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ranking, container, false);

        rvRanking = view.findViewById(R.id.rvRanking);
        rgSort = view.findViewById(R.id.rgSort);
        btnSortOrder = view.findViewById(R.id.btnSortOrder);

        setupRecyclerView();
        setupSortControls();

        loadRanking("likes"); // Mặc định
        return view;
    }

    private void setupRecyclerView() {
        rvRanking.setLayoutManager(new GridLayoutManager(requireContext(), 1));
        rvRanking.setHasFixedSize(true);
        rankingAdapter = new RankingAdapter(comicList);
        rvRanking.setAdapter(rankingAdapter);
    }

    private void setupSortControls() {
        btnSortOrder.setOnClickListener(v -> {
            isAscending = !isAscending;
            updateSortIcon();
            loadRanking(getCurrentSortField());
        });
        rgSort.setOnCheckedChangeListener((group, checkedId) -> loadRanking(getCurrentSortField()));

        updateSortIcon();
    }

    private String getCurrentSortField() {
        return rgSort.getCheckedRadioButtonId() == R.id.rbLikes ? "likes" : "views";
    }

    private void updateSortIcon() {
        btnSortOrder.setImageResource(isAscending ? R.drawable.ic_sort_asc : R.drawable.ic_sort_desc);
    }

    private void loadRanking(String sortBy) {
        db.collection("comics").get()
                .addOnSuccessListener(querySnapshot -> {
                    comicList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
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
                })
                .addOnFailureListener(e -> Log.e("Ranking", "Error", e));
    }
}