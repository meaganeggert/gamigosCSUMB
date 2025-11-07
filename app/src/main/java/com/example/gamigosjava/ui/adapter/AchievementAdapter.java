package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.AchievementSummary;
import com.example.gamigosjava.data.model.GameSummary;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<AchievementSummary> events = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, players, playtime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageAchieve);
            title = itemView.findViewById(R.id.achieveTitle);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.achieve_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementSummary event = events.get(position);
        android.util.Log.d("EventAdapter", "Binding: " + event.title);

        holder.title.setText(event.title != null ? event.title : "Unknown");

        if (event.imageUrl != null && !event.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(event.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background);
        }
    }


    @Override
    public int getItemCount() {
        return events.size();
    }

    public void setItems(List<AchievementSummary> newAchievements) {
        events.clear();
        if (newAchievements != null) {
            events.addAll(newAchievements);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

}