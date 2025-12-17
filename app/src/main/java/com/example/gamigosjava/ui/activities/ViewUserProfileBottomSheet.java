package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ViewUserProfileBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_USER_ID = "USER_ID";

    public static ViewUserProfileBottomSheet newInstance(String userId) {
        ViewUserProfileBottomSheet f = new ViewUserProfileBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    // relationship state
    private static final int REL_NONE = 0;
    private static final int REL_OUTGOING = 1;
    private static final int REL_INCOMING = 2;
    private static final int REL_FRIEND = 3;

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private ImageView ivAvatar;
    private TextView tvName, tvEmail;
    private Button btnPrimary, btnSecondary, btnPending;

    private String myUid;
    private String viewedUserId;
    private String viewedName;
    private String viewedPhoto;

    private int currentRelation = REL_NONE;

    private ListenerRegistration friendListener;
    private ListenerRegistration outgoingListener;
    private ListenerRegistration incomingListener;

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_App_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottomsheet_view_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            dismissAllowingStateLoss();
            return;
        }

        myUid = currentUser.getUid();
        viewedUserId = (getArguments() != null) ? getArguments().getString(ARG_USER_ID) : null;
        if (viewedUserId == null) {
            dismissAllowingStateLoss();
            return;
        }

        // bind views
        ivAvatar = view.findViewById(R.id.ivProfilePhoto);
        tvName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnPrimary = view.findViewById(R.id.btnPrimaryAction);
        btnSecondary = view.findViewById(R.id.btnSecondaryAction);
        btnPending = view.findViewById(R.id.btnPendingDisabled);

        loadProfile();
        startRelationshipListeners();

        btnPrimary.setOnClickListener(v -> {
            switch (currentRelation) {
                case REL_NONE:
                    sendFriendRequest();
                    break;
                case REL_INCOMING:
                    acceptFriendRequest();
                    break;
                case REL_OUTGOING:
                    Toast.makeText(requireContext(), "Request pending.", Toast.LENGTH_SHORT).show();
                    break;
                case REL_FRIEND:
                    startOrOpenDM();
                    break;
            }
        });

        btnSecondary.setOnClickListener(v -> {
            switch (currentRelation) {
                case REL_INCOMING:
                    denyFriendRequest();
                    break;
                case REL_FRIEND:
                    unfriend();
                    break;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Make it open expanded like a real sheet, not a tiny peek.
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) getDialog();
            View sheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true); // optional: prevents half-collapsed state
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (friendListener != null) friendListener.remove();
        if (outgoingListener != null) outgoingListener.remove();
        if (incomingListener != null) incomingListener.remove();
    }

    private void loadProfile() {
        db.collection("users").document(viewedUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String email = doc.getString("email");
                        String photo = doc.getString("photoUrl");

                        viewedName = name;
                        viewedPhoto = photo;

                        tvName.setText(name != null ? name : "");
                        tvEmail.setText(email != null ? email : "");

                        if (photo != null && !photo.isEmpty()) {
                            Glide.with(this).load(photo).into(ivAvatar);
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_person_24); // adjust to your drawable
                        }
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
            if (!isAdded()) return;

            if (friendSnap.exists()) {
                currentRelation = REL_FRIEND;
                updateButtons();
                return;
            }

            outgoingRef.get().addOnSuccessListener(outSnap -> {
                if (!isAdded()) return;

                if (outSnap.exists()) {
                    currentRelation = REL_OUTGOING;
                    updateButtons();
                    return;
                }

                incomingRef.get().addOnSuccessListener(inSnap -> {
                    if (!isAdded()) return;

                    currentRelation = inSnap.exists() ? REL_INCOMING : REL_NONE;
                    updateButtons();
                });
            });
        });
    }

    private void updateButtons() {
        if (!isAdded()) return;

        switch (currentRelation) {
            case REL_NONE:
                btnPrimary.setText(R.string.add_friend);
                btnPrimary.setVisibility(View.VISIBLE);
                btnSecondary.setVisibility(View.GONE);
                btnPending.setVisibility(View.GONE);
                break;

            case REL_OUTGOING:
                btnPrimary.setVisibility(View.GONE);
                btnSecondary.setVisibility(View.GONE);
                btnPending.setVisibility(View.VISIBLE);
                break;

            case REL_INCOMING:
                btnPrimary.setText(R.string.accept);
                btnPrimary.setVisibility(View.VISIBLE);
                btnSecondary.setText(R.string.deny);
                btnSecondary.setVisibility(View.VISIBLE);
                btnPending.setVisibility(View.GONE);
                break;

            case REL_FRIEND:
                btnPrimary.setText(R.string.message);
                btnPrimary.setVisibility(View.VISIBLE);
                btnSecondary.setText(R.string.unfriend);
                btnSecondary.setVisibility(View.VISIBLE);
                btnPending.setVisibility(View.GONE);
                break;
        }
    }

    private void sendFriendRequest() {
        if (viewedName == null) {
            Toast.makeText(requireContext(), "Loading profile...", Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference myOutgoingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_outgoing")
                .document(viewedUserId);

        DocumentReference theirIncomingRef = db.collection("users")
                .document(viewedUserId)
                .collection("friendRequests_incoming")
                .document(myUid);

        Map<String, Object> reqData = new HashMap<>();
        reqData.put("createdAt", Timestamp.now());
        reqData.put("from", myUid);
        reqData.put("fromDisplayName", currentUser.getDisplayName());
        reqData.put("fromPhotoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        reqData.put("to", viewedUserId);
        reqData.put("toDisplayName", viewedName);
        reqData.put("toPhotoUrl", viewedPhoto);

        batch.set(myOutgoingRef, reqData);
        batch.set(theirIncomingRef, reqData);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    currentRelation = REL_OUTGOING;
                    updateButtons();
                    Toast.makeText(requireContext(), "Friend request sent.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void acceptFriendRequest() {
        WriteBatch batch = db.batch();

        DocumentReference myFriendRef = db.collection("users")
                .document(myUid)
                .collection("friends")
                .document(viewedUserId);

        DocumentReference theirFriendRef = db.collection("users")
                .document(viewedUserId)
                .collection("friends")
                .document(myUid);

        DocumentReference myIncomingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .document(viewedUserId);

        DocumentReference theirOutgoingRef = db.collection("users")
                .document(viewedUserId)
                .collection("friendRequests_outgoing")
                .document(myUid);

        Map<String, Object> friendDataForMe = new HashMap<>();
        friendDataForMe.put("uid", viewedUserId);
        friendDataForMe.put("displayName", viewedName);
        friendDataForMe.put("photoUrl", viewedPhoto);
        friendDataForMe.put("createdAt", Timestamp.now());

        Map<String, Object> friendDataForThem = new HashMap<>();
        friendDataForThem.put("uid", myUid);
        friendDataForThem.put("displayName", currentUser.getDisplayName());
        friendDataForThem.put("photoUrl", currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        friendDataForThem.put("createdAt", Timestamp.now());

        batch.set(myFriendRef, friendDataForMe);
        batch.set(theirFriendRef, friendDataForThem);
        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    currentRelation = REL_FRIEND;
                    updateButtons();
                    Toast.makeText(requireContext(), "Friend request accepted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void denyFriendRequest() {
        WriteBatch batch = db.batch();

        DocumentReference myIncomingRef = db.collection("users")
                .document(myUid)
                .collection("friendRequests_incoming")
                .document(viewedUserId);

        DocumentReference theirOutgoingRef = db.collection("users")
                .document(viewedUserId)
                .collection("friendRequests_outgoing")
                .document(myUid);

        batch.delete(myIncomingRef);
        batch.delete(theirOutgoingRef);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    currentRelation = REL_NONE;
                    updateButtons();
                    Toast.makeText(requireContext(), "Friend request denied.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void unfriend() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove this friend?\nThis will delete the messages between the two of you as well.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    WriteBatch batch = db.batch();

                    DocumentReference myFriendRef = db.collection("users")
                            .document(myUid)
                            .collection("friends")
                            .document(viewedUserId);

                    DocumentReference theirFriendRef = db.collection("users")
                            .document(viewedUserId)
                            .collection("friends")
                            .document(myUid);

                    batch.delete(myFriendRef);
                    batch.delete(theirFriendRef);

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                currentRelation = REL_NONE;
                                updateButtons();
                                Toast.makeText(requireContext(), "Friend removed", Toast.LENGTH_SHORT).show();

                                String convoId = dmId(myUid, viewedUserId);
                                deleteConversationAndSubcollections(convoId);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteConversationAndSubcollections(String convoId) {
        DocumentReference convoRef = db.collection("conversations").document(convoId);

        convoRef.collection("messages").get()
                .addOnSuccessListener(messageSnap -> {
                    if (!isAdded()) return;

                    if (!messageSnap.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : messageSnap.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit()
                                .addOnSuccessListener(unused -> deleteParticipantsAndParent(convoRef))
                                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                        "Error deleting messages: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    } else {
                        deleteParticipantsAndParent(convoRef);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Error retrieving messages: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteParticipantsAndParent(DocumentReference convoRef) {
        convoRef.collection("participantsData").get()
                .addOnSuccessListener(partSnap -> {
                    if (!isAdded()) return;

                    if (!partSnap.isEmpty()) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : partSnap.getDocuments()) {
                            batch.delete(doc.getReference());
                        }
                        batch.commit()
                                .addOnSuccessListener(unused -> deleteConversationDoc(convoRef))
                                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                                        "Error deleting participant data: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                    } else {
                        deleteConversationDoc(convoRef);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Failed to retrieve participant data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteConversationDoc(DocumentReference convoRef) {
        convoRef.delete()
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Error deleting conversation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startOrOpenDM() {
        if (viewedName == null) {
            Toast.makeText(requireContext(), "Loading profile...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPrimary.setEnabled(false);

        String convoId = dmId(myUid, viewedUserId);
        DocumentReference convoRef = db.collection("conversations").document(convoId);

        convoRef.get().addOnSuccessListener(snapshot -> {
            if (!isAdded()) return;

            if (snapshot.exists()) {
                launchMessages(convoId, viewedName, viewedUserId);
                btnPrimary.setEnabled(true);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("participants", Arrays.asList(myUid, viewedUserId));
                data.put("isGroup", false);
                data.put("lastMessage", "");
                data.put("lastMessageAt", null);

                convoRef.set(data).addOnSuccessListener(unused -> {
                    createParticipantData(convoRef, myUid);
                    createParticipantData(convoRef, viewedUserId);
                    btnPrimary.setEnabled(true);
                    launchMessages(convoId, viewedName, viewedUserId);
                }).addOnFailureListener(error -> {
                    btnPrimary.setEnabled(true);
                    Toast.makeText(requireContext(),
                            "Failed to start chat: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).addOnFailureListener(error -> {
            if (!isAdded()) return;
            btnPrimary.setEnabled(true);
            Toast.makeText(requireContext(),
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
        Intent intent = MessagesActivity.newIntent(
                requireContext(),
                conversationId,
                title,
                otherUid,
                false
        );
        startActivity(intent);

        dismissAllowingStateLoss();
    }

    // Stable DM id helper
    private String dmId(String userA, String userB) {
        return (userA.compareTo(userB) < 0) ? userA + "_" + userB : userB + "_" + userA;
    }
}
