package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewMatchActivity extends BaseActivity {
    private String TAG = "View Match";
    BGG_API api;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    LinearLayout matchFormContainerHandle;
    private ArrayAdapter<GameSummary> userGameAdapter;
    private ArrayAdapter<GameSummary> apiGameAdapter;
    private List<GameSummary> userGameList;
    private List<GameSummary> apiGameList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_match);
        setTopTitle("Match");

        addMatchForm();


    }

    private void addMatchForm() {
        api = BGGService.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userGameList = new ArrayList<>();
        apiGameList = new ArrayList<>();

        matchFormContainerHandle = findViewById(R.id.matchFormContainer);
        userGameAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                userGameList
        );
        userGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        apiGameAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                apiGameList
        );
        apiGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        getGames();

        View match = LayoutInflater.from(this).inflate(R.layout.fragment_match_form, matchFormContainerHandle, false);
        matchFormContainerHandle.addView(match);

        // Set board game dropdown for each new match form.
        Spinner gameName = matchFormContainerHandle
                .getChildAt(matchFormContainerHandle.getChildCount()-1)
                .findViewById(R.id.dropdown_gameName);

        if (gameName != null) {
            gameName.setAdapter(userGameAdapter);
        } else {
            Log.e(TAG, "Game name dropdown not found");
        }

        SearchView search = match.findViewById(R.id.searchView_bggSearch);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String s) {
                if (s.isEmpty()) {
                    Log.d(TAG, "Added gameSelection via userDB");
                    gameName.setAdapter(userGameAdapter);
                }

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!s.isEmpty()) {
                    Log.d(TAG, "added gameSelection via API");
                    fetchGamesForQuery(s);
                    gameName.setAdapter(apiGameAdapter);
                    search.clearFocus();
                }
                return true;
            }
        });
    }

        private void getGames() {
        if (currentUser == null) {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        userGameList.clear();
        // Get games the user previously played
        CollectionReference gamesRef = db
                .collection("users")
                .document(uid)
                .collection("gamesPlayed");

        gamesRef
                .orderBy("title")
                .get()
                .addOnSuccessListener(this::applyKnownUserGames)
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to find games previously played.");
                });

        // Get games the user previously hosted.
        gamesRef = db
                .collection("users")
                .document(uid)
                .collection("gamesHosted");

        gamesRef
                .orderBy("title")
                .get()
                .addOnSuccessListener(this::applyKnownUserGames)
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to find games previously hosted.");
                });

        // Get games the user owns in BGG
        gamesRef = db
                .collection("users")
                .document(uid)
                .collection("userBGGCollection");

        gamesRef
                .orderBy("title")
                .get()
                .addOnSuccessListener(this::applyKnownUserGames)
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to user BGG collection.");
                });

        userGameList.add(new GameSummary(null, "Search BGG", null, null, null, null));
        userGameAdapter.notifyDataSetChanged();
    }

    private void applyKnownUserGames(QuerySnapshot snap) {
        if (snap.isEmpty()) {
            return;
        }

        for (DocumentSnapshot d : snap.getDocuments()) {
            String id = d.getId();
            String title = d.getString("title");
            String imageUrl = d.getString("imageUrl");
            Integer minPlayers = d.get("minPlayers", Integer.class);
            Integer maxPlayers = d.get("maxPlayers", Integer.class);
            Integer playTime = d.get("time", Integer.class);

            GameSummary game = new GameSummary(id, title, imageUrl,
                    minPlayers, maxPlayers, playTime);

            if (!userGameList.contains(game)) {
                userGameList.add(new GameSummary(id, title, imageUrl,
                        minPlayers, maxPlayers, playTime));
            }

        }
    }

    private void fetchGamesForQuery(String query) {
        api.search(query, "boardgame").enqueue(new Callback<SearchResponse>() {
            @Override public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> resp) {
                if (!resp.isSuccessful()) {
                    Toast.makeText(ViewMatchActivity.this, "Response not successful", Toast.LENGTH_SHORT).show();
                    String err = null;
                    try { err = resp.errorBody() != null ? resp.errorBody().string() : null; } catch (Exception ignored) {}
                    Log.e("BGG", "Search HTTP " + resp.code() + " " + err);
                    return;
                }
                if (resp.body() == null || resp.body().items == null || resp.body().items.isEmpty()) {
                    Toast.makeText(ViewMatchActivity.this, "No search results", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Build CSV of a few IDs to batch the /thing call
                StringBuilder ids = new StringBuilder();
                int max = Math.min(10, resp.body().items.size());
                for (int i = 0; i < max; i++) {
                    if (i > 0) ids.append(',');
                    ids.append(resp.body().items.get(i).id);
                }
                fetchThingDetails(ids.toString());
            }

            @Override public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(ViewMatchActivity.this, "Search failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchThingDetails(String idsCsv) {
        api.thing(idsCsv, 0).enqueue(new Callback<ThingResponse>() {
            @Override public void onResponse(Call<ThingResponse> call, Response<ThingResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().items == null) {
                    Toast.makeText(ViewMatchActivity.this, "No details", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<GameSummary> list = new ArrayList<>();
                for (BGGItem it : resp.body().items) {
                    list.add(BGGMappers.toSummary(it));
                }

                // 🟢 Add this to confirm data size
                Toast.makeText(ViewMatchActivity.this, "Loaded " + list.size() + " games", Toast.LENGTH_SHORT).show();
                Log.d("BGG", "Loaded " + list.size() + " game(s)"); // debug

                apiGameList.clear();
                apiGameList.addAll(list);
                apiGameAdapter.notifyDataSetChanged();
            }

            @Override public void onFailure(Call<ThingResponse> call, Throwable t) {
                Toast.makeText(ViewMatchActivity.this, "Thing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}

