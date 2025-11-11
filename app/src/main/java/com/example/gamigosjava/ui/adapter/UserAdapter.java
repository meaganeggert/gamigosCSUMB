package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.UserViewHolder;

import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {

    public interface FriendActionClickListener {
        void onAddClick(Map<String, Object> user);
        void onDenyClick(Map<String, Object> user);
    }

    public interface OnUserClickListener {
        void onUserClicked(Map<String, Object> user);
    }

    private final List<Map<String, Object>> userList;
    private final FriendActionClickListener listener;
    private final OnUserClickListener userClickListener;

    // same constants as activity
    private static final int STATUS_NONE = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_FRIEND = 2;
    private static final int STATUS_INCOMING = 3;

    public UserAdapter(List<Map<String, Object>> userList, FriendActionClickListener listener, OnUserClickListener userClickListener) {
        this.userList = userList;
        this.listener = listener;
        this.userClickListener = userClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        Map<String, Object> user = userList.get(position);
        String name = (String) user.get("displayName");
        String photoUrl = (String) user.get("photoUrl");

        holder.tvName.setText(name);

        holder.itemView.setOnClickListener(v -> {
            if (userClickListener != null) {
                userClickListener.onUserClicked(user);
            }
        });

        Object s = user.get("status");
        int status = (s instanceof Integer) ? (int) s: STATUS_NONE;

        switch (status) {
            case STATUS_NONE:
                holder.btnAddFriend.setText("Add");
                holder.btnAddFriend.setEnabled(true);
                holder.btnDenyFriend.setVisibility(View.GONE);
                break;
            case STATUS_PENDING:
                holder.btnAddFriend.setText("Pending");
                holder.btnAddFriend.setEnabled(false);
                holder.btnDenyFriend.setVisibility(View.GONE);
                break;
            case STATUS_FRIEND:
                holder.btnAddFriend.setText("Friend");
                holder.btnAddFriend.setEnabled(false);
                holder.btnDenyFriend.setVisibility(View.GONE);
                break;
            case STATUS_INCOMING:
                holder.btnAddFriend.setText("Accept");
                holder.btnAddFriend.setEnabled(true);
                holder.btnDenyFriend.setVisibility(View.VISIBLE);
                break;
        }

        Glide.with(holder.itemView.getContext())
                .load(photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto);

        holder.btnAddFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddClick(user);
            }
        });

        holder.btnDenyFriend.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDenyClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


}
