package com.example.gamigosjava.data.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class AchievementAwarder {
    private final FirebaseFirestore db;
    private static final String TAG = "AchievementAwarder";

    public AchievementAwarder(FirebaseFirestore db) {
        this.db = db;
    }

    // Task to award achievements
    public Task<List<String>> awardAchievements(String userId) {
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
        DocumentReference gamesPlayed_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("game_count");
        // Reference to friendsAdded
        DocumentReference friendsAdded_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("friend_count");
        // Reference to userInfo
        DocumentReference userInfo_ref = db.collection("users").document(userId);

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

        // Get all achievements of group type "FRIENDS"
        Task<QuerySnapshot> allFriendAchievements_task = db.collection("achievements")
                .whereEqualTo("group", "FRIENDS")
                .whereEqualTo("isActive", true)
                .get();

        // Check to see if achievements have already been earned
        Task<QuerySnapshot> userEarnedAchievements_task = db.collection("users").document(userId)
                .collection("achievements")
                .get();

        // Get user info
        Task<DocumentSnapshot> userInfo_task = userInfo_ref.get();

        Task<DocumentSnapshot> loginCount_task = loginCount_Met.get();
        Task<DocumentSnapshot> loginStreak_task = loginStreak_Met.get();
        Task<DocumentSnapshot> gamesPlayed_task = gamesPlayed_Met.get();
        Task<DocumentSnapshot> friendsAdded_task = friendsAdded_Met.get();

        // Read all the info from the references
        //* All reads before all writes
        return Tasks.whenAllSuccess(
                loginCount_task,
                loginStreak_task,
                gamesPlayed_task,
                friendsAdded_task,
                allLoginAchievements_task,
                allGameAchievements_task,
                allFriendAchievements_task,
                userEarnedAchievements_task,
                userInfo_task
        ).continueWithTask(t-> {
            // Keep track of snapshots
            DocumentSnapshot loginCount_snap = loginCount_task.getResult();
            DocumentSnapshot loginStreak_snap = loginStreak_task.getResult();
            DocumentSnapshot gamesPlayed_snap = gamesPlayed_task.getResult();
            DocumentSnapshot friendsAdded_snap = friendsAdded_task.getResult();
            DocumentSnapshot userInfo_snap = userInfo_task.getResult();

            // Make sure the metrics exist. Otherwise, send 0.
            long loginCount = (loginCount_snap.exists() && loginCount_snap.contains("count")) ? loginCount_snap.getLong("count") : 0L;
            Log.d(TAG, "Count: " + loginCount_snap.getLong("count"));
            long loginCurrent = (loginStreak_snap.exists() && loginStreak_snap.contains("current")) ? loginStreak_snap.getLong("current") : 0L;
            Log.d(TAG, "Current: " + loginStreak_snap.getLong("current"));
            long gameCount = (gamesPlayed_snap.exists() && gamesPlayed_snap.contains("count")) ? gamesPlayed_snap.getLong("count") : 0L;
            long friendCount = (friendsAdded_snap.exists() && friendsAdded_snap.contains("count")) ? friendsAdded_snap.getLong("count") : 0L;

            QuerySnapshot allLoginAchievements_snap = allLoginAchievements_task.getResult();
            QuerySnapshot allGameAchievements_snap = allGameAchievements_task.getResult();
            QuerySnapshot allFriendAchievements_snap = allFriendAchievements_task.getResult();
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

            // Iterate over login-type achievements
            for (DocumentSnapshot docSnap : allLoginAchievements_snap.getDocuments()) {
                String achievementID = docSnap.getId();

                // If already earned, don't add
                if (alreadyEarned.contains(achievementID)) continue;

                String type = docSnap.getString("type");
                String metric = docSnap.getString("metric");
                long goal = docSnap.contains("goal") ? docSnap.getLong("goal") : 1L;
                String achievementName = docSnap.getString("name");
                String userName = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("displayName") : "Unknown";
                String actorImageUrl = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("photoUrl") : "Unknown";

                boolean shouldAward = false;
                long metricValue = 0L;

                // Determine appropriate metricValue
                if ("login_streak".equals(metric)) {
                    metricValue = loginCurrent;
                } else if ("login_count".equals(metric)) {
                    metricValue = loginCount;
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

                    addAchievementAsFeedActivity(batch, userId, achievementID, achievementName, userName, actorImageUrl);

                    newlyEarned.add(
                            achievementName != null ? achievementName : achievementID
                    );
                }
            }

            // Iterate over game-type achievements
            for (DocumentSnapshot docSnap : allGameAchievements_snap.getDocuments()) {
                String achievementID = docSnap.getId();

                // If already earned, don't add
                if (alreadyEarned.contains(achievementID)) continue;

                String type = docSnap.getString("type");
                String metric = docSnap.getString("metric");
                long goal = docSnap.contains("goal") ? docSnap.getLong("goal") : 1L;
                String achievementName = docSnap.getString("name");
                String userName = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("displayName") : "Unknown";
                String actorImageUrl = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("photoUrl") : "Unknown";

                boolean shouldAward = false;
                long metricValue = 0L;

                // Determine appropriate metricValue
                if ("game_count".equals(metric)) {
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
                    addAchievementAsFeedActivity(batch, userId, achievementID, achievementName, userName, actorImageUrl);

                    newlyEarned.add(
                            achievementName != null ? achievementName : achievementID
                    );
                }
            }

            // Iterate over friend-type achievements
            for (DocumentSnapshot docSnap : allFriendAchievements_snap.getDocuments()) {
                String achievementID = docSnap.getId();

                // If already earned, don't add
                if (alreadyEarned.contains(achievementID)) continue;

                String type = docSnap.getString("type");
                String metric = docSnap.getString("metric");
                long goal = docSnap.contains("goal") ? docSnap.getLong("goal") : 1L;
                String achievementName = docSnap.getString("name");
                String userName = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("displayName") : "Unknown";
                String actorImageUrl = (userInfo_snap != null && userInfo_snap.exists()) ? userInfo_snap.getString("photoUrl") : "Unknown";

                boolean shouldAward = false;
                long metricValue = 0L;

                // Determine appropriate metricValue
                if ("friend_count".equals(metric)) {
                    metricValue = friendCount;
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
                    addAchievementAsFeedActivity(batch, userId, achievementID, achievementName, userName, actorImageUrl);

                    newlyEarned.add(
                            achievementName != null ? achievementName : achievementID
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

    private void addAchievementAsFeedActivity(WriteBatch batch, String userId, String achievementId, String achievementName, String userName, String userPhotoUrl) {
        // Find the right activity doc
        DocumentReference activity_ref = db.collection("activities")
                .document();

        // Store necessary data
        Map<String, Object> newActivity = new HashMap<>();
        newActivity.put("type", "ACHIEVEMENT_EARNED");
        newActivity.put("targetId", achievementId);
        newActivity.put("targetName", achievementName);
        newActivity.put("actorId", userId);
        newActivity.put("actorName", userName);
        newActivity.put("actorImage", userPhotoUrl);
        newActivity.put("visibility", "friends");
        newActivity.put("message", userName.split(" ")[0] + " earned " + achievementName);
        newActivity.put("createdAt", FieldValue.serverTimestamp());

        batch.set(activity_ref, newActivity);
    }

    public Task<List<String>> awardGameSpecificAchievements(String userId, String gameId) {
        // User Ref
        DocumentReference user_Ref = db.collection("users").document(userId);

        // Document Ref
        DocumentReference perGameMetric_Ref = user_Ref
                .collection("gameMetrics")
                .document(gameId);

//        DocumentReference perGameMetric_Ref = user_Ref
//                .collection("metrics")
//                .document("games_played")
//                .collection("game_metrics")
//                .document(gameId);

        // Reads before writes
        Task<DocumentSnapshot> gameMetric_Task = perGameMetric_Ref.get();
        Task<QuerySnapshot> userEarnedAchievements_Task = user_Ref.collection("achievements").get();
        Task<DocumentSnapshot> userInfo_Task = user_Ref.get();

        // Find achievements for this game
        Task<QuerySnapshot> perGameAchievements_Task = db.collection("achievements")
                .whereEqualTo("type", "GAME_PLAY")
                .whereEqualTo("gameId", gameId)
                .whereEqualTo("isActive", true)
                .get();

        return Tasks.whenAllSuccess(
                gameMetric_Task,
                userEarnedAchievements_Task,
                userInfo_Task,
                perGameAchievements_Task
        ).continueWithTask( task -> {
            DocumentSnapshot gameMetric_Snap = gameMetric_Task.getResult();
            QuerySnapshot userEarned_Snap = userEarnedAchievements_Task.getResult();
            DocumentSnapshot userInfo_Snap = userInfo_Task.getResult();
            QuerySnapshot perGameAchieve_Snap = perGameAchievements_Task.getResult();

            long timesPlayed = 0L;

            if (gameMetric_Snap != null && gameMetric_Snap.exists() && gameMetric_Snap.contains("times_played")) {
                timesPlayed = gameMetric_Snap.getLong("times_played");
            }

            // Find already earned achievements
            List<String> alreadyEarned = new ArrayList<>();
            for (DocumentSnapshot doc : userEarned_Snap.getDocuments()) {
                Boolean earned = doc.getBoolean("earned");
                if (Boolean.TRUE.equals(earned)) {
                    alreadyEarned.add(doc.getId());
                }
            }

            WriteBatch batch = db.batch();
            List<String> newlyEarned = new ArrayList<>();

            String userName = (userInfo_Snap != null && userInfo_Snap.exists()) ? userInfo_Snap.getString("displayName") : "Unknown";
            String userPhoto = (userInfo_Snap != null && userInfo_Snap.exists()) ? userInfo_Snap.getString("photoUrl") : "";

            for (DocumentSnapshot doc_Snap : perGameAchieve_Snap.getDocuments()) {
                String achievementId = doc_Snap.getId();
                if (alreadyEarned.contains(achievementId)) continue; // If we already earned this, skip

                String thisMetric = doc_Snap.getString("metric"); // the metric for this achievement
                long thisGoal = doc_Snap.contains("goal") ? doc_Snap.getLong("goal") : 1L; // the goal for this achievement
                String thisAchievementMessage = userName + doc_Snap.getString("description"); // the description for this achievement
                String thisType = doc_Snap.getString("type"); // the type - currently only GAME_PLAY

                long metricValue = 0L;
                if ("times_played".equals(thisMetric)) {
                    metricValue = timesPlayed;
                }
                // TODO: add option for GAME_STREAK

                // For count-based achievements
                boolean shouldAward = metricValue >= thisGoal;
                // TODO: add functionality for GAME_STREAK

                if (shouldAward) {
                    DocumentReference userAchievement_Ref = user_Ref
                            .collection("achievements")
                            .document(achievementId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("earned", true);
                    updates.put("earnedAt", FieldValue.serverTimestamp());

                    batch.set(userAchievement_Ref, updates, SetOptions.merge());

                    addAchievementAsFeedActivity(batch, userId, achievementId, thisAchievementMessage, userName, userPhoto);

                    newlyEarned.add(thisAchievementMessage != null ? thisAchievementMessage : achievementId);
                }
            }
            if (!newlyEarned.isEmpty()) {
                return batch.commit().continueWith(x -> newlyEarned);
            } else {
                return Tasks.forResult(newlyEarned);
            }
        });
    }

}
