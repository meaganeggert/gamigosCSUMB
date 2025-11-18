package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FriendRequestRowAdapter;
import com.example.gamigosjava.ui.adapter.FriendsListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsLanding extends BaseActivity {
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private TextView tvRequestsHeader;
    private FriendsListAdapter friendsAdapter;
    private FriendRequestRowAdapter requestsAdapter;
    private final List<Map<String, Object>> friendlist = new ArrayList<>();
    private final List<FriendRequestRowAdapter.RequestRow> requestRows = new ArrayList<>();
    private ListenerRegistration friendsListener;
    private ListenerRegistration incomingListener;
    private ListenerRegistration outgoingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_friends_landing);

        // Set title for NavBar
        setTopTitle("Friends");

        //  Setting up variables for use in recycler view
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //  Setting up Requests Recycler View
        tvRequestsHeader = findViewById(R.id.tvRequestsHeader);
        RecyclerView rvRequests = findViewById(R.id.rvRequests);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        requestsAdapter = new FriendRequestRowAdapter(requestRows, new FriendRequestRowAdapter.RequestActionListener() {
            @Override
            public void onAccept(String uid) {
                acceptRequest(uid);
            }

            @Override
            public void onDecline(String uid) {
                declineRequest(uid);
            }

            @Override
            public void onRowClick(String uid) {
                Intent intent = new Intent(FriendsLanding.this, ViewUserProfileActivity.class);
                intent.putExtra("USER_ID", uid);
                startActivity(intent);
            }
        });
        rvRequests.setAdapter(requestsAdapter);

        //  Find button
        Button btnSearchFriends = findViewById(R.id.btnFriendSearchActivity);
        //  Set up click listener
        btnSearchFriends.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsLanding.this, FriendsSearchActivity.class);
            startActivity(intent);
        });

        //  Setting up recycler view
        // in FriendsLanding.onCreate(...)
        RecyclerView rvFriends = findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        friendsAdapter = new FriendsListAdapter(friendlist, new FriendsListAdapter.FriendActionListener() {
            @Override
            public void onProfileClick(Map<String, Object> friend) {
                String friendUid = (String) friend.get("uid");
                Intent intent = new Intent(FriendsLanding.this, ViewUserProfileActivity.class);
                intent.putExtra("USER_ID", friendUid);
                startActivity(intent);
            }

            @Override
            public void onMessageClick(Map<String, Object> friend) {
                String friendUid = (String) friend.get("uid");
                String friendName = (String) friend.get("displayName");
                ensureDmAndOpen(friendUid, friendName);
            }
        });
        rvFriends.setAdapter(friendsAdapter);
        listenForFriends();
        listenForIncomingRequests();
        listenForOutgoingRequests();
    }

    private String dmId(String a, String b) {
        return (a.compareTo(b) < 0) ? a + "_" + b : b + "_" + a;
    }

    private void ensureDmAndOpen(String otherUid, String title) {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String myUid = currentUser.getUid();
        String convoId = dmId(myUid, otherUid);
        DocumentReference convoRef = db.collection("conversations").document(convoId);

        convoRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                // open existing DM
                startActivity(
                        MessagesActivity.newIntent(
                                this,
                                convoId,
                                title,
                                otherUid,
                                false          // <-- DM, not group
                        )
                );
            } else {
                // create then open (hybrid model)
                Map<String, Object> data = new HashMap<>();
                data.put("participants", java.util.Arrays.asList(myUid, otherUid));
                data.put("isGroup", false);
                data.put("lastMessage", "");
                data.put("lastMessageAt", null);

                convoRef.set(data).addOnSuccessListener(unused -> {
                    // seed participantsData
                    createParticipantData(convoRef, myUid);
                    createParticipantData(convoRef, otherUid);
                    startActivity(
                            MessagesActivity.newIntent(
                                    this,
                                    convoId,
                                    title,
                                    otherUid,
                                    false      // <-- DM, not group
                            )
                    );
                });
            }
        });
    }


    private void createParticipantData(DocumentReference convoRef, String uid) {
        Map<String, Object> pd = new HashMap<>();
        pd.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        pd.put("lastReadAt", null);
        pd.put("unreadCount", 0);
        pd.put("role", "member");
        convoRef.collection("participantsData").document(uid).set(pd);
    }


    private void listenForFriends() {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String myUid = currentUser.getUid();

        friendsListener = db.collection("users")
                .document(myUid)
                .collection("friends")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    friendlist.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> friend = new HashMap<>();
                        friend.put("uid", doc.getId());
                        friend.put("displayName", doc.getString("displayName"));
                        friend.put("photoUrl", doc.getString("photoUrl"));
                        friendlist.add(friend);
                    }
                    friendsAdapter.notifyDataSetChanged();
                });
    }

    private void listenForIncomingRequests() {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        incomingListener = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    rebuildRequests(snap, true);
                });
    }

    private void listenForOutgoingRequests() {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        outgoingListener = db.collection("users")
                .document(myUid)
                .collection("friendRequests_outgoing")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    rebuildRequests(snap, false);
                });
    }

    private final List<FriendRequestRowAdapter.RequestRow> incomingCache = new ArrayList<>();
    private final List<FriendRequestRowAdapter.RequestRow> outgoingCache = new ArrayList<>();

    private void rebuildRequests(QuerySnapshot snap, boolean incoming) {
        List<FriendRequestRowAdapter.RequestRow> target = incoming ? incomingCache : outgoingCache;
        target.clear();

        for (DocumentSnapshot doc : snap.getDocuments()) {
            FriendRequestRowAdapter.RequestRow row = new FriendRequestRowAdapter.RequestRow();
            row.uid = doc.getId();
            row.displayName = doc.getString("fromDisplayName"); // for incoming
            row.photoUrl = doc.getString("fromPhotoUrl");
            if (!incoming) {
                // for outgoing, you might have stored "toDisplayName" instead; adjust to match yours
                row.displayName = doc.getString("toDisplayName");
                row.photoUrl = doc.getString("toPhotoUrl");
            }
            row.type = incoming
                    ? FriendRequestRowAdapter.RequestRow.TYPE_INCOMING
                    : FriendRequestRowAdapter.RequestRow.TYPE_OUTGOING;
            target.add(row);
        }

        // merge both into main list
        requestRows.clear();
        requestRows.addAll(incomingCache);
        requestRows.addAll(outgoingCache);
        requestsAdapter.notifyDataSetChanged();

        // toggle header
        tvRequestsHeader.setVisibility(requestRows.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void acceptRequest(String otherUid) {
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

                    // data for my friends collection (info about THEM)
                    Map<String, Object> myFriendData = new HashMap<>();
                    myFriendData.put("uid", otherUid);
                    myFriendData.put("displayName", otherName);
                    myFriendData.put("photoUrl", otherPhoto);
                    myFriendData.put("createdAt", com.google.firebase.Timestamp.now());

                    // data for their friends collection (info about me)
                    Map<String, Object> theirFriendData = new HashMap<>();
                    theirFriendData.put("uid", currentUser.getUid());
                    theirFriendData.put("displayName", currentUser.getDisplayName());
                    theirFriendData.put("photoUrl",
                            currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
                    theirFriendData.put("createdAt", com.google.firebase.Timestamp.now());

                    batch.set(myFriendRef, myFriendData);
                    batch.set(theirFriendRef, theirFriendData);
                    batch.delete(myIncomingRef);
                    batch.delete(theirOutgoingRef);

                    batch.commit();
                });
    }

    private void declineRequest(String otherUid) {
        String myUid = currentUser.getUid();

        var batch = db.batch();

        DocumentReference myIncomingRef = db.collection("users").document(myUid)
                .collection("friendRequests_incoming").document(otherUid);
        DocumentReference theirOutgoingRef = db.collection("users").document(otherUid)
                .collection("friendRequests_outgoing").document(myUid);

        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);
        batch.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendsListener != null) friendsListener.remove();
        if (outgoingListener != null) outgoingListener.remove();
        if (incomingListener != null) incomingListener.remove();
    }
}