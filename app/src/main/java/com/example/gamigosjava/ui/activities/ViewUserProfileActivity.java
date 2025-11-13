package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class ViewUserProfileActivity extends BaseActivity {
    private static final int REL_NONE = 0;       // no relation
    private static final int REL_OUTGOING = 1;   // we sent request -> pending
    private static final int REL_INCOMING = 2;   // they sent request -> accept/deny
    private static final int REL_FRIEND = 3;     // already friends
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private ImageView ivAvatar;
    private TextView tvName, tvEmail;
    private Button btnPrimary, btnSecondary;
    private String viewedUserId;
    private String viewedName;
    private String viewedPhoto;
    private int currentRelation = REL_NONE;
    String myUid;
    String otherUid;
    private ListenerRegistration friendListener;
    private ListenerRegistration outgoingListener;
    private ListenerRegistration incomingListener;

    //  DM id helper
        private String dmId(String userA, String userB) {
        return (userA.compareTo(userB) < 0) ? userA + "_" + userB : userB + "_" + userA;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_view_user_profile);
        setChildLayout(R.layout.activity_view_user_profile);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        assert currentUser != null;
        myUid = currentUser.getUid();
        viewedUserId = getIntent().getStringExtra("USER_ID");
        if (viewedUserId == null) {
            finish();
            return;
        }
        otherUid = viewedUserId;


        //  Connect to xml elements
        ivAvatar = findViewById(R.id.ivProfilePhoto);
        tvName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        btnPrimary = findViewById(R.id.btnPrimaryAction);
        btnSecondary = findViewById(R.id.btnSecondaryAction);

        //  Set temporary title
        setTopTitle("Profile");

        loadProfile();
        startRelationshipListeners();

        btnPrimary.setOnClickListener(v -> {
            switch (currentRelation) {
                case REL_NONE -> sendFriendRequest();
                case REL_INCOMING -> acceptFriendRequest();
                case REL_OUTGOING -> Toast.makeText(this, "Request pending.", Toast.LENGTH_SHORT).show();
                case REL_FRIEND -> startOrOpenDM();
            }
        });

        btnSecondary.setOnClickListener(v -> {
            switch (currentRelation) {
                case REL_INCOMING -> denyFriendRequest();
                case REL_FRIEND -> unfriend();
            }
        });
    }

    private void loadProfile() {
        //  Load user's info
        db.collection("users").document(viewedUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String email = doc.getString("email");
                        String photo = doc.getString("photoUrl");

                        viewedName = name;
                        viewedPhoto = photo;

                        tvName.setText(name);
                        tvEmail.setText(email);
                        // Set title for NavBar
                        setTopTitle(name);

                        Glide.with(this).load(photo).into(ivAvatar);
                    }
                });
    }

    private void startRelationshipListeners() {
        DocumentReference friendRef = db.collection("users").document(myUid)
                .collection("friends").document(viewedUserId);
        DocumentReference outgoingRef = db.collection("users").document(myUid)
                .collection("friendRequests_outgoing").document(viewedUserId);
        DocumentReference incomingRef = db.collection("users").document(myUid)
                .collection("friendRequests_incoming").document(viewedUserId);

        friendListener = friendRef.addSnapshotListener((snap, e) -> recalcRelation());
        outgoingListener = outgoingRef.addSnapshotListener((snap, e) -> recalcRelation());
        incomingListener = incomingRef.addSnapshotListener((snap, e) -> recalcRelation());
    }

    private void recalcRelation() {
        DocumentReference friendRef = db.collection("users").document(myUid)
                .collection("friends").document(viewedUserId);
        DocumentReference outgoingRef = db.collection("users").document(myUid)
                .collection("friendRequests_outgoing").document(viewedUserId);
        DocumentReference incomingRef = db.collection("users").document(myUid)
                .collection("friendRequests_incoming").document(viewedUserId);

        friendRef.get().addOnSuccessListener(friendSnap -> {
            if (friendSnap.exists()) {
                currentRelation = REL_FRIEND;
                updateButtons();
                return;
            }

            outgoingRef.get().addOnSuccessListener(outSnap -> {
                if (outSnap.exists()) {
                    currentRelation = REL_OUTGOING;
                    updateButtons();
                    return;
                }

                incomingRef.get().addOnSuccessListener(inSnap -> {
                    if (inSnap.exists()) {
                        currentRelation = REL_INCOMING;
                    } else {
                        currentRelation = REL_NONE;
                    }
                    updateButtons();
                });
            });
        });
    }

    private void updateButtons() {
        switch (currentRelation) {
            case REL_NONE -> {
                btnPrimary.setText(R.string.add_friend);
                btnPrimary.setEnabled(true);
                btnSecondary.setVisibility(View.GONE);
            }
            case REL_OUTGOING -> {
                btnPrimary.setText(R.string.pending);
                btnPrimary.setEnabled(false);
                btnSecondary.setVisibility(View.GONE);
            }
            case REL_INCOMING -> {
                btnPrimary.setText(R.string.accept);
                btnPrimary.setEnabled(true);
                btnSecondary.setText(R.string.deny);
                btnSecondary.setVisibility(View.VISIBLE);
            }
            case REL_FRIEND -> {
                btnPrimary.setText(R.string.message);
                btnPrimary.setEnabled(true);
                btnSecondary.setText(R.string.unfriend);
                btnSecondary.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendFriendRequest() {
        var batch = db.batch();

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
        reqData.put("to", viewedUserId);
        reqData.put("toDisplayName", viewedName);
        reqData.put("toPhotoUrl", viewedPhoto);

        batch.set(myOutgoingRef, reqData);
        batch.set(theirIncomingRef, reqData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    currentRelation = REL_OUTGOING;
                    updateButtons();
                    Toast.makeText(this, "Friend request sent.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed" + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest() {
        var batch = db.batch();

        DocumentReference myFriendRef = db.collection("users")
                .document(myUid)
                .collection("friends")
                .document(otherUid);
        DocumentReference theirFriendRef = db.collection("users")
                .document(otherUid)
                .collection("friends")
                .document(myUid);
        DocumentReference myIncomingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .document(otherUid);
        DocumentReference theirOutgoingRef = db.collection("users")
                .document(otherUid)
                .collection("friendRequests_outgoing")
                .document(myUid);

        var friendDataForMe = new java.util.HashMap<String, Object>();
        friendDataForMe.put("uid", viewedUserId);
        friendDataForMe.put("displayName", viewedName);
        friendDataForMe.put("photoUrl", viewedPhoto);
        friendDataForMe.put("createdAt", com.google.firebase.Timestamp.now());

        var friendDataForThem = new java.util.HashMap<String, Object>();
        friendDataForThem.put("uid", currentUser.getUid());
        friendDataForThem.put("displayName", currentUser.getDisplayName());
        friendDataForThem.put("photoUrl", currentUser.getPhotoUrl());
        friendDataForThem.put("createdAt", com.google.firebase.Timestamp.now());

        batch.set(myFriendRef, friendDataForMe);
        batch.set(theirFriendRef, friendDataForThem);
        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    currentRelation = REL_FRIEND;
                    updateButtons();
                    Toast.makeText(this, "Friend request accepted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void denyFriendRequest() {
        var batch = db.batch();
        DocumentReference myIncomingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .document(otherUid);
        DocumentReference theirOutgoingRef = db.collection("users")
                .document(otherUid)
                .collection("friendRequests_outgoing")
                .document(myUid);

        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    currentRelation = REL_NONE;
                    updateButtons();
                    Toast.makeText(this, "Friend request denied.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void unfriend() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove this friend?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    var batch = db.batch();
                    DocumentReference myFriendRef = db.collection("users")
                            .document(myUid)
                            .collection("friends")
                            .document(otherUid);
                    DocumentReference theirFriendRef = db.collection("users")
                            .document(otherUid)
                            .collection("friends")
                            .document(myUid);

                    batch.delete(myFriendRef);
                    batch.delete(theirFriendRef);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                currentRelation = REL_NONE;
                                updateButtons();
                                Toast.makeText(this, "Friend removed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    //  DM helper
    private void startOrOpenDM() {
            if (viewedName == null) {
                Toast.makeText(this, "Loading profile...", Toast.LENGTH_SHORT).show();
                return;
            }
            btnPrimary.setEnabled(false);
            String convoId = dmId(myUid, otherUid);
            var convoRef = db.collection("conversations").document(convoId);

            //  Check DB for convo. If exists, open that convo. If not, then create it.
            convoRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    launchMessages(convoId, viewedName, otherUid);
                    btnPrimary.setEnabled(true);
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("participants", java.util.Arrays.asList(myUid, otherUid));
                    data.put("isGroup", false);
                    data.put("lastMessage", "");
                    data.put("lastMessageAt", null);

                    convoRef.set(data).addOnSuccessListener(aVoid -> {
                        createParticipantData(convoRef, myUid);
                        createParticipantData(convoRef, otherUid);
                        btnPrimary.setEnabled(true);
                        launchMessages(convoId, viewedName, otherUid);
                    }).addOnFailureListener(error -> {
                        btnPrimary.setEnabled(true);
                        Toast.makeText(this,
                                "Failed to start chat: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }).addOnFailureListener(error -> {
                btnPrimary.setEnabled(true);
                Toast.makeText(this,
                        "Failed to load conversation: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
    }

    private void createParticipantData(DocumentReference convoRef, String uid) {
            Map<String, Object> participantData = new HashMap<>();
            participantData.put("joinedAt", FieldValue.serverTimestamp());
            participantData.put("lastReadAt", null);
            participantData.put("unreadCount", 0);
            participantData.put("role", "member");
            convoRef.collection("participantsData").document(uid).set(participantData);
    }

    private void launchMessages(String conversationId, String title, String otherUid) {
            Intent intent = MessagesActivity.newIntent(this, conversationId, title, otherUid);
            startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (friendListener != null) friendListener.remove();
        if (outgoingListener != null) outgoingListener.remove();
        if (incomingListener != null) incomingListener.remove();
    }
}
