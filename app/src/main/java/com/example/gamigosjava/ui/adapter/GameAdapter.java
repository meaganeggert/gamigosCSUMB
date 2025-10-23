package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.GameSummary;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.ViewHolder> {

    private final List<GameSummary> games = new ArrayList<>();

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.game_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GameSummary game = games.get(position);
        android.util.Log.d("GameAdapter", "Binding: " + game.title);

        holder.title.setText(game.title != null ? game.title : "Unknown");
        holder.players.setText("Players: " + (game.minPlayers != null ? game.minPlayers : "?"));
        holder.playtime.setText("Playtime: " + (game.playingTime != null ? game.playingTime + " min" : "?"));
    }


    @Override
    public int getItemCount() {
        return games.size();
    }

    public void setItems(List<GameSummary> newGames) {
        games.clear();
        if (newGames != null) {
            games.addAll(newGames);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

}
