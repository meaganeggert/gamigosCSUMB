package com.example.gamigosjava.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

public class Match {
    public String id;
    public String eventId;
    public String gameId;
    public String notes;
    public String rulesVariant;

    public String imageUrl;
    public Timestamp startedAt;
    public Timestamp endedAt;
    public DocumentReference gameRef;
    public CollectionReference playersRef;
    public String hostId;
}
