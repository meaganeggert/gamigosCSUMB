package com.example.gamigosjava.ui.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Event;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.Image;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.data.model.OnDateTimePicked;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.data.model.UserGameMetric;
import com.example.gamigosjava.data.repository.EventsRepo;
import com.example.gamigosjava.data.repository.FirestoreUtils;
import com.example.gamigosjava.ui.adapter.ImageAdapter;
import com.example.gamigosjava.ui.adapter.MatchAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ViewEventActivity extends BaseActivity {
    private static final String CHANNEL_EVENT_STATUS = "channel_event_status";
    private final String TAG = "View Event";
    private boolean inviteesChanged = false;
    private boolean isPopulatingInvitees = false;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private RecyclerView recyclerView;
    private ArrayAdapter<Friend> friendAdapter;
    private String eventId;
    private List<Friend> friendList;
    private List<String> visibilityList;
    private Date eventStart;
    private final Calendar calendar = Calendar.getInstance();
    private Event eventItem;
    ViewGroup eventContainer;
    List<Match> matches;
    List<MatchSummary> matchSummaryList;
    CollectionReference matchCollectionRef;
    List<DocumentReference> matchDocumentRefList = new ArrayList<>();
    private MatchAdapter matchAdapter;

    Button startEvent, endEvent, deleteEvent, updateEventButton, cancelChanges, addGameButton, photos;
    List<Image> images;
    ImageAdapter imageAdapter;
    private Set<String> originalInviteeUids = new HashSet<>();
    boolean isHost = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        api = BGGService.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_view_event);
        setTopTitle("Event");

        matches = new ArrayList<>();
        matchSummaryList = new ArrayList<>();
        images = new ArrayList<>();

        eventId = getIntent().getStringExtra("selectedEventId");
        Log.d(TAG, "Event ID: " + eventId);

        getMatches(eventId);
        getEventImages();

        recyclerView = findViewById(R.id.recyclerViewMatchGame);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter();
        recyclerView.setAdapter(matchAdapter);

        eventContainer = findViewById(R.id.eventFormContainer);
        initEventForm(R.layout.fragment_event_form, eventContainer.getId());
        getEventDetails(eventId);

        startEvent = findViewById(R.id.button_startEvent);
        endEvent = findViewById(R.id.button_endEvent);
        if (startEvent != null) {
            startEvent.setOnClickListener(v -> {
                if (eventItem.id == null) {
                    Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                eventItem.status = "active";
                eventItem.scheduledAt = Timestamp.now();
                updateEvent();

                //  Show event started notification
                showEventStartedNotification(eventItem);

                startEvent.setEnabled(false);
                endEvent.setEnabled(true);
            });
        }

        if (endEvent != null) {
            endEvent.setOnClickListener(v -> {
                if (eventItem.id == null) {
                    Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                eventItem.status = "past";
                eventItem.endedAt = Timestamp.now();
                updateEvent();
                uploadUserMatchMetrics();
                endEvent.setEnabled(false);
                startEvent.setEnabled(true);
            });
        }

        updateEventButton = findViewById(R.id.button_saveEvent);
        if (updateEventButton != null) {
            updateEventButton.setOnClickListener(v -> {
                if (eventItem.id == null) {
                    Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                getUserInput();
                updateEvent();

                new AlertDialog.Builder(ViewEventActivity.this)
                        .setTitle("Done for the night?")
                        .setMessage("We'll save the details for now. User statistics will update when you end your event.")
                        .setPositiveButton("Yes, we're done", (dialog, which) -> {
                            eventItem.status = "past";
                            eventItem.endedAt = Timestamp.now();
                            updateEvent();
                            uploadUserMatchMetrics();
                            endEvent.setEnabled(false);
                            startEvent.setEnabled(true);
                            dialog.dismiss();
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .show();

//                finish();
            });
        }

        cancelChanges = findViewById(R.id.button_cancelEventChanges);
        if (cancelChanges != null) {
            cancelChanges.setOnClickListener(v -> {
                if (eventItem.id == null) {
                    Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                finish();
            });
        }

        deleteEvent = findViewById(R.id.button_deleteEvent);
        if (deleteEvent != null) {
            deleteEvent.setOnClickListener(v -> {
                if (eventItem.id == null) {
                    Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(ViewEventActivity.this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            //  Cancel local alarm for this event
                            cancelEventStartAlarm(eventItem.id);
                            //  Delete the event doc (cloud function will notify invitees)
                            db.collection("events").document(eventItem.id).delete();
                            finish();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();

            });
        }

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerView.getChildLayoutPosition(view);
                    MatchSummary selectedMatch = matchAdapter.getItemAt(index);
                    String selectedMatchId = "";
                    if (selectedMatch != null) {
                        selectedMatchId = selectedMatch.id;
                    }

                    Intent intent = new Intent(ViewEventActivity.this, ViewMatchActivity.class);
                    intent.putExtra("selectedMatchId", selectedMatchId);
                    intent.putExtra("selectedEventId", eventId);
                    startActivity(intent);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });

        addGameButton = findViewById(R.id.button_addMatch);
        if(addGameButton != null) {
            addGameButton.setOnClickListener(v -> {
                String selectedMatchId = "";
                Intent intent = new Intent(ViewEventActivity.this, ViewMatchActivity.class);
                intent.putExtra("selectedEventId", eventId);
                intent.putExtra("selectedMatchId", selectedMatchId);
                startActivity(intent);
            });
        }

        photos = findViewById(R.id.button_eventPhotos);
        if(photos != null) {
            photos.setOnClickListener(v -> {
                Intent intent = new Intent(ViewEventActivity.this, ImageUploadActivity.class);
                intent.putExtra("selectedEventId", eventId);
                startActivity(intent);
            });
        }

        RecyclerView imagesView = findViewById(R.id.recyclerViewEventImages);
        imagesView.setLayoutManager(new GridLayoutManager(this, 3));
        imageAdapter = new ImageAdapter();
        imageAdapter.setItems(images);
        imagesView.setAdapter(imageAdapter);

        recyclerView = findViewById(R.id.recyclerViewMatchGame);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter();
        recyclerView.setAdapter(matchAdapter);

    }

    private void getEventImages() {
        if (currentUser == null) return;

        db.collection("events")
                .document(eventId)
                .collection("images")
                .addSnapshotListener((snaps, e) -> {
                    if (snaps.isEmpty()) return;

                    images.clear();
                    for (DocumentSnapshot docSnap: snaps) {
                        Image existingImage = new Image();
                        existingImage.imageId = docSnap.getId();
                        existingImage.authorId = docSnap.getString("authorId");
                        existingImage.imageUrl = docSnap.getString("photoUrl");
                        existingImage.uploadedAt = docSnap.getTimestamp("uploadedAt");
                        existingImage.eventId = docSnap.getString("eventId");
                        Log.d("IMAGE", "Adding image: " + existingImage.imageUrl);
                        images.add(existingImage);
                        imageAdapter.setItems(images);
                    }
                });
    }

    private void getUserInput() {
        EditText titleText = eventContainer.findViewById(R.id.editText_eventTitle);
        EditText notesText = eventContainer.findViewById(R.id.editTextTextMultiLine_eventNotes);
        Spinner visibilityDropdown = eventContainer.findViewById(R.id.dropdown_visibility);

        eventItem.title = titleText.getText().toString();
        eventItem.visibility = visibilityDropdown.getSelectedItem().toString().toLowerCase();
        eventItem.notes = notesText.getText().toString();
    }

    private void updateEvent() {
        if (currentUser == null) {
            Toast.makeText(this, "User must be signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventItem.id == null) {
            Toast.makeText(this, "Event hasn't loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("events").document(eventItem.id)
                .set(eventItem)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Successfully updated the event.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Event Updated.");

                    // Recreate local alarm ONLY for planned events
                    cancelEventStartAlarm(eventItem.id);
                    if ("planned".equals(eventItem.status) && eventItem.scheduledAt != null) {
                        long triggerAtMillis = eventItem.scheduledAt.toDate().getTime();
                        scheduleEventStartAlarm(
                                eventItem.id,
                                eventItem.title,
                                currentUser.getDisplayName(),
                                triggerAtMillis
                        );
                    }

                    //  Only rewrite invitees when event is still planned AND the set actually changed
                    if ("planned".equals(eventItem.status) && haveInviteesActuallyChanged()) {
                        CollectionReference invitees = db.collection("events")
                                .document(eventItem.id)
                                .collection("invitees");

                        FirestoreUtils.deleteCollection(db, invitees, 10)
                                .addOnSuccessListener(unused -> uploadFriendInvites())
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to refresh invitees: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update the event: " + e.getMessage());
                    Toast.makeText(this, "Failed to update the event.", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadFriendInvites() {
        List<Friend> friendsInvited = new ArrayList<>();
        LinearLayout friendSection = findViewById(R.id.linearLayout_friend);

        // Collect unique invitees from the spinners
        for (int i = 0; i < friendSection.getChildCount(); i++) {
            Spinner friendSpinner = (Spinner) friendSection.getChildAt(i);
            Friend friendItem = (Friend) friendSpinner.getSelectedItem();

            if (friendItem != null && !friendsInvited.contains(friendItem)) {
                friendsInvited.add(friendItem);
            }
        }

        if (currentUser == null) {
            Log.w(TAG, "uploadFriendInvites: currentUser is null");
            return;
        }

        for (Friend friendItem : friendsInvited) {
            DocumentReference inviteRef = db.collection("events")
                    .document(eventItem.id)
                    .collection("invitees")
                    .document(friendItem.id);  // Stable per-friend doc ID

            Map<String, Object> invite = new HashMap<>();
            invite.put("status", "invited");
            invite.put("userRef", db.collection("users").document(friendItem.friendUId));
            invite.put("eventId", eventItem.id);
            invite.put("eventTitle", eventItem.title);
            invite.put("hostName", currentUser.getDisplayName());
            invite.put("hostId", currentUser.getUid());
            invite.put("scheduledAt", eventItem.scheduledAt); // Firestore Timestamp

            inviteRef.set(invite)
                    .addOnSuccessListener(v -> Log.d(TAG, "Successfully uploaded friend invite for " + friendItem.displayName))
                    .addOnFailureListener(e -> Log.d(TAG, "Failed to upload friend invite: " + e.getMessage()));
        }
    }


    private void getMatches(String eventId) {
        if (currentUser == null) {
            Log.e(TAG, "Failed to get event details: User is not logged in.");
            return;
        }

        matchCollectionRef = db.collection("events")
                .document(eventId)
                .collection("matches");

        matchCollectionRef.addSnapshotListener((snaps, e) -> {
            if (e != null || snaps == null) return;

            matchSummaryList.clear();
            matchDocumentRefList.clear();
            for (DocumentSnapshot snap: snaps) {
                matchDocumentRefList.add(snap.getDocumentReference("matchRef"));
            }

            for (DocumentReference matchDoc: matchDocumentRefList) {
                matchDoc.addSnapshotListener((matchSnap, matchError) -> {
                    if (matchError != null || matchSnap == null) {
                        Log.d(TAG, "Match doesn't have a reference.");
                    } else {
                        Match matchResult = new Match();
                        matchResult.id = matchSnap.getId();

                        matchResult.eventId = matchSnap.getString("eventId");
                        matchResult.notes = matchSnap.getString("notes");
                        matchResult.rulesVariant = matchSnap.getString("rules_variant");
                        matchResult.startedAt = matchSnap.getTimestamp("startedAt");
                        matchResult.endedAt = matchSnap.getTimestamp("endedAt");
                        matchResult.gameRef = matchSnap.getDocumentReference("gameRef");
                        assert matchResult.gameRef != null;
                        matchResult.gameId = matchResult.gameRef.getId();

                        matchResult.playersRef = db
                                .collection("matches")
                                .document(matchResult.id)
                                .collection("players");
                        matches.add(matchResult);

                        getGameDetails(matchResult);
                        Log.d(TAG, "Found match: " + matchResult.id);
                    }
                });
            }
        });
    }

    private void uploadUserMatchMetrics() {
        matches.sort(Comparator.comparing(m -> m.startedAt));

        // Get user reference from players involved in each match.
        for (Match m: matches) {
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
                    if (p.friend.id == null || p.friend.id.isEmpty()) continue;

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

                        gameMetric.set(metricHash).addOnSuccessListener(v -> Log.d(TAG, "Successfully updated user game metrics.")).addOnFailureListener(e -> Log.e(TAG, "Failed to update user game metrics: " + e.getMessage()));
                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to find user game metrics: " + e.getMessage()));


                }
            });

        }
    }




    private void getGameDetails(Match match) {
        DocumentReference gameDoc = match.gameRef;
        if (gameDoc == null) {
            Log.d(TAG, "Game document was null.");
            return;
        }

        match.gameRef.addSnapshotListener((snap, gameError) -> {
            if (snap == null) {
                Log.d(TAG, "Couldn't find game details");
            }
            assert snap != null;
            String title = snap.getString("title");
            String imageUrl = snap.getString("imageUrl");
            Integer maxPlayers = snap.get("maxPlayers", Integer.class);
            Integer minPlayers = snap.get("minPlayers", Integer.class);
            Integer playingTime = snap.get("playingTime", Integer.class);

            MatchSummary matchSummary = new MatchSummary(match.id, eventId, title, imageUrl, minPlayers, maxPlayers, playingTime);

            boolean matchInList = false;
            for (int i = 0; i < matchSummaryList.size(); i++) {
                if (matchSummaryList.get(i).id.equals(matchSummary.id)) {
                    Log.d(TAG, "MATCH WAS FOUND IN LIST FOR GAME DETAILS FUNCTION: " + matchSummary.title);
                    matchInList = true;
                    matchSummaryList.set(i, matchSummary);
                    matchAdapter.setItems(matchSummaryList);
                    break;
                }
            }

            if (!matchInList) {
                matchSummaryList.add(matchSummary);
                matchAdapter.setItems(matchSummaryList);
                Log.d(TAG, "Found game: " + matchSummary.id);
            }
        });

    }

    private void getEventDetails(String eventId) {
        if (currentUser == null) {
            Log.e(TAG, "Failed to get event details: User is not logged in.");
            return;
        }

        DocumentReference eventRef = db.collection("events")
                .document(eventId);

        eventRef.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                Log.e(TAG, "Event " + eventId + " not found.");
                return;
            }

            eventItem = new Event();
            eventItem.id = snap.getId();
            eventItem.createdAt = snap.getTimestamp("createdAt");
            eventItem.endedAt = snap.getTimestamp("endedAt");
            eventItem.scheduledAt = snap.getTimestamp("scheduledAt");
            eventItem.title = snap.getString("title");
            eventItem.visibility = snap.getString("visibility");
            eventItem.status = snap.getString("status");
            eventItem.hostId = snap.getString("hostId");
            eventItem.notes = snap.getString("notes");
            setEventForm(eventItem);

            if(eventItem.status.equals("active")) {
                startEvent.setEnabled(false);
                endEvent.setEnabled(true);
            }

        }).addOnFailureListener(e -> Log.e(TAG, "Error getting event " + eventId + ": " + e.getMessage()));
    }

    // Creates and adds a default event form.
    private void initEventForm(@LayoutRes int layoutRes, @IdRes int containerId) {
        friendList = new ArrayList<>();
        friendAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                friendList
        );
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        getFriends();

        visibilityList = new ArrayList<>();
        visibilityList.add("Private");
        visibilityList.add("Friends");
        visibilityList.add("Public");

        ViewGroup container = findViewById(containerId);
        LayoutInflater.from(this).inflate(layoutRes, container, true);

        // Set up schedule creation
        TextView eventStartText = findViewById(R.id.textView_eventStart);
        Button selectDateButton = findViewById(R.id.button_selectSchedule);
        selectDateButton.setOnClickListener(v -> {
            // When user input for date/time is complete, set the necessary data.
            showDateTimePicker(eventStart, date -> {
                eventStart = date;
                eventStartText.setText(date.toString());
                eventItem.scheduledAt = new Timestamp(date);
                //  If the event was past/active and user picks a future time, set to planned
                long now = System.currentTimeMillis();
                long selectedTime = date.getTime();
                if (selectedTime > now) {
                    if (!"planned".equals(eventItem.status)) {
                        eventItem.status = "planned";
                        eventItem.endedAt = null;
                        Log.d(TAG, "Rescheduled event in the future, setting status = planned");
                    }
                } else {
                    Log.d(TAG, "Event in the past, not rescheduling");
                }
            });
        });

        // Set up the values for the visibility dropdown
        Spinner visibilityDropdown = findViewById(R.id.dropdown_visibility);
        setDropdown(visibilityDropdown, visibilityList);

        // Create first friend dropdown, and allow additional friend dropdowns
        // on "+ friend" button click, or remove friend dropdown on "- Friend"
        // button click.
        LinearLayout friendLayout = findViewById(R.id.linearLayout_friend);
        if (friendLayout != null) {

            // Add additional friend dropdown
            View addFriend = findViewById(R.id.button_addFriend);
            if (addFriend != null) {
                addFriend.setOnClickListener(v -> {
                    if (!friendList.isEmpty() && friendLayout.getChildCount() < friendList.size()) {
                        setFriendDropdown(friendLayout, true);
                        inviteesChanged = true;
                    } else {
                        Toast.makeText(this, "No friend to add.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Add friend button not found");
            }

            // Remove additional friend dropdown
            View removeFriend = findViewById(R.id.button_removeFriend);
            if (removeFriend != null) {
                removeFriend.setOnClickListener(v -> {
                    if (friendLayout.getChildCount() > 0) {
                        removeDropdown(friendLayout);
                        inviteesChanged = true;
                    } else {
                        Toast.makeText(this, "No friend to remove.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Remove friend button not found");
            }
        } else {
            Log.e(TAG, "Friend layout not found.");
        }
    }


    private void setHostUIElements() {
        // Event Form Fragment UI Elements
        EditText title = eventContainer.findViewById(R.id.editText_eventTitle);
        EditText notes = eventContainer.findViewById(R.id.editTextTextMultiLine_eventNotes);
        Button schedule = eventContainer.findViewById(R.id.button_selectSchedule);
        Button addInvitee = eventContainer.findViewById(R.id.button_addFriend);
        Button removeInvitee = eventContainer.findViewById(R.id.button_removeFriend);
        Spinner visibility = eventContainer.findViewById(R.id.dropdown_visibility);
        LinearLayout inviteeLayout = eventContainer.findViewById(R.id.linearLayout_friend);

        // Set visibility for UI elements the host is meant to change.
        if (isHost) {
            startEvent.setVisibility(Button.VISIBLE);
            endEvent.setVisibility(Button.VISIBLE);
            deleteEvent.setVisibility(Button.VISIBLE);
            updateEventButton.setVisibility(Button.VISIBLE);
        } else { // Disable/Remove UI elements for non-hosts
            title.setEnabled(false);
            notes.setEnabled(false);
            schedule.setVisibility(Button.GONE);
            addInvitee.setVisibility(Button.GONE);
            removeInvitee.setVisibility(Button.GONE);
            visibility.setEnabled(false);

            inviteeLayout.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View view, View view1) {
                    view1.setEnabled(false);
                }

                @Override
                public void onChildViewRemoved(View view, View view1) {}
            });

            // UI elements oustide the event form fragment.
            addGameButton.setVisibility(Button.GONE);
            photos.setVisibility(Button.GONE);
            cancelChanges.setText("Back");
        }

        matchAdapter.setIsHost(isHost);
    }


    // Set the event forms UI values to reflect the passed in event.
    private void setEventForm(Event event) {
        EditText title = eventContainer.findViewById(R.id.editText_eventTitle);
        EditText notes = eventContainer.findViewById(R.id.editTextTextMultiLine_eventNotes);
        TextView schedule = eventContainer.findViewById(R.id.textView_eventStart);
        Spinner visibility = eventContainer.findViewById(R.id.dropdown_visibility);

        title.setText(event.title);
        notes.setText(event.notes);
        schedule.setText(event.scheduledAt.toDate().toString());
        visibility.setSelection(visibilityList.indexOf(event.visibility));

        isPopulatingInvitees = true;

        if (currentUser.getUid().equals(event.hostId)) isHost = true;

        setHostUIElements();

        if (event.status.equals("past")) {
            startEvent.setEnabled(false);
            endEvent.setEnabled(false);
            deleteEvent.setEnabled(false);
            updateEventButton.setEnabled(false);

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                Button deleteMatchBtn = recyclerView.getChildAt(i).findViewById(R.id.button_deleteMatch);
                deleteMatchBtn.setEnabled(false);
            }
        }
        //  Clear out any existing invitees
        originalInviteeUids.clear();

        CollectionReference inviteRef = db.collection("events")
                .document(eventId)
                .collection("invitees");

        inviteRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No invitees found.");
                isPopulatingInvitees = false;
                return;
            }

            LinearLayout inviteLayout = eventContainer.findViewById(R.id.linearLayout_friend);
            for (DocumentSnapshot snap: snaps) {
                DocumentReference userRef = snap.getDocumentReference("userRef");
                if (userRef == null) {
                    Log.w(TAG, "Invite doc without userRef");
                    continue;
                }

                // This is the invitee's user UID (users/{uid})
                String inviteeUid = userRef.getId();
                originalInviteeUids.add(inviteeUid);

                userRef.get().addOnSuccessListener(s -> {
                    setFriendDropdown(inviteLayout, false);
                    Spinner friendDropdown =
                            (Spinner) inviteLayout.getChildAt(inviteLayout.getChildCount() - 1);

                    Friend f = new Friend();
                    f.id = s.getId();
                    f.displayName = s.getString("displayName");
                    f.friendUId = s.getString("uid");

                    boolean friendInList = false;
                    for (int i = 0; i < friendList.size(); i++) {
                        if (friendList.get(i).friendUId.equals(f.friendUId)) { // compare by uid
                            friendDropdown.setSelection(i);
                            friendInList = true;
                            break;
                        }
                    }

                    if (!friendInList) {
                        friendList.add(f);
                        friendAdapter.notifyDataSetChanged();
                        friendDropdown.setSelection(friendList.indexOf(f));
                    }
                }).addOnFailureListener(e ->
                        Log.e(TAG, "Failed to get friend invite: " + e.getMessage()));
            }
            isPopulatingInvitees = false;

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get friend invite list: " + e.getMessage());
            isPopulatingInvitees = false;
      });

    }

    private boolean haveInviteesActuallyChanged() {
        LinearLayout friendSection = findViewById(R.id.linearLayout_friend);
        Set<String> currentInviteeUids = new HashSet<>();

        if (friendSection != null) {
            for (int i = 0; i < friendSection.getChildCount(); i++) {
                Spinner friendSpinner = (Spinner) friendSection.getChildAt(i);
                Friend friendItem = (Friend) friendSpinner.getSelectedItem();
                if (friendItem != null && friendItem.friendUId != null) {
                    currentInviteeUids.add(friendItem.friendUId);
                }
            }
        }

        // If the sets differ, then the invitees have changed.
        return !currentInviteeUids.equals(originalInviteeUids);
    }


    // Get current users friend list.
    private void getFriends() {

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        CollectionReference friendsRef = db
                .collection("users")
                .document(uid)
                .collection("friends");

        friendsRef
                .orderBy("displayName")
                .get()
                .addOnSuccessListener(snap -> {

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getId();
                        String friendUid = d.getString("uid");
                        String displayName = d.getString("displayName");

                        Friend f = new Friend(id, friendUid, displayName);

                        boolean inFriendList = false;
                        for (int i = 0; i < friendList.size(); i++) {
                            if (friendList.get(i).id.equals(f.id)) {
                                inFriendList = true;
                                break;
                            }
                        }

                        if (!inFriendList) friendList.add(f);
                    }
                    friendAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Show UI to select date and time.
    private void showDateTimePicker(@Nullable Date initial, OnDateTimePicked callBack) {
        if (initial != null) calendar.setTime(initial);
        // User selects the date
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    // User Selects the time of day
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (timeView, hour, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                // HEADS UP: Here is where the user is done selecting the time.
                                // Because this function uses lambda and such, we cannot just
                                // refer to an outside function, so instead we have to use a
                                // callback. In short, whenever the user is done selecting the
                                // date and time, use the callback to set the passed in date object.
                                callBack.onPicked(calendar.getTime());

                            }, calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE), false); // Show time via am/pm
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    // Adds a dropdown to a linear layout, displaying a selection of the user's friends.
    private void setFriendDropdown(LinearLayout layout, boolean trackChanges) {
        Spinner newSpinner = new Spinner(this);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        newSpinner.setAdapter(friendAdapter);

        if (trackChanges) {
            // ignore the very first onItemSelected (initial population)
            final boolean[] firstCall = { true };

            newSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent,
                                           View view,
                                           int position,
                                           long id) {
                    if (firstCall[0]) {
                        firstCall[0] = false;  // initial programmatic selection, ignore
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                    // do nothing
                }
            });
        } else {
            // No listener needed – this path is now optional
            newSpinner.setOnItemSelectedListener(null);
        }

        layout.addView(newSpinner);
    }



    // Sets the specified dropdown's selection by passing in a list.
    private void setDropdown(Spinner dropdown, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        dropdown.setAdapter(adapter);
    }

    private void removeDropdown(LinearLayout layout) {
        layout.removeViewAt(layout.getChildCount() - 1);
    }

    // Local "event started" notification
    private void showEventStartedNotification(Event event) {
        if (event == null || event.id == null) return;

        createEventStatusChannelIfNeeded();

        String title = "Event is starting now";
        String body = (event.title != null && !event.title.isEmpty())
                ? event.title + " is starting now"
                : "Your event is starting now";

        // Tapping the notification should reopen this event
        Intent intent = new Intent(this, ViewEventActivity.class);
        intent.putExtra("selectedEventId", event.id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = event.id.hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_STATUS)
                        .setSmallIcon(R.drawable.ic_event_24)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }

    private void createEventStatusChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (manager.getNotificationChannel(CHANNEL_EVENT_STATUS) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_EVENT_STATUS,
                    "Event status",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications when your events start");
            manager.createNotificationChannel(channel);
        }
    }

}