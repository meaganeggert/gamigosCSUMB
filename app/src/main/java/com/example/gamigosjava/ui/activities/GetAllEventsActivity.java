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

public class GetAllEventsActivity extends BaseActivity {
    private static final String TAG = "PastEvents";
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout that should fill the frame
        setChildLayout(R.layout.activity_get_all_events);

        String filter = getIntent().getStringExtra("filter");
        assert filter != null;
        String statusValue = filter.equals("active") ? "Planned" : "past";

        // Set title for NavBar
        setTopTitle(filter.equalsIgnoreCase("active") ? "Active Events" : "Past Events");

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Read event collection from database
        db.collection("events")
                .whereEqualTo("status", statusValue) // get the appropriate events based on the filter
                .get()
                .addOnSuccessListener(query -> {
                    List<EventSummary> eventList = new ArrayList<>();
                    for (DocumentSnapshot doc : query) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String status = doc.getString("status");

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