package com.example.gamigosjava.data.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AchievementAwarder {
    private final FirebaseFirestore db;
    private static final String TAG = "AchievementAwarder";

    public AchievementAwarder(FirebaseFirestore db) {
        this.db = db;
    }

    // Task to award login-related achievements
    public Task<List<String>> awardLoginAchievements(String userId) {
        // References
        // Reference to loginCount
        DocumentReference loginCount_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("login_count");
        // Reference to loginStreak
        DocumentReference loginStreak_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("login_streak");
        // Reference to gamesPlayed
        DocumentReference gamesPlayed_met = db.collection("users")
                .document(userId)
                .collection("metrics").document("game_count");

        // Achievement References
        // Get all achievements of group type "LOGIN"
        Task<QuerySnapshot> allLoginAchievements_task = db.collection("achievements")
                .whereEqualTo("group", "LOGIN")
                .whereEqualTo("isActive", true)
                .get();

        // Get all achievements of group type "GAMES"
        Task<QuerySnapshot> allGameAchievements_task = db.collection("achievements")
                .whereEqualTo("group", "GAMES")
                .whereEqualTo("isActive", true)
                .get();

        // Check to see if achievements have already been earned
        Task<QuerySnapshot> userEarnedAchievements_task = db.collection("users").document(userId)
                .collection("achievements")
                .get();

        Task<DocumentSnapshot> loginCount_task = loginCount_Met.get();
        Task<DocumentSnapshot> loginStreak_task = loginStreak_Met.get();
        Task<DocumentSnapshot> gamesPlayed_task = gamesPlayed_met.get();

        // Read all the info from the references
        //* All reads before all writes
        return Tasks.whenAllSuccess(
                loginCount_task,
                loginStreak_task,
                gamesPlayed_task,
                allLoginAchievements_task,
                userEarnedAchievements_task
        ).continueWithTask(t-> {
            // Keep track of snapshots
            DocumentSnapshot loginCount_snap = loginCount_task.getResult();
            DocumentSnapshot loginStreak_snap = loginStreak_task.getResult();
            DocumentSnapshot gamesPlayed_snap = gamesPlayed_task.getResult();

            // Make sure the metrics exist. Otherwise, send 0.
            long loginCount = (loginCount_snap.exists() && loginCount_snap.contains("count")) ? loginCount_snap.getLong("count") : 0L;
            Log.d(TAG, "Count: " + loginCount_snap.getLong("count"));
            long loginCurrent = (loginStreak_snap.exists() && loginStreak_snap.contains("current")) ? loginStreak_snap.getLong("current") : 0L;
            Log.d(TAG, "Current: " + loginStreak_snap.getLong("current"));
            long gameCount = (gamesPlayed_snap.exists() && gamesPlayed_snap.contains("count")) ? gamesPlayed_snap.getLong("count") : 0L;

            QuerySnapshot allLoginAchievements_snap = allLoginAchievements_task.getResult();
            QuerySnapshot userEarnedAchievements_snap = userEarnedAchievements_task.getResult();

            // Create a list to keep track of earned achievements
            // Return this for use with award banners in the desired activity
            List<String> alreadyEarned = new ArrayList<>();
            for (DocumentSnapshot doc : userEarnedAchievements_snap.getDocuments()) {
                Boolean earned = doc.getBoolean("earned");
                if (Boolean.TRUE.equals(earned)) {
                    alreadyEarned.add(doc.getId());
                }
            }

            // Set up the batch
            WriteBatch batch = db.batch();
            int writes = 0;

            // List for newly earned achievements
            List<String> newlyEarned = new ArrayList<>();

            for (DocumentSnapshot docSnap : allLoginAchievements_snap.getDocuments()) {
                String achievementID = docSnap.getId();

                // If already earned, don't add
                if (alreadyEarned.contains(achievementID)) continue;

                String type = docSnap.getString("type");
                String metric = docSnap.getString("metric");
                long goal = docSnap.contains("goal") ? docSnap.getLong("goal") : 1L;
                String name = docSnap.getString("name");

                boolean shouldAward = false;
                long metricValue = 0L;

                // Determine appropriate metricValue
                if ("login_streak".equals(metric)) {
                    metricValue = loginCurrent;
                } else if ("login_count".equals(metric)) {
                    metricValue = loginCount;
                } else if ("game_count".equals(metric)) {
                    metricValue = gameCount;
                }

                if ("FIRST_TIME".equals(type)) {
                    shouldAward = metricValue >= goal;
                } else if ("COUNT".equals(type)) {
                    shouldAward = metricValue >= goal;
                } else if ("STREAK".equals(type)) {
                    shouldAward = metricValue >= goal;
                }

                if (shouldAward) {
                    DocumentReference userAchievement_ref = db.collection("users")
                            .document(userId)
                            .collection("achievements")
                            .document(achievementID);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("earned", true);
                    updates.put("earnedAt", FieldValue.serverTimestamp());

                    batch.set(userAchievement_ref, updates, SetOptions.merge());

                    newlyEarned.add(
                            name != null ? name : achievementID
                    );
                }
            }

            if (!newlyEarned.isEmpty()) {
                return batch.commit().continueWith(x -> newlyEarned);
            } else {
                return Tasks.forResult(newlyEarned); // We don't have anything to update, but return successfully - Works like Promise.resolve()
            }
        });
    }


}
