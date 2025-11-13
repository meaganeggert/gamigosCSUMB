package com.example.gamigosjava.ui.viewholder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration messageReg;

    private final MutableLiveData<String> _conversationId = new MutableLiveData<>();
    public final LiveData<String> conversationId = _conversationId;

    private final MutableLiveData<List<ChatMessage>> _messages = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<ChatMessage>> messages = _messages;

    public void start(String maybeConvoId, String otherUid) {
        if (maybeConvoId != null && !maybeConvoId.isEmpty()) {
            _conversationId.setValue(maybeConvoId);
            attachMessagesListener(maybeConvoId);
            return;
        }

        if (otherUid != null && !otherUid.isEmpty()) {
            String me = FirebaseAuth.getInstance().getUid();
            if (me == null) return; // not signed in; bail safely

            String convoId = (me.compareTo(otherUid) < 0) ? me + "_" + otherUid : otherUid + "_" + me;
            ensureDm(convoId, me, otherUid);
        }
    }

    private void ensureDm(String convoId, String me, String other) {
        DocumentReference convoRef = db.collection("conversations").document(convoId);
        convoRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                Map<String, Object> data = new HashMap<>();
                data.put("participants", Arrays.asList(me, other));
                data.put("isGroup", false);
                data.put("lastMessage", "");
                data.put("lastMessageAt", null);

                convoRef.set(data).addOnSuccessListener(aVoid -> {
                    createParticipantData(convoRef, me);
                    createParticipantData(convoRef, other);
                    _conversationId.setValue(convoId);
                    attachMessagesListener(convoId);
                });
            } else {
                _conversationId.setValue(convoId);
                attachMessagesListener(convoId);
            }
        });
    }

    private void createParticipantData(DocumentReference convoRef, String uid) {
        Map<String, Object> pd = new HashMap<>();
        pd.put("joinedAt", FieldValue.serverTimestamp());
        pd.put("lastReadAt", null);
        pd.put("unreadCount", 0);
        pd.put("role", "member");
        convoRef.collection("participantsData").document(uid).set(pd);
    }

    private void attachMessagesListener(String convoId) {
        detach();
        messageReg = db.collection("conversations")
                .document(convoId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        ChatMessage m = d.toObject(ChatMessage.class);
                        if (m != null) list.add(m);
                    }
                    _messages.setValue(list);
                });
    }

    public void sendMessage(String text) {
        String convoId = _conversationId.getValue();
        if (convoId == null || text == null || text.isEmpty()) return;

        String me = FirebaseAuth.getInstance().getUid();
        if (me == null) return;

        DocumentReference convoRef = db.collection("conversations").document(convoId);
        CollectionReference messagesRef = convoRef.collection("messages");

        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", me);
        msg.put("text", text);
        msg.put("createdAt", FieldValue.serverTimestamp());

        messagesRef.add(msg).addOnSuccessListener(r -> {
            Map<String, Object> u = new HashMap<>();
            u.put("lastMessage", text);
            u.put("lastMessageAt", FieldValue.serverTimestamp());
            convoRef.update(u);

            // increment others' unread (safe cast)
            convoRef.get().addOnSuccessListener(snap -> {
                Object raw = snap.get("participants");
                if (!(raw instanceof List<?> participants)) return;
                for (Object o : participants) {
                    String uid = String.valueOf(o);
                    if (!uid.equals(me)) {
                        convoRef.collection("participantsData").document(uid)
                                .update("unreadCount", FieldValue.increment(1));
                    }
                }
            });
        });
    }

    public void markRead() {
        String convoId = _conversationId.getValue();
        String me = FirebaseAuth.getInstance().getUid();
        if (convoId == null || me == null) return;

        db.collection("conversations").document(convoId)
                .collection("participantsData").document(me)
                .update("unreadCount", 0, "lastReadAt", Timestamp.now());
    }

    private void detach() {
        if (messageReg != null) {
            messageReg.remove();
            messageReg = null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        detach();
    }
}
