package com.example.gamigosjava.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.AchievementSummary;

import com.squareup.picasso.Picasso;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    private static final String TAG = "AchievementAdapter";

    private final List<AchievementSummary> achievements = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, description, progressBarText;
        ProgressBar progressBar;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.achievementIcon);
            title = itemView.findViewById(R.id.achievementTitle);
            description = itemView.findViewById(R.id.achievementDescription);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressBarText = itemView.findViewById(R.id.progressBar_text);
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
        AchievementSummary achievement = achievements.get(position);
        Log.d(TAG,  "Binding: " + achievement.title + " CurrentProgress: " + achievement.current);

//        Set values for title and description
        holder.title.setText(achievement.title != null ? achievement.title : "Unknown");
        holder.description.setText(achievement.description != null ? achievement.description : "Do something.");

//        Set values for progress bar
        int current = Math.max(0, achievement.current);
        Log.d(TAG, achievement.title + ", Current: " + current);
        int goal = achievement.goal > 0 ? achievement.goal : 1;

        holder.progressBar.setMax(goal);
        holder.progressBar.setProgress(current);

        // Color the bar green if complete
        if (current >= goal) {
            holder.progressBar.setProgressTintList(
                    ColorStateList.valueOf(Color.parseColor("#566E3D"))
            );
        }

        // If you have already completed progress, don't keep counting
        // For example, for an achievement for logging in once, don't display 3/1
        int currentDisplay = Math.min(current, goal);
        holder.progressBarText.setText(currentDisplay + "/" + goal);

        if (achievement.achieved || current != 0) {
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.itemView.setAlpha(0.5f); // semi-transparent if achievement hasn't been started
        }

        if (achievement.iconUrl != null && !achievement.iconUrl.isEmpty()) {
            Picasso.get()
                    .load(achievement.iconUrl)
                    .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                    .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.ic_launcher_background);
        }
    }


    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public void setItems(List<AchievementSummary> newAchievements) {
        achievements.clear();
        if (newAchievements != null) {
            achievements.addAll(newAchievements);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

}
