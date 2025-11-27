package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.example.gamigosjava.ui.adapter.MatchAdapter;
import com.example.gamigosjava.ui.adapter.ScoresAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
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
    private Match matchItem;

    // to be used if we are not updated a match and instead are creating a new one.
    private String eventId;
    private String matchId;



    private List<Friend> inviteeList = new ArrayList<>();
    private ArrayAdapter inviteeAdapter;

    private List<Player> playerList;

    private RecyclerView recyclerView;
    private ScoresAdapter scoresAdapter;


    // TODO: get match info.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_view_match);
        setTopTitle("Game");

        eventId = getIntent().getStringExtra("selectedEventId");
        matchId = getIntent().getStringExtra("selectedMatchId");

        Toast.makeText(this, "Selected Event: " + eventId + "\nSelectedMatch: " + matchId, Toast.LENGTH_SHORT).show();
        addMatchForm();
        getMatchDetails(matchId);

        Button saveButton = findViewById(R.id.button_saveMatch);
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                uploadGameInfo();
            });
        } else {
            Log.e(TAG, "Failed to find save match button.");
        }

        Button cancelButton = findViewById(R.id.button_cancelMatch);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                finish();
            });
        } else {
            Log.e(TAG, "Failed to find cancel match button.");
        }

    }

    private void addMatchForm() {
        api = BGGService.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        userGameList = new ArrayList<>();
        apiGameList = new ArrayList<>();
        playerList = new ArrayList<>();


        inviteeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                inviteeList
        );
        inviteeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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
        getInvitees();



        View match = LayoutInflater.from(this).inflate(R.layout.fragment_match_form, matchFormContainerHandle, false);
        matchFormContainerHandle.addView(match);

        recyclerView = findViewById(R.id.recyclerViewPlayerScores);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        scoresAdapter = new ScoresAdapter();
        scoresAdapter.setItems(playerList);
        recyclerView.setAdapter(scoresAdapter);

        getPlayers();

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

        Spinner inviteeDropDown = findViewById(R.id.dropdown_invitees);
        inviteeDropDown.setAdapter(inviteeAdapter);
        Button addPlayer = findViewById(R.id.button_addPlayer);
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                Player newPlayer = new Player();
                newPlayer.friend = (Friend) inviteeDropDown.getSelectedItem();

                if (newPlayer.friend == null) {
                    return;
                }

                boolean inList = false;
                for (int i = 0; i < scoresAdapter.getItemCount(); i++) {
                    if (scoresAdapter.playerList.get(i).friend.id.equals(newPlayer.friend.id)) {
                        inList = true;
                        break;
                    }
                }
                if (inList) {
                    Toast.makeText(this, "User is already a player in this match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                scoresAdapter.playerList.add(newPlayer);
                scoresAdapter.notifyDataSetChanged();
            });
        }

        matchItem = new Match();

    }



    private void getPlayers() {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (matchId.isEmpty()) {
            Log.d(TAG, "Cannot get players, no match Id selected.");
            return;
        }

        CollectionReference playerRefs = db.collection("matches").document(matchId).collection("players");

        playerRefs.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No Players found.");
                return;
            }

            for (DocumentSnapshot doc: snaps) {
                String playerId = doc.getString("userId");
                Integer score = doc.get("score", Integer.class);
                Integer placement = doc.get("placement", Integer.class);

                DocumentReference playerRef = db.collection("users").document(playerId);
                playerRef.get().onSuccessTask(docSnap -> {
                    Friend friend = new Friend();
                    friend.id = playerId;
                    friend.displayName = docSnap.getString("displayName");
                    friend.friendUId = docSnap.getString("uid");

                    Player knownPlayer = new Player(friend, score, placement);
                    scoresAdapter.playerList.add(knownPlayer);
                    scoresAdapter.notifyDataSetChanged();
                    return null;
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to find known players for this match: " + e.getMessage());
        });
    }


    private void getHost() {
        db.collection("events").document(eventId).get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                db.collection("users").document(snap.getString("hostId")).get().addOnSuccessListener(s -> {
                    if (!s.exists()) return;

                    Friend host = new Friend();
                    host.id = s.getId();
                    host.displayName = s.getString("displayName");
                    host.friendUId = s.getString("uid");

                    Log.d(TAG, "HOST NAME: " + host.displayName);
                    Log.d(TAG, "HOST ID: " + host.id);
                    Log.d(TAG, "HOST UID: " + host.friendUId);

                    inviteeList.add(host);
                    inviteeAdapter.notifyDataSetChanged();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get host info: " + e.getMessage());
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to find event info: " + e.getMessage());
        });

    }
    private void getInvitees() {

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        getHost();

        String uid = currentUser.getUid();

        CollectionReference inviteesRef = db
                .collection("events")
                .document(eventId)
                .collection("invitees");

        inviteesRef
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d(TAG, "Invitees list is empty.");
                        return;
                    }

                    for (DocumentSnapshot d : snap) {
                        DocumentReference inviteeRef = d.getDocumentReference("userRef");
                        inviteeRef.get().onSuccessTask(inviteeSnap -> {
                            String id = inviteeSnap.getId();
                            String friendUid = inviteeSnap.getString("uid");
                            String displayName = inviteeSnap.getString("displayName");

                            Friend f = new Friend(id, friendUid, displayName);
                            boolean inFriendList = false;
                            for (int i = 0; i < inviteeList.size(); i++) {
                                if (inviteeList.get(i).id.equals(f.id)) {
                                    inFriendList = true;
                                    break;
                                }
                            }

                            if (!inFriendList) {
                                Log.d(TAG, "Added invitee to list");
                                inviteeList.add(f);
                                inviteeAdapter.notifyDataSetChanged();

                            }
                            return null;
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load invitees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }






    private void getGames() {
        if (currentUser == null) {
            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

//        userGameList.clear();
        userGameList.add(new GameSummary(null, "Search BGG", null, null, null, null));

            // Get games the user previously played
        CollectionReference gamesRef = db
                .collection("users")
                .document(uid)
                .collection("gamesPlayed");

        gamesRef
//                .orderBy("title")
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
//                .orderBy("title")
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
//                .orderBy("title")
                .get()
                .addOnSuccessListener(this::applyKnownUserGames)
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to user BGG collection.");
                });

        userGameAdapter.notifyDataSetChanged();
    }

    private void applyKnownUserGames(QuerySnapshot snap) {
        if (snap.isEmpty()) {
            Log.d("TAG", "Game Snap list was null");
            return;
        }

        for (DocumentSnapshot d : snap.getDocuments()) {
            d.getDocumentReference("gameRef").get().addOnSuccessListener(s -> {
                if (s == null) {
                    Log.d(TAG, "Game Reference was null");
                    return;
                }

                String id = s.getId();
                String title = s.getString("title");
                String imageUrl = s.getString("imageUrl");
                Integer minPlayers = s.get("minPlayers", Integer.class);
                Integer maxPlayers = s.get("maxPlayers", Integer.class);
                Integer playTime = s.get("time", Integer.class);

                GameSummary game = new GameSummary(id, title, imageUrl,
                        minPlayers, maxPlayers, playTime);

                boolean inGameList = false;
                for (int i = 0; i < userGameList.size(); i++) {
                    if (userGameList.get(i).id != null) {
                        if (userGameList.get(i).id.equals(game.id)) {
                            inGameList = true;
                            break;
                        }
                    }
                }

                if (!inGameList) userGameList.add(game);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "ERROR: " + e.getMessage());
            });


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



    // Uploads the match to firebase.
    // eventId, notes, rules variant, imageUrl, startedAt, endedAt, and a reference to the game played.
    private void uploadMatch() {
        if (currentUser == null) {
            Log.e(TAG, "Must be logged in.");
            return;
        }

        if (matchFormContainerHandle.getChildCount() < 1) {
            Log.d(TAG, "No matches to upload, skipping uploadMatches");
            Toast.makeText(this, "No matches uploaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        View matchForm = matchFormContainerHandle.getChildAt(0);

        // Get text values from the UI
        EditText ruleChangeValue = matchForm.findViewById(R.id.editTextTextMultiLine_rules);
        EditText notesValue = matchForm.findViewById(R.id.editTextTextMultiLine_notes);
        Spinner gameName = matchForm.findViewById(R.id.dropdown_gameName);
        GameSummary game = (GameSummary) gameName.getSelectedItem();

        // Connect values to the match object.
        matchItem.eventId = eventId;
        matchItem.notes = notesValue.getText().toString();
        matchItem.rulesVariant = ruleChangeValue.getText().toString();
        matchItem.gameId = game.id;
        matchItem.imageUrl = game.imageUrl;
        matchItem.endedAt = Timestamp.now();
        // Timestamps will have been set by the showDateTime interface.

        // Connect values from the match object to the hashmap to be uploaded.
        HashMap<String, Object> match = new HashMap<>();
        match.put("eventId", matchItem.eventId);
        match.put("notes", matchItem.notes);
        match.put("rules_variant", matchItem.rulesVariant);
        match.put("startedAt", matchItem.startedAt);
        match.put("endedAt", matchItem.endedAt);
        match.put("imageUrl", matchItem.imageUrl);

        if (game.id != null) {
            match.put("gameRef", db.collection("games").document(game.id));
        } else {
            match.put("gameRef", game.id);
        }


        // Update the match
        if (!matchId.isEmpty()) {
            db.collection("matches").document(matchItem.id).set(match)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully updated match database element " + matchItem.id + ".");
                        Toast.makeText(this, "Saved Game", Toast.LENGTH_SHORT).show();
                        uploadUserGamesHosted(uid, game);
                        uploadUserGamesPlayed(uid, game);
                        scoresAdapter.uploadPlayerScores(db, currentUser, matchId);
                        finish();
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update match database element " + matchItem.id + ": " + e.getMessage());
                    });
        } else {
            // Upload the new match and add a reference to it in the event subcollection "matches"
            match.replace("startedAt", Timestamp.now());
            db.collection("matches").add(match)
                    .addOnSuccessListener(documentReference -> {
                        matchItem.id = documentReference.getId();
                        Log.d(TAG, "Saved Match: " + matchItem.id);
                        Toast.makeText(this, "Saved Game", Toast.LENGTH_SHORT).show();

                        uploadUserGamesHosted(uid, game);
                        uploadUserGamesPlayed(uid, game);
                        scoresAdapter.uploadPlayerScores(db, currentUser, matchItem.id);


                        HashMap<String, Object> eventMatchHash = new HashMap<>();
                        eventMatchHash.put("matchRef", db.collection("matches").document(matchItem.id));
                        db.collection("events")
                                .document(eventId)
                                .collection("matches")
                                .document(matchItem.id)
                                .set(eventMatchHash).addOnSuccessListener(v -> {
                                    Log.d(TAG, "Successfully connected match " + matchItem.id + " to event " + matchItem.eventId + ".");
                                }).addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to connect match " + matchItem.id + " to event " + matchItem.eventId + ": " + e.getMessage());
                                });

                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save match: " + e.getMessage());
                        Toast.makeText(this, "Failed to save game.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void uploadUserGamesHosted(String uid, GameSummary gameSummary) {
        if (gameSummary.id == null) return;

        DocumentReference gamePlayed = db.collection("users")
                .document(uid)
                .collection("gamesHosted")
                .document(gameSummary.id);

        HashMap<String, Object> gameHash = new HashMap<>();
        gameHash.put("gameRef", db.collection("games").document(gameSummary.id));
        gamePlayed.set(gameHash).addOnSuccessListener(v -> {
            Log.d(TAG, "Successfully updated user's hosted game database element: " + gameSummary.id);
        })
        .addOnFailureListener(e -> {
            Log.d(TAG, "Failed to update user's hosted game database element " + gameSummary.id + ": " + e.getMessage());
        });

    }

    private void uploadUserGamesPlayed(String uid, GameSummary gameSummary) {
        if (gameSummary.id == null) return;

        DocumentReference gamePlayedRef = db.collection("users")
                .document(uid)
                .collection("gamesPlayed")
                .document(gameSummary.id);

        HashMap<String, Object> gameHash = new HashMap<>();
        gameHash.put("gameRef", db.collection("games").document(gameSummary.id));

        gamePlayedRef.get().addOnSuccessListener(snap -> {

            if (snap == null) {
                gameHash.put("timesPlayed", 0);
            } else {
                Integer timesPlayed;
                if (snap.get("timesPlayed", Integer.class) == null) {
                    timesPlayed = 0;
                } else {
                    timesPlayed = snap.get("timesPlayed", Integer.class);
                }
                int incrementedTimesPlayed = timesPlayed;
                incrementedTimesPlayed++;
                gameHash.put("timesPlayed", incrementedTimesPlayed);
            }
            gamePlayedRef.set(gameHash).addOnSuccessListener(v -> {
                Log.d(TAG, "Successfully updated user's played game database element: " + gameSummary.id);
            }).addOnFailureListener(e -> {
                Log.d(TAG, "Failed to update user's played game database element " + gameSummary.id + ": " + e.getMessage());
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get userGamePlayed info: " + e.getMessage());
        });

    }

    private void getMatchDetails(String matchId) {
        if (matchId.isEmpty()) {
            Log.d(TAG, "No match id was passed in.");
            matchItem.startedAt = Timestamp.now();
            return;
        }

        DocumentReference matchRef = db.collection("matches")
                .document(matchId);

        matchRef.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                Log.d(TAG, "Match " + matchId + " was not found.");
                return;
            }

            matchItem.id = snap.getId();
            matchItem.endedAt = snap.getTimestamp("endedAt");
            matchItem.gameId = snap.getId();
            matchItem.gameRef = snap.getDocumentReference("gameRef");
            matchItem.startedAt = snap.getTimestamp("startedAt");
            matchItem.notes = snap.getString("notes");
            matchItem.eventId = snap.getString("eventId");
            matchItem.rulesVariant = snap.getString("rules_variant");

            setMatchDetails(matchItem);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get match " + matchId + " details: " + e.getMessage());
        });
    }

    private void setMatchDetails(Match match) {
        View matchForm = matchFormContainerHandle.getChildAt(0);
        TextView ruleChanges = matchForm.findViewById(R.id.editTextTextMultiLine_rules);
        TextView notes = matchForm.findViewById(R.id.editTextTextMultiLine_notes);

        ruleChanges.setText(match.rulesVariant);
        notes.setText(match.notes);

        match.gameRef.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                return;
            }

            String id = snap.getId();
            String title = snap.getString("title");
            String imageUrl = snap.getString("imageUrl");
            Integer minPlayers = snap.get("minPlayers", Integer.class);
            Integer maxPlayers = snap.get("maxPlayers", Integer.class);
            Integer playTime = snap.get("time", Integer.class);

            GameSummary game = new GameSummary(id, title, imageUrl,
                    minPlayers, maxPlayers, playTime);

            Spinner gameDropdown = matchForm.findViewById(R.id.dropdown_gameName);

            boolean inGameList = false;
            for (int i = 0; i < userGameList.size(); i++) {
                if (userGameList.get(i).id != null) {
                    if (userGameList.get(i).id.equals(game.id)) {
                        gameDropdown.setSelection(i);
                        inGameList = true;
                        break;
                    }
                }

            }

            if (!inGameList)  {
                userGameList.add(game);
                userGameAdapter.notifyDataSetChanged();
                gameDropdown.setSelection(userGameList.indexOf(game));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get selected game info: " + e.getMessage());
        });
    }

    private void uploadGameInfo() {
        View matchForm = matchFormContainerHandle.getChildAt(0);
        Spinner gameName = matchForm.findViewById(R.id.dropdown_gameName);
        GameSummary gameSummary = (GameSummary) gameName.getSelectedItem();

        if (gameSummary.id == null) {
            Toast.makeText(this, "Must select game.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference gameRef = db.collection("games")
                .document(gameSummary.id);

        HashMap<String, Object> gameHash = new HashMap<>();
        gameHash.put("id", gameSummary.id);
        gameHash.put("title", gameSummary.title);
        gameHash.put("imageUrl", gameSummary.imageUrl);
        gameHash.put("maxPlayers", gameSummary.maxPlayers);
        gameHash.put("minPlayers", gameSummary.minPlayers);
        gameHash.put("playingTime", gameSummary.playingTime);
        gameRef.set(gameHash).addOnSuccessListener(v -> {
                    Log.d(TAG, "Successfully updated board game database element: " + gameSummary.id);
                    uploadMatch();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to update board game database element " + gameSummary.id + ": " + e.getMessage());
                });
    }
}

