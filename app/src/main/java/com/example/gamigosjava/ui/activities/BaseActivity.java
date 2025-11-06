package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.net.Uri;
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

import androidx.drawerlayout.widget.DrawerLayout;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawer;
    protected NavigationView navView;
    protected Toolbar toolbar;
    protected ShapeableImageView avatarView;
    private ActionBarDrawerToggle toggle;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        drawer = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        avatarView = findViewById(R.id.imageAvatar);

        setSupportActionBar(toolbar);

        // Hook up hamburger icon to DrawerLayout
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        toggle.getDrawerArrowDrawable().setColor(
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
//            } else if (id == R.id.nav_games && !(this instanceof com.example.gamigosjava.ui.GamesActivity)) {
//                startActivity(new Intent(this, com.example.gamigosjava.ui.GamesActivity.class));
//                overridePendingTransition(0,0);
//                finish();
//                return true;
            } else if (id == R.id.nav_profile && !(this instanceof com.example.gamigosjava.ui.activities.ProfileActivity)) {
                startActivity(new Intent(this, com.example.gamigosjava.ui.activities.ProfileActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;
            }
            return true;
        });

        // Avatar click takes you to the profile page
        avatarView.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.gamigosjava.ui.activities.ProfileActivity.class));
        });

        // Optional: give a default title
//        setTitle("HelloWorld");
    }

    /** Inflate each child Activity’s layout into the shared container. */
    protected void setChildLayout(@LayoutRes int layoutRes) {
        FrameLayout container = findViewById(R.id.contentContainer);
        getLayoutInflater().inflate(layoutRes, container, true);
    }

    /** Call this from children to center a title in the toolbar. */
    protected void setTopTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        toolbar.setTitle(title);
    }

    /** Call to update avatar (e.g., after login). Accepts null-safe Uri or URL string. */
    protected void setAvatar(Object photo) {
        if (photo == null) {
            avatarView.setImageResource(R.drawable.ic_person_24);
            return;
        }
        Glide.with(this)
                .load(photo)               // Uri, String, File… Glide handles it
                .placeholder(R.drawable.ic_person_24)
                .error(R.drawable.ic_person_24)
                .circleCrop()
                .into(avatarView);
    }
}
