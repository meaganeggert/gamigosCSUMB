package com.example.gamigosjava.data.model;

import androidx.annotation.NonNull;

public class GameSummary {
    public final String id;
    public final String title;
    public final String imageUrl;
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

    @NonNull
    @Override
    public String toString() {
        if ( title!= null) return title;
        else return "";
    }
}
