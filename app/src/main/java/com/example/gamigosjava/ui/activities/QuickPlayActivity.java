package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.example.gamigosjava.data.model.UserGameMetric;
import com.example.gamigosjava.ui.adapter.ScoresAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuickPlayActivity extends ViewMatchActivity {
    private static final String TAG = "Quick Play";
    BGG_API api;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();
    Match matchItem = new Match();

    Button startMatch, endMatch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_quick_play);
        setTopTitle("Quick Play");

        startMatch = findViewById(R.id.button_startMatch);
        endMatch = findViewById(R.id.button_endMatch);

        if (startMatch != null) {
            startMatch.setOnClickListener(v -> {
                setMatchStart(Timestamp.now());
                endMatch.setEnabled(true);
                startMatch.setEnabled(false);
                uploadGameInfo();
            });
        }

        if (endMatch != null) {
            endMatch.setOnClickListener(v -> {
                setMatchEnd(Timestamp.now());
                endMatch.setEnabled(false);
                uploadGameInfo();
                uploadUserMatchMetrics();
            });
        }

    }

    private void uploadUserMatchMetrics() {
        // Get user reference from players involved in each match.
        Match m = getMatchItem();
        m.playersRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No players in match " + m.id);
                return;
            }

            List<Player> matchResults = new ArrayList<>();
            for (DocumentSnapshot player: snaps) {
                Player user = new Player();
                user.friend.id = player.getString("userId");
                user.placement = player.get("placement", Integer.class);
                user.score = player.get("score", Integer.class);

                matchResults.add(user);
            }

            // Update each users metrics
            for (Player p: matchResults) {
                DocumentReference gamesPlayedRef = db.collection("users")
                        .document(p.friend.id)
                        .collection("metrics")
                        .document("games_played");

                // Update the user's games_played count
                gamesPlayedRef.get().addOnSuccessListener(snap -> {
                    Integer gamesPlayed = 1;
                    HashMap<String, Object> gamesPlayedHash = new HashMap<>();

                    if (!snap.exists()) {
                        gamesPlayedHash.put("count", gamesPlayed);
                        gamesPlayedRef.set(gamesPlayedHash).addOnSuccessListener(v -> {
                            Log.d(TAG, "Successfully updated user games_played count.");
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update user games_played count: " + e.getMessage());
                        });

                        return;
                    }

                    gamesPlayedHash.put("count", snap.get("count", Integer.class) + gamesPlayed);
                    gamesPlayedRef.set(gamesPlayedHash);
                });


                // Update the user's game_metrics
                CollectionReference userMetrics = db
                        .collection("users")
                        .document(p.friend.id)
                        .collection("metrics")
                        .document("games_played")
                        .collection("game_metrics");

                DocumentReference gameMetric = userMetrics.document(m.gameId);

                gameMetric.get().addOnSuccessListener(snap -> {
                    HashMap<String, Object> metricHash = new HashMap<>();

                    UserGameMetric result = new UserGameMetric();
                    // Set default values for user match results.
                    if (p.placement == 1) {
                        result.timesWon++;
                        result.winStreak++;
                        result.bestWinStreak++;
                        result.averageWinStreak++;
                        result.winningStreakCount++;
                    } else {
                        result.timesLost++;
                        result.lossStreak++;
                        result.worstLosingStreak++;
                    }

                    result.bestScore = p.score;
                    result.worstScore = p.score;
                    result.averageScore = p.score;
                    result.scoreTotal = p.score;

                    result.timesPlayed++;
                    result.firstTimePlayed = m.startedAt;
                    result.lastTimePlayed = m.endedAt;

                    // If user has played before, get user data from database to update
                    if (snap.exists()) {
                        result.timesPlayed = result.timesPlayed + snap.get("times_played", Integer.class);
                        result.firstTimePlayed = snap.getTimestamp("first_time_played");

                        // Score related ===========================
                        result.scoreTotal = p.score +  snap.get("score_total", Integer.class);
                        result.bestScore = snap.get("best_score", Integer.class);
                        result.worstScore = snap.get("worst_score", Integer.class);
                        result.averageScore = result.scoreTotal / result.timesPlayed;

                        if (p.score > result.bestScore) result.bestScore = p.score;
                        if (p.score < result.worstScore) result.worstScore = p.score;


                        // Win/Loss related ===========================
                        result.timesWon = result.timesWon + snap.get("times_won", Integer.class);
                        result.timesLost = result.timesLost + snap.get("times_lost", Integer.class);
                        result.bestWinStreak = snap.get("best_win_streak", Integer.class);
                        result.winningStreakCount = snap.get("win_streak_count", Integer.class);
                        result.averageWinStreak = snap.get("average_win_streak", Integer.class);
                        result.worstLosingStreak = snap.get("worst_losing_streak", Integer.class);

                        // If user won, keep the loss streak set to 0 and update win streak info.
                        if (result.winStreak > result.lossStreak) {
                            Integer existingStreak = snap.get("win_streak", Integer.class);

                            if (existingStreak > 0) {   // Already on win streak
                                result.averageWinStreak = result.timesWon / result.winningStreakCount;
                            } else {                    // New win streak
                                result.winningStreakCount++;
                                result.averageWinStreak = result.timesWon / result.winningStreakCount;
                            }
                            result.winStreak = result.winStreak + existingStreak;

                            if (result.winStreak > result.bestWinStreak) result.bestWinStreak = result.winStreak;
                        }

                        // If user lost, keep the win streak set to 0 and update loss streak info.
                        else {
                            Integer existingLosingStreak = snap.get("loss_streak", Integer.class);

                            result.lossStreak = result.lossStreak + existingLosingStreak;
                            if (result.lossStreak > result.worstLosingStreak) result.worstLosingStreak = result.lossStreak;
                        }
                    }

                    // Reference to game details
                    metricHash.put("game_ref", m.gameRef);

                    // Win results
                    metricHash.put("times_won", result.timesWon);
                    metricHash.put("win_streak", result.winStreak);
                    metricHash.put("best_win_streak", result.bestWinStreak);
                    metricHash.put("average_win_streak", result.averageWinStreak);
                    metricHash.put("win_streak_count", result.winningStreakCount);

                    // Loss results
                    metricHash.put("times_lost", result.timesLost);
                    metricHash.put("loss_streak", result.lossStreak);
                    metricHash.put("worst_losing_streak", result.worstLosingStreak);

                    // Score results
                    metricHash.put("best_score", result.bestScore);
                    metricHash.put("worst_score", result.worstScore);
                    metricHash.put("average_score", result.averageScore);
                    metricHash.put("score_total", result.scoreTotal);

                    // Timestamp results
                    metricHash.put("times_played", result.timesPlayed);
                    metricHash.put("first_time_played", result.firstTimePlayed);
                    metricHash.put("last_time_played", result.lastTimePlayed);

                    gameMetric.set(metricHash).addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully updated user game metrics.");
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user game metrics: " + e.getMessage());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to find user game metrics: " + e.getMessage());
                });


            }
        });

    }

}