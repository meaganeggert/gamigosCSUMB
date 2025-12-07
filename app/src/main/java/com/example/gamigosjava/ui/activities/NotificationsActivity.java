package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

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
import com.google.firebase.firestore.WriteBatch;

public class NotificationsActivity extends BaseActivity {

    private static final String TAG = "NotificationsActivity";
    private Button btnClearAll;
    private Button btnSelectMode;
    private Button btnDeleteSelected;
    private Button btnCancelSelection;
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

        //  Button wiring
        btnClearAll = findViewById(R.id.btnClearAllNotifications);
        btnSelectMode = findViewById(R.id.btnSelectMode);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);

        if (btnClearAll != null) {
            btnClearAll.setOnClickListener(v -> showClearAllConfirmDialog());
        }

        if (btnSelectMode != null) {
            btnSelectMode.setOnClickListener(v -> enterSelectionMode());
        }

        if (btnDeleteSelected != null) {
            btnDeleteSelected.setOnClickListener(v -> showDeleteSelectedConfirmDialog());
        }

        if (btnCancelSelection != null) {
            btnCancelSelection.setOnClickListener(v -> exitSelectionMode());
        }

        // listen to selection count from adapter to enable/disable delete button
        adapter.setOnSelectionChangedListener(count -> {
            if (btnDeleteSelected != null) {
                boolean enabled = count > 0;
                btnDeleteSelected.setEnabled(enabled);
                btnDeleteSelected.setAlpha(enabled ? 1f : 0.4f);
            }
        });

        loadNotifications();
    }

    private void enterSelectionMode() {
        adapter.setSelectionMode(true);

        // Hide stuff that only makes sense in normal mode
        if (btnClearAll != null) {
            btnClearAll.setVisibility(Button.GONE);
        }
        if (btnSelectMode != null) {
            btnSelectMode.setVisibility(Button.GONE);
        }

        // Show selection controls
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setVisibility(Button.VISIBLE);
            btnDeleteSelected.setEnabled(false);
            btnDeleteSelected.setAlpha(0.4f);
        }
        if (btnCancelSelection != null) {
            btnCancelSelection.setVisibility(Button.VISIBLE);
        }
    }

    private void exitSelectionMode() {
        adapter.setSelectionMode(false);  // clears selections internally

        // Show normal mode buttons
        if (btnClearAll != null) {
            btnClearAll.setVisibility(Button.VISIBLE);
        }
        if (btnSelectMode != null) {
            btnSelectMode.setVisibility(Button.VISIBLE);
        }

        // Hide selection controls
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setVisibility(Button.GONE);
        }
        if (btnCancelSelection != null) {
            btnCancelSelection.setVisibility(Button.GONE);
        }
    }

    private void showDeleteSelectedConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete selected")
                .setMessage("Are you sure you want to delete the selected notifications?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSelectedNotifications())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteSelectedNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        java.util.List<AppNotificationModel> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (AppNotificationModel notif : selected) {
            if (notif.getId() == null || notif.getId().isEmpty()) continue;
            batch.delete(db.collection("users")
                    .document(user.getUid())
                    .collection("notifications")
                    .document(notif.getId()));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted selected notifications", Toast.LENGTH_SHORT).show();
                    exitSelectionMode();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete selected notifications", e);
                    Toast.makeText(this, "Failed to delete selected notifications", Toast.LENGTH_SHORT).show();
                });
    }



    private void showClearAllConfirmDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear all notifications")
                .setMessage("Are you sure you want to clear all notifications? This cannot be undone")
                .setPositiveButton("Clear all", (dialog, which) -> clearAllNotifications())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteNotification(AppNotificationModel notif) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User is null; cannot delete notification");
            return;
        }

        String notifId = notif.getId();
        if (notifId == null || notifId.isEmpty()) {
            Log.w(TAG, "Notification has no id; cannot delete");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .document(notifId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Notification deleted: " + notifId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to delete notification: " + notifId, e));
    }


    private void clearAllNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        //  Prevents spam-clicking
        btnClearAll.setEnabled(false);

        //  Clear notifications from database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No notifications to clear.", Toast.LENGTH_SHORT).show();
                        btnClearAll.setEnabled(true);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Successfully cleared all notifications.", Toast.LENGTH_SHORT).show();
                                btnClearAll.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to clear notifications", e);
                                Toast.makeText(this, "Failed to clear notifications.", Toast.LENGTH_SHORT).show();
                                btnClearAll.setEnabled(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query notifications", e);
                    Toast.makeText(this, "Failed to query notifications.", Toast.LENGTH_SHORT).show();
                    btnClearAll.setEnabled(true);
                });
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
                    //  Disable clear all button if list is empty.
                    btnClearAll.setEnabled(!list.isEmpty());
                    btnClearAll.setAlpha(list.isEmpty() ? 0.4f : 1f);
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

        deleteNotification(notif);
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
