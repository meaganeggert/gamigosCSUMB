package com.example.gamigosjava.ui.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.model.Event;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.data.model.OnDateTimePicked;
import com.example.gamigosjava.ui.adapter.GameAdapter;
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
import java.util.Date;
import java.util.List;

public class ViewEventActivity extends BaseActivity {
    private String TAG = "View Event";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    private RecyclerView recyclerView;
    private ArrayAdapter<Friend> friendAdapter;
    private String eventId;
    private List<Friend> friendList;
    private List<String> visibilityList;

    private Date eventStart;

    private Calendar calendar = Calendar.getInstance();

    private Event eventItem;

    ViewGroup eventContainer;

    List<Match> matches;
    List<MatchSummary> matchSummaryList;
    private MatchAdapter matchAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        api = BGGService.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();


        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_view_event);
        setTopTitle("Event");

        matches = new ArrayList<>();
        matchSummaryList = new ArrayList<>();

        eventId = getIntent().getStringExtra("selectedEventId");

        getMatches(eventId);

        recyclerView = findViewById(R.id.recyclerViewMatchGame);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter();
        recyclerView.setAdapter(matchAdapter);

        eventContainer = findViewById(R.id.eventFormContainer);
        initEventForm(R.layout.fragment_event_form, eventContainer.getId());
        getEventDetails(eventId);



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

        Button addGameButton = findViewById(R.id.button_addMatch);
        if(addGameButton != null) {
            addGameButton.setOnClickListener(v -> {
                String selectedMatchId = "";
                Intent intent = new Intent(ViewEventActivity.this, ViewMatchActivity.class);
                intent.putExtra("selectedEventId", eventId);
                intent.putExtra("selectedMatchId", selectedMatchId);
                startActivity(intent);
            });
        }

    }

    private void getMatches(String eventId) {
        if (currentUser == null) {
            Log.e(TAG, "Failed to get event details: User is not logged in.");
            return;
        }

        CollectionReference matchesRef = db.collection("events")
                .document(eventId)
                .collection("matches");

        matchesRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No matches found.");
                return;
            }

            for (DocumentSnapshot snap: snaps) {
                snap.getDocumentReference("matchRef").get().addOnSuccessListener(matchSnap -> {
                    if (matchSnap == null) {
                       Log.d(TAG, "Match doesn't have a reference.");
                    } else {
                        Match matchResult = new Match();
                        matchResult.id = matchSnap.getId();
                        matchResult.gameRef = matchSnap.getDocumentReference("gameRef");
                        matches.add(matchResult);
                        getGameDetails(matchResult.id, matchResult.gameRef);
                        Log.d(TAG, "Found match: " + matchResult.id);
                    }

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get match info: " + e.getMessage());
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get match references: " + e.getMessage());
        });
    }

    private void getGameDetails(String matchId, DocumentReference doc) {
        if (doc == null) return;

        doc.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                Log.d(TAG, "Couldn't find game details");
            }
            String title = snap.getString("title");
            String imageUrl = snap.getString("imageUrl");
            Integer maxPlayers = snap.get("maxPlayers", Integer.class);
            Integer minPlayers = snap.get("minPlayers", Integer.class);
            Integer playingTime = snap.get("playingTime", Integer.class);

            MatchSummary matchSummary = new MatchSummary(matchId, title, imageUrl, minPlayers, maxPlayers, playingTime);
            matchSummaryList.add(matchSummary);
            matchAdapter.setItems(matchSummaryList);
            Log.d(TAG, "Found game: " + matchSummary.id);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get game details: " + e.getMessage());
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

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting event " + eventId + ": " + e.getMessage());
        });
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
                        setFriendDropdown(friendLayout);
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

        CollectionReference inviteRef = db.collection("events")
                .document(eventId)
                .collection("invitees");

        inviteRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No invitees found.");
                return;
            }

            LinearLayout inviteLayout = eventContainer.findViewById(R.id.linearLayout_friend);
            for (DocumentSnapshot snap: snaps) {
                snap.getDocumentReference("userRef").get().addOnSuccessListener(s -> {
                    setFriendDropdown(inviteLayout);
                    Spinner friendDropdown = (Spinner) inviteLayout.getChildAt(inviteLayout.getChildCount() - 1);

                    Friend f = new Friend();
                    f.id = s.getId();
                    f.displayName = s.getString("displayName");
                    f.friendUId = s.getString("uid");

                    boolean friendInList = false;
                    for (int i = 0; i < friendList.size(); i++) {
                        if (friendList.get(i).id.equals(f.id)) {
                            friendDropdown.setSelection(i);
                            friendInList = true;
                            break;
                        }
                    }

                    if (!friendInList)  {
                        friendList.add(f);
                        friendAdapter.notifyDataSetChanged();
                        friendDropdown.setSelection(friendList.indexOf(f));
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get friend invite: " + e.getMessage());
                });
            }

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get friend invite list: " + e.getMessage());
        });
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
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    // Adds a dropdown to a linear layout, displaying a selction of the users friends.
    private void setFriendDropdown(LinearLayout layout) {
        Spinner newSpinner = new Spinner(this);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        newSpinner.setAdapter(friendAdapter);
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
}