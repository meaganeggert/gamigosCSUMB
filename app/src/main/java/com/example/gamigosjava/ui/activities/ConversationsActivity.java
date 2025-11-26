package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.ConversationsAdapter;
import com.example.gamigosjava.ui.viewholder.ConversationsModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends BaseActivity {
    private ConversationsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUid;

    private ListenerRegistration conversationsListener;
    private final List<ConversationsModel> conversations = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setChildLayout(R.layout.activity_conversations);
        setTopTitle("Messages");

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        RecyclerView rvConversations = findViewById(R.id.rvConversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ConversationsAdapter(conversation -> {
            String title = resolveTitleForConversation(conversation);
            String otherUid = conversation.isGroup() ? null : conversation.getOtherUid();

            startActivity(
                    MessagesActivity.newIntent(
                            this,
                            conversation.getId(),
                            title,
                            otherUid,
                            conversation.isGroup()
                    )
            );
        });

        rvConversations.setAdapter(adapter);

        FloatingActionButton newConversationFab = findViewById(R.id.fabNewConversation);
        newConversationFab.setOnClickListener(
                v -> startActivity(new Intent(this, NewConversationActivity.class))
        );

        startConversationsListener();
    }

    private void startConversationsListener() {
        if (currentUid == null) return;

        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }

        conversationsListener = db.collection("conversations")
                .whereArrayContains("participants", currentUid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("Convos", "Listen failed: ", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    android.util.Log.d("Convos", "Realtime got " + querySnapshot.size() + " conversations");

                    conversations.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ConversationsModel c = doc.toObject(ConversationsModel.class);
                        if (c == null) continue;
                        c.setId(doc.getId());

                        android.util.Log.d(
                                "Convos",
                                "doc " + doc.getId() + " isGroup=" + c.isGroup()
                                        + " titleOverride=" + c.getTitleOverride()
                        );

                        // compute otherUid for 1–1 chats
                        if (!c.isGroup() && c.getParticipants() != null) {
                            for (String uid : c.getParticipants()) {
                                if (!uid.equals(currentUid)) {
                                    c.setOtherUid(uid);
                                    break;
                                }
                            }
                        }
                        conversations.add(c);
                    }

                    adapter.setConversations(new ArrayList<>(conversations));
                    fetchUserInfoForDMs(conversations);
                });
    }

    private void fetchUserInfoForDMs(List<ConversationsModel> conversations) {
        for (ConversationsModel c : conversations) {
            if (c.isGroup()) continue;
            String otherUid = c.getOtherUid();
            if (otherUid == null) continue;

            db.collection("users")
                    .document(otherUid)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        c.setOtherName(userDoc.getString("displayName"));
                        c.setOtherPhotoUrl(userDoc.getString("photoUrl"));
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    private String resolveTitleForConversation(ConversationsModel c) {
        if (c.isGroup()) {
            if (c.getTitleOverride() != null && !c.getTitleOverride().isEmpty()) {
                return c.getTitleOverride();
            }
            return "Group chat";
        } else {
            if (c.getOtherName() != null && !c.getOtherName().isEmpty()) {
                return c.getOtherName();
            }
            return "Direct message";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsListener != null) {
            conversationsListener.remove();
            conversationsListener = null;
        }
    }
}
