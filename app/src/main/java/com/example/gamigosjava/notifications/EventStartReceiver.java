package com.example.gamigosjava.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.activities.ViewEventActivity;

public class EventStartReceiver extends BroadcastReceiver {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_EVENT_TITLE = "extra_event_title";
    public static final String EXTRA_HOST_NAME = "extra_host_name";
    private static final String CHANNEL_EVENT_START = "channel_event_start";

    @Override
    public void onReceive(Context context, Intent intent) {
        String eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        String eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE);
        String hostName = intent.getStringExtra(EXTRA_HOST_NAME);

        String titleText = "Event starting";
        String bodyText;
        if (eventTitle != null && !eventTitle.isEmpty()) {
            if (hostName != null && !hostName.isEmpty()) {
                bodyText = hostName + "'s event \"" + eventTitle + "\" is starting now";
            } else {
                bodyText = "Event \"" + eventTitle + "\" is starting now";
            }
        } else {
            bodyText = "An event is starting now";
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel if needed (like your helper)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                manager.getNotificationChannel(CHANNEL_EVENT_START) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_EVENT_START,
                    "Event Start",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        // Tap opens the event page
        Intent openIntent = new Intent(context, ViewEventActivity.class);
        openIntent.putExtra("selectedEventId", eventId);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        int requestCode = (eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                openIntent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_EVENT_START)
                        .setSmallIcon(R.drawable.ic_event_24)   // reuse your event icon
                        .setContentTitle(titleText)
                        .setContentText(bodyText)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(bodyText))
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
        manager.notify(notificationId, builder.build());
    }
}
