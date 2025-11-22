package com.example.gamigosjava.data.model;

public class Player {
    public Friend friend;
    public Integer score;
    public Integer placement;


    public Player() {
        friend = new Friend();
        score = 0;
        placement = 0;
    }
    public Player(Friend friend, int score, int placement) {
        this.friend = friend;
        this.score = score;
        this.placement = placement;
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

    public void setPlacement(String string) {
        placement = Integer.parseInt(string);
    }
}
