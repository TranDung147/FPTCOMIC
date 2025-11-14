package com.example.fptcomicapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private EditText edtDisplayName;
    private Button  btnSave, btnCancel;
    private CardView btnSelectImage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri selectedImageUri;
    private String currentAvatarUrl;
    private ProgressDialog progressDialog;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Initialize views
        imgAvatar = findViewById(R.id.imgAvatar);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating profile...");
        progressDialog.setCancelable(false);

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        // Display selected image
                        Glide.with(this)
                                .load(selectedImageUri)
                                .circleCrop()
                                .into(imgAvatar);
                    }
                }
        );

        // Load current user data
        loadUserData();

        // Select image button
        btnSelectImage.setOnClickListener(v -> openImagePicker());

        // Save button
        btnSave.setOnClickListener(v -> saveProfile());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String displayName = documentSnapshot.getString("displayName");
                            currentAvatarUrl = documentSnapshot.getString("avatar");

                            // Set display name
                            if (displayName != null) {
                                edtDisplayName.setText(displayName);
                            }

                            // Load avatar
                            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(currentAvatarUrl)
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .circleCrop()
                                        .into(imgAvatar);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void saveProfile() {
        String newDisplayName = edtDisplayName.getText().toString().trim();

        if (newDisplayName.isEmpty()) {
            edtDisplayName.setError("Display name is required");
            edtDisplayName.requestFocus();
            return;
        }

        progressDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();

            // If user selected new image, upload it first
            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(uid, newDisplayName);
            } else {
                // No new image, just update display name
                updateFirestore(uid, newDisplayName, currentAvatarUrl);
            }
        }
    }

    private void uploadImageAndSaveProfile(String uid, String displayName) {
        // Create reference to avatar storage path

        StorageReference avatarRef = storageRef.child("avatars/" + uid + ".jpg");

        avatarRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String avatarUrl = uri.toString();
                        updateFirestore(uid, displayName, avatarUrl);
                    }).addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error getting image URL: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Uploading Avatar Coming Soon!",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFirestore(String uid, String displayName, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            // Gán avatar mặc định
            avatarUrl = "https://th.bing.com/th/id/R.cf617bf1054e50f7321d18574d8192c8?rik=sAwHhwnmQRBgbA&pid=ImgRaw&r=0";
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            updates.put("avatar", avatarUrl);
        }

        db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Uploading Avatar Coming Soon!",
                            Toast.LENGTH_SHORT).show();
                });
    }
}