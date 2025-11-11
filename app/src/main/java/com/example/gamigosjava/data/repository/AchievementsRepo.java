package com.example.gamigosjava.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class AchievementsRepo {

    private static final String TAG = "AchievementsRepo";

    public static final String M_LOGIN_COUNT = "login_count";

    private final FirebaseFirestore db;

    public AchievementsRepo (FirebaseFirestore db) {
        this.db = db;
    }

//    Make sure user has the minimal metrics needed for achievement tracking
//    Call after sign-in to ensure the document exists and increment by 1
    public Task<Void> checkAndIncrementLoginCount(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("updatedAt", FieldValue.serverTimestamp());
        updates.put("count", FieldValue.increment(1));
        return db.collection("users")
                .document(userId)
                .collection("metrics").document(M_LOGIN_COUNT)
                .set(updates, SetOptions.merge());
    }

}
