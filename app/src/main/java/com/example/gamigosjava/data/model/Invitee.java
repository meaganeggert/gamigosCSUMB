package com.example.gamigosjava.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

public class Invitee {
    public String eventId;
    public String eventTitle;
    public String hostId;
    public String hostName;
    public String status;
    public Timestamp scheduledAt;
    public DocumentReference userRef;

    public Friend userInfo;


}
