package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.repository.EventsRepo;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.example.gamigosjava.ui.adapter.MatchAdapter;
import com.example.gamigosjava.ui.adapter.MatchViewAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MatchLandingActivity extends BaseActivity {

    private static final String TAG = "MatchLanding";
    private RecyclerView recyclerViewMatches;
    private MatchAdapter matchAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_match_landing);
//        Timestamp now = Timestamp.now();
//        Log.i(TAG, "Today's date: " + now);

        // Set title for NavBar
        setTopTitle("Games");

        // Find create quick match button
        View createQuickMatchButton = findViewById(R.id.button_createQuickMatch);
        if (createQuickMatchButton != null) {
            createQuickMatchButton.setOnClickListener(v -> {
                Log.d(TAG, "Create Quick Match Button CLICKED"); // debug
                Intent intent = new Intent(MatchLandingActivity.this, QuickPlayActivity.class);
                intent.putExtra("selectedEventId", "");
                intent.putExtra("selectedMatchId", "");
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Create Quick Match Button NOT FOUND"); // debug
        }

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Get EventsRepo
        EventsRepo eventsRepo = new EventsRepo(db);

        // RecyclerView + Adapter
        recyclerViewMatches = findViewById(R.id.recyclerView_Matches);
        recyclerViewMatches.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter();
        recyclerViewMatches.setAdapter(matchAdapter);

        // Allow the individual events to be clickable
        recyclerViewMatches.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerViewMatches.getChildLayoutPosition(view);
                    String selectedEventId = matchAdapter.getItemAt(index).id;

                    Intent intent = new Intent(MatchLandingActivity.this, ViewMatchActivity.class);
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