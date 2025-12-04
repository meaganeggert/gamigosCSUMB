package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.AppNotificationModel;
import com.example.gamigosjava.ui.viewholder.NotificationsViewHolder;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotificationModel notification);
    }

    private final List<AppNotificationModel> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AppNotificationModel> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationsViewHolder holder, int position) {
        AppNotificationModel notif = items.get(position);
        holder.bind(notif, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
