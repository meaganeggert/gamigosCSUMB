package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

// Firebase
import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;

// Credential Manager (AndroidX)
import androidx.credentials.CredentialManager;

public class LandingActivity extends AppCompatActivity {

    private static final String TAG = "Logout";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Link to sign-in button
        View logoutButton = findViewById(R.id.button_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Toast.makeText(this, "Logout button CLICKED", Toast.LENGTH_SHORT).show(); // debug
                Log.d(TAG, "Logout button CLICKED"); // debug
                logOut();
            });
        } else {
            Log.e(TAG, "Logout button NOT FOUND"); // debug
        }

        // Link to BGGTest Activity
        // Debugging Purposes
        View testButton = findViewById(R.id.button_test);
        if (testButton != null) {
            testButton.setOnClickListener(v -> {
//                Toast.makeText(this, "Test button CLICKED", Toast.LENGTH_SHORT).show(); // debug
                Log.d(TAG, "Test button CLICKED"); // debug
                // Navigate to BGGTestActivity
                Intent intent = new Intent(this, BGGTestActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("TESTBUTTON", "Test button NOT FOUND"); // debug
        }

        // Link to Firebase Database Test Activity
        // Debugging Purposes
        View firebaseTestButton = findViewById(R.id.button_firebaseTest);
        if (firebaseTestButton != null) {
            firebaseTestButton.setOnClickListener(v -> {
                Log.d(TAG, "Firebase Test button CLICKED"); // debug
                Intent intent = new Intent(this, FirebaseTestActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "Firebase Test button NOT FOUND"); // debug
        }

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

    } // End onCreate

    private void logOut() {
        // Sign Out of Firebase
        FirebaseAuth.getInstance().signOut();

        // Return to sign-in screen
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    } // End logOut
}