package com.example.gamigosjava.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.data.repository.FirestoreUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ScoresAdapter extends RecyclerView.Adapter<ScoresAdapter.ViewHolder>{
    private final static String TAG = "ScoresAdapter";
    public List<Player> playerList = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView playerName;
        EditText playerScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.textView_playerName);
            playerScore = itemView.findViewById(R.id.editTextNumber_playerScore);
        }


    }

    @NonNull
    @Override
    public ScoresAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.player_row, parent, false);
        return new ScoresAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScoresAdapter.ViewHolder holder, int position) {
        Player player = playerList.get(position);
        android.util.Log.d(TAG, "Binding: " + player.friend.displayName);

        holder.playerName.setText(player.friend.displayName != null ? player.friend.displayName : "Unknown");

        holder.playerScore.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String scoreText = holder.playerScore.getText().toString();
                if (scoreText.isEmpty()) {
                    player.score = 0;
                } else {
                    player.setScore(scoreText);
                }
            }
        });
        holder.playerScore.setText(player.score.toString());
    }

    @Override
    public int getItemCount() {
        return playerList.size();
    }


    public void setItems(List<Player> newPlayers) {
        playerList.clear();
        if (newPlayers != null) {
            playerList.addAll(newPlayers);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

    public Player getItemAt(int i) {
        if (i > playerList.size()-1) return null;
        return playerList.get(i);
    }

    public void uploadPlayerScores(FirebaseFirestore db, FirebaseUser currentUser, String matchId) {
        if (currentUser == null) {
            Log.d(TAG, "User Not Signed in. Cannot upload player scores.");
            return;
        }

        CollectionReference playersCol = db.collection("matches").document(matchId).collection("players");
        FirestoreUtils.deleteCollection(db, playersCol, 10).onSuccessTask(v -> {
            playerList.sort((p1,p2) -> Integer.compare(p2.score, p1.score));

            for (int i = 0; i < playerList.size(); i++) {
                Player p = playerList.get(i);
                HashMap<String, Object> playerHash = new HashMap<>();
                playerHash.put("userId", p.friend.id);
                playerHash.put("score", p.score);
                playerHash.put("placement", i + 1);

                playersCol.add(playerHash).addOnSuccessListener(doc -> {
                    Log.d(TAG, "Successfully added player details to database: " + p.friend.displayName);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload player details: " + e.getMessage());
                });
            }

            return null;
        });

    }
}
