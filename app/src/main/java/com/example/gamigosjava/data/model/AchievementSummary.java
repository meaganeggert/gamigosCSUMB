package com.example.gamigosjava.data.model;

import java.time.LocalDate;

public class AchievementSummary {
    public final String id;
    public final String title;
    public final String description;
    public final String iconUrl;

    public final int current; // current progress toward achievement (user metric)
    public final int goal; // goal to unlock achievement
    public final boolean achieved;


    public AchievementSummary(String id, String title, String description, String iconUrl, int current, int goal, boolean achieved) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.current = current;
        this.goal = goal;
        this.achieved = achieved;
    }
}
