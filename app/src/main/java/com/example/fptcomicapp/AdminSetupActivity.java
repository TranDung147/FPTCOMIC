package com.example.fptcomicapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fptcomicapp.constant.Role;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper activity to create admin account in Firebase Authentication
 * This should only be used once to set up the admin account
 */
public class AdminSetupActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etDisplayName;
    private Button btnCreateAdmin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_setup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnCreateAdmin = findViewById(R.id.btnCreateAdmin);

        // Pre-fill with admin data if needed
        etEmail.setText("admin@gmail.com");
        etDisplayName.setText("ADMIN");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating admin account...");
        progressDialog.setCancelable(false);

        btnCreateAdmin.setOnClickListener(v -> createAdminAccount());
    }

    private void createAdminAccount() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String displayName = etDisplayName.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Check if user already exists in Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().getSignInMethods() != null
                                && !task.getResult().getSignInMethods().isEmpty()) {
                            // User already exists in Firebase Auth
                            progressDialog.dismiss();
                            Toast.makeText(this, "Admin account already exists in Firebase Auth. You can login now.", 
                                    Toast.LENGTH_LONG).show();
                            
                            // Update Firestore with admin role
                            updateFirestoreWithAdminRole(email);
                        } else {
                            // User doesn't exist, create it
                            createNewAdminAccount(email, password, displayName);
                        }
                    } else {
                        // Try to create anyway
                        createNewAdminAccount(email, password, displayName);
                    }
                });
    }

    private void createNewAdminAccount(String email, String password, String displayName) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save to Firestore with ADMIN role
                            saveAdminToFirestore(user.getUid(), displayName, email);
                        }
                    } else {
                        progressDialog.dismiss();
                        String error = task.getException() != null 
                                ? task.getException().getMessage() 
                                : "Failed to create admin account";
                        
                        if (error.contains("email address is already in use")) {
                            Toast.makeText(this, 
                                    "Admin account already exists. Updating Firestore...", 
                                    Toast.LENGTH_LONG).show();
                            updateFirestoreWithAdminRole(email);
                        } else {
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveAdminToFirestore(String uid, String displayName, String email) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("displayName", displayName);
        userMap.put("email", email);
        userMap.put("role", Role.ADMIN.name());
        userMap.put("isBlocked", false);

        db.collection("users").document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Admin account created successfully!", Toast.LENGTH_SHORT).show();
                    // Sign out and go to login
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Admin created but failed to save to Firestore: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void updateFirestoreWithAdminRole(String email) {
        // Find user by email and update role to ADMIN
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Update existing document
                        String uid = querySnapshot.getDocuments().get(0).getId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("role", Role.ADMIN.name());
                        
                        db.collection("users").document(uid)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, 
                                            "Admin role updated in Firestore. You can login now.", 
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, 
                                            "Failed to update role: " + e.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, 
                                "User found in Auth but not in Firestore. Please login first to create Firestore document.", 
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating Firestore: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
}

