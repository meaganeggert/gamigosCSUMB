package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.gamigosjava.R;

public class FriendsLanding extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_friends_landing);

        // Set title for NavBar
        setTopTitle("Friends");

        //  Find button
        Button btnSearchFriends = findViewById(R.id.btnFriendSearchActivity);

        //  Set up click listener
        btnSearchFriends.setOnClickListener(v -> {
            Intent intent = new Intent(FriendsLanding.this, FriendsSearchActivity.class);
            startActivity(intent);
        });
    }
}