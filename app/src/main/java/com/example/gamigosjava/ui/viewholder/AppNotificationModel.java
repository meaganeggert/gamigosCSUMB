package com.example.gamigosjava.ui.viewholder;

public class AppNotificationModel {
    private String id;          // Firestore doc id
    private String type;        // "message", "friend_request", etc.
    private String title;
    private String body;
    private long timestamp;

    // Message-specific
    private String conversationId;
    private Boolean isGroup;
    private String groupTitle;
    private String otherUid;    // DM partner
    private String senderName;

    // Friend-request-specific
    private String fromUserId;
    private String fromName;

    //  Event-specific
    private String eventId;
    private String eventTitle;
    private String hostName;

    // Required public no-arg constructor for Firestore
    public AppNotificationModel() {}

    // Getters & setters (generated with IDE)

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getHostName() {return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Boolean getIsGroup() { return isGroup; }
    public void setIsGroup(Boolean group) { isGroup = group; }

    public String getGroupTitle() { return groupTitle; }
    public void setGroupTitle(String groupTitle) { this.groupTitle = groupTitle; }

    public String getOtherUid() { return otherUid; }
    public void setOtherUid(String otherUid) { this.otherUid = otherUid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
