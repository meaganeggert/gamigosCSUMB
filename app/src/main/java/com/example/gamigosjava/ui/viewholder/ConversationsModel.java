package com.example.gamigosjava.ui.viewholder;

import com.google.firebase.Timestamp;
import java.util.List;

public class ConversationsModel {

    private String id;                // doc id (set manually from snapshot)
    private List<String> participants;
    private boolean isGroup;
    private String lastMessage;
    private String otherPhotoUrl;
    private Timestamp lastMessageAt;

    // --- Derived fields (not saved in Firestore) ---
    private String otherUid;          // for 1:1 DM
    private String otherName;         // displayName from users/{uid}
    private String titleOverride;     // for groups

    public ConversationsModel() { }

    public String getOtherPhotoUrl() {
        return otherPhotoUrl;
    }
    public void setOtherPhotoUrl(String otherPhotoUrl) {
        this.otherPhotoUrl = otherPhotoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public Timestamp getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Timestamp lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getOtherUid() { return otherUid; }
    public void setOtherUid(String otherUid) { this.otherUid = otherUid; }

    public String getOtherName() { return otherName; }
    public void setOtherName(String otherName) { this.otherName = otherName; }

    public String getTitleOverride() { return titleOverride; }
    public void setTitleOverride(String titleOverride) { this.titleOverride = titleOverride; }
}
