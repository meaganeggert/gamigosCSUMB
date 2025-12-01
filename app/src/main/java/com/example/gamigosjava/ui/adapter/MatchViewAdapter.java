package com.example.gamigosjava.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Attendee;
import com.example.gamigosjava.data.model.Match;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MatchViewAdapter extends RecyclerView.Adapter<MatchViewAdapter.ViewHolder> {

        private final List<Match> matches;

        public MatchViewAdapter(List<Match> matches) {
            this.matches = matches;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView gameImage;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                gameImage = itemView.findViewById(R.id.avatarImage);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.small_avatar_image, parent, false);
            return new ViewHolder(v);
        }


    @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Match match = matches.get(position);

            Picasso.get()
                    .load(match.imageUrl)
                    .placeholder(R.drawable.ic_videogame_24)
                    .error(R.drawable.ic_videogame_24)
                    .into(holder.gameImage);
        }

        @Override
        public int getItemCount() {
            return matches.size();
        }
}
