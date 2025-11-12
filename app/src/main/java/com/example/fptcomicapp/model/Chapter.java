package com.example.fptcomicapp.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chapter {
    private String id;
    private String title;
    private long index;
    private List<String> pageStoragePaths;
    private List<String> pageImageUrls;
    private long createdAt;

    public Chapter() {
        // Required for Firestore deserialization
    }

    public Chapter(String id, String title, long index, List<String> pageStoragePaths, List<String> pageImageUrls, long createdAt) {
        this.id = id;
        this.title = title;
        this.index = index;
        this.pageStoragePaths = pageStoragePaths;
        this.pageImageUrls = pageImageUrls;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public List<String> getPageStoragePaths() {
        return pageStoragePaths;
    }

    public void setPageStoragePaths(List<String> pageStoragePaths) {
        this.pageStoragePaths = pageStoragePaths;
    }

    public List<String> getPageImageUrls() {
        return pageImageUrls;
    }

    public void setPageImageUrls(List<String> pageImageUrls) {
        this.pageImageUrls = pageImageUrls;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("index", index);
        map.put("pageStoragePaths", pageStoragePaths);
        map.put("pageImageUrls", pageImageUrls);
        map.put("createdAt", createdAt);
        return map;
    }
}


