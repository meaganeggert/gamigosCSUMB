package com.example.gamigosjava.data.model;

import com.google.firebase.Timestamp;

public class UserGameMetric {
    public int timesWon = 0;
    public int winStreak = 0;
    public int bestWinStreak = 0;
    public int averageWinStreak = 0;
    public int winningStreakCount = 0;    // necessary for average win streak.

    public int timesLost = 0;
    public int lossStreak = 0;
    public int worstLosingStreak = 0;

    public int bestScore = 0;
    public int worstScore = 0;
    public int averageScore = 0;
    public int scoreTotal = 0;  // necessary for average score.

    public int timesPlayed = 0;
    public Timestamp firstTimePlayed;
    public Timestamp lastTimePlayed;

    public UserGameMetric() {}
}
