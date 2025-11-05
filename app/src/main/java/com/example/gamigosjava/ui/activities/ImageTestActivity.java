package com.example.gamigosjava.ui.activities;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;

import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class ImageTestActivity extends AppCompatActivity {

    private static final String TAG = "Image Upload";

    private ImageView imageView;
    private Button selectButton, uploadButton;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private Uri pendingImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_test);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
//                        uploadImageToFirebase(uri);
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
            if (pendingImageUri != null) {
                uploadImageToFirebase(pendingImageUri);
            } else {
                Toast.makeText(this, "Select Image First", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This is called after selecting an image from the pick image activity.
    // It sends the image to the firebase storage.
    // NOTICE: THIS USES THE USERS AUTHENTICATION TO UPLOAD THE IMAGE. MAY OR MAY NOT INTERFERE
    //         WITH SHARING AN IMAGE WITH OTHER USERS. IF SO, CHECK THE FIREBASE STORAGE RULES.
    private void uploadImageToFirebase(Uri uri) {
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

                // TODO: Implement a way to save the url to the database.
            });
        })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Upload failed", e);
                Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

}