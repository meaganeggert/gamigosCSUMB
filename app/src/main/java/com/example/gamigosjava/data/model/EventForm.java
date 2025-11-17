package com.example.gamigosjava.data.model;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Layout;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventForm {
    private String TAG = "Event Form";
    private Calendar calendar = Calendar.getInstance();
    private Context context;
    private ViewGroup container;
    public Event eventItem;


    public Date eventStart; // May not be useful at the moment
    public TextView eventStartText;
    public EditText titleText, notesText;
    public Spinner visibilityDropdown;
    public ArrayAdapter<Friend> friendAdapter;

    public List<String> visibilityList;
    public List<Friend> friendList;

    public EventForm(View parent, Context context, @LayoutRes int newLayoutId, @IdRes int containerId) {
        friendList = new ArrayList<>();
        visibilityList = new ArrayList<>();
        visibilityList.add("Private");
        visibilityList.add("Friends");
        visibilityList.add("Public");

        friendAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                friendList
        );
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        this.context = context;
        this.container = parent.findViewById(containerId);
        LayoutInflater.from(context).inflate(newLayoutId, container, true);

        this.titleText = container.findViewById(R.id.editText_eventTitle);
        this.notesText = container.findViewById(R.id.editTextTextMultiLine_eventNotes);
        this.eventStartText = container.findViewById(R.id.textView_eventStart);
        Button selectDateButton = container.findViewById(R.id.button_selectSchedule);
        selectDateButton.setOnClickListener(v -> {
            // When user input for date/time is complete, set the necessary data.
            showDateTimePicker(eventStart, date -> {
                eventStart = date;
                eventStartText.setText(date.toString());
                eventItem.scheduledAt = new Timestamp(date);
            });
        });

        // Set up the values for the visibility dropdown
        this.visibilityDropdown = container.findViewById(R.id.dropdown_visibility);
        setDropdown(visibilityDropdown, visibilityList);

        // Create first friend dropdown, and allow additional friend dropdowns
        // on "+ friend" button click, or remove friend dropdown on "- Friend"
        // button click.
        LinearLayout friendLayout = container.findViewById(R.id.linearLayout_friend);
        if (friendLayout != null) {

            // Add additional friend dropdown
            View addFriend = container.findViewById(R.id.button_addFriend);
            if (addFriend != null) {
                addFriend.setOnClickListener(v -> {
                    if (!friendList.isEmpty() && friendLayout.getChildCount() < friendList.size()) {
                        setFriendDropdown(friendLayout);
                    } else {
                        Toast.makeText(context, "No friend to add.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Add friend button not found");
            }

            // Remove additional friend dropdown
            View removeFriend = container.findViewById(R.id.button_removeFriend);
            if (removeFriend != null) {
                removeFriend.setOnClickListener(v -> {
                    if (friendLayout.getChildCount() > 0) {
                        removeDropdown(friendLayout);
                    } else {
                        Toast.makeText(context, "No friend to remove.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Remove friend button not found");
            }
        } else {
            Log.e(TAG, "Friend layout not found.");
        }
    }

    public void showDateTimePicker(@Nullable Date initial, OnDateTimePicked callBack) {
        if (initial != null) calendar.setTime(initial);
        // User selects the date
        DatePickerDialog datePicker = new DatePickerDialog(context,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    // User Selects the time of day
                    TimePickerDialog timePicker = new TimePickerDialog(context,
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
    public void setFriendDropdown(LinearLayout layout) {
        Spinner newSpinner = new Spinner(context);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        newSpinner.setAdapter(friendAdapter);
        layout.addView(newSpinner);
    }

    private void setDropdown(Spinner dropdown, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, list);
        dropdown.setAdapter(adapter);
    }

    public void removeDropdown(LinearLayout layout) {
        layout.removeViewAt(layout.getChildCount() - 1);
    }

    public void getEvent(String eventId, FirebaseUser currentUser) {
        if (currentUser == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference result = db.collection("events")
                .document(eventId);

        result.get().addOnSuccessListener(snap -> {
            this.eventItem = new Event();

            eventItem.id = eventId;
            eventItem.createdAt = snap.getTimestamp("createdAt");
            eventItem.endedAt = snap.getTimestamp("endedAt");
            eventItem.scheduledAt = snap.getTimestamp("scheduledAt");
            eventItem.title = snap.getString("title");
            eventItem.visibility = snap.getString("visibility");
            eventItem.status = snap.getString("status");
            eventItem.hostId = snap.getString("hostId");
            eventItem.notes = snap.getString("notes");

            Log.d(TAG, "Successfully pulled event " + snap.getId() + " from database.");

            eventStart = eventItem.scheduledAt.toDate();
            eventStartText.setText(eventStart.toString());
            titleText.setText(eventItem.title);
            notesText.setText(eventItem.notes);

            visibilityDropdown.setSelection(visibilityList.indexOf(eventItem.visibility));

            getFriends(currentUser);
            getInvites(currentUser);
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Failed to pull event from the database: " + e.getMessage());
            Toast.makeText(context, "Failed to get event data.", Toast.LENGTH_SHORT).show();

        });
    }

    public void getInvites(FirebaseUser currentUser) {
        if (currentUser == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        CollectionReference results = db.collection("events")
                .document(eventItem.id)
                .collection("invitees");

        results.get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d(TAG, "Invites are empty");
                        return;
                    }

                    LinearLayout friendLayout = container.findViewById(R.id.linearLayout_friend);
                    for (DocumentSnapshot doc: snap) {
                        DocumentReference user = doc.getDocumentReference("userRef");
                        user.get().addOnSuccessListener(s -> {

                            // Add the dropdown if we found someone that is invited.
                            setFriendDropdown(friendLayout);
                            Spinner dropdown = (Spinner) friendLayout.getChildAt(friendLayout.getChildCount() - 1);

                            // Get the invited friend info.
                            Friend invite = new Friend();
                            for (Friend friend: friendList) {
                                if (friend.id.equals(s.getId())) {
                                    invite = friend;
                                    break;
                                }
                            }

                            // If the invited friend isn't already in the list, add them.
                            if (invite.id == null) {
                                invite.id = s.getId();
                                invite.friendUId = s.getString("uid");
                                invite.displayName = s.getString("displayName");
                                friendList.add(invite);
                                friendAdapter.notifyDataSetChanged();
                            }

                            // Add the dropdown for the friend;
                            dropdown.setSelection(friendList.indexOf(invite));

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to pull friend info: " + e.getMessage());
                        });

                        Log.d(TAG, "Doc thing: " + doc.get("userRef"));
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get friend invites: " + e.getMessage());
                });
    }

    // This gets the users friends from the database and loads it into an array adapter to be used
    // in the dropdown of friends to invite.
    public void getFriends(FirebaseUser currentUser) {

        if (currentUser == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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
                    Toast.makeText(context, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
