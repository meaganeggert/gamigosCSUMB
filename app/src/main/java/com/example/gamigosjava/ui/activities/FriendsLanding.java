package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FriendsListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendsLanding extends BaseActivity {
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private RecyclerView rvFriends;
    private FriendsListAdapter friendsAdapter;
    private final List<Map<String, Object>> friendlist = new ArrayList<>();
    private ListenerRegistration friendsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_friends_landing);

        // Set title for NavBar
        setTopTitle("Friends");

        //  Setting up variables for use in recycler view

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        //  For Friend Search Button. Might be removed/moved later

        //  Find button
        Button btnSearchFriends = findViewById(R.id.btnFriendSearchActivity);
        //  Set up click listener
        btnSearchFriends.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsLanding.this, FriendsSearchActivity.class);
            startActivity(intent);
        });

        //  For Friend Requests Button. Might be removed/moved later
        Button btnFriendRequests = findViewById(R.id.btnFriendRequestsActivity);
        //  Set up click listener
        btnFriendRequests.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsLanding.this, FriendRequestsActivity.class);
            startActivity(intent);
        });

        //  Setting up recycler view
        rvFriends = findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        friendsAdapter = new FriendsListAdapter(friendlist, friend -> {
            //  Open profile of friend
            String friendUid = (String) friend.get("uid");
            Intent intent = new Intent(FriendsLanding.this, ViewUserProfileActivity.class);
            intent.putExtra("userId", friendUid);
            startActivity(intent);
        });
        rvFriends.setAdapter(friendsAdapter);
        listenForFriends();
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
}