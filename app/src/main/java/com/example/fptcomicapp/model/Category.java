package com.example.fptcomicapp.model;

public class Category {
    private String name;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    private String slug;

    public Category() {} // required for Firestore

    public Category(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}

