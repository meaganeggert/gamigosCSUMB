package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import com.bumptech.glide.Glide;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.example.gamigosjava.R;
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
        avatarView.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

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

}
