package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.gamigosjava.data.model.ActivityItem;

// Firebase
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FeedAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

// Credential Manager (AndroidX)
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LandingActivity extends BaseActivity {
    private static final String TAG = "LandingActivity";

    private RecyclerView feedRecycler;
    private TextView defaultEmptyText;
    private FeedAdapter feedAdapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration feedListener; // Will allow for real-time feed updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_landing);

        setChildLayout(R.layout.activity_landing);

        // Set title for NavBar
        setTopTitle("Gamigos");

        checkAndRequestNotificationPermission();

        feedRecycler = findViewById(R.id.recyclerViewFeed);
        defaultEmptyText = findViewById(R.id.emptyText);

        feedRecycler.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter();
        feedRecycler.setAdapter(feedAdapter);

        loadFeed();

        Button quickGame = findViewById(R.id.buttonQuickGame);
        if (quickGame != null) {
            quickGame.setOnClickListener(v -> {
                Intent intent = new Intent(LandingActivity.this, GetAllQuickPlayActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadFeed() {
        // Kill the listener if it's already running
        if (feedListener != null) feedListener.remove();

        feedListener = db.collection("activities")
                .whereEqualTo("type", "ACHIEVEMENT_EARNED")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null ) {
                        Log.e(TAG, "Feed Listener Error: ", e);
                        defaultEmptyText.setText("Feed failed to load.");
                        defaultEmptyText.setVisibility(View.VISIBLE);
                        feedRecycler.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    List<ActivityItem> feedList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ActivityItem feedItem = doc.toObject(ActivityItem.class);
                        if (feedItem != null) {
                            feedItem.setId(doc.getId());
                            feedList.add(feedItem);
                        }
                    }

                    feedAdapter.setItems(feedList);

                    // If there are no items to display, show the filler text
                    // If there ARE items to display, hide the filler text
                    if (feedList.isEmpty()) {
                        defaultEmptyText.setVisibility(View.VISIBLE);
                        feedRecycler.setVisibility(View.GONE);
                    } else {
                        defaultEmptyText.setVisibility(View.GONE);
                        feedRecycler.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Kill accidental duplicates
        if (feedListener != null) {
            feedListener.remove();
            feedListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Listen when the feed starts
        loadFeed();
    }
}