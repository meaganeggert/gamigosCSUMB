package com.example.gamigosjava.data.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MatchSummary {
    public String id;
    public String eventId;
    public String gameName;
    public String imageUrl;
    public Integer minPlayers;
    public Integer maxPlayers;
    public Integer playingTime;  // minutes

    public List<String> playerNames = new ArrayList<>();
    public List<String> playerIds = new ArrayList<>();
    public List<String> playerAvatars = new ArrayList<>();
    public String winnerId;
    public String winnerName;
    public String winnerAvatarUrl;

    public MatchSummary() {}

    public MatchSummary(String id, String eventId, String gameName, String imageUrl,
                       Integer minPlayers, Integer maxPlayers, Integer playingTime) {
        this.id = id;
        this.eventId = eventId;
        this.gameName = gameName;
        this.imageUrl = imageUrl;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.playingTime = playingTime;
    }

    @NonNull
    @Override
    public String toString() {
        if (gameName != null) return gameName;
        else return "";
    }
}
