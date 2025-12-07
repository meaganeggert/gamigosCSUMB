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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationsAdapter
        extends RecyclerView.Adapter<NotificationsViewHolder> {

    // Fired when a notification is clicked in NORMAL mode
    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotificationModel notification);
    }

    // Fired when the selection count changes in SELECTION mode
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private OnSelectionChangedListener selectionChangedListener;
    private final List<AppNotificationModel> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    // Track selected notifications by their Firestore id
    private final Set<String> selectedIds = new HashSet<>();
    private boolean selectionMode = false;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setItems(List<AppNotificationModel> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedIds.clear();
        notifyDataSetChanged();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
    }

    /**
     * Enable or disable selection mode.
     * In selection mode: tapping an item toggles selection.
     * In normal mode: tapping an item opens it.
     */
    public void setSelectionMode(boolean enable) {
        this.selectionMode = enable;
        if (!enable) {
            // Exiting selection mode -> clear current selections
            selectedIds.clear();
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    /**
     * Returns all selected notifications (for Delete Selected).
     */
    public List<AppNotificationModel> getSelectedItems() {
        List<AppNotificationModel> results = new ArrayList<>();
        for (AppNotificationModel notif : items) {
            if (notif.getId() != null && selectedIds.contains(notif.getId())) {
                results.add(notif);
            }
        }
        return results;
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

        // Is this item currently selected?
        boolean isSelected = notif.getId() != null && selectedIds.contains(notif.getId());

        // Delegate all view logic to the ViewHolder, but keep state/selection logic here
        holder.bind(
                notif,
                selectionMode,
                isSelected,
                new NotificationsViewHolder.OnItemClickHandler() {
                    @Override
                    public void onNormalClick() {
                        // Normal mode behavior: open the notification
                        if (listener != null) {
                            listener.onNotificationClick(notif);
                        }
                    }

                    @Override
                    public void onToggleSelection(boolean nowSelected) {
                        // Selection mode behavior: update selectedIds and notify listener
                        if (notif.getId() == null) return;

                        if (nowSelected) {
                            selectedIds.add(notif.getId());
                        } else {
                            selectedIds.remove(notif.getId());
                        }

                        if (selectionChangedListener != null) {
                            selectionChangedListener.onSelectionChanged(selectedIds.size());
                        }
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
