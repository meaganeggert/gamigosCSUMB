package com.example.gamigosjava.data.model;

import java.util.ArrayList;
import java.util.List;

public class EventSummary {
    public String id;
    public String title;
    public String imageUrl;
    public String status;
    public String timeElapsed;
    public List<Attendee> playersAttending = new ArrayList<>();

    public EventSummary() {}

//    public EventSummary(String id, String title, String imageUrl, String status) {
//        this.id = id;
//        this.title = title;
//        this.imageUrl = imageUrl;
//        this.status = status;
//    }

    public EventSummary(String id, String title, String imageUrl, String status, List<Attendee> playersAttending) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.status = status;
        if (playersAttending != null) {
            this.playersAttending = playersAttending;
        }
    }
}
