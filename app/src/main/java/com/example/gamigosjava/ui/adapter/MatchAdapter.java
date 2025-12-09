package com.example.gamigosjava.ui.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.MatchSummary;
import com.example.gamigosjava.data.repository.FirestoreUtils;
import com.example.gamigosjava.ui.activities.ViewEventActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder>{

    private final List<MatchSummary> matches = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public boolean isHost = true;


    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, players, playtime;
        Button deleteMatchButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageGame);
            title = itemView.findViewById(R.id.matchTitle);
            players = itemView.findViewById(R.id.textAttendees);
            playtime = itemView.findViewById(R.id.textPlaytime);
            deleteMatchButton = itemView.findViewById(R.id.button_deleteMatch);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_match, parent, false);
        return new MatchAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchAdapter.ViewHolder holder, int position) {
        MatchSummary match = matches.get(position);
        android.util.Log.d("MatchAdapter", "Binding: " + match.title);

        holder.title.setText(match.title != null ? match.title : "Unknown");
        holder.players.setText("Players: " + (match.minPlayers != null ? match.minPlayers : "?"));
        holder.playtime.setText("Playtime: " + (match.playingTime != null ? match.playingTime + " min" : "?"));

        if (match.imageUrl != null && !match.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(match.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background);
        }

        if (isHost) holder.deleteMatchButton.setVisibility(Button.VISIBLE);
        else holder.deleteMatchButton.setVisibility(Button.GONE);

        holder.deleteMatchButton.setOnClickListener(v -> {
            Context context = v.getContext();
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete this game (" + match.title + ")? This action cannot be undone.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!match.eventId.isEmpty()) {
                                DocumentReference matchRefDoc = db.collection("events")
                                        .document(match.eventId)
                                        .collection("matches")
                                        .document(match.id);

                                matchRefDoc.delete().onSuccessTask(refVoid -> {
                                    Log.d("Match Adapter", "Deleted match from event " + match.eventId);
                                    return null;
                                });
                            }

                            DocumentReference matchDoc = db.collection("matches")
                                    .document(match.id);

                            FirestoreUtils.deleteCollection(db, matchDoc.collection("players"), 10);
                            matchDoc.delete().onSuccessTask(matchVoid -> {
                                Toast.makeText(context, "Deleted game " + match.title, Toast.LENGTH_SHORT).show();
                                    matches.remove(match);
                                    notifyDataSetChanged();
                                return null;
                            });

                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }


    public void setItems(List<MatchSummary> newGames) {
        matches.clear();
        if (newGames != null) {
            matches.addAll(newGames);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

    public MatchSummary getItemAt(int i) {
        if (i > matches.size()-1) return null;
        return matches.get(i);
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
        notifyDataSetChanged();
    }
}
