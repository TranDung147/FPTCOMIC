package com.example.fptcomicapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.adapter.ComicAdapter;
import com.example.fptcomicapp.model.Comic;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvHorizontal, rvVertical;
    private ComicAdapter horizontalAdapter, verticalAdapter;
    private List<Comic> horizontalList = new ArrayList<>();
    private List<Comic> verticalList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvHorizontal = view.findViewById(R.id.rvHorizontal);
        rvVertical = view.findViewById(R.id.rvVertical);

        setupRecyclerViews();
        loadData();

        return view;
    }

    private void setupRecyclerViews() {
        LinearLayoutManager hlm = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvHorizontal.setLayoutManager(hlm);
        rvHorizontal.setHasFixedSize(true);
        horizontalAdapter = new ComicAdapter(horizontalList, true);
        rvHorizontal.setAdapter(horizontalAdapter);

        GridLayoutManager glm = new GridLayoutManager(requireContext(), 2);
        rvVertical.setLayoutManager(glm);
        rvVertical.setHasFixedSize(true);
        verticalAdapter = new ComicAdapter(verticalList, false);
        rvVertical.setAdapter(verticalAdapter);
    }

    private void loadData() {
        db.collection("comics").limit(10).get()
                .addOnSuccessListener(snapshot -> {
                    horizontalList.clear();
                    for (var doc : snapshot) {
                        Comic c = doc.toObject(Comic.class);
                        if (c != null) horizontalList.add(c);
                    }
                    horizontalAdapter.notifyDataSetChanged();
                });

        db.collection("comics").get()
                .addOnSuccessListener(snapshot -> {
                    verticalList.clear();
                    for (var doc : snapshot) {
                        Comic c = doc.toObject(Comic.class);
                        if (c != null) verticalList.add(c);
                    }
                    verticalAdapter.notifyDataSetChanged();
                });
    }
}