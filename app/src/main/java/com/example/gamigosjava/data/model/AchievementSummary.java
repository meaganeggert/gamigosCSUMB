package com.example.gamigosjava.data.model;

public class AchievementSummary {
    public final String id;
    public final String title;
    public final String imageUrl;
    public final String status;



    public AchievementSummary(String id, String title, String imageUrl, String status) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;;
        this.status = status;
    }
}