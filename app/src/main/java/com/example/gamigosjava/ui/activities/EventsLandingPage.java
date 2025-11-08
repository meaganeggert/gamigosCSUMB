package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EventsLandingPage extends BaseActivity {

    private static final String TAG = "PastEvents";
    private RecyclerView recyclerViewActive, recyclerViewPast;
    private EventAdapter eventAdapterActive, eventAdapterPast;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_events_landing_page);

        // Set title for NavBar
        setTopTitle("Events");

        // Find create event button
        View createEventTestButton = findViewById(R.id.button_createEventTest);
        if (createEventTestButton != null) {
            createEventTestButton.setOnClickListener(v -> {
                Log.d(TAG, "Create Event Test button CLICKED"); // debug
                Intent intent = new Intent(this, CreateEventActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Create Event Test button NOT FOUND"); // debug
        }

        // Find Active Events Link
        View activeEventsLink = findViewById(R.id.activeEventsTitle);
        if (activeEventsLink != null) {
            activeEventsLink.setOnClickListener(v -> {
                Log.d(TAG, "Active events link CLICKED"); // debug
                Intent intent = new Intent(this, GetAllEventsActivity.class);
                intent.putExtra("filter", "active");
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Active events link NOT FOUND"); // debug
        }

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Read event collection from database
        db.collection("events")
                .whereEqualTo("status", "Planned") // only get planned events
                .limit(2) // Limit to two results
                .get()
                .addOnSuccessListener(query -> {
                    List<EventSummary> eventList = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String status = doc.getString("status");

                        eventList.add(new EventSummary(id, title, "", status));
                    }
                    eventAdapterActive.setItems(eventList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error: ", e));


        // RecyclerView + Adapter
        recyclerViewActive = findViewById(R.id.recyclerViewActiveEvents);
        recyclerViewActive.setLayoutManager(new LinearLayoutManager(this));
        eventAdapterActive = new EventAdapter();
        recyclerViewActive.setAdapter(eventAdapterActive);

        // Find Past Events Link
        View pastEventsLink = findViewById(R.id.pastEventsTitle);
        if (pastEventsLink != null) {
            pastEventsLink.setOnClickListener(v -> {
                Log.d(TAG, "Past events link CLICKED"); // debug
                Intent intent = new Intent(this, GetAllEventsActivity.class);
                intent.putExtra("filter", "past");
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Past events link NOT FOUND"); // debug
        }

        // Read event collection from database
        db.collection("events")
                .whereEqualTo("status", "past") // only get past events
                .limit(2)
                .get()
                .addOnSuccessListener(query -> {
                    List<EventSummary> eventList = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String status = doc.getString("status");

                        eventList.add(new EventSummary(id, title, "", status));
                    }
                    eventAdapterPast.setItems(eventList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error: ", e));


        // RecyclerView + Adapter
        recyclerViewPast = findViewById(R.id.recyclerViewPastEvents);
        recyclerViewPast.setLayoutManager(new LinearLayoutManager(this));
        eventAdapterPast = new EventAdapter();
        recyclerViewPast.setAdapter(eventAdapterPast);
    }
}