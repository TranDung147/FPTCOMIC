package com.example.fptcomicapp;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText edtCurrentPassword;
    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private Button btnChangePassword;
    private Button btnCancel;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        edtCurrentPassword = findViewById(R.id.edtCurrentPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnCancel = findViewById(R.id.btnCancel);

        // Setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Changing password...");
        progressDialog.setCancelable(false);

        // Change password button
        btnChangePassword.setOnClickListener(v -> changePassword());

        // Cancel button
        btnCancel.setOnClickListener(v -> finish());

        // Clear errors when user starts typing
        edtCurrentPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                edtCurrentPassword.setError(null);
            }
        });
        
        edtNewPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                edtNewPassword.setError(null);
            }
        });
        
        edtConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                edtConfirmPassword.setError(null);
            }
        });
    }

    private void changePassword() {
        String currentPassword = edtCurrentPassword.getText().toString().trim();
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(currentPassword)) {
            edtCurrentPassword.setError("Current password is required");
            edtCurrentPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            edtNewPassword.setError("New password is required");
            edtNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            edtNewPassword.setError("Password must be at least 6 characters");
            edtNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Please confirm your password");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            edtConfirmPassword.setError("Passwords do not match");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (currentPassword.equals(newPassword)) {
            edtNewPassword.setError("New password must be different from current password");
            edtNewPassword.requestFocus();
            return;
        }

        progressDialog.show();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Re-authenticate user
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Re-authentication successful, update password
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    progressDialog.dismiss();
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ChangePasswordActivity.this,
                                                "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        finish();
                                    } else {
                                        String error = updateTask.getException() != null
                                                ? updateTask.getException().getMessage()
                                                : "Failed to update password";
                                        Toast.makeText(ChangePasswordActivity.this,
                                                "Error: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Exception exception = task.getException();
                        String errorMessage = "Re-authentication failed";
                        
                        if (exception != null) {
                            String error = exception.getMessage();
                            errorMessage = error != null ? error : errorMessage;
                            
                            // Check for specific error types
                            if (error != null) {
                                if (error.contains("wrong-password") || 
                                    error.contains("invalid-credential") ||
                                    error.contains("incorrect") ||
                                    error.contains("malformed") ||
                                    error.contains("expired")) {
                                    edtCurrentPassword.setError("Current password is incorrect");
                                    edtCurrentPassword.requestFocus();
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Please check your current password", Toast.LENGTH_LONG).show();
                                    return;
                                } else if (error.contains("network")) {
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Network error. Please check your connection.", Toast.LENGTH_LONG).show();
                                    return;
                                } else if (error.contains("too-many-requests")) {
                                    Toast.makeText(ChangePasswordActivity.this,
                                            "Too many attempts. Please try again later.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                            }
                        }
                        
                        // Generic error message
                        Toast.makeText(ChangePasswordActivity.this,
                                "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}

