package com.example.gamigosjava.ui.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.example.gamigosjava.R;
import com.example.gamigosjava.notifications.EventStartReceiver;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.drawerlayout.widget.DrawerLayout;

public abstract class BaseActivity extends AppCompatActivity {
    protected DrawerLayout drawer;
    protected NavigationView navView;
    protected Toolbar toolbar;
    protected ShapeableImageView avatarView;

    private DocumentReference userDocRef;
    protected ActionBarDrawerToggle drawerToggle;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        drawer = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        avatarView = findViewById(R.id.imageAvatar);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userDocRef = db.collection("users").document(currentUser.getUid());
            loadAvatar();
        } else {
            avatarView.setImageResource(android.R.drawable.ic_menu_camera);
        }

        setSupportActionBar(toolbar);

        // Hook up hamburger icon to DrawerLayout
        drawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        drawerToggle.getDrawerArrowDrawable().setColor(
                ContextCompat.getColor(this, R.color.white)
        );

        // Drawer item navigation (replace with your Activities)
        navView.setNavigationItemSelectedListener(item -> {
            drawer.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            //TODO: WIRE UP INTENTS
            if (id == R.id.nav_home && !(this instanceof com.example.gamigosjava.ui.activities.LandingActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.LandingActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_events && !(this instanceof com.example.gamigosjava.ui.activities.EventsLandingPage)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.EventsLandingPage.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_profile && !(this instanceof com.example.gamigosjava.ui.activities.ProfileActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.ProfileActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_achievements && !(this instanceof com.example.gamigosjava.ui.activities.AchievementsActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.AchievementsActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_friends && !(this instanceof com.example.gamigosjava.ui.activities.FriendsLanding)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.FriendsLanding.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_messages && !(this instanceof com.example.gamigosjava.ui.activities.ConversationsActivity)) {
                startActivity(new Intent(this, ConversationsActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_notifications && !(this instanceof com.example.gamigosjava.ui.activities.NotificationsActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.NotificationsActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_games && !(this instanceof com.example.gamigosjava.ui.activities.MatchLandingActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.MatchLandingActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            } else if (id == R.id.nav_logout) {
                // Sign Out of Firebase
                FirebaseAuth.getInstance().signOut();

                // Return to sign-in screen
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            return true;
        });

        // Avatar click takes you to the profile page
        avatarView.setOnClickListener(v ->
                MyProfileBottomSheet.newInstance().show(getSupportFragmentManager(), "my_profile_sheet"));

        // Optional: give a default title
        // setTitle("HelloWorld");
    }

    // Inflate each child Activity’s layout into the shared container.
    protected void setChildLayout(@LayoutRes int layoutRes) {
        FrameLayout container = findViewById(R.id.contentContainer);
        getLayoutInflater().inflate(layoutRes, container, true);
    }

    // Function to set navbar title
    protected void setTopTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        toolbar.setTitle(title);
    }


    private void loadAvatar() {
        if (userDocRef == null || avatarView == null) {
            if (avatarView != null) {
                avatarView.setImageResource(android.R.drawable.ic_menu_camera);
            }
        }

        assert userDocRef != null;
        userDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String photoUrl = doc.getString("photoUrl");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .into(avatarView);
                } else {
                    avatarView.setImageResource(android.R.drawable.ic_menu_camera);
                }
            } else {
                avatarView.setImageResource(android.R.drawable.ic_menu_camera);
            }
                })
                .addOnFailureListener(e -> avatarView.setImageResource(android.R.drawable.ic_menu_camera));
    }

    protected void enableBackToConversations() {
        // Lock the drawer closed so swiping from the edge doesn’t open it
        if (drawer != null) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        // Disable the hamburger behavior
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            drawer.removeDrawerListener(drawerToggle);
        }

        // Show the back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24)
            );
        }

        // Clicking the arrow explicitly goes to ConversationsActivity
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                Intent intent = new Intent(this, ConversationsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }
    }

    /**
     * Replace the hamburger with a simple back arrow that behaves like the native Android back button.
     */
    protected void enableToolbarBackArrow() {
        // Lock the drawer closed so swiping from the edge doesn’t open it
        if (drawer != null) {
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        // Disable the hamburger behavior
        if (drawerToggle != null) {
            drawerToggle.setDrawerIndicatorEnabled(false);
            drawer.removeDrawerListener(drawerToggle);
        }

        // Show the back arrow in the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(
                    ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24)
            );
        }

        // Clicking the arrow just goes "back" (finishes this Activity) [Same as back button press]
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private static final int REQ_POST_NOTIFICATIONS = 2001;
    private static final String PREFS_NAME = "gamigos_prefs";
    private static final String KEY_NOTIF_SETTINGS_DIALOG_SHOWN = "notif_settings_dialog_shown";

    protected void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            } else {
                // Permission already granted → only maybe show our dialog once
                maybePromptEnableNotificationsOnce();
            }
        } else {
            // Below Android 13 → no runtime permission, just maybe show our dialog once
            maybePromptEnableNotificationsOnce();
        }
    }

    private void maybePromptEnableNotificationsOnce() {
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);

        // If OS-level notifications are ON, nothing to do
        if (nm.areNotificationsEnabled()) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean alreadyShown = prefs.getBoolean(KEY_NOTIF_SETTINGS_DIALOG_SHOWN, false);

        // We've already shown the dialog before → don't show it again
        if (alreadyShown) return;

        new AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("Gamigos uses notifications for messages and friend requests. Turn them on in settings?")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    prefs.edit().putBoolean(KEY_NOTIF_SETTINGS_DIALOG_SHOWN, true).apply();
                    openNotificationSettings();
                })
                .setNegativeButton("Not now", (dialog, which) -> {
                    // Still mark as shown so we don’t nag them again
                    prefs.edit().putBoolean(KEY_NOTIF_SETTINGS_DIALOG_SHOWN, true).apply();
                })
                .show();
