package com.example.fptcomicapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.fptcomicapp.R;
import com.example.fptcomicapp.UserManagementActivity;
import com.example.fptcomicapp.model.Comic;
import com.example.fptcomicapp.util.AuthUtil;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminDashboardFragment extends Fragment {

    private TextView tvComicCount;
    private TextView tvUserCount;
    private TextView tvTotalViews;
    private Button btnManageUsers;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvComicCount = view.findViewById(R.id.tvComicCount);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        tvTotalViews = view.findViewById(R.id.tvTotalViews);
        btnManageUsers = view.findViewById(R.id.btnManageUsers);

        // Load statistics
        loadStatistics();

        // Manage Users button
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserManagementActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload statistics when fragment becomes visible
        loadStatistics();
    }

    private void loadStatistics() {
        // Load comic count
        db.collection("comics").get()
                .addOnSuccessListener(querySnapshot -> {
                    int comicCount = querySnapshot.size();
                    tvComicCount.setText(String.valueOf(comicCount));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading comic count: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        // Load user count
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    int userCount = querySnapshot.size();
                    tvUserCount.setText(String.valueOf(userCount));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading user count: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });

        // Load total views (sum of all comic views)
        db.collection("comics").get()
                .addOnSuccessListener(querySnapshot -> {
                    long totalViews = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Comic comic = doc.toObject(Comic.class);
                        if (comic != null) {
                            totalViews += comic.getViews();
                        }
                    }
                    tvTotalViews.setText(String.valueOf(totalViews));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading total views: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}

