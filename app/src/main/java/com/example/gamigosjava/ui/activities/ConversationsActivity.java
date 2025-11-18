package com.example.gamigosjava.ui.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.Intent;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.ConversationsModel;
import com.example.gamigosjava.ui.adapter.ConversationsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ConversationsActivity extends BaseActivity {
    private ConversationsAdapter adapter;
    private FirebaseFirestore db;
    private String currentUid;

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
                            otherUid
                    )
            );
        });

        rvConversations.setAdapter(adapter);

        FloatingActionButton newConversationFab = findViewById(R.id.fabNewConversation);
        newConversationFab.setOnClickListener(v -> startActivity(new Intent(this, NewConversationActivity.class)));

        loadConversations();
    }

    private void loadConversations() {
        if (currentUid == null) return;

        db.collection("conversations")
                .whereArrayContains("participants", currentUid)
                .orderBy("lastMessageAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("Convos", "Got " + querySnapshot.size() + " conversations");
                    List<ConversationsModel> list = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ConversationsModel c = doc.toObject(ConversationsModel.class);
                        if (c == null) continue;
                        c.setId(doc.getId());

                        // find otherUid for 1–1 chats
                        if (!c.isGroup() && c.getParticipants() != null) {
                            for (String uid : c.getParticipants()) {
                                if (!uid.equals(currentUid)) {
                                    c.setOtherUid(uid);
                                    break;
                                }
                            }
                        }
                        list.add(c);
                    }
                    adapter.setConversations(list);
                    fetchUserInfoForDMs(list);
                })
                .addOnFailureListener(error -> android.util.Log.d("Convos", "Failed to load conversations: ", error));
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
                        adapter.notifyDataSetChanged(); // simple & fine for small lists
                    });
        }
    }

    private String resolveTitleForConversation(ConversationsModel c) {
        if (c.isGroup()) {
            // if you eventually add a title field:
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
}
