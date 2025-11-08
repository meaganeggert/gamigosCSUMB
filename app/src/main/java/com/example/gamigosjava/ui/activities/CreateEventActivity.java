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
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gamigosjava.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateEventActivity extends BaseActivity {
    String TAG = "Create Event";
    FirebaseFirestore db;
    private FirebaseAuth auth;


    // Handle on match forms
    private LinearLayout matchFormContainerHandle;


    // Values used for uploading/validation
    private Calendar calendar = Calendar.getInstance();
    private TextView dateTimeText;
    private EditText titleText, notesText;
    private Spinner visibilityDropdown, statusDropdown;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // TODO: Get list of owned games from database.
        List<String> gameList = new ArrayList<>();
        gameList.add("Catan");
        gameList.add("Monopoly");

        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_create_event);

        // Set title for NavBar
        setTopTitle("Create Event");

        // ===================================Event Details=========================================
        // Add event form to the page and set the needed textViews/buttons/dropdowns/etc.
        setChildLayoutForm(R.layout.fragment_event_form, R.id.eventFormContainer);


        // ===================================Match Details=========================================
        matchFormContainerHandle = findViewById(R.id.matchFormContainer);

        // Add new match details form on button click.
        Button addMatchButton = findViewById(R.id.button_addMatch);
        if (addMatchButton != null) {
            addMatchButton.setOnClickListener(v -> {
                addMatchForm();

                // Set board game dropdown for each new match form.
                Spinner gameName = matchFormContainerHandle
                        .getChildAt(matchFormContainerHandle.getChildCount()-1)
                        .findViewById(R.id.dropdown_gameName);

                if (gameName != null) {
                    setDropdown(gameName, gameList);
                } else {
                    Log.e(TAG, "Game name dropdown not found");
                }
                });
        } else {
            Log.e(TAG, "Match creation button not found");
        }

        // Remove match details form on button click only if a match form was added.
        Button removeMatchButton = findViewById(R.id.button_removeMatch);
        if (removeMatchButton != null) {
            removeMatchButton.setOnClickListener(v -> {
                if (matchFormContainerHandle.getChildCount() > 0) {
                    removeMatchForm();
                } else {
                    Toast.makeText(this, "No matches to remove.", Toast.LENGTH_SHORT).show();
                }

            });
        } else {
            Log.e(TAG, "Match creation button not found");
        }



        // =====================================Finish==============================================
        // Finish and upload event details
        View createEventButton = findViewById(R.id.button_createEvent);
        if (createEventButton != null) {
            createEventButton.setOnClickListener(v -> {
                uploadEvent();
            });
        } else {
            Log.e(TAG, "Create Event Button not found");
        }
    }




    // =======================================Layout Helpers========================================
    private void setDropdown(Spinner dropdown, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        dropdown.setAdapter(adapter);
    }

    private void addDropdown(LinearLayout layout, List<String> list) {
        Spinner newSpinner = new Spinner(this);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        setDropdown(newSpinner, list);
        layout.addView(newSpinner);
    }

    private void removeDropdown(LinearLayout layout) {
        layout.removeViewAt(layout.getChildCount() - 1);
    }

    private void showDateTimePicker() {
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

                                // Display the selected datetime to user
                                dateTimeText.setText(calendar.getTime().toString());

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

    private void setChildLayoutForm(@LayoutRes int layoutRes, @IdRes int containerId) {
        ViewGroup container = findViewById(containerId);
        LayoutInflater.from(this).inflate(layoutRes, container, true);

        titleText = findViewById(R.id.editText_eventTitle);
        notesText = findViewById(R.id.editTextTextMultiLine_eventNotes);

        // Set up schedule creation
        dateTimeText = findViewById(R.id.textView_eventStart);
        Button selectDateButton = findViewById(R.id.button_selectSchedule);
        selectDateButton.setOnClickListener(v -> showDateTimePicker());

        // Set up the values for the visibility dropdown
        List<String> visibilityList = new ArrayList<>();
        visibilityList.add("Private");
        visibilityList.add("Friends");
        visibilityList.add("Public");

        visibilityDropdown = findViewById(R.id.dropdown_visibility);
        setDropdown(visibilityDropdown, visibilityList);

        // Set up the values for the status dropdown
        List<String> statusList = new ArrayList<>();
        statusList.add("Planned");
        statusList.add("In-Progress");
        statusList.add("Complete");

        statusDropdown = findViewById(R.id.dropdown_status);
        setDropdown(statusDropdown, statusList);

        // TODO: Get list of friend's from database.
        List<String> friendList = new ArrayList<>();
        friendList.add("Alice");
        friendList.add("Bob");
        friendList.add("Charlie");

        // Create first friend dropdown, and allow additional friend dropdowns
        // on "+ friend" button click, or remove friend dropdown on "- Friend"
        // button click.
        LinearLayout friendLayout = findViewById(R.id.linearLayout_friend);
        if (friendLayout != null) {
            addDropdown(friendLayout, friendList);  // initial friend dropdown

            // Add additional friend dropdown
            View addFriend = findViewById(R.id.button_addFriend);
            if (addFriend != null) {
                addFriend.setOnClickListener(v -> {
                    if (!friendList.isEmpty()) {
                        addDropdown(friendLayout, friendList);
                    } else {
                        Toast.makeText(this, "Friends not found.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Add friend button not found");
            }

            // Remove additional friend dropdown
            View removeFriend = findViewById(R.id.button_removeFriend);
            if (removeFriend != null) {
                removeFriend.setOnClickListener(v -> {
                    if (friendLayout.getChildCount() > 1) {
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

    private void addMatchForm() {
        View match = LayoutInflater.from(this).inflate(R.layout.fragment_match_form, matchFormContainerHandle, false);
        matchFormContainerHandle.addView(match);
    }

    private void removeMatchForm() {
        matchFormContainerHandle.removeViewAt(matchFormContainerHandle.getChildCount() - 1);
    }




    // ===================================Database Helpers==========================================
    private void uploadEvent() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "Must be logged in.");
            return;
        }

        String hostId = auth.getUid();
        String title = titleText.getText().toString();
        String visibility = visibilityDropdown.getSelectedItem().toString();
        String status = statusDropdown.getSelectedItem().toString();
        String notes = notesText.getText().toString();
        Timestamp scheduledAt = new Timestamp(calendar.getTime());
        Timestamp createdAt = Timestamp.now();
        Timestamp endedAt = null;

        if (title.isEmpty()) {
            Toast.makeText(this, "Title Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dateTimeText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Schedule Required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("hostId", hostId);
        eventData.put("title", title);
        eventData.put("visibility", visibility);
        eventData.put("status", status);
        eventData.put("notes", notes);
        eventData.put("scheduledAt", scheduledAt);
        eventData.put("createdAt", createdAt);
        eventData.put("endedAt", endedAt);

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Saved Event: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save event: " + e.getMessage());
                });
    }

    private void uploadMatch() {
        //TODO: implement match uploads
    }

    private void uploadFriendInvites() {
        //TODO: implement friend invite uploads
    }
}