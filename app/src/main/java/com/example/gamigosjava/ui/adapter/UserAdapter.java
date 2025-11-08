package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;

import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnAddClickListener {
        void onAddClick(Map<String, Object> user);
    }

    public interface OnUserClickListener {
        void onUserClicked(Map<String, Object> user);
    }

    private final List<Map<String, Object>> userList;
    private final OnAddClickListener listener;
    private final OnUserClickListener userClickListener;

    // same constants as activity
    private static final int STATUS_NONE = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_FRIEND = 2;

    public UserAdapter(List<Map<String, Object>> userList, OnAddClickListener listener, OnUserClickListener userClickListener) {
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
                break;
            case STATUS_PENDING:
                holder.btnAddFriend.setText("Pending");
                holder.btnAddFriend.setEnabled(false);
                break;
            case STATUS_FRIEND:
                holder.btnAddFriend.setText("Friend");
                holder.btnAddFriend.setEnabled(false);
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
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivPhoto;
        Button btnAddFriend;

        UserViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            ivPhoto = itemView.findViewById(R.id.ivPhoto);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }
    }
}
