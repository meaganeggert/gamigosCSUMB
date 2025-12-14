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
import com.example.gamigosjava.notifications.NotificationTokenManager;
import com.example.gamigosjava.ui.adapter.FeedAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

// Credential Manager (AndroidX)
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LandingActivity extends BaseActivity {
    private static final String TAG = "LandingActivity";

    private enum FeedFilter {
        ALL,
        EVENTS,
        ACHIEVEMENTS,
        GAMES
    }

    private RecyclerView feedRecycler;
    private TextView defaultEmptyText;
    private FeedAdapter feedAdapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration feedListener; // Will allow for real-time feed updates

    // List of ALL feed items from Firestore so we don't have to continuously query
    private final List<ActivityItem> allFeedList = new ArrayList<>();
    private FeedFilter selectedFilter = FeedFilter.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_landing);

        setChildLayout(R.layout.activity_landing);

        // Set title for NavBar
        setTopTitle("Gamigos");

        checkAndRequestNotificationPermission();
        saveFCMToken();

        feedRecycler = findViewById(R.id.recyclerViewFeed);
        defaultEmptyText = findViewById(R.id.emptyText);

        // Find buttons for feed filtering
        Button allFeedFilter = findViewById(R.id.buttonAll);
        Button eventFeedFilter = findViewById(R.id.buttonEvents);
        Button achieveFeedFilter = findViewById(R.id.buttonAchievements);
        Button matchFeedFilter = findViewById(R.id.buttonGames);

        // Set button listeners
        allFeedFilter.setOnClickListener( l -> {
            selectedFilter = FeedFilter.ALL;
            applyFilter();
        });
        eventFeedFilter.setOnClickListener( l -> {
            selectedFilter = FeedFilter.EVENTS;
            applyFilter();
        });
        achieveFeedFilter.setOnClickListener( l -> {
            selectedFilter = FeedFilter.ACHIEVEMENTS;
            applyFilter();
        });
        matchFeedFilter.setOnClickListener( l -> {
            selectedFilter = FeedFilter.GAMES;
            applyFilter();
        });

        feedRecycler.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new FeedAdapter();
        feedRecycler.setAdapter(feedAdapter);

        loadFeed();
    }

    private void saveFCMToken() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseMessaging.getInstance()
                    .getToken()
                    .addOnSuccessListener(token -> {
                        NotificationTokenManager.saveTokenForCurrentUser(token);
                        Log.d("TOKEN_DEBUG", "LandingActivity refresh token: " + token);
                    });
        }
    }

    private void loadFeed() {
        // Kill the listener if it's already running
        if (feedListener != null) feedListener.remove();

        feedListener = db.collection("activities")
                .whereIn("type", Arrays.asList("ACHIEVEMENT_EARNED", "EVENT_CREATED", "FRIEND_ADDED", "MATCH_WON"))
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

                    allFeedList.clear();

//                    List<ActivityItem> feedList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        ActivityItem feedItem = doc.toObject(ActivityItem.class);
                        if (feedItem != null) {
                            feedItem.setId(doc.getId());
                            allFeedList.add(feedItem);
                        }
                    }

                    applyFilter();
//                    feedAdapter.setItems(feedList);
                });
    }

    private void applyFilter() {
        List<ActivityItem> filteredFeedList = new ArrayList<>();

        for (ActivityItem item : allFeedList) {
            String type = item.getType();

            switch (selectedFilter) {
                case ALL:
                    filteredFeedList.add(item);
                    break;
                case EVENTS:
                    if ("EVENT_CREATED".equals(type)) {
                        filteredFeedList.add(item);
                    }
                    break;
                case ACHIEVEMENTS:
                    if ("ACHIEVEMENT_EARNED".equals(type)) {
                        filteredFeedList.add(item);
                    }
                    break;
                case GAMES:
                    if ("GAMES_WON".equals(type)) {
                        filteredFeedList.add(item);
                    }
                    break;
            }
        }

        // Send the filtered feed list to the adapter
        feedAdapter.setItems(filteredFeedList);

        // If there are no items to display, show the filler text
        // If there ARE items to display, hide the filler text
        if (filteredFeedList.isEmpty()) {
            defaultEmptyText.setVisibility(View.VISIBLE);
            feedRecycler.setVisibility(View.GONE);
        } else {
            defaultEmptyText.setVisibility(View.GONE);
            feedRecycler.setVisibility(View.VISIBLE);
        }
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