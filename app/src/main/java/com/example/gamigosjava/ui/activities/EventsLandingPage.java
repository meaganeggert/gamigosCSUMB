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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventsLandingPage extends BaseActivity {

    private static final String TAG = "EventsHome";
    private RecyclerView recyclerViewActive, recyclerViewPast;
    private EventAdapter eventAdapterActive, eventAdapterPast;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_events_landing_page);
        Timestamp now = Timestamp.now();
        Log.i(TAG, "Today's date: " + now);

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

        // Get EventsRepo
        EventsRepo eventsRepo = new EventsRepo(db);

        // Read event collection from database
//        Query query = db.collection("events");
//        query = query.whereGreaterThanOrEqualTo("scheduledAt", now)
//                .orderBy("scheduledAt", Query.Direction.ASCENDING)
//                .limit(2); // Pull active events
//
//        query.get(Source.SERVER)
//                .addOnSuccessListener(q -> {
//                    List<EventSummary> eventList = new ArrayList<>();
//                    for (DocumentSnapshot doc : q) {
//                        String id = doc.getId();
//                        String title = doc.getString("title");
//                        String status = doc.getString("status");
//                        Timestamp scheduledTime = doc.getTimestamp("scheduledAt");
//                        Log.i(TAG, "ScheduledAt " + scheduledTime);
//
//                        eventList.add(new EventSummary(id, title, "", status));
//                    }
//                    eventAdapterActive.setItems(eventList);
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Error: ", e));



        // RecyclerView + Adapter
        recyclerViewActive = findViewById(R.id.recyclerViewActiveEvents);
        recyclerViewActive.setLayoutManager(new LinearLayoutManager(this));
        eventAdapterActive = new EventAdapter(true);
        recyclerViewActive.setAdapter(eventAdapterActive);

        // Changes to load events with attendees
        eventsRepo.loadAllEventAttendees(true, 10)
                .addOnSuccessListener( events -> {
                    eventAdapterActive.setItems(events);
                })
                .addOnFailureListener( e-> {
                    Log.e(TAG, "Error loading active events: ", e);
                });

        // Allow the individual events to be clickable
        recyclerViewActive.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
           @Override
           public void onChildViewAttachedToWindow(@NonNull View view) {
               view.setOnClickListener(v -> {
                   int index = recyclerViewActive.getChildLayoutPosition(view);
                   String selectedEventId = eventAdapterActive.getItemAt(index).id;

                   Intent intent = new Intent(EventsLandingPage.this, ViewEventActivity.class);
                   intent.putExtra("selectedEventId", selectedEventId);
                   startActivity(intent);
               });
           }

           @Override
           public void onChildViewDetachedFromWindow(@NonNull View view) {

           }
       });

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
//        query = db.collection("events");
//        query = query.whereLessThan("scheduledAt", now)
//                .orderBy("scheduledAt", Query.Direction.DESCENDING)
//                .limit(2); // Pull past events
//
//        query.get(Source.SERVER)
//                .addOnSuccessListener(q -> {
//                    List<EventSummary> eventList = new ArrayList<>();
//                    for (DocumentSnapshot doc : q) {
//                        String id = doc.getId();
//                        String title = doc.getString("title");
//                        String status = doc.getString("status");
//                        Timestamp scheduledTime = doc.getTimestamp("scheduledAt");
//                        Log.i(TAG, "ScheduledAt " + scheduledTime);
//
//                        eventList.add(new EventSummary(id, title, "", status));
//                    }
//                    eventAdapterPast.setItems(eventList);
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Error: ", e));

        // RecyclerView + Adapter
        recyclerViewPast = findViewById(R.id.recyclerViewPastEvents);
        recyclerViewPast.setLayoutManager(new LinearLayoutManager(this));
        eventAdapterPast = new EventAdapter(false);
        recyclerViewPast.setAdapter(eventAdapterPast);

        // Changes to load events with attendees
        eventsRepo.loadAllEventAttendees(false, 2)
                .addOnSuccessListener( events -> {
                    eventAdapterPast.setItems(events);
                    Log.i(TAG, "Past events loaded successfully");
                })
                .addOnFailureListener( e-> {
                    Log.e(TAG, "Error loading past events: ", e);
                });

        // Allow the individual events to be clickable
        recyclerViewPast.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerViewPast.getChildLayoutPosition(view);
                    String selectedEventId = eventAdapterPast.getItemAt(index).id;

                    Intent intent = new Intent(EventsLandingPage.this, ViewEventActivity.class);
                    intent.putExtra("selectedEventId", selectedEventId);
                    startActivity(intent);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });
    }
}