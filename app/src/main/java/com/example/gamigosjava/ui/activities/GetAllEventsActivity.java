package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GetAllEventsActivity extends BaseActivity {
    private static final String TAG = "AllEvents";
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout that should fill the frame
        setChildLayout(R.layout.activity_get_all_events);

        // Get current time
        com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
        Log.i(TAG, "Today's date: " + now);

        String filter = getIntent().getStringExtra("filter");

        // Set title for NavBar
        setTopTitle(filter.equalsIgnoreCase("active") ? "Active Events" : "Past Events");

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Read event collection from database
        Query query = db.collection("events");
            if(filter.equals("active")) {
                Log.d(TAG, "Looking for active events");
                query = query.whereGreaterThanOrEqualTo("scheduledAt", now)
                        .orderBy("scheduledAt", Query.Direction.ASCENDING);
            }
            else {
                Log.d(TAG, "Looking for past events");
                query = query.whereLessThan("scheduledAt", now)
                        .orderBy("scheduledAt", Query.Direction.DESCENDING);
            }

            query.get(Source.SERVER)
                .addOnSuccessListener(q -> {
                    List<EventSummary> eventList = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String status = doc.getString("status");
                        Timestamp scheduledTime = doc.getTimestamp("scheduledAt");
                        Log.i(TAG, "ScheduledAt " + scheduledTime);

                        eventList.add(new EventSummary(id, title, "", status));
                    }
                    eventAdapter.setItems(eventList);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error: ", e));


        // RecyclerView + Adapter
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter();
        recyclerView.setAdapter(eventAdapter);

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerView.getChildLayoutPosition(view);
                    String selectedEventId = eventAdapter.getItemAt(index).id;

                    Intent intent = new Intent(GetAllEventsActivity.this, UpdateEventActivity.class);
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

}