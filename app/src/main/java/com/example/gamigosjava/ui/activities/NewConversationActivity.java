package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FriendPickAdapter;
import com.example.gamigosjava.ui.viewholder.FriendItemModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewConversationActivity extends BaseActivity {

    public static final String EXTRA_EDIT_CONVO_ID = "EDIT_CONVO_ID";

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private FriendPickAdapter adapter;
    private final List<FriendItemModel> allFriends = new ArrayList<>();
    private final List<FriendItemModel> selectedFriends = new ArrayList<>();

    private EditText etSearch;
    private ChipGroup cgSelectedFriends;
    private TextView tvSelectedLabel;
    private EditText etGroupTitle;
    private EditText etInitialMessage;
    private Button btnStartConversation;
    private Button btnGroupPhoto;
    private Uri groupPhotoUri;
    private ShapeableImageView imageGroupPhotoPreview;
    private FrameLayout groupPhotoContainer;
    private ActivityResultLauncher<Intent> pickGroupPhotoLauncher;

    // ---------- EDIT MODE ----------
    private boolean isEditMode = false;
    private String editConvoId = null;
    private String existingGroupPhotoUrl = null;
    private boolean clearExistingGroupPhoto = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setChildLayout(R.layout.activity_new_conversation);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // check if we're editing an existing convo
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_EDIT_CONVO_ID)) {
            isEditMode = true;
            editConvoId = intent.getStringExtra(EXTRA_EDIT_CONVO_ID);
            setTopTitle("Edit conversation");
        } else {
            setTopTitle("New conversation");
        }

        etSearch = findViewById(R.id.etSearchFriend);
        RecyclerView rvResults = findViewById(R.id.rvFriendResults);
        cgSelectedFriends = findViewById(R.id.cgSelectedFriends);
        tvSelectedLabel = findViewById(R.id.tvSelectedLabel);
        etGroupTitle = findViewById(R.id.etGroupTitle);
        etInitialMessage = findViewById(R.id.etInitialMessage);
        btnStartConversation = findViewById(R.id.btnStartConversation);
        btnGroupPhoto = findViewById(R.id.btnGroupPhoto);
        imageGroupPhotoPreview = findViewById(R.id.imageGroupPhotoPreview);
        groupPhotoContainer = findViewById(R.id.groupPhotoContainer);
        ImageButton btnClearGroupPhoto = findViewById(R.id.btnClearGroupPhoto);

        // edit-mode UI tweaks
        if (isEditMode) {
            etInitialMessage.setVisibility(View.GONE);
            btnStartConversation.setText(R.string.save_changes);
        }

        pickGroupPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        groupPhotoUri = result.getData().getData();
                        clearExistingGroupPhoto = false; // we're replacing with a new one
                        if (groupPhotoUri != null) {
                            btnGroupPhoto.setText(R.string.group_photo_selected);
                            updateGroupPhotoPreview();
                        }
                    }
                }
        );

        btnGroupPhoto.setOnClickListener(v -> pickGroupPhoto());

        btnClearGroupPhoto.setOnClickListener(v -> {
            groupPhotoUri = null;
            clearExistingGroupPhoto = true;       // signal we want to remove the existing photo
            existingGroupPhotoUrl = null;
            btnGroupPhoto.setText(R.string.choose_group_photo_optional);
            updateGroupPhotoPreview();
        });

        rvResults.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FriendPickAdapter(friend -> {
            addFriendToSelection(friend);
            etSearch.setText("");
            adapter.setItems(new ArrayList<>());
        });
        rvResults.setAdapter(adapter);

        loadFriends();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnStartConversation.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFriends.isEmpty()) {
                Toast.makeText(this, "Select at least one friend", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                saveConversationEdits();
                return;
            }

            // NEW conversation flow
            String initialMessage = etInitialMessage.getText().toString().trim();
            if (TextUtils.isEmpty(initialMessage)) {
                Toast.makeText(this, "Please enter an initial message", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedFriends.size() == 1) {
                FriendItemModel f = selectedFriends.get(0);
                startDmWithInitialMessage(f, initialMessage);
            } else {
                String groupTitle = etGroupTitle.getText().toString().trim();
                if (TextUtils.isEmpty(groupTitle)) {
                    groupTitle = buildGroupTitle(selectedFriends);
                }
                createGroupConversation(selectedFriends, groupTitle, initialMessage);
            }
        });

        updateUIForSelection();

        // if editing, load existing convo data after basic setup
        if (isEditMode && editConvoId != null) {
            loadConversationForEdit();
        }
    }

    // ---------- LOAD EXISTING CONVERSATION FOR EDIT MODE ----------

    private void loadConversationForEdit() {
        if (editConvoId == null) return;
        if (currentUser == null) return;

        String myUid = currentUser.getUid();

        db.collection("conversations")
                .document(editConvoId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    List<String> participants = (List<String>) doc.get("participants");
                    Boolean isGroup = doc.getBoolean("isGroup");
                    String titleOverride = doc.getString("titleOverride");
                    existingGroupPhotoUrl = doc.getString("groupPhotoUrl");

                    // prefill title (for groups or DM becoming group)
                    if (titleOverride != null) {
                        etGroupTitle.setText(titleOverride);
                    }

                    // prefill photo preview
                    if (existingGroupPhotoUrl != null && !existingGroupPhotoUrl.isEmpty()) {
                        groupPhotoContainer.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .load(existingGroupPhotoUrl)
                                .centerCrop()
                                .placeholder(R.drawable.ic_friends_24)
                                .error(R.drawable.ic_friends_24)
                                .into(imageGroupPhotoPreview);
                        btnGroupPhoto.setText(R.string.group_photo_selected);
                    }

                    // preselect participants (exclude myself)
                    if (participants != null) {
                        for (String uid : participants) {
                            if (uid.equals(myUid)) continue;
                            db.collection("users")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(this::addUserDocToSelection);
                        }
                    }
                });
    }

    private void addUserDocToSelection(DocumentSnapshot userDoc) {
        if (userDoc == null || !userDoc.exists()) return;

        FriendItemModel f = new FriendItemModel();
        f.uid = userDoc.getId();
        f.displayName = userDoc.getString("displayName");
        f.photoUrl = userDoc.getString("photoUrl");

        if (!isAlreadySelected(f.uid)) {
            selectedFriends.add(f);
            updateUIForSelection();
        }
    }

    // ---------- FRIENDS LIST & SEARCH ----------

    private void loadFriends() {
        if (currentUser == null) return;
        String myUid = currentUser.getUid();

        db.collection("users")
                .document(myUid)
                .collection("friends")
                .get()
                .addOnSuccessListener(snap -> {
                    allFriends.clear();
                    for (var doc : snap.getDocuments()) {
                        FriendItemModel f = new FriendItemModel();
                        f.uid = doc.getId();
                        f.displayName = doc.getString("displayName");
                        f.photoUrl = doc.getString("photoUrl");
                        allFriends.add(f);
                    }
                    adapter.setItems(new ArrayList<>());
                });
    }

    private void filterFriends(String query) {
        if (query == null) query = "";
        String qLower = query.toLowerCase().trim();
        if (qLower.isEmpty()) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        List<FriendItemModel> filtered = new ArrayList<>();
        for (FriendItemModel f : allFriends) {
            String name = f.displayName != null ? f.displayName : "";
            if (name.toLowerCase().contains(qLower) && !isAlreadySelected(f.uid)) {
                filtered.add(f);
            }
        }
        adapter.setItems(filtered);
    }

    private boolean isAlreadySelected(String uid) {
        for (FriendItemModel f : selectedFriends) {
            if (f.uid.equals(uid)) return true;
        }
        return false;
    }

    private void addFriendToSelection(FriendItemModel friend) {
        if (isAlreadySelected(friend.uid)) return;
        selectedFriends.add(friend);
        updateUIForSelection();
    }

    private void removeFriendFromSelection(String uid) {
        for (int i = 0; i < selectedFriends.size(); i++) {
            if (selectedFriends.get(i).uid.equals(uid)) {
                selectedFriends.remove(i);
                break;
            }
        }
        updateUIForSelection();
    }

    private void updateUIForSelection() {
        int count = selectedFriends.size();
        boolean isGroup = count > 1;

        etGroupTitle.setVisibility(isGroup ? View.VISIBLE : View.GONE);
        btnGroupPhoto.setVisibility(isGroup ? View.VISIBLE : View.GONE);

        if (isGroup && (groupPhotoUri != null || existingGroupPhotoUrl != null)) {
            updateGroupPhotoPreview();
        } else {
            groupPhotoContainer.setVisibility(View.GONE);
        }

        // chips + label
        if (count == 0) {
            cgSelectedFriends.removeAllViews();
            cgSelectedFriends.setVisibility(View.GONE);
            tvSelectedLabel.setVisibility(View.GONE);
        } else {
            cgSelectedFriends.setVisibility(View.VISIBLE);
            tvSelectedLabel.setVisibility(View.VISIBLE);

            cgSelectedFriends.removeAllViews();
            for (FriendItemModel f : selectedFriends) {
                Chip chip = new Chip(this);
                chip.setText(f.displayName != null ? f.displayName : "Unknown");
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> removeFriendFromSelection(f.uid));
                cgSelectedFriends.addView(chip);
            }
        }

        // button text
        if (isEditMode) {
            btnStartConversation.setEnabled(count > 0);
            btnStartConversation.setText(R.string.save_changes);
        } else {
            btnStartConversation.setEnabled(count > 0);
            if (count == 0) {
                btnStartConversation.setText(R.string.start_conversation);
            } else if (count == 1) {
                btnStartConversation.setText(R.string.start_direct_message);
            } else {
                btnStartConversation.setText(R.string.start_group_chat);
            }
        }
    }

    // ---------- NEW CONVERSATION CREATION (unchanged) ----------

    private String dmId(String a, String b) {
        return (a.compareTo(b) < 0) ? a + "_" + b : b + "_" + a;
    }

    private void startDmWithInitialMessage(FriendItemModel friend, String initialMessage) {
        String myUid = currentUser.getUid();
        String otherUid = friend.uid;
        String convoId = dmId(myUid, otherUid);
        DocumentReference convoRef = db.collection("conversations").document(convoId);

        convoRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                sendInitialMessage(convoRef, convoId, friend.displayName, otherUid, initialMessage, false);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("participants", java.util.Arrays.asList(myUid, otherUid));
                data.put("isGroup", false);
                data.put("lastMessage", initialMessage);
                data.put("lastMessageAt", FieldValue.serverTimestamp());

                convoRef.set(data).addOnSuccessListener(unused -> {
                    createParticipantData(convoRef, myUid);
                    createParticipantData(convoRef, otherUid);
                    sendInitialMessage(convoRef, convoId, friend.displayName, otherUid, initialMessage, false);
                });
            }
        });
    }

    private void createGroupConversation(List<FriendItemModel> selected,
                                         String groupTitle,
                                         String initialMessage) {
        String myUid = currentUser.getUid();

        List<String> participantUids = new ArrayList<>();
        participantUids.add(myUid);
        for (FriendItemModel f : selected) {
            participantUids.add(f.uid);
        }

        DocumentReference convoRef = db.collection("conversations").document();
        String convoId = convoRef.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("participants", participantUids);
        data.put("isGroup", true);
        data.put("lastMessage", initialMessage);
        data.put("lastMessageAt", FieldValue.serverTimestamp());
        data.put("titleOverride", groupTitle);

        if (groupPhotoUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("group_photos")
                    .child(convoId);

            storageRef.putFile(groupPhotoUri)
                    .continueWithTask(task -> storageRef.getDownloadUrl())
                    .addOnSuccessListener(uri -> {
                        data.put("groupPhotoUrl", uri.toString());
                        createGroupConversationDoc(convoRef, convoId, data, selected, groupTitle, initialMessage);
                    })
                    .addOnFailureListener(e ->
                            createGroupConversationDoc(convoRef, convoId, data, selected, groupTitle, initialMessage));
        } else {
            createGroupConversationDoc(convoRef, convoId, data, selected, groupTitle, initialMessage);
        }
    }

    private void createGroupConversationDoc(DocumentReference convoRef,
                                            String convoId,
                                            Map<String, Object> data,
                                            List<FriendItemModel> selected,
                                            String groupTitle,
                                            String initialMessage) {
        String myUid = currentUser.getUid();

        convoRef.set(data).addOnSuccessListener(unused -> {
            createParticipantData(convoRef, myUid);
            for (FriendItemModel f : selected) {
                createParticipantData(convoRef, f.uid);
            }

            sendInitialMessage(convoRef, convoId, groupTitle, null, initialMessage, true);
        });
    }

    private void sendInitialMessage(DocumentReference convoRef,
                                    String convoId,
                                    String titleForActivity,
                                    String otherUidForActivity,
                                    String initialMessage,
                                    boolean isGroup) {
        String myUid = currentUser.getUid();
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", myUid);
        msg.put("text", initialMessage);
        msg.put("createdAt", FieldValue.serverTimestamp());

        convoRef.collection("messages")
                .add(msg)
                .addOnSuccessListener(unused -> startActivity(
                        MessagesActivity.newIntent(
                                NewConversationActivity.this,
                                convoId,
                                titleForActivity != null ? titleForActivity : "Conversation",
                                isGroup ? null : otherUidForActivity,
                                isGroup
                        )));
    }

    private String buildGroupTitle(List<FriendItemModel> selected) {
        List<String> names = new ArrayList<>();
        for (FriendItemModel f : selected) {
            if (f.displayName != null && !f.displayName.isEmpty()) {
                names.add(f.displayName);
            }
        }
        if (names.isEmpty()) return "Group chat";

        if (names.size() == 1) {
            return names.get(0);
        } else if (names.size() == 2) {
            return names.get(0) + ", " + names.get(1);
        } else {
            return names.get(0) + ", " + names.get(1) + " (+" + (names.size() - 2) + ")";
        }
    }

    private void pickGroupPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickGroupPhotoLauncher.launch(intent);
    }

    private void updateGroupPhotoPreview() {
        if (groupPhotoUri != null) {
            groupPhotoContainer.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(groupPhotoUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_friends_24)
                    .error(R.drawable.ic_friends_24)
                    .into(imageGroupPhotoPreview);
        } else if (existingGroupPhotoUrl != null && !existingGroupPhotoUrl.isEmpty()) {
            groupPhotoContainer.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(existingGroupPhotoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_friends_24)
                    .error(R.drawable.ic_friends_24)
                    .into(imageGroupPhotoPreview);
        } else {
            groupPhotoContainer.setVisibility(View.GONE);
        }
    }

    private void createParticipantData(DocumentReference convoRef, String uid) {
        Map<String, Object> pd = new HashMap<>();
        pd.put("joinedAt", FieldValue.serverTimestamp());
        pd.put("lastReadAt", null);
        pd.put("unreadCount", 0);
        pd.put("role", "member");
        convoRef.collection("participantsData").document(uid).set(pd);
    }

    // ---------- SAVE EDITS FOR EXISTING CONVERSATION ----------

    private void saveConversationEdits() {
        if (currentUser == null || editConvoId == null) return;

        int count = selectedFriends.size();
        if (count == 0) {
            Toast.makeText(this, "Select at least one participant", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = currentUser.getUid();
        boolean isGroupNow = count > 1;

        List<String> participantUids = new ArrayList<>();
        participantUids.add(myUid);
        for (FriendItemModel f : selectedFriends) {
            participantUids.add(f.uid);
        }

        String groupTitle = etGroupTitle.getText().toString().trim();
        if (isGroupNow && TextUtils.isEmpty(groupTitle)) {
            groupTitle = buildGroupTitle(selectedFriends);
        }

        DocumentReference convoRef = db.collection("conversations").document(editConvoId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("participants", participantUids);
        updates.put("isGroup", isGroupNow);
        if (isGroupNow) {
            updates.put("titleOverride", groupTitle);
        } else {
            updates.put("titleOverride", null);
        }

        if (groupPhotoUri != null) {
            // uploading a new photo
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("group_photos")
                    .child(editConvoId);

            String finalGroupTitle = groupTitle;

            storageRef.putFile(groupPhotoUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e != null) {
                                android.util.Log.e("NewConversation", "Upload failed", e);
                            }
                            throw (e != null ? e : new Exception("Unknown upload error"));
                        }
                        return storageRef.getDownloadUrl();
                    })
                    .addOnSuccessListener(uri -> {
                        // ✅ New URL from Storage
                        String url = uri.toString();
                        android.util.Log.d("NewConversation", "Upload success, url=" + url);

                        updates.put("groupPhotoUrl", url);
                        // update local state so preview + later edits use the new URL
                        existingGroupPhotoUrl = url;
                        clearExistingGroupPhoto = false;

                        applyConversationUpdates(convoRef, updates, isGroupNow, finalGroupTitle);
                    })
                    .addOnFailureListener(e -> {
                        // Upload failed
                        android.util.Log.e("NewConversation", "Failed to upload group photo", e);
                        Toast.makeText(this, "Failed to upload group photo", Toast.LENGTH_SHORT).show();

                        // Keep the old photo URL by *not* touching groupPhotoUrl in updates
                        applyConversationUpdates(convoRef, updates, isGroupNow, finalGroupTitle);
                    });
        } else {
            // no new photo picked
            if (clearExistingGroupPhoto) {
                updates.put("groupPhotoUrl", null);  // explicit delete
            }
            applyConversationUpdates(convoRef, updates, isGroupNow, groupTitle);
        }

    }

    private void applyConversationUpdates(DocumentReference convoRef,
                                          Map<String, Object> updates,
                                          boolean isGroupNow,
                                          String groupTitle) {
        convoRef.update(updates)
                .addOnSuccessListener(unused -> {
                    // after saving, go back into the conversation
                    String dmOtherUid = null;
                    String dmTitle = "Conversation";
                    if (!isGroupNow && !selectedFriends.isEmpty()) {
                        FriendItemModel f = selectedFriends.get(0);
                        dmOtherUid = f.uid;
                        dmTitle = f.displayName != null ? f.displayName : "Conversation";
                    }

                    String finalTitle = isGroupNow
                            ? (groupTitle != null ? groupTitle : "Group chat")
                            : dmTitle;

                    startActivity(
                            MessagesActivity.newIntent(
                                    NewConversationActivity.this,
                                    editConvoId,
                                    finalTitle,
                                    dmOtherUid,
                                    isGroupNow
                            )
                    );
                    finish();
                });
    }
}
