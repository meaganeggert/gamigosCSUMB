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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.Event;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.example.gamigosjava.data.model.UserGameMetric;
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
    private ArrayAdapter<GameSummary> userGameAdapter, apiGameAdapter;
    private List<GameSummary> userGameList, apiGameList;
    private Match matchItem;

    // to be used if we are not updated a match and instead are creating a new one.
    private String eventId, matchId;
    private Event eventItem;


    private Friend hostUser;
    private List<Friend> inviteeList = new ArrayList<>();
    private ArrayAdapter inviteeAdapter;

    private List<Player> playerList;

    private RecyclerView recyclerView;
    private ScoresAdapter scoresAdapter;
    public Button saveButton, startMatch, endMatch, addPlayer;
    private EditText customPlayerInput;
    private Spinner inviteeDropDown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_view_match);
        setTopTitle("Game");

        api = BGGService.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("selectedEventId");
        matchId = getIntent().getStringExtra("selectedMatchId");
        matchItem = new Match();
        matchItem.hostId = currentUser.getUid();

        Toast.makeText(this, "Selected Event: " + eventId + "\nSelectedMatch: " + matchId, Toast.LENGTH_SHORT).show();
        addMatchForm(R.id.matchFormContainer);

        getMatchDetails(matchId);
        getEventDetails(eventId);

        saveButton = findViewById(R.id.button_saveMatch);
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

    // Sets the UI element variables for the match form.
    public void addMatchForm(@IdRes int containerId) {
        userGameList = new ArrayList<>();
        apiGameList = new ArrayList<>();
        playerList = new ArrayList<>();

        matchFormContainerHandle = findViewById(containerId);

        inviteeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                inviteeList
        );
        inviteeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

        inviteeDropDown = findViewById(R.id.dropdown_invitees);
        inviteeDropDown.setAdapter(inviteeAdapter);

        customPlayerInput = findViewById(R.id.editText_customPlayer);

        Switch playerTypeToggle = findViewById(R.id.switch_customPlayerToggle);
        if (playerTypeToggle != null) {
            playerTypeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    inviteeDropDown.setVisibility(Spinner.GONE);
                    customPlayerInput.setVisibility(Button.VISIBLE);
                    addPlayerFromText();
                } else {
                    inviteeDropDown.setVisibility(Spinner.VISIBLE);
                    customPlayerInput.setVisibility(Button.GONE);
                    addPlayerFromSpinner();
                }
            });
        }

        addPlayer = findViewById(R.id.button_addPlayer);
        addPlayerFromSpinner();

    }

    private void addPlayerFromSpinner() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                Player newPlayer = new Player();
                newPlayer.friend = (Friend) inviteeDropDown.getSelectedItem();

                if (newPlayer.friend == null) return;
                if (newPlayer.friend.id == null) return;

                boolean inList = false;
                for (int i = 0; i < scoresAdapter.getItemCount(); i++) {
                    Player player = scoresAdapter.playerList.get(i);
                    if (player.friend.id == null) continue;

                    if (player.friend.id.equals(newPlayer.friend.id)) {
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
    }
    private void addPlayerFromText() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                String customName = customPlayerInput.getText().toString();
                if (customName.isEmpty()) return;

                Player newPlayer = new Player();
                newPlayer.friend = new Friend();
                newPlayer.friend.displayName = customName;

                boolean inList = false;
                for (int i = 0; i < scoresAdapter.getItemCount(); i++) {
                    if (scoresAdapter.playerList.get(i).friend.displayName.equals(newPlayer.friend.displayName)) {
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
    }



    private void getPlayers() {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (matchId.isEmpty() || matchId == null) {
            Log.d(TAG, "Cannot get players, no match Id selected.");
            return;
        }

        CollectionReference playerRefs = db.collection("matches").document(matchId).collection("players");
        matchItem.playersRef = playerRefs;

        playerRefs.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No Players found.");
                return;
            }

            for (DocumentSnapshot doc: snaps) {
                String playerId = doc.getString("userId");
                String displayName = doc.getString("displayName");
                Integer score = doc.get("score", Integer.class);
                Integer placement = doc.get("placement", Integer.class);

                Log.d(TAG, "Got Player " + displayName);

                if (playerId ==  null && displayName != null) {
                    Friend friend = new Friend();
                    friend.id = playerId;
                    friend.displayName = displayName;
                    friend.friendUId = playerId;

                    Player knownPlayer = new Player(friend, score, placement);
                    scoresAdapter.playerList.add(knownPlayer);
                    scoresAdapter.notifyDataSetChanged();
                    continue;
                }

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


    private void getEventDetails(String eventId) {
        if (currentUser == null) {
            Log.e(TAG, "Failed to get event details: User is not logged in.");
            return;
        }

        if (eventId.isEmpty()) return;

        DocumentReference eventRef = db.collection("events")
                .document(eventId);

        eventRef.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                Log.e(TAG, "Event " + eventId + " not found.");
                return;
            }

            eventItem = new Event();
            eventItem.id = snap.getId();
            eventItem.createdAt = snap.getTimestamp("createdAt");
            eventItem.endedAt = snap.getTimestamp("endedAt");
            eventItem.scheduledAt = snap.getTimestamp("scheduledAt");
            eventItem.title = snap.getString("title");
            eventItem.visibility = snap.getString("visibility");
            eventItem.status = snap.getString("status");
            eventItem.hostId = snap.getString("hostId");
            eventItem.notes = snap.getString("notes");

            if(eventItem.status.equals("past")) {
                saveButton.setEnabled(false);
            }

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error getting event " + eventId + ": " + e.getMessage());
        });
    }
    private void getHost() {
        // Search match for host id.
        if (eventId.isEmpty() || eventId == null) {
            db.collection("users").document(matchItem.hostId).get().addOnSuccessListener(s -> {
                if (!s.exists()) return;

                Friend host = new Friend();
                host.id = s.getId();
                host.displayName = s.getString("displayName");
                host.friendUId = s.getString("uid");

                Log.d(TAG, "HOST NAME: " + host.displayName);
                Log.d(TAG, "HOST ID: " + host.id);
                Log.d(TAG, "HOST UID: " + host.friendUId);

                hostUser = host;
                inviteeList.add(host);
                inviteeAdapter.notifyDataSetChanged();

                enableHostOptions();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get host info: " + e.getMessage());
            });
            return;
        }

        // Search event for host.
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

                    hostUser = host;
                    inviteeList.add(host);
                    inviteeAdapter.notifyDataSetChanged();

                    enableHostOptions();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get host info: " + e.getMessage());
                });
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to find event info: " + e.getMessage());
        });

    }

    public void enableHostOptions() {
        if (!currentUser.getUid().equals(hostUser.id)) {
            return;
        }

        // Set match buttons available to host.
        startMatch = findViewById(R.id.button_startMatch);
        endMatch = findViewById(R.id.button_endMatch);

        if (startMatch != null) {
            startMatch.setVisibility(Button.VISIBLE);
            startMatch.setOnClickListener(v -> {
                matchItem.startedAt = Timestamp.now();
                endMatch.setEnabled(true);
                startMatch.setEnabled(false);
                if (!matchId.isEmpty()) uploadMatch();
            });
        }
        if (endMatch != null) {
            endMatch.setVisibility(Button.VISIBLE);
            endMatch.setOnClickListener(v -> {
                matchItem.endedAt = Timestamp.now();
                endMatch.setEnabled(false);

                // assuming the game is a quickplay match.
                if (eventId.isEmpty() && !matchId.isEmpty()) {
                    uploadUserMatchMetrics();
                }
                if (!matchId.isEmpty()) uploadMatch();


                finish();
            });
        }
    }

    private void getFriends() {
        if (currentUser == null) return;

        db.collection("users")
                .document(matchItem.hostId)
                .collection("friends")
                .get()
                .addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) return;

                    for (DocumentSnapshot docSnap: snaps) {
                        String id = docSnap.getId();
                        String friendUid = docSnap.getString("uid");
                        String displayName = docSnap.getString("displayName");

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
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user friends: " + e.getMessage());
                });
    }
    private void getInvitees() {
        if (eventId.isEmpty() || eventId == null) {
            getHost();
            getFriends();
            return;
        }

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
        matchItem.updatedAt = Timestamp.now();
//        matchItem.endedAt = Timestamp.now();
        // Timestamps will have been set by the showDateTime interface.

        // Connect values from the match object to the hashmap to be uploaded.
        HashMap<String, Object> match = new HashMap<>();
        match.put("eventId", matchItem.eventId);
        match.put("notes", matchItem.notes);
        match.put("rules_variant", matchItem.rulesVariant);
        match.put("startedAt", matchItem.startedAt);
        match.put("endedAt", matchItem.endedAt);
        match.put("imageUrl", matchItem.imageUrl);
        match.put("updatedAt",  matchItem.updatedAt);

        if (game.id != null) {
            match.put("gameRef", db.collection("games").document(game.id));
        } else {
            match.put("gameRef", game.id);
        }

        if (matchItem.hostId == null || matchItem.hostId.isEmpty()) {
            matchItem.hostId = currentUser.getUid();
        }
        match.put("hostId", matchItem.hostId);


        // Update the match
        if (!matchId.isEmpty()) {
            db.collection("matches").document(matchItem.id).set(match)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully updated match database element " + matchItem.id + ".");
                        Toast.makeText(this, "Saved Game", Toast.LENGTH_SHORT).show();
                        uploadUserGamesHosted(uid, game);
                        uploadUserGamesPlayed(uid, game);
                        scoresAdapter.uploadPlayerScores(db, currentUser, matchId);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update match database element " + matchItem.id + ": " + e.getMessage());
                    });
        } else {
            // Upload the new match and add a reference to it in the event subcollection "matches"
//            match.replace("startedAt", Timestamp.now());
            db.collection("matches").add(match)
                    .addOnSuccessListener(documentReference -> {
                        matchItem.id = documentReference.getId();
                        Log.d(TAG, "Saved Match: " + matchItem.id);
                        Toast.makeText(this, "Saved Game", Toast.LENGTH_SHORT).show();

                        uploadUserGamesHosted(uid, game);
                        uploadUserGamesPlayed(uid, game);
                        scoresAdapter.uploadPlayerScores(db, currentUser, matchItem.id);


                        HashMap<String, Object> eventMatchHash = new HashMap<>();

                        // Match is tied to an event
                        if (!eventId.equals("")) {
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
                        }
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
//            matchItem.startedAt = Timestamp.now();
            getFriends();
            return;
        }

        DocumentReference matchRef = db.collection("matches")
                .document(matchId);

        matchRef.get().addOnSuccessListener(snap -> {
            if (snap == null) {
                Log.d(TAG, "Match " + matchId + " was not found.");
                return;
            }

            String id = snap.getId();
            Timestamp endedAt = snap.getTimestamp("endedAt");
//            matchItem.gameId = snap.getId();
            DocumentReference gameRef = snap.getDocumentReference("gameRef");
            Timestamp startedAt = snap.getTimestamp("startedAt");
            String notes = snap.getString("notes");
            String eventIdResult = snap.getString("eventId");
            String rulesVariantResult = snap.getString("rules_variant");
            String hostId = snap.getString("hostId");

            if (id != null) matchItem.id = id;
            if (endedAt != null) matchItem.endedAt = endedAt;
            if (gameRef != null) matchItem.gameRef = gameRef;
            if (startedAt != null) matchItem.startedAt = startedAt;
            if (notes != null) matchItem.notes = notes;
            if (eventIdResult != null) matchItem.eventId = eventIdResult;
            if (rulesVariantResult != null) matchItem.rulesVariant = rulesVariantResult;
            if (hostId != null) matchItem.hostId = hostId;

            setMatchDetails(matchItem);

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get match " + matchId + " details: " + e.getMessage());
        });
    }

    private void setMatchDetails(Match match) {
        if (match.startedAt != null) {
            startMatch.setEnabled(false);
            if (match.endedAt == null) endMatch.setEnabled(true);
        }


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

    public void uploadGameInfo() {
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
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Failed to update board game database element " + gameSummary.id + ": " + e.getMessage());
                });
    }

    public Match getMatchItem() {
        return matchItem;
    }

    public void setMatchStart(Timestamp start) {
        matchItem.startedAt = start;
    }

    public void setMatchEnd(Timestamp end) {
        matchItem.endedAt = end;
    }

    private void uploadUserMatchMetrics() {
        // Get user reference from players involved in each match.
        Match m = getMatchItem();
        m.playersRef.get().addOnSuccessListener(snaps -> {
            if (snaps.isEmpty()) {
                Log.d(TAG, "No players in match " + m.id);
                return;
            }

            List<Player> matchResults = new ArrayList<>();
            for (DocumentSnapshot player: snaps) {
                Player user = new Player();
                user.friend.id = player.getString("userId");
                user.placement = player.get("placement", Integer.class);
                user.score = player.get("score", Integer.class);

                matchResults.add(user);
            }

            // Update each users metrics
            for (Player p: matchResults) {
                if (p.friend.id == null || p.friend.id.isEmpty()) return;

                DocumentReference gamesPlayedRef = db.collection("users")
                        .document(p.friend.id)
                        .collection("metrics")
                        .document("games_played");

                // Update the user's games_played count
                gamesPlayedRef.get().addOnSuccessListener(snap -> {
                    Integer gamesPlayed = 1;
                    HashMap<String, Object> gamesPlayedHash = new HashMap<>();

                    if (!snap.exists()) {
                        gamesPlayedHash.put("count", gamesPlayed);
                        gamesPlayedRef.set(gamesPlayedHash).addOnSuccessListener(v -> {
                            Log.d(TAG, "Successfully updated user games_played count.");
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update user games_played count: " + e.getMessage());
                        });

                        return;
                    }

                    gamesPlayedHash.put("count", snap.get("count", Integer.class) + gamesPlayed);
                    gamesPlayedRef.set(gamesPlayedHash);
                });


                // Update the user's game_metrics
                CollectionReference userMetrics = db
                        .collection("users")
                        .document(p.friend.id)
                        .collection("metrics")
                        .document("games_played")
                        .collection("game_metrics");

                DocumentReference gameMetric = userMetrics.document(m.gameId);

                gameMetric.get().addOnSuccessListener(snap -> {
                    HashMap<String, Object> metricHash = new HashMap<>();

                    UserGameMetric result = new UserGameMetric();
                    // Set default values for user match results.
                    if (p.placement == 1) {
                        result.timesWon++;
                        result.winStreak++;
                        result.bestWinStreak++;
                        result.averageWinStreak++;
                        result.winningStreakCount++;
                    } else {
                        result.timesLost++;
                        result.lossStreak++;
                        result.worstLosingStreak++;
                    }

                    result.bestScore = p.score;
                    result.worstScore = p.score;
                    result.averageScore = p.score;
                    result.scoreTotal = p.score;

                    result.timesPlayed++;
                    result.firstTimePlayed = m.startedAt;
                    result.lastTimePlayed = m.endedAt;

                    // If user has played before, get user data from database to update
                    if (snap.exists()) {
                        result.timesPlayed = result.timesPlayed + snap.get("times_played", Integer.class);
                        result.firstTimePlayed = snap.getTimestamp("first_time_played");

                        // Score related ===========================
                        result.scoreTotal = p.score +  snap.get("score_total", Integer.class);
                        result.bestScore = snap.get("best_score", Integer.class);
                        result.worstScore = snap.get("worst_score", Integer.class);
                        result.averageScore = result.scoreTotal / result.timesPlayed;

                        if (p.score > result.bestScore) result.bestScore = p.score;
                        if (p.score < result.worstScore) result.worstScore = p.score;


                        // Win/Loss related ===========================
                        result.timesWon = result.timesWon + snap.get("times_won", Integer.class);
                        result.timesLost = result.timesLost + snap.get("times_lost", Integer.class);
                        result.bestWinStreak = snap.get("best_win_streak", Integer.class);
                        result.winningStreakCount = snap.get("win_streak_count", Integer.class);
                        result.averageWinStreak = snap.get("average_win_streak", Integer.class);
                        result.worstLosingStreak = snap.get("worst_losing_streak", Integer.class);

                        // If user won, keep the loss streak set to 0 and update win streak info.
                        if (result.winStreak > result.lossStreak) {
                            Integer existingStreak = snap.get("win_streak", Integer.class);

                            if (existingStreak > 0) {   // Already on win streak
                                result.averageWinStreak = result.timesWon / result.winningStreakCount;
                            } else {                    // New win streak
                                result.winningStreakCount++;
                                result.averageWinStreak = result.timesWon / result.winningStreakCount;
                            }
                            result.winStreak = result.winStreak + existingStreak;

                            if (result.winStreak > result.bestWinStreak) result.bestWinStreak = result.winStreak;
                        }

                        // If user lost, keep the win streak set to 0 and update loss streak info.
                        else {
                            Integer existingLosingStreak = snap.get("loss_streak", Integer.class);

                            result.lossStreak = result.lossStreak + existingLosingStreak;
                            if (result.lossStreak > result.worstLosingStreak) result.worstLosingStreak = result.lossStreak;
                        }
                    }

                    // Reference to game details
                    metricHash.put("game_ref", m.gameRef);

                    // Win results
                    metricHash.put("times_won", result.timesWon);
                    metricHash.put("win_streak", result.winStreak);
                    metricHash.put("best_win_streak", result.bestWinStreak);
                    metricHash.put("average_win_streak", result.averageWinStreak);
                    metricHash.put("win_streak_count", result.winningStreakCount);

                    // Loss results
                    metricHash.put("times_lost", result.timesLost);
                    metricHash.put("loss_streak", result.lossStreak);
                    metricHash.put("worst_losing_streak", result.worstLosingStreak);

                    // Score results
                    metricHash.put("best_score", result.bestScore);
                    metricHash.put("worst_score", result.worstScore);
                    metricHash.put("average_score", result.averageScore);
                    metricHash.put("score_total", result.scoreTotal);

                    // Timestamp results
                    metricHash.put("times_played", result.timesPlayed);
                    metricHash.put("first_time_played", result.firstTimePlayed);
                    metricHash.put("last_time_played", result.lastTimePlayed);

                    gameMetric.set(metricHash).addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully updated user game metrics.");
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update user game metrics: " + e.getMessage());
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to find user game metrics: " + e.getMessage());
                });


            }
        });

    }
}

