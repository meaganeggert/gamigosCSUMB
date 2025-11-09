package com.example.gamigosjava.data.model;

public class Friend {
    public String id;
    public String friendUId;
    public String displayName;

    public Friend() {}
    public Friend(String id, String friendUId, String displayName) {
        this.id = id;
        this.friendUId = friendUId;
        this.displayName = displayName;
    }

    @Override public String toString() {
        if (displayName != null) {
            return displayName;
        } else {
            return "";
        }
    }
}
