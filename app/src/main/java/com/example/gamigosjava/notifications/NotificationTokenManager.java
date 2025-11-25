package com.example.gamigosjava.notifications;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NotificationTokenManager {

    private static final String TAG = "NotificationTokenManager";

    public static void saveTokenForCurrentUser(@Nullable String token) {
        if (token == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);

        db.collection("users")
                .document(uid)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
    }
}