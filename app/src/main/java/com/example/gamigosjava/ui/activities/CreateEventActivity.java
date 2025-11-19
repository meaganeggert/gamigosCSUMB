package com.example.gamigosjava.ui.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.Event;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.OnDateTimePicked;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEventActivity extends BaseActivity {
    String TAG = "Create Event";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    BGG_API api;

    // Handle on match forms
//    private LinearLayout matchFormContainerHandle;

    // Values used for uploading/validation
    private Calendar calendar = Calendar.getInstance();
    private Event eventItem;
    private Date eventStart, matchStart, matchEnd; // May not be useful at the moment
    private TextView eventStartText;
    private EditText titleText, notesText;
    private Spinner visibilityDropdown, statusDropdown;
    private ArrayAdapter<Friend> friendAdapter;
//    private ArrayAdapter<GameSummary> userGameAdapter;
//    private ArrayAdapter<GameSummary> apiGameAdapter;

    List<String> visibilityList;
    List<Friend> friendList;
//    List<Match> matchList;
//    List<GameSummary> userGameList;
//    List<GameSummary> apiGameList;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        eventItem = new Event();
//        matchList = new ArrayList<>();
//        userGameList = new ArrayList<>();
//        apiGameList = new ArrayList<>();
        friendList = new ArrayList<>();
        visibilityList = new ArrayList<>();
        visibilityList.add("Private");
        visibilityList.add("Friends");
        visibilityList.add("Public");

        friendAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                friendList
        );
        friendAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

//        userGameAdapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_spinner_dropdown_item,
//                userGameList
//        );
//        userGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//
//        apiGameAdapter = new ArrayAdapter<>(
//                this,
//                android.R.layout.simple_spinner_dropdown_item,
//                apiGameList
//        );
//        apiGameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Important to make these two calls after the adapters have been set.
        getFriends();
//        getGames();

        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_create_event);

        // Set title for NavBar
        setTopTitle("Create Event");


        // ===================================Event Details=========================================
        // Add event form to the page and set the needed textViews/buttons/dropdowns/etc.
        setChildLayoutForm(R.layout.fragment_event_form, R.id.eventFormContainer);



        // ===================================Match Details=========================================
//        matchFormContainerHandle = findViewById(R.id.matchFormContainer);

        // Add new match details form on button click.
