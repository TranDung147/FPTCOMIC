package com.example.fptcomicapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fptcomicapp.util.AuthUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        String text = "Don't have an account? Register";
        SpannableString ss = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(Color.parseColor("#00D4FF"));
                ds.setUnderlineText(true);
            }
        };

        int start = text.indexOf("Register");
        int end = start + "Register".length();
        ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvRegister.setText(ss);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
        btnLogin.setOnClickListener(v -> loginUser());
        
        // Admin setup link
        TextView tvAdminSetup = findViewById(R.id.tvAdminSetup);
        if (tvAdminSetup != null) {
            tvAdminSetup.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, AdminSetupActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if(user != null) {
                            // Clear admin cache on login
                            AuthUtil.clearCache();
                            
                            // Ensure user document exists in Firestore
                            ensureUserDocumentExists(user.getUid(), email);
                            
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        String errorMessage = "Login failed";
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            if (error != null) {
                                if (error.contains("wrong-password") || error.contains("invalid-credential")) {
                                    errorMessage = "Incorrect email or password";
                                } else if (error.contains("user-not-found")) {
                                    errorMessage = "User not found. Please register first.";
                                } else if (error.contains("network")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = "Login failed: " + error;
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Ensure user document exists in Firestore
     * If document doesn't exist, create it with default values
     */
    private void ensureUserDocumentExists(String uid, String email) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // User document doesn't exist, create it
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("email", email);
                        userData.put("displayName", email != null ? email.split("@")[0] : "User");
                        userData.put("role", "USER"); // Default role
                        userData.put("isBlocked", false);
                        
                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    // User document created successfully
                                })
                                .addOnFailureListener(e -> {
                                    // Failed to create user document, but login is still successful
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to check user document, but login is still successful
                });
    }
}
