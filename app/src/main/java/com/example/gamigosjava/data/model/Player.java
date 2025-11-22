package com.example.gamigosjava.data.model;

public class Player {
    public Friend friend;
    public Integer score;


    public Player() {
        friend = new Friend();
        score = 0;
    }
    public Player(Friend friend, int score) {
        this.friend = friend;
        this.score = score;
    }

    @Override public String toString() {
        if (friend.displayName != null) {
            return friend.displayName;
        } else {
            return "";
        }
    }

    public void setScore(String string) {
        score = Integer.parseInt(string);
    }
}
