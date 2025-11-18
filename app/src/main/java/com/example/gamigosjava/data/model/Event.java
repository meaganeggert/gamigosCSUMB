package com.example.gamigosjava.data.model;

import com.google.firebase.Timestamp;

public class Event {
    public String id;
    public String hostId;
    public String title;
    public String visibility;
    public String status;
    public String notes;
    public Timestamp scheduledAt;
    public Timestamp createdAt;
    public Timestamp endedAt;
}