//        Button addMatchButton = findViewById(R.id.button_addMatch);
//        if (addMatchButton != null) {
//            addMatchButton.setOnClickListener(v -> {
//                addMatchForm();
//                });
//        } else {
//            Log.e(TAG, "Match creation button not found");
//        }
//
//        // Remove match details form on button click only if a match form was added.
//        Button removeMatchButton = findViewById(R.id.button_removeMatch);
//        if (removeMatchButton != null) {
//            removeMatchButton.setOnClickListener(v -> {
//                if (matchFormContainerHandle.getChildCount() > 0) {
//                    removeMatchForm();
//                } else {
//                    Toast.makeText(this, "No matches to remove.", Toast.LENGTH_SHORT).show();
//                }
//
//            });
//        } else {
//            Log.e(TAG, "Match creation button not found");
//        }



        // =====================================Finish==============================================
        // Finish and upload event details
        View createEventButton = findViewById(R.id.button_createEvent);
        if (createEventButton != null) {
            createEventButton.setOnClickListener(v -> {
                uploadAllForms();
            });
        } else {
            Log.e(TAG, "Create Event Button not found");
        }

        Button cancel = findViewById(R.id.button_cancelCreateEvent);
        if (cancel != null) {
            cancel.setOnClickListener(v -> finish());
        }
    }




    // =======================================Main Form Function Helpers============================
    private void setChildLayoutForm(@LayoutRes int layoutRes, @IdRes int containerId) {
        ViewGroup container = findViewById(containerId);
        LayoutInflater.from(this).inflate(layoutRes, container, true);

        titleText = findViewById(R.id.editText_eventTitle);
        notesText = findViewById(R.id.editTextTextMultiLine_eventNotes);

        // Set up schedule creation
        eventStartText = findViewById(R.id.textView_eventStart);
        Button selectDateButton = findViewById(R.id.button_selectSchedule);
        selectDateButton.setOnClickListener(v -> {
            // When user input for date/time is complete, set the necessary data.
            showDateTimePicker(eventStart, date -> {
                eventStart = date;
                eventStartText.setText(date.toString());
                eventItem.scheduledAt = new Timestamp(date);
            });
        });

        // Set up the values for the visibility dropdown
        visibilityDropdown = findViewById(R.id.dropdown_visibility);
        setDropdown(visibilityDropdown, visibilityList);

        // Create first friend dropdown, and allow additional friend dropdowns
        // on "+ friend" button click, or remove friend dropdown on "- Friend"
        // button click.
        LinearLayout friendLayout = findViewById(R.id.linearLayout_friend);
        if (friendLayout != null) {
//            setFriendDropdown(friendLayout);

            // Add additional friend dropdown
            View addFriend = findViewById(R.id.button_addFriend);
            if (addFriend != null) {
                addFriend.setOnClickListener(v -> {
                    if (!friendList.isEmpty() && friendLayout.getChildCount() < friendList.size()) {
                        setFriendDropdown(friendLayout);
                    } else {
                        Toast.makeText(this, "No friend to add.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Add friend button not found");
            }

            // Remove additional friend dropdown
            View removeFriend = findViewById(R.id.button_removeFriend);
            if (removeFriend != null) {
                removeFriend.setOnClickListener(v -> {
                    if (friendLayout.getChildCount() > 0) {
                        removeDropdown(friendLayout);
                    } else {
                        Toast.makeText(this, "No friend to remove.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e(TAG, "Remove friend button not found");
            }
        } else {
            Log.e(TAG, "Friend layout not found.");
        }
    }

//    private void addMatchForm() {
//        api = BGGService.getInstance();
//
//        View match = LayoutInflater.from(this).inflate(R.layout.fragment_match_form, matchFormContainerHandle, false);
//        matchFormContainerHandle.addView(match);
//
//        int matchIndex = matchFormContainerHandle.getChildCount() - 1;
//        matchList.add(matchIndex, new Match());
//
//        TextView matchStartText = match.findViewById(R.id.textView_matchStart);
//        TextView matchEndText = match.findViewById(R.id.textView_matchEnd);
//
//        // Set board game dropdown for each new match form.
//        Spinner gameName = matchFormContainerHandle
//                .getChildAt(matchFormContainerHandle.getChildCount()-1)
//                .findViewById(R.id.dropdown_gameName);
//
//        if (gameName != null) {
//            gameName.setAdapter(userGameAdapter);
//        } else {
//            Log.e(TAG, "Game name dropdown not found");
//        }
//
//        SearchView search = match.findViewById(R.id.searchView_bggSearch);
//        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextChange(String s) {
//                if (s.isEmpty()) {
//                    Log.d(TAG, "Added gameSelection via userDB");
//                    gameName.setAdapter(userGameAdapter);
//                }
//
//                return true;
//            }
//
//            @Override
//            public boolean onQueryTextSubmit(String s) {
//                if (!s.isEmpty()) {
//                    Log.d(TAG, "added gameSelection via API");
//                    fetchGamesForQuery(s);
//                    gameName.setAdapter(apiGameAdapter);
//                    search.clearFocus();
//                }
//                return true;
//            }
//        });
//
//        Button matchStartButton = match.findViewById(R.id.button_selectTimeStart);
//        if (matchStartButton != null) {
//            matchStartButton.setOnClickListener(v2 -> {
//                showDateTimePicker(matchStart, date -> {
//                    matchStart = date;
//                    matchStartText.setText(date.toString());
//                    matchList.get(matchIndex).startedAt = new Timestamp(date);
//                });
//            });
//        }
//
//        Button matchEndButton = match.findViewById(R.id.button_selectTimeEnd);
//        if (matchEndButton != null) {
//            matchEndButton.setOnClickListener(v2 -> {
//                showDateTimePicker(matchEnd, date -> {
//                    matchEnd = date;
//                    matchEndText.setText(date.toString());
//                    matchList.get(matchIndex).endedAt = new Timestamp(date);
//                });
//            });
//        }
//    }




    // ===================================Database Helpers==========================================

    // This function was originally meant to to upload the event form to firebase separately
    // from the matches forms, but we want the matches to upload after, only on success of uploading
    // the event. So at the bottom, in the .onSuccessListener the uploadMatches function will be called.
    private void uploadAllForms() {
        if (currentUser == null) {
            Log.e(TAG, "Must be logged in.");
            return;
        }

        eventItem.hostId = currentUser.getUid();
        eventItem.title = titleText.getText().toString();
        eventItem.visibility = visibilityDropdown.getSelectedItem().toString().toLowerCase();
        eventItem.status = "planned";
        eventItem.notes = notesText.getText().toString();
        eventItem.createdAt = Timestamp.now();
        eventItem.endedAt = null;

        // Small validation
        if (eventItem.title.isEmpty()) {
            Toast.makeText(this, "Title Required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (eventStartText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Event Start Required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Connect user values to hashmap to upload.
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("hostId", eventItem.hostId);
        eventData.put("title", eventItem.title);
        eventData.put("visibility", eventItem.visibility);
        eventData.put("status", eventItem.status);
        eventData.put("notes", eventItem.notes);
        eventData.put("scheduledAt", eventItem.scheduledAt);
        eventData.put("createdAt", eventItem.createdAt);
        eventData.put("endedAt", eventItem.endedAt);

        // Upload the event then upload the match if the event was uploaded successfully.
        db.collection("events")
                .add(eventData)

                // only when the event gets uploaded, should we upload the matches, friend invites, etc.
                .addOnSuccessListener(documentReference -> {
                    eventItem.id = documentReference.getId();
                    Log.d(TAG, "Saved Event: " + eventItem.id);
                    Toast.makeText(this, "Event uploaded successfully.", Toast.LENGTH_SHORT).show();
                    uploadFriendInvites();
//                    uploadMatches();
                    finish();

                })

                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save event: " + e.getMessage());
                    Toast.makeText(this, "Failed to save event.", Toast.LENGTH_SHORT).show();
                });
    }

    // upload friend invites to database
    private void uploadFriendInvites() {
        List<Friend> friendsInvited = new ArrayList<>();
        LinearLayout friendSection = findViewById(R.id.linearLayout_friend);

        // Filters out repeated invites.
        for (int i = 0; i < friendSection.getChildCount(); i++) {
            Spinner friendSpinner = (Spinner) friendSection.getChildAt(i);
            Friend friendItem = (Friend) friendSpinner.getSelectedItem();

            if (!friendsInvited.contains(friendItem)) {
                friendsInvited.add(friendItem);
            }
        }

        // Uploads friend invites to the database one by one.
        for (int i = 0; i < friendsInvited.size(); i++) {
            Friend friendItem = friendsInvited.get(i);

            DocumentReference inviteRef = db.collection("events")
                    .document(eventItem.id)
                    .collection("invitees")
                    .document(friendItem.id);

            Map<String, Object> invite = new HashMap<>();
            invite.put("status", "invited");
            invite.put("userRef", db.collection("users").document(friendItem.friendUId));

            inviteRef.set(invite).addOnSuccessListener(v -> {
                        Log.d(TAG, "Successfully uploaded friend invites");
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Failed to upload friend invite: " + e.getMessage());
                    });
        }

    }

    // Uploads the matches to firebase.
//    private void uploadMatches() {
//        if (currentUser == null) {
//            Log.e(TAG, "Must be logged in.");
//            return;
//        }
//
//        if (matchFormContainerHandle.getChildCount() < 1) {
//            Log.d(TAG, "No matches to upload, skipping uploadMatches");
//            Toast.makeText(this, "No matches uploaded.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String uid = currentUser.getUid();
//
//        for (int i = 0; i < matchFormContainerHandle.getChildCount(); i++) {
//            View matchForm = matchFormContainerHandle.getChildAt(i);
//            Match matchItem = matchList.get(i);
//
//            // Get text values from the UI
//            EditText ruleChangeValue = matchForm.findViewById(R.id.editTextTextMultiLine_rules);
//            EditText notesValue = matchForm.findViewById(R.id.editTextTextMultiLine_notes);
//            Spinner gameName = matchForm.findViewById(R.id.dropdown_gameName);
//            GameSummary game = (GameSummary) gameName.getSelectedItem();
//
//            // Connect values to the match object.
//            matchItem.eventId = eventItem.id;
//            matchItem.notes = notesValue.getText().toString();
//            matchItem.rulesVariant = ruleChangeValue.getText().toString();
//            matchItem.gameId = game.id;
//            // Timestamps will have been set by the showDateTime interface.
//
//            // Connect values from the match object to the hashmap to be uploaded.
//            HashMap<String, Object> match = new HashMap<>();
//            match.put("gameId", matchItem.gameId);
//            match.put("eventId", matchItem.eventId);
//            match.put("notes", matchItem.notes);
//            match.put("rules_variant", matchItem.rulesVariant);
//            match.put("startedAt", matchItem.startedAt);
//            match.put("endedAt", matchItem.endedAt);
//
//            // Upload the match
//            db.collection("matches").add(match)
//                    .addOnSuccessListener(documentReference -> {
//                        matchItem.id = documentReference.getId();
//                        Log.d(TAG, "Saved Match: " + matchItem.id);
//
//                        uploadUserGamesHosted(uid, game);
//                        uploadUserGamesPlayed(uid, game);
//
//                    })
//                    .addOnFailureListener(e -> {
//                        Log.e(TAG, "Failed to save match: " + e.getMessage());
//                        Toast.makeText(this, "Failed to upload a match.", Toast.LENGTH_SHORT).show();
//                    });
//
//            // TODO: Change database layout. (i.e. make the hosted/played collections different, add userBGGCollection, reference matches in event collection)
//        }
//
//    }
//
//    private void uploadUserGamesHosted(String uid, GameSummary gameSummary) {
//        if (gameSummary.id == null) return;
//
//        DocumentReference gamePlayed = db.collection("users")
//                .document(uid)
//                .collection("gamesHosted")
//                .document(gameSummary.id);
//
//        HashMap<String, Object> gameHash = new HashMap<>();
//        gameHash.put("id", gameSummary.id);
//        gameHash.put("title", gameSummary.title);
//        gameHash.put("imageUrl", gameSummary.imageUrl);
//        gameHash.put("maxPlayers", gameSummary.maxPlayers);
//        gameHash.put("minPlayers", gameSummary.minPlayers);
//        gameHash.put("playingTime", gameSummary.playingTime);
//        gamePlayed.set(gameHash).addOnSuccessListener(v -> {
//            Log.d(TAG, "Successfully updated user's hosted game database element: " + gameSummary.id);
//        })
//        .addOnFailureListener(e -> {
//            Log.d(TAG, "Failed to update user's hosted game database element " + gameSummary.id + ": " + e.getMessage());
//        });
//
//    }
//
//    private void uploadUserGamesPlayed(String uid, GameSummary gameSummary) {
//        if (gameSummary.id == null) return;
//
//        DocumentReference gamePlayed = db.collection("users")
//                .document(uid)
//                .collection("gamesPlayed")
//                .document(gameSummary.id);
//
//        HashMap<String, Object> gameHash = new HashMap<>();
//        gameHash.put("id", gameSummary.id);
//        gameHash.put("title", gameSummary.title);
//        gameHash.put("imageUrl", gameSummary.imageUrl);
//        gameHash.put("maxPlayers", gameSummary.maxPlayers);
//        gameHash.put("minPlayers", gameSummary.minPlayers);
//        gameHash.put("playingTime", gameSummary.playingTime);
//        gamePlayed.set(gameHash).addOnSuccessListener(v -> {
//            Log.d(TAG, "Successfully updated user's played game database element: " + gameSummary.id);
//        })
//        .addOnFailureListener(e -> {
//            Log.d(TAG, "Failed to update user's played game database element " + gameSummary.id + ": " + e.getMessage());
//        });;
//    }

    // This gets the users friends from the database and loads it into an array adapter to be used
    // in the dropdown of friends to invite.
    private void getFriends() {

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        CollectionReference friendsRef = db
                .collection("users")
                .document(uid)
                .collection("friends");

        friendsRef
                .orderBy("displayName")
                .get()
                .addOnSuccessListener(snap -> {
                    friendList.clear();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String id = d.getId();
                        String friendUid = d.getString("uid");
                        String displayName = d.getString("displayName");
                        friendList.add(new Friend(id, friendUid, displayName));
                    }
                    friendAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load friends: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

//    private void getGames() {
//        if (currentUser == null) {
//            Toast.makeText(this, "User is not logged in.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String uid = currentUser.getUid();
//
//        userGameList.clear();
//        // Get games the user previously played
//        CollectionReference gamesRef = db
//                .collection("users")
//                .document(uid)
//                .collection("gamesPlayed");
//
//        gamesRef
//                .orderBy("title")
//                .get()
//                .addOnSuccessListener(this::applyKnownUserGames)
//                .addOnFailureListener(e -> {
//                    Log.d(TAG, "Failed to find games previously played.");
//                });
//
//        // Get games the user previously hosted.
//        gamesRef = db
//                .collection("users")
//                .document(uid)
//                .collection("gamesHosted");
//
//        gamesRef
//                .orderBy("title")
//                .get()
//                .addOnSuccessListener(this::applyKnownUserGames)
//                .addOnFailureListener(e -> {
//                    Log.d(TAG, "Failed to find games previously hosted.");
//                });
//
//        // Get games the user owns in BGG
//        gamesRef = db
//                .collection("users")
//                .document(uid)
//                .collection("userBGGCollection");
//
//        gamesRef
//                .orderBy("title")
//                .get()
//                .addOnSuccessListener(this::applyKnownUserGames)
//                .addOnFailureListener(e -> {
//                    Log.d(TAG, "Failed to user BGG collection.");
//                });
//
//        userGameList.add(new GameSummary(null, "Search BGG", null, null, null, null));
//        userGameAdapter.notifyDataSetChanged();
//    }

//    private void applyKnownUserGames(QuerySnapshot snap) {
//        if (snap.isEmpty()) {
//            return;
//        }
//
//        for (DocumentSnapshot d : snap.getDocuments()) {
//            String id = d.getId();
//            String title = d.getString("title");
//            String imageUrl = d.getString("imageUrl");
//            Integer minPlayers = d.get("minPlayers", Integer.class);
//            Integer maxPlayers = d.get("maxPlayers", Integer.class);
//            Integer playTime = d.get("time", Integer.class);
//
//            GameSummary game = new GameSummary(id, title, imageUrl,
//                    minPlayers, maxPlayers, playTime);
//
//            if (!userGameList.contains(game)) {
//                userGameList.add(new GameSummary(id, title, imageUrl,
//                        minPlayers, maxPlayers, playTime));
//            }
//
//        }
////        gameAdapter.notifyDataSetChanged();
//    }





    // ================================== BGG API Helpers ==========================================
//    private void fetchGamesForQuery(String query) {
//        api.search(query, "boardgame").enqueue(new Callback<SearchResponse>() {
//            @Override public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> resp) {
//                if (!resp.isSuccessful()) {
//                    Toast.makeText(CreateEventActivity.this, "Response not successful", Toast.LENGTH_SHORT).show();
//                    String err = null;
//                    try { err = resp.errorBody() != null ? resp.errorBody().string() : null; } catch (Exception ignored) {}
//                    Log.e("BGG", "Search HTTP " + resp.code() + " " + err);
//                    return;
//                }
//                if (resp.body() == null || resp.body().items == null || resp.body().items.isEmpty()) {
//                    Toast.makeText(CreateEventActivity.this, "No search results", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                // Build CSV of a few IDs to batch the /thing call
//                StringBuilder ids = new StringBuilder();
//                int max = Math.min(10, resp.body().items.size());
//                for (int i = 0; i < max; i++) {
//                    if (i > 0) ids.append(',');
//                    ids.append(resp.body().items.get(i).id);
//                }
//                fetchThingDetails(ids.toString());
//            }
//
//            @Override public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
//                Toast.makeText(CreateEventActivity.this, "Search failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
//    }
//
//    private void fetchThingDetails(String idsCsv) {
//        api.thing(idsCsv, 0).enqueue(new Callback<ThingResponse>() {
//            @Override public void onResponse(Call<ThingResponse> call, Response<ThingResponse> resp) {
//                if (!resp.isSuccessful() || resp.body() == null || resp.body().items == null) {
//                    Toast.makeText(CreateEventActivity.this, "No details", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                List<GameSummary> list = new ArrayList<>();
//                for (BGGItem it : resp.body().items) {
//                    list.add(BGGMappers.toSummary(it));
//                }
//
//                // 🟢 Add this to confirm data size
//                Toast.makeText(CreateEventActivity.this, "Loaded " + list.size() + " games", Toast.LENGTH_SHORT).show();
//                Log.d("BGG", "Loaded " + list.size() + " game(s)"); // debug
//
//                apiGameList.clear();
//                apiGameList.addAll(list);
//                apiGameAdapter.notifyDataSetChanged();
//            }
//
//            @Override public void onFailure(Call<ThingResponse> call, Throwable t) {
//                Toast.makeText(CreateEventActivity.this, "Thing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
//            }
//        });
//    }





    // =======================================Global Function Helpers========================================
    private void setDropdown(Spinner dropdown, List<String> list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, list);
        dropdown.setAdapter(adapter);
    }

//    private void addDropdown(LinearLayout layout, List<String> list) {
//        Spinner newSpinner = new Spinner(this);
//        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        newSpinner.setId(View.generateViewId());
//        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);
//
//        setDropdown(newSpinner, list);
//        layout.addView(newSpinner);
//    }

    private void removeDropdown(LinearLayout layout) {
        layout.removeViewAt(layout.getChildCount() - 1);
    }

    // Show the user an interface to select a date/time
    private void showDateTimePicker(@Nullable Date initial, OnDateTimePicked callBack) {
        if (initial != null) calendar.setTime(initial);
        // User selects the date
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, day);

                    // User Selects the time of day
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (timeView, hour, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                // HEADS UP: Here is where the user is done selecting the time.
                                // Because this function uses lambda and such, we cannot just
                                // refer to an outside function, so instead we have to use a
                                // callback. In short, whenever the user is done selecting the
                                // date and time, use the callback to set the passed in date object.
                                callBack.onPicked(calendar.getTime());

                            }, calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE), false); // Show time via am/pm
                    timePicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    // Removes the last match form created.
//    private void removeMatchForm() {
//        matchList.remove(matchFormContainerHandle.getChildCount()- 1 );
//        matchFormContainerHandle.removeViewAt(matchFormContainerHandle.getChildCount() - 1);
//    }
    // Set the friend list dropdown values to users friends
    private void setFriendDropdown(LinearLayout layout) {
        Spinner newSpinner = new Spinner(this);
        newSpinner.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSpinner.setId(View.generateViewId());
        newSpinner.setBackgroundResource(android.R.drawable.btn_dropdown);

        newSpinner.setAdapter(friendAdapter);
        layout.addView(newSpinner);
    }
}