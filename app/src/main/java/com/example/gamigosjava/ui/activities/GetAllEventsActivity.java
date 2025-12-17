package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.data.repository.EventsRepo;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GetAllEventsActivity extends BaseActivity {
    private static final String TAG = "AllEvents";
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;
    private ListenerRegistration eventsListener; // To allow for real-time updates
    private FirebaseFirestore db;
    private EventsRepo eventsRepo;
    private FirebaseAuth auth;
    String filter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout that should fill the frame
        setChildLayout(R.layout.activity_get_all_events);

        // Get current time
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        Log.i(TAG, "Today's date: " + now);

        // Get filter
        filter = getIntent().getStringExtra("filter");
        // Set title for NavBar
        setTopTitle(filter.equalsIgnoreCase("active") ? "Active Events" : "Past Events");

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Get Event Repo
        eventsRepo = new EventsRepo(db);

        // RecyclerView + Adapter
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(filter.equals("active"));
        recyclerView.setAdapter(eventAdapter);

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerView.getChildLayoutPosition(view);
                    String selectedEventId = eventAdapter.getItemAt(index).id;

                    Intent intent = new Intent(GetAllEventsActivity.this, ViewEventActivity.class);
                    intent.putExtra("selectedEventId", selectedEventId);
                    startActivity(intent);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });

        // Find back button
        View backButton = findViewById(R.id.button_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button CLICKED"); // debug
                Intent intent = new Intent(this, EventsLandingPage.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Back button NOT FOUND"); // debug
        }
    }

    private void listenForEventChanges() {
        if (eventsListener != null) return; // already attached

        eventsListener = db.collection("events")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Event listener failed: ", e);
                        return;
                    }

                    if (snap == null) {
                        Log.w(TAG, "Snap == null");
                        return;
                    }

                    Log.d(TAG, "Refreshing events with " + snap.getDocumentChanges().size() + " changes.");

                    // Update events
                    loadEvents();
                });
    }


    private void loadEvents() {
        // Read event collection from database
        if (filter.equalsIgnoreCase("active")) {
            eventsRepo.loadAllEventDetails(true, 10)
                    .addOnSuccessListener( events -> {
                        eventAdapter.setItems(events);
                    })
                    .addOnFailureListener( e-> {
                        Log.e(TAG, "Error loading active events: ", e);
                    });
        } else {
            eventsRepo.loadAllEventDetails(false, 10)
                    .addOnSuccessListener( events -> {
                        eventAdapter.setItems(events);
                        Log.i(TAG, "Past events loaded successfully");
                    })
                    .addOnFailureListener( e-> {
                        Log.e(TAG, "Error loading past events: ", e);
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForEventChanges();  // start listening to keep a live feed
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (eventsListener != null) { // kill the listener
            eventsListener.remove();
            eventsListener = null;
            Log.d(TAG, "Killed events listener");
        }
    }

}