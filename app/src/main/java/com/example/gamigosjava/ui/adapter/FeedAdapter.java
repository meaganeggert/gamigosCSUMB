package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.ActivityItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private final List<ActivityItem> feedItems = new ArrayList<>();

    public void setItems(List<ActivityItem> newItems) {
        feedItems.clear();
        feedItems.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_feed_achievement, parent, false);
        return new FeedViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        ActivityItem item = feedItems.get(position);

        // Construct message
        String firstName = item.getActorName().split(" ")[0];
        String achievementName = item.getTargetName();
        String message = firstName + " earned " + achievementName + "!";

        holder.textMessage.setText(message);

        // Temporary Timestamp
        // TODO: FIX THIS
        holder.textTimestamp.setText("Tea time.");

        // Trophy image
        holder.imageIcon.setImageResource(R.drawable.ic_trophy_24);
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        TextView textMessage;
        TextView textTimestamp;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.feed_AchievementIcon);
            textMessage = itemView.findViewById(R.id.feed_AchievementTitle);
            textTimestamp = itemView.findViewById(R.id.feed_AchievementTime);
        }
    }
}