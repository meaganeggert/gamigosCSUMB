package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private EditText etDisplayName;
    private TextView tvEmail;
    private RadioGroup rgPrivacy;
    private ImageView ivProfilePhoto;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private DocumentReference userDocRef;
    private StorageReference storageRef;


    // Launcher to pick image from gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivProfilePhoto.setImageURI(uri);
                    uploadPhotoToStorage(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_profile);

        setChildLayout(R.layout.activity_profile);

        // Set title for NavBar
        setTopTitle("Profile");

        etDisplayName = findViewById(R.id.etDisplayName);
        tvEmail = findViewById(R.id.tvEmail);
        rgPrivacy = findViewById(R.id.rgPrivacy);
        Button btnSave = findViewById(R.id.btnSaveProfile);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userDocRef = db.collection("users").document(currentUser.getUid());

        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());
        ivProfilePhoto.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        Button btnLogout = findViewById(R.id.button_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show();
                logOut();
            });
        }

        Button btnBGG = findViewById(R.id.button_test);
        if (btnBGG != null) {
            btnBGG.setOnClickListener(v -> {
                Intent intent = new Intent(this, BGGTestActivity.class);
                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "BGG test button not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadPhotoToStorage(Uri imageUri) {
        StorageReference photoRef = storageRef
                .child("user_uploads")
                .child(currentUser.getUid())
                .child("profile.jpg");

        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        photoRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            Map<String, Object> update = new HashMap<>();
                            update.put("photoUrl", downloadUri.toString());
                            userDocRef.set(update, SetOptions.merge())
                                    .addOnSuccessListener(unused ->
                                            Toast.makeText(this, "Profile photo uploaded", Toast.LENGTH_SHORT).show());
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Photo upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserProfile() {
        tvEmail.setText(currentUser.getEmail());

        userDocRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String displayName = doc.getString("displayName");
                String privacyLevel = doc.getString("privacyLevel");
                String photoUrl = doc.getString("photoUrl");

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(android.R.drawable.ic_menu_camera)
                            .into(ivProfilePhoto);
                }

                if (displayName != null) etDisplayName.setText(displayName);

                // set radio based on savedS value
                if (privacyLevel != null) {
                    switch (privacyLevel) {
                        case "public":
                            ((RadioButton)findViewById(R.id.rbPublic)).setChecked(true);
                            break;
                        case "friends":
                            ((RadioButton)findViewById(R.id.rbFriends)).setChecked(true);
                            break;
                        case "private":
                            ((RadioButton)findViewById(R.id.rbPrivate)).setChecked(true);
                            break;
                    }
                } else {
                    // default
                    ((RadioButton)findViewById(R.id.rbFriends)).setChecked(true);
                }
            } else {
                // default
                ((RadioButton)findViewById(R.id.rbFriends)).setChecked(true);
            }
        });
    }

    private void saveUserProfile() {
        String displayName = etDisplayName.getText().toString().trim();

        if (displayName.isEmpty()) {
            etDisplayName.setError("Display name required");
            etDisplayName.requestFocus();
            return;
        }

        // figure out selected privacy
        String privacyLevel = "friends"; // default
        int checkedId = rgPrivacy.getCheckedRadioButtonId();
        if (checkedId == R.id.rbPublic) privacyLevel = "public";
        else if (checkedId == R.id.rbFriends) privacyLevel = "friends";
        else if (checkedId == R.id.rbPrivate) privacyLevel = "private";

        Map<String, Object> data = new HashMap<>();
        data.put("uid", currentUser.getUid());
        data.put("displayName", displayName);
        data.put("email", currentUser.getEmail());
        data.put("privacyLevel", privacyLevel);

        userDocRef.set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();

                    // Navigate back to LandingActivity
                    Intent intent = new Intent(ProfileActivity.this, LandingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    // Close the current activity so user can't press Back
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(ProfileActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

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
