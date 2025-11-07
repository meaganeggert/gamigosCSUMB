package com.example.gamigosjava.ui.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

public class CreateEventActivity extends AppCompatActivity {
    String TAG = "Create Event";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Calendar calendar = Calendar.getInstance();

    // Values used for uploading/validation
    private TextView dateTimeText;
    private EditText titleText, notesText;
    private Spinner visibilityDropdown, statusDropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO: Get list of owned games from database.
        List<String> gameList = new ArrayList<>();
        gameList.add("Catan");
        gameList.add("Monopoly");

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        titleText = findViewById(R.id.editText_eventTitle);
        notesText = findViewById(R.id.editTextTextMultiLine_eventNotes);



        //======================================Event Details=======================================
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
                Log.e("Add Friend", "Add friend button not found");
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
                Log.e("Remove Friend", "Remove friend button not found");
            }
        } else {
            Log.e("Friend Layout", "Friend layout not found.");
        }



        // ===================================Match Details=========================================
        // Board Game selection
//        Spinner gameName = findViewById(R.id.dropdown_gameName);
//        if (gameName != null) {
//            setDropdown(gameName, gameList);
//        } else {
//            Log.e("Game Dropdown", "Game name dropdown not found");
//        }



        // =====================================Finish==============================================
        // Finish and upload event details
        View createEventButton = findViewById(R.id.button_createEvent);
        if (createEventButton != null) {
            // TODO: implement form validation, and data mapping.
            createEventButton.setOnClickListener(v -> {
                uploadEvent();
            });
        } else {
            Log.e("Create Event", "Create Event Button not found");
        }
    }

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

//        Log.d(TAG, hostId);
//        Log.d(TAG, title);
//        Log.d(TAG, visibility);
//        Log.d(TAG, status);
//        Log.d(TAG, notes);
//        Log.d(TAG, dateTimeText.getText().toString());
//        Log.d(TAG, createdAt.toString());
//        Log.d(TAG, endedAt.toString());

        Map<String, Object> eventData = new HashMap<>();
//        eventData.put("hostId", hostId);
        eventData.put("hostId", hostId);
//        eventData.put("eventId", 2);
        eventData.put("title", title);
        eventData.put("visibility", visibility);
        eventData.put("status", status);
        eventData.put("notes", notes);
        eventData.put("scheduledAt", scheduledAt);
        eventData.put("createdAt", createdAt);
        eventData.put("endedAt", null);

        db.collection("events")
                .add(eventData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Saved Event: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save event: " + e.getMessage());
                });
    }
}

