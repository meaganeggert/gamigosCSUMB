package com.example.gamigosjava.ui.adapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.data.model.GameSummary;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final List<EventSummary> events = new ArrayList<>();
    private final boolean isActiveEvent; // boolean to display active/past events differently

    public EventAdapter(boolean isActiveEvent) {
        this.isActiveEvent = isActiveEvent;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, playersAttending, playtime;
        TextView gamesPlayed;

        RecyclerView attendeeAvatarRecycler;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageEvent);
            title = itemView.findViewById(R.id.eventTitle);
            playtime = itemView.findViewById(R.id.textPlaytime);
            playersAttending = itemView.findViewById(R.id.playersAttending);
            attendeeAvatarRecycler = itemView.findViewById(R.id.attendeeRecycler);
            if ( attendeeAvatarRecycler != null ) {
                attendeeAvatarRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isActiveEvent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_row, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.event_row_past, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventSummary event = events.get(position);
        android.util.Log.d("EventAdapter", "Binding: " + event.title);

        holder.title.setText(event.title != null ? event.title : "Unknown");

        // display players that attended dynamically
        if (event.playersAttending == null || event.playersAttending.isEmpty()) {
            holder.playersAttending.setText("No players");
            holder.attendeeAvatarRecycler.setAdapter(null);
            holder.attendeeAvatarRecycler.setVisibility(GONE);
        } else {
            // Uncomment if you want to display names instead of avatars
//            StringBuilder playerString = new StringBuilder();
//            for (int i = 0; i < event.playersAttending.size(); i++) {
//                if ( i > 0 ) playerString.append(", ");
//                playerString.append(event.playersAttending.get(i).getName());
//            }
//            holder.playersAttending.setText(playerString);

            // Display avatars
            holder.playersAttending.setText("");
            holder.playersAttending.setVisibility(GONE);
            holder.attendeeAvatarRecycler.setVisibility(VISIBLE);
            AttendeeAdapter attendeeAdapter = new AttendeeAdapter(event.playersAttending);
            holder.attendeeAvatarRecycler.setAdapter(attendeeAdapter);
        }

        if (isActiveEvent) {
            if (event.imageUrl != null && !event.imageUrl.isEmpty()) {
                Picasso.get()
                        .load(event.imageUrl)
                        .placeholder(R.drawable.ic_launcher_background) // optional placeholder
                        .error(R.drawable.ic_launcher_foreground)       // fallback if broken
                        .into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.ic_launcher_background);
            }
            holder.image.setVisibility(VISIBLE);

        } else {
            holder.image.setVisibility(GONE);
            holder.playtime.setText(event.timeElapsed);
            holder.playtime.setVisibility(VISIBLE);
        }
    }


    @Override
    public int getItemCount() {
        return events.size();
    }

    public void setItems(List<EventSummary> newEvents) {
        events.clear();
        if (newEvents != null) {
            events.addAll(newEvents);
        }
        notifyDataSetChanged(); // tells RecyclerView to refresh
    }

    public EventSummary getItemAt(int i) {
        return events.get(i);
    }

}
