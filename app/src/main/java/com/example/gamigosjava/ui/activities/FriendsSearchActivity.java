package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Algolia
import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsSearchActivity extends BaseActivity {
    private static final String ALGOLIA_APP_ID = "5LHKRX1QE8";
    private static final String ALGOLIA_SEARCH_KEY = "fbdb3c57a5235ca22af1c1d59fece002";
    private static final String ALGOLIA_INDEX_NAME = "users_name_asc";
    private static final int PAGE_SIZE = 25;
    private Index algoliaIndex;
    private EditText etSearch;
    private UserAdapter adapter;
    private final List<Map<String, Object>> userList = new ArrayList<>();
    private FirebaseUser currentUser;
    // pagination state
    private String currentQueryText = "";
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMore = true; // becomes false when Algolia returns 0 hits
    private FirebaseFirestore db;
    // for button states
    private final Set<String> myFriends = new HashSet<>();
    private final Set<String> myOutgoing = new HashSet<>();
    private final Set<String> myIncoming = new HashSet<>();
    // status constants
    private static final int STATUS_NONE = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_FRIEND = 2;
    private static final int STATUS_INCOMING = 3;
    private ListenerRegistration friendsListener;
    private ListenerRegistration incomingListener;
    private ListenerRegistration outgoingListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_friends_search);

        // Set title for NavBar
        setTopTitle("Friends");

        enableToolbarBackArrow();

        // Firebase user (to skip self)
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Algolia init
        Client algoliaClient = new Client(ALGOLIA_APP_ID, ALGOLIA_SEARCH_KEY);
        algoliaIndex = algoliaClient.getIndex(ALGOLIA_INDEX_NAME);

        etSearch = findViewById(R.id.etSearch);
        RecyclerView rvResults = findViewById(R.id.rvResults);
        Button btnSearch = findViewById(R.id.btnSearch);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(
                userList,
                new UserAdapter.FriendActionClickListener() {
                    @Override
                    public void onAddClick(Map<String, Object> user) {
                        onAddClicked(user);
                    }

                    @Override
                    public void onDenyClick(Map<String, Object> user) {
                        onDenyClicked(user);
                    }
                },
                this::onUserClicked
        );
        rvResults.setAdapter(adapter);

        rvResults.setAdapter(adapter);
        startFriendsListeners();

        // Scroll-to-bottom → load next page
        rvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int lastVisible = lm.findLastCompletelyVisibleItemPosition();
                if (!isLoading &&
                        hasMore &&
                        lastVisible == userList.size() - 1) {
                    // load next page of the SAME query
                    searchUsers(false);
                }
            }
        });

        // Button → new search
        btnSearch.setOnClickListener(v -> searchUsers(true));
    }

    private void startFriendsListeners() {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        // friends
        friendsListener = db.collection("users").document(myUid)
                .collection("friends")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    myFriends.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        myFriends.add(doc.getId());
                    }
                    refreshStatuses();
                });

        // outgoing friend requests
        outgoingListener = db.collection("users").document(myUid)
                .collection("friendRequests_outgoing")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    myOutgoing.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        myOutgoing.add(doc.getId());
                    }
                    refreshStatuses();
                });

        incomingListener = db.collection("users").document(myUid)
                .collection("friendRequests_incoming")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    myIncoming.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        myIncoming.add(doc.getId());
                    }
                    refreshStatuses();
                });
    }

    private void refreshStatuses() {
        for (Map<String, Object> user : userList) {
            String uid = (String) user.get("docId");
            if (uid == null) continue;

            if (myFriends.contains(uid)) {
                user.put("status", STATUS_FRIEND);
            } else if (myOutgoing.contains(uid)) {
                user.put("status", STATUS_PENDING);
            } else if (myIncoming.contains(uid)) {
                user.put("status", STATUS_INCOMING);
            } else {
                user.put("status", STATUS_NONE);
            }
        }
        adapter.notifyDataSetChanged();
    }



    /**
     * @param isNewSearch true = start from page 0 and clear list
     */
    private void searchUsers(boolean isNewSearch) {
        String queryText = etSearch.getText().toString().trim();
        if (isNewSearch) {
            if (queryText.isEmpty()) {
                Toast.makeText(this, "Enter a name to search", Toast.LENGTH_SHORT).show();
                return;
            }
            // reset state for new query
            currentQueryText = queryText;
            currentPage = 0;
            hasMore = true;
            userList.clear();
            adapter.notifyDataSetChanged();
        }

        if (!hasMore) return; // nothing left to load

        isLoading = true;

        // Algolia is case-insensitive + does substring (with your index settings),
        // so just send the text.
        Query query = new Query(currentQueryText)
                .setPage(currentPage)
                .setHitsPerPage(PAGE_SIZE);

        algoliaIndex.searchAsync(query, (content, e) -> runOnUiThread(() -> {
            isLoading = false;

            if (e != null) {
                Toast.makeText(FriendsSearchActivity.this,
                        "Search failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (content == null) {
                Toast.makeText(FriendsSearchActivity.this,
                        "No results",
                        Toast.LENGTH_SHORT).show();
                hasMore = false;
                return;
            }

            try {
                JSONArray hits = content.getJSONArray("hits");

                // case 1: Algolia really returned nothing
                if (hits.length() == 0) {
                    hasMore = false;
                    if (userList.isEmpty()) {
                        Toast.makeText(FriendsSearchActivity.this,
                                "No users found",
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // case 2: Algolia returned *something*, but maybe we skipped all of them
                int addedThisPage = 0;

                for (int i = 0; i < hits.length(); i++) {
                    JSONObject hit = hits.getJSONObject(i);

                    String uid = hit.optString("objectID", "");
                    // skip self
                    if (currentUser != null && uid.equals(currentUser.getUid())) {
                        continue;
                    }

                    //  Create user object to add to userlist

                    Map<String, Object> user = new HashMap<>();
                    user.put("docId", uid);
                    user.put("displayName", hit.optString("displayName", ""));
                    user.put("email", hit.optString("email", ""));
                    user.put("photoUrl", hit.optString("photoUrl", ""));
                    user.put("privacyLevel", hit.optString("privacyLevel", ""));

                    // set initial status based on loaded sets
                    if (myFriends.contains(uid)) {
                        user.put("status", STATUS_FRIEND);
                    } else if (myOutgoing.contains(uid)) {
                        user.put("status", STATUS_PENDING);
                    } else {
                        user.put("status", STATUS_NONE);
                    }

                    userList.add(user);
                    addedThisPage++;
                }

                // if we got hits, but all were filtered out (e.g. only yourself)
                if (addedThisPage == 0) {
                    // no more to load for this query
                    hasMore = false;
                    if (userList.isEmpty()) {
                        Toast.makeText(FriendsSearchActivity.this,
                                "No users found",
                                Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                // normal path
                adapter.notifyDataSetChanged();
                currentPage++;

            } catch (JSONException jsonException) {
                Toast.makeText(FriendsSearchActivity.this,
                        "Parse error: " + jsonException.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void onAddClicked(Map<String, Object> user) {
        if (currentUser == null) return;

        String myUid = currentUser.getUid();
        String otherUid = (String) user.get("docId");
        if (otherUid == null || otherUid.equals(myUid)) return;

        // already pending or friend? do nothing
        Object s = user.get("status");
        int status = (s instanceof Integer) ? (int) s : STATUS_NONE;
        if (status == STATUS_PENDING || status == STATUS_FRIEND) {
            return;
        }

        if (status == STATUS_INCOMING) {
            acceptIncomingRequest(otherUid, user);
            return;
        }

        sendFriendRequest(user, myUid, otherUid);
    }

    private void onDenyClicked(Map<String, Object> user) {
        String otherUid = (String) user.get("docId");
        if (currentUser == null || otherUid == null) return;
        String myUid = currentUser.getUid();

        var batch = db.batch();

        DocumentReference myIncomingRef = db.collection("users").document(myUid)
                .collection("friendRequests_incoming").document(otherUid);
        DocumentReference theirOutgoingRef = db.collection("users").document(otherUid)
                .collection("friendRequests_outgoing").document(myUid);

        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    user.put("status", STATUS_NONE);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Request denied", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void sendFriendRequest(Map<String, Object> user, String myUid, String otherUid) {
        WriteBatch batch = db.batch();

        DocumentReference myOutgoingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_outgoing")
                .document(otherUid);

        DocumentReference theirIncomingRef = db.collection("users")
                .document(otherUid)
                .collection("friendRequests_incoming")
                .document(myUid);

        Map<String, Object> reqData = new HashMap<>();
        reqData.put("createdAt", com.google.firebase.Timestamp.now());
        reqData.put("from", myUid);
        reqData.put("fromDisplayName", currentUser.getDisplayName());
        reqData.put("fromPhotoUrl", currentUser.getPhotoUrl());
        reqData.put("to", otherUid);
        reqData.put("toDisplayName", user.get("displayName"));
        reqData.put("toPhotoUrl", user.get("photoUrl"));

        batch.set(myOutgoingRef, reqData);
        batch.set(theirIncomingRef, reqData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // immediately mark locally as pending
                    user.put("status", STATUS_PENDING);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Request sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void acceptIncomingRequest(String otherUid, Map<String, Object> user) {
        String myUid = currentUser.getUid();

        db.collection("users").document(otherUid).get()
                .addOnSuccessListener(otherDoc -> {
                    String otherName = otherDoc.getString("displayName");
                    String otherPhoto = otherDoc.getString("photoUrl");

                    var batch = db.batch();

                    // my friends/{other}
                    DocumentReference myFriendRef = db.collection("users").document(myUid)
                            .collection("friends").document(otherUid);

                    // their friends/{me}
                    DocumentReference theirFriendRef = db.collection("users").document(otherUid)
                            .collection("friends").document(myUid);

                    // delete my incoming & their outgoing
                    DocumentReference myIncomingRef = db.collection("users").document(myUid)
                            .collection("friendRequests_incoming").document(otherUid);
                    DocumentReference theirOutgoingRef = db.collection("users").document(otherUid)
                            .collection("friendRequests_outgoing").document(myUid);

                    Map<String, Object> myFriendData = new HashMap<>();
                    myFriendData.put("uid", otherUid);
                    myFriendData.put("displayName", otherName);
                    myFriendData.put("photoUrl", otherPhoto);
                    myFriendData.put("createdAt", com.google.firebase.Timestamp.now());

                    Map<String, Object> theirFriendData = new HashMap<>();
                    theirFriendData.put("uid", currentUser.getUid());
                    theirFriendData.put("displayName", currentUser.getDisplayName());
                    theirFriendData.put("photoUrl", currentUser.getPhotoUrl());
                    theirFriendData.put("createdAt", com.google.firebase.Timestamp.now());

                    batch.set(myFriendRef, myFriendData);
                    batch.set(theirFriendRef, theirFriendData);
                    batch.delete(myIncomingRef);
                    batch.delete(theirOutgoingRef);

                    batch.commit()
                            .addOnSuccessListener(v -> {
                                // update local UI
                                user.put("status", STATUS_FRIEND);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void onUserClicked(Map<String, Object> user) {
        String otherUid = (String) user.get("docId");
        if (otherUid == null) return;

        Intent intent = new Intent(this, ViewUserProfileActivity.class);
        intent.putExtra("USER_ID", otherUid);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsListener != null) friendsListener.remove();
        if (outgoingListener != null) outgoingListener.remove();
        if (incomingListener != null) incomingListener.remove();
    }
}
