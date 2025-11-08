package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewUserProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView ivAvatar;
    private TextView tvName, tvEmail; // whatever you want to show

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_user_profile);

        db = FirebaseFirestore.getInstance();

        ivAvatar = findViewById(R.id.ivProfilePhoto);
        tvName = findViewById(R.id.tvDisplayName);
        tvEmail = findViewById(R.id.tvEmail);

        String userId = getIntent().getStringExtra("USER_ID");
        if (userId == null) {
            finish();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String email = doc.getString("email");
                        String photo = doc.getString("photoUrl");

                        tvName.setText(name);
                        tvEmail.setText(email);

                        // if you're using Glide/Picasso:
                         Glide.with(this).load(photo).into(ivAvatar);
                    }
                });
    }
}
