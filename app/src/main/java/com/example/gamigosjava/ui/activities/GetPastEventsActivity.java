package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.example.gamigosjava.ui.adapter.GameAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GetPastEventsActivity extends BaseActivity {
    private static final String TAG = "PastEvents";
    private RecyclerView recyclerView;
    private EventAdapter eventAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout that should fill the frame
        setChildLayout(R.layout.activity_get_past_events);

        // Set title for NavBar
        setTopTitle("Past Events");

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Read event collection from database
        db.collection("events")
                .whereEqualTo("status", "past") // only get past events
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
    }
}