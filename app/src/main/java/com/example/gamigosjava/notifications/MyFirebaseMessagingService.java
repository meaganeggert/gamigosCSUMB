package com.example.gamigosjava.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.activities.FriendsLanding;
import com.example.gamigosjava.ui.activities.MessagesActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_MESSAGES = "channel_messages";
    private static final String CHANNEL_FRIEND_REQUESTS = "channel_friend_requests";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("FCM_SERVICE", "onMessageReceived: " + remoteMessage.getData()
                + " notif=" + remoteMessage.getNotification());

        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if ("message".equals(type)) {
            showMessageNotification(data);
        } else if ("friend_request".equals(type)) {
            showFriendRequestNotification(data);
        } else if (remoteMessage.getNotification() != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                    .setSmallIcon(R.drawable.ic_notification_24)
                    .setContentTitle(remoteMessage.getNotification().getTitle())
                    .setContentText(remoteMessage.getNotification().getBody())
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify((int) (System.currentTimeMillis() & 0xfffffff), builder.build());
        }
    }


    private void showMessageNotification(Map<String, String> data) {
        String conversationId = data.get("conversationId");
        String messagePreview = data.get("messagePreview");

        boolean isGroup = "true".equals(data.get("isGroup"));
        String groupTitle = data.get("groupTitle");
        String senderName = data.get("senderName");
        String senderUid = data.get("senderUid");

        createChannelIfNeeded(CHANNEL_MESSAGES, "Messages");

        String title;
        String otherUid;

        if (isGroup) {
            title = (groupTitle != null && !groupTitle.isEmpty())
                    ? groupTitle
                    : "Group chat";
            otherUid = null;
        } else {
            title = (senderName != null && !senderName.isEmpty())
                    ? senderName
                    : "Direct message";
            otherUid = senderUid;
        }

        Intent intent = MessagesActivity.newIntent(
                this,
                conversationId,
                title,
                otherUid,
                isGroup
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        assert conversationId != null;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                conversationId.hashCode(),
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.outline_mark_email_unread_24)
                .setContentTitle(title)           // group name OR sender name
                .setContentText(
                        isGroup && senderName != null
                                ? senderName + ": " + messagePreview
                                : messagePreview
                )
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }

    private void showFriendRequestNotification(Map<String, String> data) {
        String fromName = data.get("fromName");
        String fromUserId = data.get("fromUserId");

        createChannelIfNeeded(CHANNEL_FRIEND_REQUESTS, "Friend Requests");

        Intent intent = new Intent(this, FriendsLanding.class);
        intent.putExtra(FriendsLanding.EXTRA_FOCUS_REQUEST_UID, fromUserId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        assert fromUserId != null;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                fromUserId.hashCode(),
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FRIEND_REQUESTS)
                .setSmallIcon(R.drawable.outline_person_add_24)
                .setContentTitle("New friend request")
                .setContentText(fromName + " sent you a friend request")
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }



    private void createChannelIfNeeded(String id, String name) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager.getNotificationChannel(id) == null) {
            NotificationChannel channel = new NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        //  Sends to Firestore so we can notify this device
        NotificationTokenManager.saveTokenForCurrentUser(token);
    }
}