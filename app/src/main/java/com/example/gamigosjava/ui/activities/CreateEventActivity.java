package com.example.gamigosjava.ui.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import androidx.annotation.Nullable;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Event;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.OnDateTimePicked;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEventActivity extends BaseActivity {
    String TAG = "Create Event";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;


    // Values used for uploading/validation
    private Calendar calendar = Calendar.getInstance();
    private Event eventItem;
    private Date eventStart; // May not be useful at the moment
    private TextView eventStartText;
    private EditText titleText, notesText;
    private Spinner visibilityDropdown;
    private ArrayAdapter<Friend> friendAdapter;

    List<String> visibilityList;
    List<Friend> friendList;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        eventItem = new Event();
        friendList = new ArrayList<>();
        visibilityList = new ArrayList<>();
        visibilityList.add("Private");
        visibilityList.add("Friends");
        visibilityList.add("Public");

        friendAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                friendList
        );
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Important to make these two calls after the adapters have been set.
        getFriends();

        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_create_event);

        // Set title for NavBar
        setTopTitle("Create Event");


        // ===================================Event Details=========================================
        // Add event form to the page and set the needed textViews/buttons/dropdowns/etc.
        setChildLayoutForm(R.layout.fragment_event_form, R.id.eventFormContainer);


        // =====================================Finish==============================================
        // Finish and upload event details
        View createEventButton = findViewById(R.id.button_createEvent);
        if (createEventButton != null) {
            createEventButton.setOnClickListener(v -> {
                uploadAllForms();
            });
        } else {
            Log.e(TAG, "Create Event Button not found");
        }

        Button cancel = findViewById(R.id.button_cancelCreateEvent);
        if (cancel != null) {
            cancel.setOnClickListener(v -> finish());
        }
    }




    // =======================================Main Form Function Helpers============================
    private void setChildLayoutForm(@LayoutRes int layoutRes, @IdRes int containerId) {
        ViewGroup container = findViewById(containerId);
        LayoutInflater.from(this).inflate(layoutRes, container, true);

        titleText = findViewById(R.id.editText_eventTitle);
        notesText = findViewById(R.id.editTextTextMultiLine_eventNotes);

        // Set up schedule creation
        eventStartText = findViewById(R.id.textView_eventStart);
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
        visibilityDropdown = findViewById(R.id.dropdown_visibility);
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



    // ===================================Database Helpers==========================================

    // This function was originally meant to to upload the event form to firebase separately
    // from the matches forms, but we want the matches to upload after, only on success of uploading
    // the event. So at the bottom, in the .onSuccessListener the uploadMatches function will be called.
    private void uploadAllForms() {
        if (currentUser == null) {
            Log.e(TAG, "Must be logged in.");
            return;
        }

        eventItem.hostId = currentUser.getUid();
        eventItem.title = titleText.getText().toString();
        eventItem.visibility = visibilityDropdown.getSelectedItem().toString().toLowerCase();
        eventItem.status = "planned";
        eventItem.notes = notesText.getText().toString();
        eventItem.createdAt = Timestamp.now();
        eventItem.endedAt = null;

        // Small validation
        if (eventItem.title.isEmpty()) {
            Toast.makeText(this, "Title Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventStartText.getText().toString().isEmpty()) {
            eventItem.scheduledAt = Timestamp.now();
        }

        // Connect user values to hashmap to upload.
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("hostId", eventItem.hostId);
        eventData.put("title", eventItem.title);
        eventData.put("visibility", eventItem.visibility);
        eventData.put("status", eventItem.status);
        eventData.put("notes", eventItem.notes);
        eventData.put("scheduledAt", eventItem.scheduledAt);
        eventData.put("createdAt", eventItem.createdAt);
        eventData.put("endedAt", eventItem.endedAt);

        // Upload the event then upload the match if the event was uploaded successfully.
        db.collection("events")
                .add(eventData)

                // only when the event gets uploaded, should we upload the matches, friend invites, etc.
                .addOnSuccessListener(documentReference -> {
                    eventItem.id = documentReference.getId();
                    Log.d(TAG, "Saved Event: " + eventItem.id);
                    Toast.makeText(this, "Event uploaded successfully.", Toast.LENGTH_SHORT).show();
                    uploadFriendInvites();
//                    uploadMatches();
                    addEventAsFeedActivity(eventItem.id, eventItem.title, currentUser.getUid(), currentUser.getDisplayName(), eventItem.visibility);
                    finish();

                })

                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save event: " + e.getMessage());
                    Toast.makeText(this, "Failed to save event.", Toast.LENGTH_SHORT).show();
                });
    }

    private void addEventAsFeedActivity(String eventId, String eventName, String userId, String userName, String eventVisibility) {

        // Find the right activity doc
        DocumentReference activity_ref = db.collection("activities")
                .document();

        // Store necessary data
        Map<String, Object> newActivity = new HashMap<>();
        newActivity.put("type", "EVENT_CREATED");
        newActivity.put("targetId", eventId);
        newActivity.put("targetName", eventName);
        newActivity.put("actorId", userId);
        newActivity.put("actorName", userName);
        newActivity.put("visibility", eventVisibility);
        newActivity.put("message", userName.split(" ")[0] + " created " + eventName);
        newActivity.put("createdAt", FieldValue.serverTimestamp());

        activity_ref.set(newActivity)
                .addOnSuccessListener(v-> {
                    Log.d(TAG, "Event created as an activity for feed display");
                })
                .addOnFailureListener(e-> {
                    Log.e(TAG, "Failed to add event as an activity.");
                });
    }

    // upload friend invites to database
    private void uploadFriendInvites() {
        List<Friend> friendsInvited = new ArrayList<>();
        LinearLayout friendSection = findViewById(R.id.linearLayout_friend);

        // Filters out repeated invites.
        for (int i = 0; i < friendSection.getChildCount(); i++) {
            Spinner friendSpinner = (Spinner) friendSection.getChildAt(i);
            Friend friendItem = (Friend) friendSpinner.getSelectedItem();

            if (!friendsInvited.contains(friendItem)) {
                friendsInvited.add(friendItem);
            }
        }

        // Uploads friend invites to the database one by one.
        for (int i = 0; i < friendsInvited.size(); i++) {
            Friend friendItem = friendsInvited.get(i);

            DocumentReference inviteRef = db.collection("events")
                    .document(eventItem.id)
                    .collection("invitees")
                    .document(friendItem.id);

            Map<String, Object> invite = new HashMap<>();
            invite.put("status", "invited");
            invite.put("userRef", db.collection("users").document(friendItem.friendUId));

            inviteRef.set(invite).addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully uploaded friend invites");
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to upload friend invite: " + e.getMessage());
                    });
        }
    }



    // This gets the users friends from the database and loads it into an array adapter to be used
    // in the dropdown of friends to invite.
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
                    friendList.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getId();
                        String friendUid = d.getString("uid");
                        String displayName = d.getString("displayName");
                        friendList.add(new Friend(id, friendUid, displayName));
                    }
                    friendAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    // =======================================Global Function Helpers========================================
    private void setDropdown(Spinner dropdown, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        dropdown.setAdapter(adapter);
    }

    private void removeDropdown(LinearLayout layout) {
        layout.removeViewAt(layout.getChildCount() - 1);
    }

    // Show the user an interface to select a date/time
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

    // Set the friend list dropdown values to users friends
    private void setFriendDropdown(LinearLayout layout) {
        Spinner newSpinner = new Spinner(this);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        newSpinner.setAdapter(friendAdapter);
        layout.addView(newSpinner);
    }
}