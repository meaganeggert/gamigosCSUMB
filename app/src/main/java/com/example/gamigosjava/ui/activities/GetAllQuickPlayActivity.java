package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.ui.adapter.MatchAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class GetAllQuickPlayActivity extends BaseActivity {
    private static final String TAG = "Get ALl Quick Play Matches";
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();
    List<MatchSummary> matchSummaryList = new ArrayList<>();
    List<DocumentReference> matchDocRefList = new ArrayList<>();
    MatchAdapter matchAdapter;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_get_all_quick_play);
        setTopTitle("Games");

        recyclerView = findViewById(R.id.recyclerViewQuickPlayMatches);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        matchAdapter = new MatchAdapter(false);
        recyclerView.setAdapter(matchAdapter);

        Button newGame = findViewById(R.id.button_newGame);
        if (newGame != null) {
            newGame.setOnClickListener(v -> {
                Intent intent = new Intent(GetAllQuickPlayActivity.this, QuickPlayActivity.class);
                intent.putExtra("selectedEventId", "");
                intent.putExtra("selectedMatchId", "");
                startActivity(intent);
            });
        }

        Button back = findViewById(R.id.button_allQuickPlayBack);
        if (back != null) {
            back.setOnClickListener(v -> {
                finish();
            });
        }

        getAllMatches();

        recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                view.setOnClickListener(v -> {
                    int index = recyclerView.getChildLayoutPosition(view);
                    MatchSummary selectedMatch = matchAdapter.getItemAt(index);
                    String selectedMatchId = "";
                    String selectedMatchEventId = "";
                    if (selectedMatch != null) {
                        selectedMatchId = selectedMatch.id;
                        selectedMatchEventId = selectedMatch.eventId;
                    }

                    Intent intent = new Intent(GetAllQuickPlayActivity.this, QuickPlayActivity.class);
                    intent.putExtra("selectedMatchId", selectedMatchId);
                    intent.putExtra("selectedEventId", selectedMatchEventId);
                    startActivity(intent);
                });
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {

            }
        });
    }

    private void getAllMatches() {
        if (currentUser == null) return;

        Query query = db.collection("matches")
                        .orderBy("updatedAt", Query.Direction.DESCENDING);

        query.addSnapshotListener((snaps, e) -> {
            if (e != null || snaps.isEmpty()) {
                Log.d(TAG, "Match info is empty.");
                return;
            }


            for (DocumentSnapshot matchSnap: snaps) {
                Match matchResult = new Match();
                matchResult.id = matchSnap.getId();
                matchResult.eventId = matchSnap.getString("eventId");
                matchResult.notes = matchSnap.getString("notes");
                matchResult.rulesVariant = matchSnap.getString("rules_variant");
                matchResult.startedAt = matchSnap.getTimestamp("startedAt");
                matchResult.endedAt = matchSnap.getTimestamp("endedAt");
                matchResult.gameRef = matchSnap.getDocumentReference("gameRef");
                if (matchResult.gameRef != null) {
                    matchResult.gameId = matchResult.gameRef.getId();
                }

                CollectionReference playersCollection = db
                        .collection("matches")
                        .document(matchResult.id)
                        .collection("players");

                matchResult.playersRef = playersCollection;

                getGameDetails(matchResult);
                Log.d(TAG, "Found match: " + matchResult.id);
            }
        });
    }

    private void getGameDetails(Match match) {
        DocumentReference gameDoc = match.gameRef;
        if (gameDoc == null) {
            Log.d(TAG, "Game document was null.");
            return;
        }

        match.gameRef.addSnapshotListener((snap, gameError) -> {
            if (snap == null) {
                Log.d(TAG, "Couldn't find game details");
            }

            String title = snap.getString("title");
            String imageUrl = snap.getString("imageUrl");
            Integer maxPlayers = snap.get("maxPlayers", Integer.class);
            Integer minPlayers = snap.get("minPlayers", Integer.class);
            Integer playingTime = snap.get("playingTime", Integer.class);

            MatchSummary matchSummary = new MatchSummary(match.id, "", title, imageUrl, minPlayers, maxPlayers, playingTime);

            boolean matchInList = false;
            for (int i = 0; i < matchSummaryList.size(); i++) {
                if (matchSummaryList.get(i).id.equals(matchSummary.id)) {
                    Log.d(TAG, "MATCH WAS FOUND IN LIST FOR GAME DETAILS FUNCTION: " + matchSummary.gameName);
                    matchInList = true;
                    matchSummaryList.set(i, matchSummary);
                    matchAdapter.setItems(matchSummaryList);
                    break;
                }
            }

            if (!matchInList) {
                matchSummaryList.add(matchSummary);
                matchAdapter.setItems(matchSummaryList);
                Log.d(TAG, "Found game: " + matchSummary.id);
            }
        });

    }
}