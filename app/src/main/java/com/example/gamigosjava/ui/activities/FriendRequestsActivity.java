package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FriendRequestsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// screen to show incoming friend requests
public class FriendRequestsActivity extends BaseActivity {

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private RecyclerView rvFriendRequests;
    private FriendRequestsAdapter adapter;
    private final List<Map<String, Object>> requestList = new ArrayList<>();
    private ListenerRegistration incomingListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout that should fill the frame
        setChildLayout(R.layout.activity_friend_requests);

        // Set title for NavBar
        setTopTitle("Friend Requests");

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        rvFriendRequests = findViewById(R.id.rvFriendRequests);
        rvFriendRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendRequestsAdapter(requestList, new FriendRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Map<String, Object> request) {
                acceptRequest(request);
            }

            @Override
            public void onDecline(Map<String, Object> request) {
                declineRequest(request);
            }

            @Override
            public void onViewProfile(Map<String, Object> request) {
                openProfile(request);
            }
        });

        rvFriendRequests.setAdapter(adapter);

        listenForIncomingRequests();
    }

    private void listenForIncomingRequests() {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        incomingListener = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    requestList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        // doc id is the requester UID
                        String fromUid = doc.getId();
                        Map<String, Object> map = new HashMap<>();
                        map.put("fromUid", fromUid);
                        // we stored these in the request when we sent it? If not, fallback
                        map.put("fromDisplayName", doc.getString("fromDisplayName"));
                        map.put("fromPhotoUrl", doc.getString("fromPhotoUrl"));
                        requestList.add(map);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void acceptRequest(Map<String, Object> request) {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();
        String otherUid = (String) request.get("fromUid");
        if (otherUid == null) return;

        // build friend data
        Map<String, Object> friendDataForMe = new HashMap<>();
        friendDataForMe.put("uid", otherUid);
        friendDataForMe.put("displayName", request.get("fromDisplayName"));
        friendDataForMe.put("photoUrl", request.get("fromPhotoUrl"));
        friendDataForMe.put("addedAt", com.google.firebase.Timestamp.now());

        Map<String, Object> friendDataForThem = new HashMap<>();
        friendDataForThem.put("uid", myUid);
        friendDataForThem.put("displayName", currentUser.getDisplayName());
        friendDataForThem.put("photoUrl",
                currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        friendDataForThem.put("addedAt", com.google.firebase.Timestamp.now());

        WriteBatch batch = db.batch();

        // add friendship both ways
        batch.set(
                db.collection("users").document(myUid)
                        .collection("friends").document(otherUid),
                friendDataForMe
        );

        batch.set(
                db.collection("users").document(otherUid)
                        .collection("friends").document(myUid),
                friendDataForThem
        );

        // delete incoming (mine) and outgoing (theirs)
        batch.delete(
                db.collection("users").document(myUid)
                        .collection("friendRequests_incoming").document(otherUid)
        );

        batch.delete(
                db.collection("users").document(otherUid)
                        .collection("friendRequests_outgoing").document(myUid)
        );

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Friend added!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void declineRequest(Map<String, Object> request) {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();
        String otherUid = (String) request.get("fromUid");
        if (otherUid == null) return;

        WriteBatch batch = db.batch();

        // remove incoming
        batch.delete(
                db.collection("users").document(myUid)
                        .collection("friendRequests_incoming").document(otherUid)
        );

        // remove their outgoing
        batch.delete(
                db.collection("users").document(otherUid)
                        .collection("friendRequests_outgoing").document(myUid)
        );

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Request declined", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openProfile(Map<String, Object> request) {
        String fromUid = (String) request.get("fromUid"); // we set this when we built the list
        if (fromUid == null) return;

        // start your profile activity – replace with your actual activity class
        Intent i = new Intent(this, ViewUserProfileActivity.class);
        i.putExtra("userId", fromUid);
        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (incomingListener != null) incomingListener.remove();
    }
}
