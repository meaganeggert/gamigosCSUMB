package com.example.gamigosjava.data.repository;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EventsRepo {
    private static final String TAG = "Events Repo";

    private final FirebaseFirestore db;

    public EventsRepo(FirebaseFirestore db) {this.db = db;}

    public Task<Void> deleteEvent(DocumentReference eventRef) {
        // delete last
        String eventId = eventRef.getId();

        // can delete early
        CollectionReference inviteesRef = db.collection("events").document(eventId).collection("invitees");

        // must get match references first
        CollectionReference matchesRef = db.collection("events").document(eventId).collection("matches");
        List<Task<Void>> subTasks = new ArrayList<>();

        matchesRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "Event matches are not found.");
                subTasks.add(FirestoreUtils.deleteCollection(db, matchesRef, 10));
                subTasks.add(FirestoreUtils.deleteCollection(db, inviteesRef, 10));
                return;
            }

            List<Task<Void>> matchTasks = new ArrayList<>();
            for (DocumentSnapshot matchSnap: snaps) {
                Log.d(TAG, "Deleting match: " + matchSnap.getId());
                DocumentReference matchRef = matchSnap.getDocumentReference("matchRef");
                matchTasks.add(matchRef.delete());
            }

            Tasks.whenAll(matchTasks).onSuccessTask(subV -> {
                Log.d(TAG, "Deleting event invite and match references.");
                subTasks.add(FirestoreUtils.deleteCollection(db, matchesRef, 10));
                subTasks.add(FirestoreUtils.deleteCollection(db, inviteesRef, 10));
                return null;
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get event matches.");
        });

        return Tasks.whenAll(subTasks).onSuccessTask(v -> {
//            if (subTasks.isEmpty()) {
//                Log.e(TAG, "Failed to delete event.");
//                return null;
//            }

            eventRef.delete();
            Log.d(TAG, "Successfully deleted event: " + eventId);
            return null;
        });
//        return Tasks.whenAll(subTasks).onSuccessTask(v -> eventRef.delete());
    }
}
