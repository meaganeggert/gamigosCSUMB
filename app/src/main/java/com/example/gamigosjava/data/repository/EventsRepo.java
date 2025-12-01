package com.example.gamigosjava.data.repository;

import android.util.Log;

import com.example.gamigosjava.data.model.Attendee;
import com.example.gamigosjava.data.model.EventSummary;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

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

    // Function to load all events and their attendees for display in the event page
    public Task<List<EventSummary>> loadAllEventAttendees(boolean searchingForActive, int viewLimit) {
        Timestamp now = Timestamp.now();
        if (searchingForActive) {
            Log.i(TAG, "Loading event details for active events");
            Query query = db.collection("events")
                    .whereGreaterThanOrEqualTo("scheduledAt", now)
                    .orderBy("scheduledAt", Query.Direction.ASCENDING)
                    .limit(viewLimit);

            return query.get()
                    .continueWithTask(task -> {
                        QuerySnapshot eventSnap = task.getResult();

                        List<Task<EventSummary>> taskToLoadEachEvent = new ArrayList<>();

                        for (DocumentSnapshot eventDoc : eventSnap.getDocuments()) {
                            taskToLoadEachEvent.add(loadSingleEventAttendees(eventDoc));
                        }

                        return Tasks.whenAllSuccess(taskToLoadEachEvent);
                    });
        } else {
            Log.i(TAG, "Loading event details for past events");
            Query query = db.collection("events")
                    .whereLessThan("scheduledAt", now)
                    .orderBy("scheduledAt", Query.Direction.DESCENDING)
                    .limit(viewLimit);

            return query.get()
                    .continueWithTask(task -> {
                        QuerySnapshot eventSnap = task.getResult();

                        List<Task<EventSummary>> taskToLoadEachEvent = new ArrayList<>();

                        for (DocumentSnapshot eventDoc : eventSnap.getDocuments()) {
                            taskToLoadEachEvent.add(loadSingleEventAttendees(eventDoc));
                        }

                        return Tasks.whenAllSuccess(taskToLoadEachEvent);
                    });
        }

    }

    private Task<EventSummary> loadSingleEventAttendees (DocumentSnapshot eventDoc) {

        EventSummary event = eventDoc.toObject(EventSummary.class);
        assert event != null;
        event.id = eventDoc.getId();
        String hostId = eventDoc.getString("hostId");
        assert hostId != null;

        Timestamp startTime = eventDoc.getTimestamp("createdAt");
        Timestamp endTime = eventDoc.getTimestamp("endedAt");
        if (startTime != null && endTime != null) {
            long timeDifference = endTime.toDate().getTime() - startTime.toDate().getTime();

            long timeDifferenceInSeconds = timeDifference / 1000;
            long hours = timeDifferenceInSeconds / 3600;
            long minutes = (timeDifferenceInSeconds % 3600) / 60;
            long seconds = timeDifferenceInSeconds % 60;

            Log.i(TAG, "TimeElapsed: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds.");
            String formattedTimeElapsed = String.format("%02d:%02d:%02d", hours, minutes, seconds);
            event.timeElapsed = formattedTimeElapsed;
        } else {
            event.timeElapsed = "Data Unavailable";
        }

        CollectionReference invitees_ref = eventDoc.getReference().collection("invitees");

        return invitees_ref.get().continueWithTask( task -> {
            QuerySnapshot inviteesSnap = task.getResult();

            List<Task<Void>> getAttendeesTasks = new ArrayList<>();

            DocumentReference host_ref = db.collection("users").document(hostId);

            if (host_ref == null) {
                Log.d(TAG, "Host_ref = null");
            } else {

                // Save the info for all of the attendees (event's invitee collection)
                Task<Void> hostTask = host_ref.get().continueWith(hostDocTask -> {
                    DocumentSnapshot hostDocSnap = hostDocTask.getResult();
                    if (hostDocSnap != null && hostDocSnap.exists()) {
                        Attendee host = new Attendee();
                        host.setUserId(hostDocSnap.getId());
                        host.setName(hostDocSnap.getString("displayName"));
                        host.setAvatarUrl(hostDocSnap.getString("photoUrl"));
                        host.setHost(true);
                        event.playersAttending.add(0, host); // Add host to beginning of list
                        Log.d(TAG, "Host: " + host.getName() + " added to event " + event.title);
                    } else {
                        Log.d(TAG, "Error adding host");
                    }
                    return null;
                });
                getAttendeesTasks.add(hostTask);
            }

            for (DocumentSnapshot attendeeDoc : inviteesSnap.getDocuments()) {
                DocumentReference attendee_ref = attendeeDoc.getDocumentReference("userRef");

                if (attendee_ref == null) {
                    Log.d(TAG, "Attendee_ref = null");
                    continue;
                }

                // Save the info for all of the attendees (event's invitee collection)
                Task<Void> attendeeTask = attendee_ref.get().continueWith(attendeeDocTask -> {
                    DocumentSnapshot attendeeDocSnap = attendeeDocTask.getResult();

                    Attendee a = new Attendee();
                    a.setUserId(attendeeDocSnap.getId());
                    a.setName(attendeeDocSnap.getString("displayName"));
                    a.setAvatarUrl(attendeeDocSnap.getString("photoUrl"));
                    Log.d(TAG, "Attendee: " + a.getName() + " added to event " + event.title);

                    event.playersAttending.add(a);
                    return null;
                });

                getAttendeesTasks.add(attendeeTask);
            }

            return Tasks.whenAll(getAttendeesTasks).continueWith(t-> {
                Log.d(TAG, "Final attendee count for event " + event.title + ": " + (event.playersAttending != null ? event.playersAttending.size() : -37));
                return event;
            });
        });
    }
}
