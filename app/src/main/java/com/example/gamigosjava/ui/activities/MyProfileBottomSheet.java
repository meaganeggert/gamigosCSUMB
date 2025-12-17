package com.example.gamigosjava.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class MyProfileBottomSheet extends BottomSheetDialogFragment {

    private EditText etDisplayName;
    private TextView tvEmail;
    private RadioGroup rgPrivacy;
    private ImageView ivProfilePhoto;

    private FirebaseUser currentUser;
    private DocumentReference userDocRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && isAdded()) {
                    ivProfilePhoto.setImageURI(uri);
                    uploadPhotoToStorage(uri);
                }
            });

    public static MyProfileBottomSheet newInstance() {
        return new MyProfileBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottomsheet_my_profile, container, false);
    }

    @Override
    public int getTheme() {
        return R.style.ThemeOverlay_App_BottomSheet;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDisplayName = view.findViewById(R.id.etDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        rgPrivacy = view.findViewById(R.id.rgPrivacy);
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        View btnEditPhoto = view.findViewById(R.id.btnEditPhoto);

        Button btnSave = view.findViewById(R.id.btnSaveProfile);
        Button btnLogout = view.findViewById(R.id.button_logout);
        Button btnQuickGame = view.findViewById(R.id.buttonQuickGame);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
            return;
        }

        userDocRef = db.collection("users").document(currentUser.getUid());

        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());
        ivProfilePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnEditPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));


        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logOut());
        }

        if (btnQuickGame != null) {
            btnQuickGame.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), GetAllQuickPlayActivity.class));
                dismissAllowingStateLoss();
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog d = (BottomSheetDialog) getDialog();
            View sheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void uploadPhotoToStorage(Uri imageUri) {
        if (currentUser == null) return;

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
                                            Toast.makeText(requireContext(), "Profile photo uploaded", Toast.LENGTH_SHORT).show());
                        })
                )
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadUserProfile() {
        if (currentUser == null) return;

        tvEmail.setText(currentUser.getEmail());

        userDocRef.get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;

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

                if (privacyLevel != null) {
                    switch (privacyLevel) {
                        case "public":
                            RadioButton rbPublic = getView().findViewById(R.id.rbPublic);
                            if (rbPublic != null) rbPublic.setChecked(true);
                            break;
                        case "friends":
                            RadioButton rbFriends = getView().findViewById(R.id.rbFriends);
                            if (rbFriends != null) rbFriends.setChecked(true);
                            break;
                        case "private":
                            RadioButton rbPrivate = getView().findViewById(R.id.rbPrivate);
                            if (rbPrivate != null) rbPrivate.setChecked(true);
                            break;
                    }
                } else {
                    RadioButton rbFriends = getView().findViewById(R.id.rbFriends);
                    if (rbFriends != null) rbFriends.setChecked(true);
                }
            } else {
                RadioButton rbFriends = getView().findViewById(R.id.rbFriends);
                if (rbFriends != null) rbFriends.setChecked(true);
            }
        });
    }

    private void saveUserProfile() {
        if (currentUser == null) return;

        String displayName = etDisplayName.getText().toString().trim();
        if (displayName.isEmpty()) {
            etDisplayName.setError("Display name required");
            etDisplayName.requestFocus();
            return;
        }

        String privacyLevel = "friends";
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
                    Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss(); // close sheet, no navigation needed
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void logOut() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        dismissAllowingStateLoss();
        requireActivity().finish();
    }
}
