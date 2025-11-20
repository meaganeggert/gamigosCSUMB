package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.FriendRequestViewHolder;

import java.util.List;
import java.util.Map;

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Map<String, Object> request);
        void onDecline(Map<String, Object> request);
        void onViewProfile(Map<String, Object> request);
    }

    private final List<Map<String, Object>> requests;
    private final OnRequestActionListener listener;

    public FriendRequestsAdapter(List<Map<String, Object>> requests,
                                 OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        Map<String, Object> req = requests.get(position);
        String fromName = (String) req.get("fromDisplayName");
        String photoUrl = (String) req.get("fromPhotoUrl");
        holder.tvName.setText(fromName != null ? fromName : "Unknown user");

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(req);
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(req);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onViewProfile(req);
        });

        Glide.with(holder.itemView.getContext())
                .load(photoUrl != null && !photoUrl.isEmpty() ? photoUrl : R.drawable.ic_person_24)
                .placeholder(R.drawable.ic_person_24)
                .into(holder.ivAvatar);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }
}
