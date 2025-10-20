package com.example.gamigosjava;

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

import androidx.annotation.NonNull;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SignIn";

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private GetCredentialRequest credentialRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        View signInBtn = findViewById(R.id.sign_in_button);
        if (signInBtn != null) {
            signInBtn.setOnClickListener(v -> {
                Toast.makeText(this, "Sign-in button CLICKED", Toast.LENGTH_SHORT).show(); // debug
                Log.d(TAG, "Sign-in button CLICKED"); // debug
                startGoogleSignIn();
            });
        } else {
            Log.e(TAG, "Sign-in button NOT FOUND"); // debug
        }

        // (optional) Edge-to-edge / window insets
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
        AuthCredential firebaseCred = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(firebaseCred)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Firebase sign-in success: " + (user != null ? user.getUid() : "null"));
                        updateUI(user);
                    } else {
                        Log.e(TAG, "Firebase sign-in failed", task.getException());
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Log.d(TAG, "Signed in: " + user.getUid());
            // TODO navigate
        } else {
            Log.d(TAG, "Signed out / sign-in failed");
            // TODO show sign-in UI
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());
    }
}
