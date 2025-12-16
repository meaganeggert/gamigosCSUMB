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
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.notifications.NotificationTokenManager;
import com.example.gamigosjava.ui.adapter.FeedAdapter;
import com.example.gamigosjava.ui.adapter.MatchAdapter;
import com.example.gamigosjava.ui.adapter.MatchViewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessaging;

// Credential Manager (AndroidX)
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
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

    private RecyclerView feedRecycler, matchFeedRecycler;
    private TextView defaultEmptyText, matchDefaultText;
    private FeedAdapter feedAdapter;
    private MatchAdapter matchFeedAdapter;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ListenerRegistration feedListener; // Will allow for real-time feed updates
    private ListenerRegistration matchFeedListener; // Will allow for real-time feed updates

    // List of ALL feed items from Firestore so we don't have to continuously query
    private final List<ActivityItem> allFeedList = new ArrayList<>();
    private final List<MatchSummary> allMatchList = new ArrayList<>();
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
        matchFeedRecycler = findViewById(R.id.recentMatchRecycler);
//        matchDefaultText = findViewById(R.id.matchEmptyText);

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

        matchFeedRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        matchFeedAdapter = new MatchAdapter();
        matchFeedAdapter.setIsHost(false);
        matchFeedRecycler.setAdapter(matchFeedAdapter);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(matchFeedRecycler);

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

    private void loadRecentMatches() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        if (matchFeedListener != null) matchFeedListener.remove();

        matchFeedListener = db.collection("matches")
                .whereArrayContains("playerIds", user.getUid())
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(3)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Match Feed Listener Error: ", e);
//                        matchDefaultText.setText("Feed failed to load.");
//                        matchDefaultText.setVisibility(View.VISIBLE);
                        matchFeedRecycler.setVisibility(View.GONE);
                        return;
                    }

                    if (queryDocumentSnapshots == null) return;

                    Log.d(TAG, "Recent matches count: " + queryDocumentSnapshots.size());

                    allMatchList.clear();

                    int index = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        MatchSummary matchItem = doc.toObject(MatchSummary.class);
                        if (matchItem != null) {
                            matchItem.id = doc.getId();
                            allMatchList.add(matchItem);

                            getPlayersAndWinner(matchItem, index);
                            index++;
                        }
                    }

                    matchFeedAdapter.setItems(allMatchList);

                    if (allMatchList.isEmpty()) {
//                        matchDefaultText.setText("No recent matches yet.");
//                        matchDefaultText.setVisibility(View.VISIBLE);
                        matchFeedRecycler.setVisibility(View.GONE);
                    } else {
//                        matchDefaultText.setVisibility(View.GONE);
                        matchFeedRecycler.setVisibility(View.VISIBLE);
                    }
        });
    }

    private void getPlayersAndWinner(MatchSummary matchItem, int adapterIndex) {
        if (matchItem.id == null) return;

        db.collection("matches")
                .document(matchItem.id)
                .collection("players")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) return;

                    List<String> playerNames = new ArrayList<>();
                    List<String> playerIds = new ArrayList<>();
                    List<String> playerAvatarUrls = new ArrayList<>();
                    String winnerUid = null;
                    String winnerName = null;

                    for (DocumentSnapshot playerSnap : snaps) {
                        String uId = playerSnap.getString("userId");
                        String name = playerSnap.getString("displayName");  // or whatever you stored
                        if (uId != null && !uId.isEmpty()) {
                            playerIds.add(uId);
                        }
                        if (name != null && !name.isEmpty()) {
                            playerNames.add(name);
                        }

                        Long placement = playerSnap.getLong("placement");
                        if (placement != null && placement == 1L) {
                            // first place = winner
                            winnerUid = uId;
                            winnerName = name;
                        }
                    }

                    matchItem.playerNames = playerNames;

                    for (String user_id : playerIds) {
                        // Look up winner user doc to get avatar + displayName
                        String finalWinnerUid = winnerUid;
                        db.collection("users")
                                .document(user_id)
                                .get()
                                .addOnSuccessListener(userSnap -> {
                                    String avatar = userSnap.getString("photoUrl");
                                    if (avatar != null && !avatar.isEmpty()) {
                                        playerAvatarUrls.add(avatar);
                                    }

                                    matchItem.playerAvatars = playerAvatarUrls;

                                    if (user_id.equals(finalWinnerUid)) {
                                        matchItem.winnerAvatarUrl = avatar;
                                        matchItem.winnerId = user_id;
                                    }

                                    matchFeedAdapter.notifyItemChanged(adapterIndex);
                                })
                                .addOnFailureListener(err -> {
                                    Log.e(TAG, "Failed to load avatar info for player: " + user_id, err);
                                })
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to load players for match: " + matchItem.id, e)
                                );
                    }
                })
                .addOnFailureListener( e -> {
                    Log.e(TAG, "Failed to load players for match: " + matchItem.id, e);
                });
    }

    private void loadFeed() {
        // Kill the listener if it's already running
        if (feedListener != null) feedListener.remove();

        feedListener = db.collection("activities")
                .whereIn("type", Arrays.asList("ACHIEVEMENT_EARNED", "EVENT_CREATED", "FRIEND_ADDED", "GAME_ACHIEVEMENT_EARNED", "MATCH_WON"))
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
                    if ("ACHIEVEMENT_EARNED".equals(type) || "GAME_ACHIEVEMENT_EARNED".equals(type)) {
                        filteredFeedList.add(item);
                    }
                    break;
                case GAMES:
                    if ("MATCH_WON".equals(type)) {
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
        if (matchFeedListener != null) {
            matchFeedListener.remove();
            matchFeedListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Listen when the feed starts
        loadFeed();
        loadRecentMatches();
    }
}