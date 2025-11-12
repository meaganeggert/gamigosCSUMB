package com.example.gamigosjava.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AchievementAwarder {
    private final FirebaseFirestore db;

    public AchievementAwarder(FirebaseFirestore db) {
        this.db = db;
    }

    // Task to award login-related achievements
    public Task<Void> awardLoginAchievements(String userId) {
        // References
        // Reference to loginCount
        DocumentReference loginCount_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("login_count");
        // Reference to loginStreak
        DocumentReference loginStreak_Met = db.collection("users")
                .document(userId)
                .collection("metrics").document("login_streak");

        // Achievement References
        // Reference to firstLogin
        DocumentReference firstLogin_Achieve = db.collection("achievements").document("first_login");
        // Reference to loginStreak3
        DocumentReference streak3_Achieve = db.collection("achievements").document("streak_3");

        // Check to see if achievements have already been earned
        DocumentReference earned_firstLogin = db.collection("users").document(userId)
                .collection("achievements")
                .document("first_login");
        DocumentReference earned_streak3 = db.collection("users").document(userId)
                .collection("achievements")
                .document("streak_3");

        // Read all the info from the references
        //* All reads before all writes
        return Tasks.whenAllSuccess(
                loginCount_Met.get(),
                loginStreak_Met.get(),
                firstLogin_Achieve.get(),
                streak3_Achieve.get(),
                earned_firstLogin.get(),
                earned_streak3.get()
        ).continueWithTask(t-> {
            List<Object> results = t.getResult();
            // Keep track of snapshots
            DocumentSnapshot loginCountMet_snap = (DocumentSnapshot) results.get(0);
            DocumentSnapshot loginStreakMet_snap = (DocumentSnapshot) results.get(1);
            DocumentSnapshot firstLoginAchieve_snap = (DocumentSnapshot) results.get(2);
            DocumentSnapshot streak3Achieve_snap = (DocumentSnapshot) results.get(3);
            DocumentSnapshot firstLoginEarned_snap = (DocumentSnapshot) results.get(4);
            DocumentSnapshot streak3Earned_snap = (DocumentSnapshot) results.get(5);

            // Make sure the metrics exist. Otherwise, send 0.
            long count = (loginCountMet_snap.exists() && loginCountMet_snap.contains("count")) ? loginCountMet_snap.getLong("count") : 0L;
            long current = (loginCountMet_snap.exists() && loginCountMet_snap.contains("current")) ? loginCountMet_snap.getLong("current") : 0L;

            // Set up the batch
            WriteBatch batch = db.batch();
            int writes = 0;

            // Achievement - first_login
            if (firstLoginAchieve_snap.exists() && firstLoginAchieve_snap.getBoolean("isActive") == true) {
                long goal = firstLoginAchieve_snap.contains("goal") ? firstLoginAchieve_snap.getLong("goal") : 1L;
                if (!firstLoginEarned_snap.exists() && count >= goal) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("earned", true);
                    updates.put("earnedAt", FieldValue.serverTimestamp());
                    batch.set(earned_firstLogin, updates, SetOptions.merge());
                    writes++;
                }
            }

            // Achievement - streak_3
            if (streak3Achieve_snap.exists() && streak3Achieve_snap.getBoolean("isActive")) {
                long goal = streak3Achieve_snap.contains("goal") ? streak3Achieve_snap.getLong("goal") : 3L;
                if (!streak3Earned_snap.exists() && current >= goal) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("earned", true);
                    updates.put("earnedAt", FieldValue.serverTimestamp());
                    batch.set(earned_streak3, updates, SetOptions.merge());
                    writes++;
                }
            }

            if (writes > 0) {
                return batch.commit();
            } else {
                return Tasks.forResult(null); // We don't have anything to update, but return successfully - Works like Promise.resolve()
            }
        });
    }


}
