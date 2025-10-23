package com.example.gamigosjava.data.model;

public class GameSummary {
    public final String id;
    public final String title;
    public final String imageUrl;      // prefer full image; fallback to thumbnail
    public final Integer minPlayers;
    public final Integer maxPlayers;
    public final Integer playingTime;  // minutes

    public GameSummary(String id, String title, String imageUrl,
                       Integer minPlayers, Integer maxPlayers, Integer playingTime) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.playingTime = playingTime;
    }
}
