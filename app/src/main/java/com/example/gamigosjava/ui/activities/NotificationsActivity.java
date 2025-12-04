package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.AppNotificationModel;
import com.example.gamigosjava.ui.adapter.NotificationsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class NotificationsActivity extends BaseActivity {

    private static final String TAG = "NotificationsActivity";

    private NotificationsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setChildLayout(R.layout.activity_notifications);
        setTopTitle("Notifications");

        RecyclerView rv = findViewById(R.id.recyclerNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationsAdapter(this::handleNotificationClick);
        rv.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User is null; cannot load notifications");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening for notifications", e);
                        return;
                    }
                    if (snap == null) return;

                    java.util.List<AppNotificationModel> list = new java.util.ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        AppNotificationModel n = doc.toObject(AppNotificationModel.class);
                        if (n == null) continue;
                        n.setId(doc.getId());
                        list.add(n);
                    }
                    adapter.setItems(list);
                });
    }

    private void handleNotificationClick(AppNotificationModel notif) {
        String type = notif.getType();
        if (type == null) return;

        switch (type) {
            case "message":
                openMessageFromNotification(notif);
                break;

            case "friend_request":
                openFriendRequestFromNotification(notif);
                break;

            // Event notifications:
            case "event_started":
            case "event_invite":
            case "event_ended":
            case "event_rescheduled":
                openEventFromNotification(notif);
                break;

            default:
                Log.i(TAG, "Unknown notification type: " + type);
                break;
        }
    }

    private void openEventFromNotification(AppNotificationModel notif) {
        String eventId = notif.getEventId();
        if (eventId == null || eventId.isEmpty()) {
            Log.w(TAG, "Notification has no eventId: " + notif.getId());
            return;
        }

        Intent intent = new Intent(this, ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        startActivity(intent);
    }



    private void openMessageFromNotification(AppNotificationModel notif) {
        String conversationId = notif.getConversationId();
        if (conversationId == null) return;

        boolean isGroup = Boolean.TRUE.equals(notif.getIsGroup());
        String title = notif.getIsGroup() != null && notif.getIsGroup()
                ? (notif.getGroupTitle() != null ? notif.getGroupTitle() : "Group chat")
                : (notif.getSenderName() != null ? notif.getSenderName() : "Direct message");

        String otherUid = notif.getOtherUid();

        Intent intent = MessagesActivity.newIntent(
                this,
                conversationId,
                title,
                otherUid,
                isGroup
        );
        startActivity(intent);
    }

    private void openFriendRequestFromNotification(AppNotificationModel notif) {
        String fromUserId = notif.getFromUserId();
        Intent intent = new Intent(this, FriendsLanding.class);
        intent.putExtra(FriendsLanding.EXTRA_FOCUS_REQUEST_UID, fromUserId);
        startActivity(intent);
    }
}
