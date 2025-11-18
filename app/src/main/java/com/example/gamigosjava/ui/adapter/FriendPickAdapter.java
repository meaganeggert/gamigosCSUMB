package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.FriendItemModel;
import com.example.gamigosjava.ui.viewholder.FriendItemViewHolder;

import java.util.ArrayList;
import java.util.List;

public class FriendPickAdapter extends RecyclerView.Adapter<FriendItemViewHolder> {

    public interface OnFriendClickListener {
        void onFriendClick(FriendItemModel friend);
    }

    private final OnFriendClickListener listener;
    private List<FriendItemModel> items = new ArrayList<>();

    public FriendPickAdapter(OnFriendClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<FriendItemModel> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_pick, parent, false);
        return new FriendItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendItemViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}