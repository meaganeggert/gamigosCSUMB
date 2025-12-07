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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FirebaseMessagingService";

    private static final String CHANNEL_MESSAGES = "channel_messages";
    private static final String CHANNEL_FRIEND_REQUESTS = "channel_friend_requests";
    private static final String CHANNEL_EVENT_INVITES = "channel_event_invites";
    private static final String CHANNEL_EVENT_START = "channel_event_start";
    private static final String CHANNEL_EVENT_STATUS = "channel_event_status";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d("FCM_SERVICE", "onMessageReceived: " + remoteMessage.getData()
                + " notif=" + remoteMessage.getNotification());

        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");
        String senderUid = data.get("senderUid");
        String currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        if ("message".equals(type)) {
            assert senderUid != null;
            if (senderUid.equals(currentUserId)) {
                Log.i(TAG, "Ignoring message from myself: " + currentUserName);
                return;
            }
            showMessageNotification(data);
        } else if ("friend_request".equals(type)) {
            showFriendRequestNotification(data);
        } else if ("event_invite".equals(type)) {
            showEventInviteNotification(data);
        } else if ("event_started".equals(type)) {
            showEventStartedNotification(data);
        } else if ("event_ended".equals(type)) {
            showEventEndedNotification(data);
        } else if ("event_rescheduled".equals(type)) {
            showEventRescheduledNotification(data);
        } else if("event_deleted".equals(type)) {
            showEventDeletedNotification(data);
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

    private void showEventDeletedNotification(Map<String, String> data) {
        String eventId = data.get("eventId");
        String eventTitle = data.get("eventTitle");
        String hostName = data.get("hostName");

        createChannelIfNeeded(CHANNEL_EVENT_STATUS, "Event Status");

        String titleText = "Event cancelled";
        String bodyText;

        if (eventTitle != null && !eventTitle.isEmpty()) {
            if (hostName != null && !hostName.isEmpty()) {
                bodyText = hostName + " cancelled \"" + eventTitle + "\"";
            } else {
                bodyText = "The event \"" + eventTitle + "\" was cancelled";
            }
        } else {
            bodyText = "An event you’re in was cancelled";
        }

        //  Save to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("eventId", eventId);
        extras.put("eventTitle", eventTitle);
        extras.put("hostName", hostName);

        saveNotificationToFirestore(
                "event_deleted",
                titleText,
                bodyText,
                extras
        );

        Intent intent = new Intent(this, com.example.gamigosjava.ui.activities.ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_STATUS)
                        .setSmallIcon(R.drawable.ic_event_24)
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }


    private void showEventRescheduledNotification(Map<String, String> data) {
        String eventId = data.get("eventId");
        String eventTitle = data.get("eventTitle");
        String hostName = data.get("hostName");
        String newScheduledAtStr = data.get("scheduledAt");

        createChannelIfNeeded(CHANNEL_EVENT_STATUS, "Event Status");

        String whenText = "";
        if (newScheduledAtStr != null && !newScheduledAtStr.isEmpty()) {
            try {
                long millis = Long.parseLong(newScheduledAtStr);
                DateFormat df = DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM,
                        DateFormat.SHORT
                );
                whenText = " to " + df.format(new Date(millis));
            } catch (NumberFormatException ignored) {}
        }

        String titleText = "Event rescheduled";
        String bodyText;

        if (eventTitle != null && !eventTitle.isEmpty()) {
            if (hostName != null && !hostName.isEmpty()) {
                bodyText = hostName + " changed \"" + eventTitle + "\"" + whenText;
            } else {
                bodyText = "\"" + eventTitle + "\" was rescheduled" + whenText;
            }
        } else {
            bodyText = "An event you’re invited to was rescheduled" + whenText;
        }

        //  Save to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("eventId", eventId);
        extras.put("eventTitle", eventTitle);
        extras.put("hostName", hostName);
        extras.put("scheduledAt", newScheduledAtStr);

        saveNotificationToFirestore(
                "event_rescheduled",
                titleText,
                bodyText,
                extras
        );

        Intent intent = new Intent(this, com.example.gamigosjava.ui.activities.ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_STATUS)
                        .setSmallIcon(R.drawable.ic_event_24)
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }

    private void showEventEndedNotification(Map<String, String> data) {
        String eventId = data.get("eventId");
        String eventTitle = data.get("eventTitle");
        String hostName = data.get("hostName");

        createChannelIfNeeded(CHANNEL_EVENT_STATUS, "Event Status");

        String titleText = "Event ended";
        String bodyText;

        if (eventTitle != null && !eventTitle.isEmpty()) {
            if (hostName != null && !hostName.isEmpty()) {
                bodyText = hostName + "'s event \"" + eventTitle + "\" has ended";
            } else {
                bodyText = "The event \"" + eventTitle + "\" has ended";
            }
        } else {
            bodyText = "An event you’re in has ended";
        }

        //  Save to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("eventId", eventId);
        extras.put("eventTitle", eventTitle);
        extras.put("hostName", hostName);

        saveNotificationToFirestore(
                "event_ended",
                titleText,
                bodyText,
                extras
        );

        Intent intent = new Intent(this, com.example.gamigosjava.ui.activities.ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_STATUS)
                        .setSmallIcon(R.drawable.ic_event_24)
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }

    private void saveNotificationToFirestore(
            String type,
            String title,
            String body,
            Map<String, Object> extraFields
    ) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "Cannot save notification, user is null");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .document();  // auto ID

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("type", type);
        data.put("title", title);
        data.put("body", body);
        data.put("timestamp", System.currentTimeMillis());

        if (extraFields != null) {
            data.putAll(extraFields);
        }

        docRef.set(data)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Saved notification doc " + docRef.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed saving notification doc", e));
    }

    private void showEventStartedNotification(Map<String, String> data) {
        String eventId = data.get("eventId");
        String eventTitle = data.get("eventTitle");
        String hostName = data.get("hostName");

        createChannelIfNeeded(CHANNEL_EVENT_STATUS, "Event Status");

        String titleText = "Event is starting";
        String bodyText;

        if (hostName != null && !hostName.isEmpty() &&
                eventTitle != null && !eventTitle.isEmpty()) {
            bodyText = hostName + "'s event " + eventTitle + " is starting now";
        } else if (eventTitle != null && !eventTitle.isEmpty()) {
            bodyText = eventTitle + " is starting now";
        } else {
            bodyText = "An event you're invited to is starting now";
        }

        //  Save to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("eventId", eventId);
        extras.put("eventTitle", eventTitle);
        extras.put("hostName", hostName);

        saveNotificationToFirestore(
                "event_started",
                titleText,
                bodyText,
                extras
        );

        Intent intent = new Intent(this, com.example.gamigosjava.ui.activities.ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_STATUS)
                        .setSmallIcon(R.drawable.ic_event_24) // pick whatever icon fits
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }

    private void showEventInviteNotification(Map<String, String> data) {
        String eventId = data.get("eventId");
        String eventTitle = data.get("eventTitle");
        String hostName = data.get("hostName");
        String scheduledAtStr = data.get("scheduledAt");  // 👈 comes from FCM data

        createChannelIfNeeded(CHANNEL_EVENT_INVITES, "Event Invites");
        createChannelIfNeeded(CHANNEL_EVENT_START, "Event Start"); // for start alarm notif

        String titleText = "Event invite";
        String bodyText;

        if (hostName != null && !hostName.isEmpty() && eventTitle != null && !eventTitle.isEmpty()) {
            bodyText = hostName + " invited you to " + eventTitle;
        } else if (eventTitle != null && !eventTitle.isEmpty()) {
            bodyText = "You were invited to " + eventTitle;
        } else {
            bodyText = "You received an event invite";
        }

        //  Save to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("eventId", eventId);
        extras.put("eventTitle", eventTitle);
        extras.put("hostName", hostName);
        extras.put("scheduledAt", scheduledAtStr);

        saveNotificationToFirestore(
                "event_invite",
                titleText,
                bodyText,
                extras
        );

        // === Existing invite notification ===
        Intent intent = new Intent(this, com.example.gamigosjava.ui.activities.ViewEventActivity.class);
        intent.putExtra("selectedEventId", eventId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_EVENT_INVITES)
                        .setSmallIcon(R.drawable.ic_event_24)
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());

        // === "Event starting" alarm scheduling (unchanged) ===
        if (scheduledAtStr != null) {
            try {
                long scheduledAtMillis = Long.parseLong(scheduledAtStr);

                // e.g. notify 10 minutes before start
                long triggerAtMillis = scheduledAtMillis - 10L * 60L * 1000L;

                long now = System.currentTimeMillis();
                if (triggerAtMillis < now) {
                    // If it's already within 10 minutes, fire at "now" + a few seconds
                    triggerAtMillis = now + 5_000L;
                }

                scheduleEventStartAlarm(eventId, eventTitle, hostName, triggerAtMillis);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid scheduledAt millis: " + scheduledAtStr, e);
            }
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

//         NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES)
//                 .setSmallIcon(R.drawable.outline_mark_email_unread_24)
//                 .setContentTitle(title)
//                 .setContentText(
//                         isGroup && senderName != null
//                                 ? senderName + ": " + messagePreview
//                                 : messagePreview
//                 )
          
          
        //  Save notification to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("conversationId", conversationId);
        extras.put("isGroup", isGroup);
        extras.put("groupTitle", groupTitle);
        extras.put("otherUid", otherUid);
        extras.put("senderName", senderName);

        String bodyText = isGroup && senderName != null
                ? senderName + ": " + messagePreview
                : messagePreview;

        saveNotificationToFirestore(
                "message",
                title,
                bodyText,
                extras
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.outline_mark_email_unread_24)
                .setContentTitle(title)
                .setContentText(bodyText)
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

//         NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FRIEND_REQUESTS)
//                 .setSmallIcon(R.drawable.outline_person_add_24)
//                 .setContentTitle("New friend request")
//                 .setContentText(fromName + " sent you a friend request")
      
        String bodyText = fromName + " sent you a friend request";

        //  Save notification to Firestore
        Map<String, Object> extras = new java.util.HashMap<>();
        extras.put("fromUserId", fromUserId);
        extras.put("fromName", fromName);

        saveNotificationToFirestore(
                "friend_request",
                "New friend request",
                bodyText,
                extras
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_FRIEND_REQUESTS)
                .setSmallIcon(R.drawable.outline_person_add_24)
                .setContentTitle("New friend request")
                .setContentText(bodyText)
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

    private void scheduleEventStartAlarm(
            String eventId,
            String eventTitle,
            String hostName,
            long triggerAtMillis
    ) {
        if (eventId == null) return;

        Intent alarmIntent = new Intent(this, EventStartReceiver.class);
        alarmIntent.putExtra(EventStartReceiver.EXTRA_EVENT_ID, eventId);
        alarmIntent.putExtra(EventStartReceiver.EXTRA_EVENT_TITLE, eventTitle);
        alarmIntent.putExtra(EventStartReceiver.EXTRA_HOST_NAME, hostName);

        int requestCode = eventId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                alarmIntent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Give the OS a 5-minute window to fire inside
            long windowLength = 5L * 60L * 1000L; // 5 minutes
            alarmManager.setWindow(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    windowLength,
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        Log.d(TAG, "Scheduled (inexact) event start alarm for " + eventId + " at " + triggerAtMillis);
    }
}