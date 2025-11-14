package com.example.fptcomicapp.model;

public class User {
    private String uid;
    private String displayName;
    private String email;
    private String avatar;
    private String role;
    private boolean isBlocked;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String displayName, String email, String avatar, String role, boolean isBlocked) {
        this.uid = uid;
        this.displayName = displayName;
        this.email = email;
        this.avatar = avatar;
        this.role = role;
        this.isBlocked = isBlocked;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}

