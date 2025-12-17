package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.FriendViewHolder;

import java.util.List;
import java.util.Map;

public class FriendsListAdapter extends RecyclerView.Adapter<FriendViewHolder> {

    public interface FriendActionListener {
        void onProfileClick(Map<String, Object> friend);
        void onMessageClick(Map<String, Object> friend);

        // NEW: star toggle
        void onFavoriteToggle(Map<String, Object> friend, boolean newValue);
    }

    private final List<Map<String, Object>> items;
    private final FriendActionListener listener;

    public FriendsListAdapter(List<Map<String, Object>> items,
                              FriendActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Map<String, Object> friend = items.get(position);
        String name = (String) friend.get("displayName");
        String photoUrl = (String) friend.get("photoUrl");

        holder.tvName.setText(name != null ? name : "Unknown");

        Glide.with(holder.itemView.getContext())
                .load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);

        // Favorite icon state
        boolean isFavorite;
        Object favObj = friend.get("favorite");
        if (favObj instanceof Boolean) {
            isFavorite = (Boolean) favObj;
        } else {
            isFavorite = false;
        }

        holder.btnFavorite.setImageResource(
                isFavorite ? R.drawable.sharp_star_32 : R.drawable.sharp_star_border_32
        );

        holder.btnFavorite.setBackgroundResource(
                isFavorite ? R.drawable.bg_favorite_halo : android.R.color.transparent
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClick(friend);
        });

        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(friend);
        });

        holder.btnFavorite.setOnClickListener(v -> {
            boolean newValue = !isFavorite;

            // Optimistic UI: update local map so it feels instant
            friend.put("favorite", newValue);
            notifyItemChanged(holder.getBindingAdapterPosition());

            if (listener != null) listener.onFavoriteToggle(friend, newValue);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
