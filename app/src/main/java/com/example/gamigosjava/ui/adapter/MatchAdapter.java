package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.MatchSummary;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder>{

    private final List<MatchSummary> matches = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, players, playtime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageGame);
            title = itemView.findViewById(R.id.textTitle);
            players = itemView.findViewById(R.id.textPlayers);
            playtime = itemView.findViewById(R.id.textPlaytime);
        }
    }

    // TODO: use something besides game row.
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_row, parent, false);
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
}
