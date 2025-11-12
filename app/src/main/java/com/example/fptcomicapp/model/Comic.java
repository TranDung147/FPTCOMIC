package com.example.fptcomicapp.model;

public class Comic {
    private String title,id;
    private String coverUrl,description;
    private long likes,chaptersCount;
    private long views;

    public Comic() {} // required for Firestore

    public Comic(String title, String coverUrl,String id,long likes,long views,String description,long chaptersCount) {
        this.title = title;
        this.coverUrl = coverUrl;
        this.chaptersCount=chaptersCount;
        this.likes = likes;
        this.views = views;
        this.id=id;
        this.description=description;
    }

    public long getChaptersCount() {
        return chaptersCount;
    }

    public void setChaptersCount(long chaptersCount) {
        this.chaptersCount = chaptersCount;
    }

    public long getLikes() {
        return likes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLikes(long likes) {
        this.likes = likes;
    }

    public long getViews() {
        return views;
    }

    public void setViews(long views) {
        this.views = views;
    }

    public String getTitle() { return title; }
    public String getCoverUrl() { return coverUrl; }
}

