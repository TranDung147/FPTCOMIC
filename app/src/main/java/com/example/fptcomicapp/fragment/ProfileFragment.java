package com.example.fptcomicapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fptcomicapp.R;
import com.example.fptcomicapp.EditProfileActivity;
import com.example.fptcomicapp.ChangePasswordActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView imgAvatar;
    private TextView tvDisplayName;
    private TextView tvEmail;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        Button editButton = view.findViewById(R.id.btnEditProfile);
        Button changePasswordButton = view.findViewById(R.id.btnChangePassword);

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });

        changePasswordButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            String email = user.getEmail();

            // Set email
            if (email != null && !email.isEmpty()) {
                tvEmail.setText(email);
            } else {
                tvEmail.setText("No email");
            }

            // Fetch user data from Firestore
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Get displayName from Firestore
                            String displayName = documentSnapshot.getString("displayName");
                            String avatarUrl = documentSnapshot.getString("avatar");

                            // Set display name
                            if (displayName != null && !displayName.isEmpty()) {
                                tvDisplayName.setText(displayName);
                            } else if (email != null && !email.isEmpty()) {
                                String username = email.split("@")[0];
                                tvDisplayName.setText(username);
                            } else {
                                tvDisplayName.setText("User");
                            }

                            // Load avatar
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(ProfileFragment.this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.user)
                                        .circleCrop()
                                        .into(imgAvatar);
                            } else {
                                imgAvatar.setImageResource(R.drawable.user);
                            }
                        } else {
                            // Document doesn't exist, use fallback
                            setFallbackDisplayName(email);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error loading profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        setFallbackDisplayName(email);
                    });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when fragment becomes visible again
        // This ensures profile updates are reflected after returning from EditProfileActivity
        loadUserData();
    }

    private void setFallbackDisplayName(String email) {
        if (email != null && !email.isEmpty()) {
            String username = email.split("@")[0];
            tvDisplayName.setText(username);
        } else {
            tvDisplayName.setText("User");
        }
        imgAvatar.setImageResource(R.drawable.user);
    }
}