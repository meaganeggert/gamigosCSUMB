package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gamigosjava.R;

import java.util.ArrayList;
import java.util.List;

public class CreateEventActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // TODO: Get list of friend's from database.
        List<String> friendList = new ArrayList<>();
        friendList.add("Alice");
        friendList.add("Bob");
        friendList.add("Charlie");

        // TODO: Get list of owned games from database.
        List<String> gameList = new ArrayList<>();
        gameList.add("Catan");
        gameList.add("Monopoly");



        //======================================Event Details=======================================
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
                    addDropdown(friendLayout, friendList);
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
                        Toast.makeText(this, "Must invite at least one person.", Toast.LENGTH_SHORT).show();
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
        Spinner gameName = findViewById(R.id.dropdown_gameName);
        if (gameName != null) {
            setDropdown(gameName, gameList);
        } else {
            Log.e("Game Dropdown", "Game name dropdown not found");
        }



        // =====================================Finish==============================================
        // Finish and upload event details
        View createEventButton = findViewById(R.id.button_createEvent);
        if (createEventButton != null) {
            // TODO: implement form validation, and data mapping.
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
}

