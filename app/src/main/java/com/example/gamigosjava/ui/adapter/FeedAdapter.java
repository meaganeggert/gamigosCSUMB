package com.example.gamigosjava.ui.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.ActivityItem;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {
    private static final String TAG = "FeedAdapter";

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

        String avatarUrl = item.getActorImage();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Picasso.get()
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(holder.avatar);
        } else {
            Log.d(TAG, "Actor Avatar not found");
            holder.avatar.setImageResource(R.drawable.ic_person_24);
        }

        if (item.getType().equals("ACHIEVEMENT_EARNED")) {
            // Construct message
            String firstName = item.getActorName().split(" ")[0];
            String achievementName = item.getTargetName();
            String message = firstName + " earned " + achievementName + "!";

            holder.textTitle.setText(message);
            holder.textDescript.setVisibility(GONE);

            // Temporary Timestamp
            Timestamp whenAchieved = item.getCreatedAt();
            LocalDateTime achievementTimeDate = whenAchieved.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));

            // Trophy image
            holder.imageIcon.setImageResource(R.drawable.ic_trophy_24);
        } else if (item.getType().equals("EVENT_CREATED")) {
            // Construct message
            String firstName = item.getActorName().split(" ")[0];
            String eventName = item.getTargetName();
            Log.d(TAG, "eventName: " + eventName);
            String message = firstName + " created an event!";

            holder.textTitle.setText(message);
            holder.textDescript.setVisibility(VISIBLE);
            holder.textDescript.setText(eventName);

            // Temporary Timestamp
            Timestamp whenCreated = item.getCreatedAt();
            LocalDateTime achievementTimeDate = whenCreated.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));

            // Trophy image
            holder.imageIcon.setImageResource(R.drawable.ic_event_24);
        } else if (item.getType().equals("FRIEND_ADDED")) {
            // Construct message
            String friendOneName = item.getActorName().split(" ")[0];
            String friendTwoName = item.getTargetName().split(" ")[0];
            String message = friendOneName + " and " + friendTwoName + " are now friends.";

            holder.textTitle.setText(message);
            holder.textDescript.setVisibility(GONE);
            holder.avatar.setVisibility(GONE);

            // Temporary Timestamp
            Timestamp whenCreated = item.getCreatedAt();
            LocalDateTime achievementTimeDate = whenCreated.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));

            // Trophy image
            holder.imageIcon.setImageResource(R.drawable.ic_friends_24);
        } else if (item.getType().equals("EVENT_ATTENDED")) {
            // TODO: Fill this in
        } else if (item.getType().equals("GAME_WON")) {
            // TODO: Fill this in
        } else {
            holder.textTitle.setText("Error retrieving content.");
            Log.d(TAG, "ActivityItem Type: " + item.getType());

            // Temporary Timestamp
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            if (item != null) {
                Timestamp whenAchieved = item.getCreatedAt();
                LocalDateTime achievementTimeDate = whenAchieved.toDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));
            }
            holder.textTimestamp.setText(LocalDateTime.now().format(dateFormatter));

            // Question image
            holder.imageIcon.setImageResource(R.drawable.ic_question_24);
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon;
        ShapeableImageView avatar;
        TextView textTitle;
        TextView textDescript;
        TextView textTimestamp;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.feed_icon);
            textTitle = itemView.findViewById(R.id.feed_title);
            textDescript = itemView.findViewById(R.id.feed_description);
            textTimestamp = itemView.findViewById(R.id.feed_time);
            avatar = itemView.findViewById(R.id.feed_avatar);
        }
    }
}