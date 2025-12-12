package com.example.gamigosjava.data.model;

public class Player {
    public Friend friend;
    public Integer score;
    public Integer placement;
    public Integer team;


    public Player() {
        friend = new Friend();
        score = 0;
        placement = 0;
        team = 0;
    }
    public Player(Friend friend, Integer score, Integer placement, Integer team) {
        this.friend = friend;
        this.score = score;
        this.placement = placement;
        this.team = team;
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
