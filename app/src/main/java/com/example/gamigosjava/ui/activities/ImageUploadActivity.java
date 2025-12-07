package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;

import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;

import com.example.gamigosjava.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.UUID;

public class ImageUploadActivity extends BaseActivity {
    private static final String TAG = "Image Upload";

    private ImageView imageView;
    private Button selectButton, uploadButton;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private Uri pendingImageUri = null;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private String eventId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_image_upload);

        setTopTitle("Upload Photo");

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("selectedEventId");

        imageView = findViewById(R.id.imageView_image);
        selectButton = findViewById(R.id.button_selectImage);
        uploadButton = findViewById(R.id.button_uploadImage);

        // Initializes the pick image activity and sets what the
        // activity will do with selected image
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        Log.d(TAG, "Selected image: " + uri);
                        pendingImageUri = uri;
                        imageView.setImageURI(uri);
                        uploadButton.setEnabled(true);
//                        uploadImageToFirebaseStorage(uri);
                    } else {
                        Log.e(TAG, "No image selected");
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                });

        // Starts the pick image activity
        selectButton.setOnClickListener(v -> {
            pickImageLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        });

        // Uploads the selected image
        uploadButton.setOnClickListener(v -> {
            if (eventId.isEmpty()) return;

            if (pendingImageUri != null) {
                uploadImageToFirebaseStorage(pendingImageUri);
            } else {
                Toast.makeText(this, "Select Image First", Toast.LENGTH_SHORT).show();
            }
        });


        Button back = findViewById(R.id.button_imageBackBtn);
        if (back != null) {
            back.setOnClickListener(v -> {
                finish();
            });
        }
    }

    // This is called after selecting an image from the pick image activity.
    // It sends the image to the firebase storage.
    // NOTICE: THIS USES THE USERS AUTHENTICATION TO UPLOAD THE IMAGE. MAY OR MAY NOT INTERFERE
    //         WITH SHARING AN IMAGE WITH OTHER USERS. IF SO, CHECK THE FIREBASE STORAGE RULES.
    private void uploadImageToFirebaseStorage(Uri uri) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "User not signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        uploadButton.setEnabled(false);

        // Associate the image with the user
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get reference to firebase storage in users upload directory.
        // Should generate a random id as the file name, can be used for database integration
        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference()
                .child("user_uploads/" + uid + "/" + UUID.randomUUID() + ".jpg");

        // Uploading object
        UploadTask uploadTask = storageRef.putFile(uri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String downloadUrl = downloadUri.toString();
                        Log.d(TAG, "Uploaded Image: " + downloadUrl);
                        Toast.makeText(this, "Upload successful!", Toast.LENGTH_SHORT).show();

                        uploadImageToEvent(downloadUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload failed", e);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadImageToEvent(String imageUrl) {
        if (currentUser == null) return;

        CollectionReference eventImgRef = db.collection("events")
                .document(eventId)
                .collection("images");

        HashMap<String, Object> imageHash = new HashMap<>();
        imageHash.put("authorId", currentUser.getUid());
        imageHash.put("photoUrl", imageUrl);
        imageHash.put("eventId", eventId);
        imageHash.put("uploadedAt", Timestamp.now());

        eventImgRef.add(imageHash).addOnSuccessListener(docRef -> {
            Toast.makeText(this, "Image Uploaded Successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Uploaded image " + docRef.getId() + " to event " + eventId);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Failed to upload image to event " + eventId + ": " + e.getMessage());
        });
    }

}