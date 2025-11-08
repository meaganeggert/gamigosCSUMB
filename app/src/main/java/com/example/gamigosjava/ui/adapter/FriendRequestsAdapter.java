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

public class FriendRequestsAdapter extends RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder> {

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
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new RequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
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

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName;
        Button btnAccept, btnDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
