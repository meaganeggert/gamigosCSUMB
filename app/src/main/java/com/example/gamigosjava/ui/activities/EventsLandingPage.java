package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gamigosjava.R;

public class EventsLandingPage extends BaseActivity {

    private static final String TAG = "Events";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_events_landing_page);

        // Set title for NavBar
        setTopTitle("Events");

        View createEventTestButton = findViewById(R.id.button_createEventTest);
        if (createEventTestButton != null) {
            createEventTestButton.setOnClickListener(v -> {
                Log.d(TAG, "Create Event Test button CLICKED"); // debug
                Intent intent = new Intent(this, CreateEventActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Create Event Test button NOT FOUND"); // debug
        }
    }
}