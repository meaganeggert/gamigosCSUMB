package com.example.gamigosjava;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import android.content.Intent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Firebase
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

// Credential Manager (AndroidX)
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

// Google Identity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

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