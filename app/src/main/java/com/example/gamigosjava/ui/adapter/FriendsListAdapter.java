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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProfileClick(friend);
        });
        holder.btnMessage.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(friend);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
