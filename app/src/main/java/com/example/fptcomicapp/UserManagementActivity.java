package com.example.fptcomicapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.adapter.UserAdapter;
import com.example.fptcomicapp.constant.Role;
import com.example.fptcomicapp.model.User;
import com.example.fptcomicapp.util.AuthUtil;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserManagementActivity extends AppCompatActivity implements UserAdapter.UserActionListener {

    private RecyclerView recyclerView;
    private EditText edtSearch;
    private UserAdapter adapter;
    private List<User> allUsers;
    private List<User> filteredUsers;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Check if user is admin
        AuthUtil.checkAdminStatus(isAdmin -> {
            if (!isAdmin) {
                Toast.makeText(this, "Access denied. Admin only.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            initializeViews();
            loadUsers();
        });
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("User Management");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.recyclerView);
        edtSearch = findViewById(R.id.edtSearch);

        db = FirebaseFirestore.getInstance();
        allUsers = new ArrayList<>();
        filteredUsers = new ArrayList<>();

        adapter = new UserAdapter(filteredUsers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading users...");
        progressDialog.setCancelable(false);

        // Setup search
        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadUsers() {
        progressDialog.show();
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    allUsers.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = new User();
                        user.setUid(doc.getId());
                        user.setDisplayName(doc.getString("displayName"));
                        user.setEmail(doc.getString("email"));
                        user.setAvatar(doc.getString("avatar"));
                        user.setRole(doc.getString("role"));
                        
                        // Handle isBlocked field (may not exist for old users)
                        Boolean isBlocked = doc.getBoolean("isBlocked");
                        user.setBlocked(isBlocked != null && isBlocked);
                        
                        allUsers.add(user);
                    }
                    filterUsers("");
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading users: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();
        if (TextUtils.isEmpty(query)) {
            filteredUsers.addAll(allUsers);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User user : allUsers) {
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                String displayName = user.getDisplayName() != null ? user.getDisplayName().toLowerCase() : "";
                if (email.contains(lowerQuery) || displayName.contains(lowerQuery)) {
                    filteredUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRoleChange(User user, String newRole) {
        progressDialog.setMessage("Updating role...");
        progressDialog.show();
        
        db.collection("users").document(user.getUid())
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    user.setRole(newRole);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Role updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBlockToggle(User user, boolean isBlocked) {
        progressDialog.setMessage(isBlocked ? "Blocking user..." : "Unblocking user...");
        progressDialog.show();
        
        db.collection("users").document(user.getUid())
                .update("isBlocked", isBlocked)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    user.setBlocked(isBlocked);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, 
                            isBlocked ? "User blocked successfully" : "User unblocked successfully",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating user status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}

