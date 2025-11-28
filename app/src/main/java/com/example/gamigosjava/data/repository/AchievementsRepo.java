package com.example.gamigosjava.data.repository;

import android.os.LocaleList;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementsRepo {

    private static final String TAG = "AchievementsRepo";

    public static final String M_LOGIN_COUNT = "login_count";
    public static final String M_GAME_COUNT = "game_count";
    public static final String M_LOGIN_STREAK = "login_streak";

    private final FirebaseFirestore db;

    public AchievementsRepo (FirebaseFirestore db) {
        this.db = db;
    }

    //    Make sure user has the minimal metrics needed for achievement tracking
//    Call after sign-in to ensure the document exists and increment by 1 (if appropriate)
    public Task<Void> ensureMetrics(String userId) {
        Map<String, Map<String, Object>> requiredMetrics = new HashMap<>();

        // ensure game_count metric
        requiredMetrics.put("game_count", new HashMap<String, Object>() {{
            put("count", 0L);
        }});

        // ensure friend_count metric
        requiredMetrics.put("friend_count", new HashMap<String, Object>() {{
            put("count", 0L);
        }});

        // Task to read all current metrics
        List<Task<DocumentSnapshot>> reads = new ArrayList<>();
        for (String key : requiredMetrics.keySet()) {
            DocumentReference currentMetrics = db.collection("users")
                    .document(userId)
                    .collection("metrics")
                    .document(key);

            reads.add(currentMetrics.get());
        }

        // Write missing metrics
        return Tasks.whenAllSuccess(reads).continueWithTask(t-> {
                    WriteBatch batch = db.batch();
                    List<Object> resultsObj = t.getResult();
                    List<DocumentSnapshot> userMetrics_snap = new ArrayList<>();
                    for (Object o : resultsObj) {
                        userMetrics_snap.add((DocumentSnapshot) o);
                    }
                    int index = 0;

                    for (String key : requiredMetrics.keySet()) {
                        DocumentSnapshot newMetric_snap = userMetrics_snap.get(index++);
                        if (!newMetric_snap.exists()) {
                            DocumentReference ref = newMetric_snap.getReference();
                            batch.set(ref, requiredMetrics.get(key));
                        }
                    }

                    if (batch == null) {
                        return Tasks.forResult(null);
                    } else {
                        return batch.commit();
                    }

                })
                // after initializing metrics, make sure the friend count is accurate
                .continueWithTask(t-> friendTracker(userId));
    }

    public Task<Void> friendTracker(String userId) {
        // Reference to user's friend list
        return db.collection("users")
                .document(userId)
                .collection("friends")
                .get()
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) {
                        throw t.getException();
                    }

                    int friendCount = t.getResult().size();

                    // Reference to friendCount metric
                    DocumentReference friendCount_Ref = db.collection("users")
                            .document(userId)
                            .collection("metrics")
                            .document("friend_count");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("count", (long) friendCount);

                    return friendCount_Ref.set(updates, SetOptions.merge());
                });
    }

    public Task<Void> loginTracker(String userId) {
        // Reference to loginCount
        DocumentReference loginCountRef = db.collection("users")
                .document(userId)
                .collection("metrics").document(M_LOGIN_COUNT);
        // Reference to loginStreak
        DocumentReference loginStreakRef = db.collection("users")
                .document(userId)
                .collection("metrics").document(M_LOGIN_STREAK);

        return db.runTransaction(t -> {
            LocalDate today = LocalDate.now();
            String todayString = today.toString();
            Log.i(TAG, "Today's Date: " + today);

            // Snapshot of login streak data
            DocumentSnapshot streakSnap = t.get(loginStreakRef);
            // Retrieve last login date
            Timestamp lastUpdate = streakSnap.getTimestamp("updatedAt");
            long currentStreak = streakSnap.exists() && streakSnap.contains("current") ? streakSnap.getLong("current") : 1L;
            long bestStreak = streakSnap.exists() && streakSnap.contains("best") ? streakSnap.getLong("best") : 1L;
            if (lastUpdate == null) {
                lastUpdate = Timestamp.now();
            }
            LocalDate lastLoginDay = lastUpdate.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            boolean sameDay = today.isEqual(lastLoginDay);
            boolean nextDay = today.equals(lastLoginDay.plusDays(1));
            Log.i(TAG, "SameDay: " + sameDay + " NextDay: " + nextDay);

            if (nextDay) {
                currentStreak += 1;
            } else if ( !sameDay ){
                currentStreak = 1; // Not the first time, not part of a streak
            }

            // Update the longest streak if the new streak is better
            bestStreak = Math.max(bestStreak, currentStreak);

            // Snapshot of login count data
            DocumentSnapshot countSnap = t.get(loginCountRef);
            long count = countSnap.exists() && countSnap.contains("count") ? countSnap.getLong("count") : 0L;

            if (!sameDay) {
                count += 1;
                Map<String, Object> countUpdates = new HashMap<>();
                countUpdates.put("count", count);
                countUpdates.put("lastDay", FieldValue.serverTimestamp());
                countUpdates.put("updatedAt", FieldValue.serverTimestamp());
                t.set(loginCountRef, countUpdates, SetOptions.merge());
            }

            // Update streak document
            Map<String, Object> streakUpdates = new HashMap<>();
            streakUpdates.put("current", currentStreak);
            streakUpdates.put("best", bestStreak);
            streakUpdates.put("lastDay", FieldValue.serverTimestamp());
            streakUpdates.put("updatedAt", FieldValue.serverTimestamp());
            t.set(loginStreakRef, streakUpdates, SetOptions.merge());
            return null;
        });

    }

}