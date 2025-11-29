package com.example.gamigosjava.data.model;

import com.google.firebase.Timestamp;

public class ActivityItem {
    private String id;
    private String type; // ACHIEVEMENT_EARNED, EVENT_CREATED, GAME_WON, EVENT_ATTENDED,
    private String actorId; // user id
    private String actorName; // user who earned the achievement, created the event, etc.
    private String targetId; // achievement id, event id, game id
    private String targetName; // achievement name, event name, game name
    private String message; // pre-built message
    private String visibility; // who can see this
    private Timestamp createdAt; // when achievement was earned, event was created, game was won

    public ActivityItem() {}

    // Auto-generated getters/setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}