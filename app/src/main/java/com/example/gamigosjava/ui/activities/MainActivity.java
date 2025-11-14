package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Firebase
import com.example.gamigosjava.R;
import com.example.gamigosjava.data.repository.AchievementAwarder;
import com.example.gamigosjava.data.repository.AchievementsRepo;
import com.example.gamigosjava.ui.AchievementNotifier;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SignIn";

    private static final boolean debugging = true;

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private GetCredentialRequest credentialRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Force sign-out so user must log in every time while debugging
//        if (debugging) {
//            FirebaseAuth.getInstance().signOut();
//        }

        // Initiate Firebase
        mAuth = FirebaseAuth.getInstance();

        // Build Google Sign-In Request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        credentialRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager = CredentialManager.create(this);

        // Link to sign-in button
        View signInButton = findViewById(R.id.button_signIn);
        if (signInButton != null) {
            signInButton.setOnClickListener(v -> {
                Toast.makeText(this, "Sign-in button CLICKED", Toast.LENGTH_SHORT).show(); // debug
                Log.d(TAG, "Sign-in button CLICKED"); // debug
                startGoogleSignIn();
            });
        } else {
            Log.e(TAG, "Sign-in button NOT FOUND"); // debug
        }

        // Default View
        View root = findViewById(R.id.main);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }
    }

    private void startGoogleSignIn() {
        credentialManager.getCredentialAsync(
                /* context */ this,
                /* request */ credentialRequest,
                /* cancellationSignal */ new CancellationSignal(),
                /* executor */ ContextCompat.getMainExecutor(this),
                /* callback */ new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "getCredentialAsync failed", e);
                        updateUI(null);
                    }
                }
        );
    }


    private void handleSignIn(Credential credential) {
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                GoogleIdTokenCredential idTokenCred =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());
                firebaseAuthWithGoogle(idTokenCred.getIdToken());
                return;
            }
        }
        Log.w(TAG, "Credential is not a Google ID token");
        updateUI(null);
    }


    private void firebaseAuthWithGoogle(String idToken) {
        AchievementsRepo repo = new AchievementsRepo(
                FirebaseFirestore.getInstance()
        );

        AuthCredential firebaseCred = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(firebaseCred)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Firebase sign-in success: " + (user != null ? user.getUid() : "null"));
                        if (user != null) {
                            Log.d(TAG, "User != null");
                            ensureUserDocExists(user);
                            repo.loginTracker(user.getUid())
                                    .continueWithTask(t->
                                            new AchievementAwarder(FirebaseFirestore.getInstance())
                                    .awardLoginAchievements(user.getUid()) // check for any earned achievements
                                    )
                                    .addOnSuccessListener( earned -> {
                                        if (earned != null && !earned.isEmpty()) {
                                            AchievementNotifier notifier = new AchievementNotifier(this, findViewById(R.id.main));
                                            for (String title : earned) {
                                                notifier.pickAchievementBanner(title, null);
                                            }
                                        }
                                        updateUI(user);
                                    })
                                    .addOnFailureListener(e-> {
                                        Log.e(TAG, "Metrics & Achievement flow FAILED", e);
                                        updateUI(user);
                                    }
                            );
                        }

                    } else {
                        Log.e(TAG, "Firebase sign-in failed", task.getException());
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Log.d(TAG, "Signed in: " + user.getUid());
            // Navigate to the landing screen on successful sign-in
            Intent intent = new Intent(this, LandingActivity.class);
            startActivity(intent);

            // Don't allow users to go back to the sign-in screen after a successful sign-in
            finish();
        } else {
            Log.d(TAG, "Signed out / sign-in failed");
            // Show error pop-up
            // TODO: Modify this in the future to allow for an error-text, instead of a Toast message
            Toast.makeText(this, "Sign-in failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void ensureUserDocExists(FirebaseUser firebaseUser) {
        Log.i(TAG, "ensureUserDocExists function called");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection("users").document(firebaseUser.getUid());

        userDoc.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Log.d(TAG, "user doc does not exist"); // debug
                // first time: create basic doc
                Map<String, Object> data = new HashMap<>();
                data.put("uid", firebaseUser.getUid());
                data.put("email", firebaseUser.getEmail());
                data.put("displayName", firebaseUser.getDisplayName());
                data.put("privacyLevel", "friends"); // default
                if (firebaseUser.getPhotoUrl() != null) {
                    data.put("photoUrl", firebaseUser.getPhotoUrl().toString());
                }
                userDoc.set(data)
                        .addOnSuccessListener(v-> {
                            Log.d(TAG, "User doc created successfully");
                        })
                        .addOnFailureListener(e->{
                            Log.e(TAG, "Failed to create User doc");
                            Toast.makeText(this, "FAILED" + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Log.d(TAG, "user doc does exist"); // debug
                // Fetch existing fields first
                String currentName = doc.getString("displayName");
                String currentPhoto = doc.getString("photoUrl");

                Map<String, Object> updates = new HashMap<>();

                // Only set displayName if it's missing or empty
                if ((currentName == null || currentName.isEmpty()) && firebaseUser.getDisplayName() != null) {
                    updates.put("displayName", firebaseUser.getDisplayName());
                }

                // Only set photoUrl if it's missing or empty
                if ((currentPhoto == null || currentPhoto.isEmpty()) && firebaseUser.getPhotoUrl() != null) {
                    updates.put("photoUrl", firebaseUser.getPhotoUrl().toString());
                }

                if (!updates.isEmpty()) {
                    Log.d(TAG, "updates is not empty"); // debug
                    userDoc.set(updates, SetOptions.merge())
                            .addOnSuccessListener(v-> {
                                Log.d(TAG, "User doc updated successfully");
                            })
                            .addOnFailureListener(e->{
                                Log.e(TAG, "Failed to update User doc");
                                Toast.makeText(this, "FAILED" + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            }

        });
    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            ensureUserDocExists(user);
            updateUI(user);
        }
    }
}
