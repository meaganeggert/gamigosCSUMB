package com.example.gamigosjava.ui.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    private static final int VIEW_TYPE_GLOBAL_ACHIEVEMENT = 1;
    private static final int VIEW_TYPE_GAME_ACHIEVEMENT = 2;
    private static final int VIEW_TYPE_EVENT = 3;
    private static final int VIEW_TYPE_FRIEND = 4;
    private static final int VIEW_TYPE_MATCH = 5;
    private static final int VIEW_TYPE_UNKNOWN = 99;

    public FeedAdapter() {}

    public void setItems(List<ActivityItem> newItems) {
        feedItems.clear();
        feedItems.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        Log.i(TAG, "ViewType: " + viewType);
        switch (viewType) {
            case VIEW_TYPE_GAME_ACHIEVEMENT:
                // Game Specific Row Layout
                view = inflater.inflate(R.layout.row_feed_game_achievement, parent, false);
                break;

            case VIEW_TYPE_GLOBAL_ACHIEVEMENT:
                view = inflater.inflate(R.layout.row_feed_achievement, parent, false);
                break;

            case VIEW_TYPE_EVENT:
                view = inflater.inflate(R.layout.row_feed_achievement, parent, false);
                break;

            case VIEW_TYPE_FRIEND:
                // same note as above
                view = inflater.inflate(R.layout.row_feed_achievement, parent, false);
                break;

            case VIEW_TYPE_MATCH:
                // Game Specific Row Layout
                view = inflater.inflate(R.layout.row_feed_game_won, parent, false);
                break;

            case VIEW_TYPE_UNKNOWN:
            default:
                view = inflater.inflate(R.layout.row_feed_achievement, parent, false);
                break;
        }

        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        ActivityItem item = feedItems.get(position);

        // Default visibility
        holder.avatar.setVisibility(VISIBLE);
        holder.textDescript.setVisibility(GONE);
        holder.gameImage.setVisibility(GONE);
        holder.gameImage.setImageResource(R.drawable.die_solid);

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

            holder.titleBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.rose));
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
            holder.imageIcon.setImageResource(R.drawable.achievement_24);
            holder.gameImage.setVisibility(GONE);
        } else if (item.getType().equals("EVENT_CREATED")) {
            // Construct message
            String firstName = item.getActorName().split(" ")[0];
            String eventName = item.getTargetName();
            Log.d(TAG, "eventName: " + eventName);
            String message = firstName + " created an event!";

            holder.titleBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.greyblue));
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
            holder.gameImage.setVisibility(GONE);
        } else if (item.getType().equals("FRIEND_ADDED")) {
            // Construct message
            String friendOneName = item.getActorName().split(" ")[0];
            String friendTwoName = item.getTargetName().split(" ")[0];
            String message = friendOneName + " and " + friendTwoName + " are now friends.";

            holder.titleBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.darkblue));
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
            holder.gameImage.setVisibility(GONE);
        } else if (item.getType().equals("EVENT_ATTENDED")) {
            // TODO: Fill this in
        } else if (item.getType().equals("MATCH_WON")) {
            // TODO: Fill this in
            // Construct message
            String firstName = item.getActorName().split(" ")[0];
            String message = firstName + item.getMessage();
            String gameUrl = item.getTargetImage();

            String statMessage = "Win Streak: " + item.getWinStreak();

            holder.titleBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blackberry));
            holder.textAchieveMessage.setText(message);
            holder.textDescript.setText(statMessage);
            holder.textDescript.setVisibility(VISIBLE);
            holder.gameImage.setVisibility(VISIBLE);
            holder.gameImage.setColorFilter(null);

            // Temporary Timestamp
            Timestamp whenCreated = item.getCreatedAt();
            LocalDateTime achievementTimeDate = whenCreated.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));

            // Trophy image
            holder.imageIcon.setImageResource(R.drawable.ic_trophy_24);
            // Board Game Image
            if (gameUrl != null && !gameUrl.isEmpty()) {
                Picasso.get()
                        .load(gameUrl)
                        .placeholder(R.drawable.die_solid)
                        .error(R.drawable.die_solid)
                        .into(holder.gameImage);
            } else {
                Log.d(TAG, "Board game image not found");
                holder.gameImage.setImageResource(R.drawable.die_solid);

                holder.gameImage.setColorFilter(
                        holder.itemView.getContext()
                                .getColor(R.color.orange)
                );
            }
            holder.gameImage.setVisibility(VISIBLE);

            holder.imageIcon.setImageResource(R.drawable.ic_trophy_24);
        } else if (item.getType().equals("GAME_ACHIEVEMENT_EARNED")) {
            // Construct message
            String firstName = item.getActorName().split(" ")[0];
            String message = item.getMessage();
            String gameUrl = item.getTargetImage();

            holder.titleBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.rose));
            holder.textAchieveMessage.setText(message);
            holder.textDescript.setVisibility(GONE);
            holder.gameImage.setVisibility(VISIBLE);
            holder.gameImage.setColorFilter(null);

            // Temporary Timestamp
            Timestamp whenCreated = item.getCreatedAt();
            LocalDateTime achievementTimeDate = whenCreated.toDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • hh:mm a");
            holder.textTimestamp.setText(achievementTimeDate.format(dateFormatter));

            // Trophy image
            holder.imageIcon.setImageResource(R.drawable.achievement_24);
            // Board Game Image
            if (gameUrl != null && !gameUrl.isEmpty()) {
                Picasso.get()
                        .load(gameUrl)
                        .placeholder(R.drawable.die_solid)
                        .error(R.drawable.die_solid)
                        .into(holder.gameImage);
            } else {
                Log.d(TAG, "Board game image not found");
                holder.gameImage.setImageResource(R.drawable.die_solid);

                holder.gameImage.setColorFilter(
                        holder.itemView.getContext()
                                .getColor(R.color.orange)
                );
            }
            holder.gameImage.setVisibility(VISIBLE);
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

    @Override
    public int getItemViewType(int position) {
        ActivityItem item = feedItems.get(position);
        String type = item.getType();

        if ("GAME_ACHIEVEMENT_EARNED".equals(type)) {
            return VIEW_TYPE_GAME_ACHIEVEMENT;
        } else if ("ACHIEVEMENT_EARNED".equals(type)) {
            return VIEW_TYPE_GLOBAL_ACHIEVEMENT;
        } else if ("EVENT_CREATED".equals(type)) {
            return VIEW_TYPE_EVENT;
        } else if ("FRIEND_ADDED".equals(type)) {
            return VIEW_TYPE_FRIEND;
        } else if ("MATCH_WON".equals(type)) {
            return VIEW_TYPE_MATCH;
        } else {
            return VIEW_TYPE_UNKNOWN;
        }
    }


    static class FeedViewHolder extends RecyclerView.ViewHolder {
        ImageView imageIcon, gameImage;
        ShapeableImageView avatar;
        TextView textTitle;
        TextView textDescript, textAchieveMessage;
        TextView textTimestamp;
        View titleBackground;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.feed_icon);
            textTitle = itemView.findViewById(R.id.feed_title);
            textAchieveMessage = itemView.findViewById(R.id.feed_message);
            textDescript = itemView.findViewById(R.id.feed_description);
            textTimestamp = itemView.findViewById(R.id.feed_time);
            avatar = itemView.findViewById(R.id.feed_avatar);
            gameImage= itemView.findViewById(R.id.feed_bg_image);
            titleBackground = itemView.findViewById(R.id.backgroundBar);
        }
    }
}