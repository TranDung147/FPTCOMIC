package com.example.fptcomicapp.util;

import com.example.fptcomicapp.constant.Role;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthUtil {
    private static Boolean cachedAdminStatus = null;
    private static String cachedUserId = null;

    /**
     * Check if current user is admin by querying role from Firestore
     * @return true if user is admin, false otherwise
     */
    public static boolean isAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return false;
        }

        String currentUserId = user.getUid();
        
        // Return cached result if available and user hasn't changed
        if (cachedAdminStatus != null && currentUserId.equals(cachedUserId)) {
            return cachedAdminStatus;
        }

        // For synchronous check, we'll need to use a blocking approach
        // But since Firestore is async, we'll return false and let callers use async version
        return false;
    }

    /**
     * Async version to check admin status
     * @param callback callback to receive the result
     */
    public static void checkAdminStatus(AdminStatusCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onResult(false);
            return;
        }

        String currentUserId = user.getUid();
        
        // Return cached result if available and user hasn't changed
        if (cachedAdminStatus != null && currentUserId.equals(cachedUserId)) {
            callback.onResult(cachedAdminStatus);
            return;
        }

        // Query Firestore for user role
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        boolean isAdmin = Role.ADMIN.name().equals(role);
                        
                        // Cache the result
                        cachedAdminStatus = isAdmin;
                        cachedUserId = currentUserId;
                        
                        callback.onResult(isAdmin);
                    } else {
                        cachedAdminStatus = false;
                        cachedUserId = currentUserId;
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, assume not admin
                    callback.onResult(false);
                });
    }

    /**
     * Clear cached admin status (useful when user logs out or role changes)
     */
    public static void clearCache() {
        cachedAdminStatus = null;
        cachedUserId = null;
    }

    public interface AdminStatusCallback {
        void onResult(boolean isAdmin);
    }
}