//     }

//     private void openNotificationSettings() {
//         Intent intent;

//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//             intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
//                     .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
//         } else {
//             intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                     .setData(Uri.parse("package:" + getPackageName()));
//         }

//         startActivity(intent);
//     }

//     protected void scheduleEventStartAlarm(
//             String eventId,
//             String eventTitle,
//             String hostName,
//             long triggerAtMillis
//     ) {
//         if (eventId == null) return;

//         Intent alarmIntent = new Intent(this, EventStartReceiver.class);
//         alarmIntent.putExtra(EventStartReceiver.EXTRA_EVENT_ID, eventId);
//         alarmIntent.putExtra(EventStartReceiver.EXTRA_EVENT_TITLE, eventTitle);
//         alarmIntent.putExtra(EventStartReceiver.EXTRA_HOST_NAME, hostName);

//         int requestCode = eventId.hashCode();

//         PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                 this,
//                 requestCode,
//                 alarmIntent,
//                 Build.VERSION.SDK_INT >= 31
//                         ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
//                         : PendingIntent.FLAG_UPDATE_CURRENT
//         );

//         android.app.AlarmManager alarmManager =
//                 (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
//         if (alarmManager == null) return;

//         long now = System.currentTimeMillis();
//         if (triggerAtMillis <= now) {
//             // Don't schedule alarms in the past
//             return;
//         }

//         // Inexact window (5 minutes) – avoids exact-alarm permission
//         long windowLength = 5L * 60L * 1000L;

//         if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
//             alarmManager.setWindow(
//                     android.app.AlarmManager.RTC_WAKEUP,
//                     triggerAtMillis,
//                     windowLength,
//                     pendingIntent
//             );
//         } else {
//             alarmManager.set(
//                     android.app.AlarmManager.RTC_WAKEUP,
//                     triggerAtMillis,
//                     pendingIntent
//             );
//         }

//         Log.d("EventAlarm", "Scheduled event-start alarm for " + eventId +
//                 " at " + triggerAtMillis);
//     }

    }

    private void openNotificationSettings() {
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
        }

        startActivity(intent);
    }

    protected void scheduleEventStartAlarm(
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

        long now = System.currentTimeMillis();
        if (triggerAtMillis <= now) {
            // Don't schedule alarms in the past
            return;
        }

        // Inexact window (5 minutes) – avoids exact-alarm permission
        long windowLength = 5L * 60L * 1000L;

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
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

        Log.d("EventAlarm", "Scheduled event-start alarm for " + eventId +
                " at " + triggerAtMillis);
    }

    protected void cancelEventStartAlarm(String eventId) {
        if (eventId == null) return;

        Intent alarmIntent = new Intent(this, EventStartReceiver.class);
        int requestCode = eventId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                alarmIntent,
                Build.VERSION.SDK_INT >= 31
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
                        : PendingIntent.FLAG_NO_CREATE
        );

        if (pendingIntent == null) return;  // nothing scheduled

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted → now we can check if OS toggle is off and maybe show dialog once
                maybePromptEnableNotificationsOnce();
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
