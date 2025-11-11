package com.example.gamigosjava.ui.adapter;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.FriendRequestViewHolder;

import java.util.List;

public class FriendRequestRowAdapter extends RecyclerView.Adapter<FriendRequestViewHolder> {

    public interface RequestActionListener {
        void onAccept(String uid);
        void onDecline(String uid);
        void onRowClick(String uid);
    }

    private final List<RequestRow> items;
    private final RequestActionListener listener;

    public FriendRequestRowAdapter(List<RequestRow> items, RequestActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request_row, parent, false);
        return new FriendRequestViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder h, int pos) {
        RequestRow row = items.get(pos);
        h.tvName.setText(row.displayName != null ? row.displayName : "(unknown)");

        //  Row click -> open profile
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRowClick(row.uid);
            }
        });

        Glide.with(h.itemView.getContext())
                .load(row.photoUrl)
                .placeholder(R.drawable.ic_person_24)
                .into(h.ivAvatar);

        if (row.type == RequestRow.TYPE_INCOMING) {
            h.btnAccept.setVisibility(View.VISIBLE);
            h.btnDecline.setVisibility(View.VISIBLE);
            h.btnPending.setVisibility(View.GONE);

            h.btnAccept.setOnClickListener(v -> listener.onAccept(row.uid));
            h.btnDecline.setOnClickListener(v -> listener.onDecline(row.uid));
        } else { // outgoing
            h.btnAccept.setVisibility(View.GONE);
            h.btnDecline.setVisibility(View.GONE);
            h.btnPending.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class RequestRow {
        public static final int TYPE_INCOMING = 0;
        public static final int TYPE_OUTGOING = 1;

        public String uid;
        public String displayName;
        public String photoUrl;
        public int type;
    }
}
