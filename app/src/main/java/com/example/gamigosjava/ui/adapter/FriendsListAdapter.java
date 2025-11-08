package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;

import java.util.List;
import java.util.Map;

public class FriendsListAdapter extends RecyclerView.Adapter<FriendsListAdapter.FriendViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(Map<String, Object> friend);
    }

    private final List<Map<String, Object>> friends;
    private final OnFriendClickListener listener;

    public FriendsListAdapter(List<Map<String, Object>> friends,
                              OnFriendClickListener listener) {
        this.friends = friends;
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
        Map<String, Object> friend = friends.get(position);
        String name = (String) friend.get("displayName");
        String photoUrl = (String) friend.get("photoUrl");

        holder.tvName.setText(name != null ? name : "Unknown");

        Glide.with(holder.itemView.getContext())
                .load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .circleCrop()
                .into(holder.ivPhoto);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFriendClick(friend);
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivPhoto;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
        }
    }
}
