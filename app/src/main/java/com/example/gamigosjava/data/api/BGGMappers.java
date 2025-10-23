package com.example.gamigosjava.data.api;

import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.NameElement;

public final class BGGMappers {
    public static GameSummary toSummary(BGGItem it) {
        String title = null;
        if (it.names != null) {
            // Prefer primary name; else first available
            for (NameElement n : it.names) {
                if ("primary".equalsIgnoreCase(n.type)) { title = n.value; break; }
            }
            if (title == null && !it.names.isEmpty()) title = it.names.get(0).value;
        }
        String img = (it.image != null && !it.image.isEmpty()) ? it.image : it.thumbnail;
        Integer minP = it.minPlayers != null ? it.minPlayers.value : null;
        Integer maxP = it.maxPlayers != null ? it.maxPlayers.value : null;
        Integer time = it.playingTime != null ? it.playingTime.value : null;

        return new GameSummary(it.id, title, img, minP, maxP, time);
    }
}