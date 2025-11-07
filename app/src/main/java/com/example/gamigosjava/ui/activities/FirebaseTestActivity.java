package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseTestActivity extends BaseActivity {

    private EditText etTitle, etPlayers, etNotes;
    private Button btnSave;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_firebase_test);

        setChildLayout(R.layout.activity_firebase_test);

        // Set title for NavBar
        setTopTitle("Firebase Test");

        // UI
        etTitle = findViewById(R.id.etTitle);
        etPlayers = findViewById(R.id.etPlayers);
        etNotes = findViewById(R.id.etNotes);
        btnSave = findViewById(R.id.btnSave);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnSave.setOnClickListener(v -> saveToFirestore());
    }

    private void saveToFirestore() {
        String title = etTitle.getText().toString().trim();
        String playersStr = etPlayers.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Required");
            return;
        }

        int players = 0;
        if (!playersStr.isEmpty()) {
            try {
                players = Integer.parseInt(playersStr);
            } catch (NumberFormatException e) {
                etPlayers.setError("Number");
                return;
            }
        }

        // build data
        Map<String, Object> game = new HashMap<>();
        game.put("title", title);
        game.put("players", players);
        game.put("notes", notes);
        game.put("createdAt", System.currentTimeMillis());

        // if logged in, store who created it
        if (auth.getCurrentUser() != null) {
            game.put("userId", auth.getCurrentUser().getUid());
            game.put("userEmail", auth.getCurrentUser().getEmail());
        }

        // write to collection "testGames"
        db.collection("testGames")
                .add(game)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Saved: " + docRef.getId(), Toast.LENGTH_SHORT).show();
                    etTitle.setText("");
                    etPlayers.setText("");
                    etNotes.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
