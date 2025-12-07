package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;

import java.text.DateFormat;
import java.util.Date;

public class NotificationsViewHolder extends RecyclerView.ViewHolder {

    public interface OnItemClickHandler {
        /**
         * Called when the row is clicked in NORMAL mode (opens the notification).
         */
        void onNormalClick();

        /**
         * Called when selection state is toggled in SELECTION mode.
         *
         * @param nowSelected true if the item is now selected, false if unselected
         */
        void onToggleSelection(boolean nowSelected);
    }

    TextView titleView, bodyView, timeView;
    CheckBox checkboxSelect;

    public NotificationsViewHolder(@NonNull View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.textTitle);
        bodyView = itemView.findViewById(R.id.textBody);
        timeView = itemView.findViewById(R.id.textTime);
        checkboxSelect = itemView.findViewById(R.id.checkSelect);
    }

    /**
     * Binds the notification row.
     *
     * @param notif       The notification data.
     * @param selectionMode True if selection mode is active.
     * @param isSelected    True if this item is currently selected.
     * @param handler       Callback to adapter for normal-click or selection toggle.
     */
    public void bind(
            AppNotificationModel notif,
            boolean selectionMode,
            boolean isSelected,
            OnItemClickHandler handler
    ) {
        titleView.setText(notif.getTitle());
        bodyView.setText(notif.getBody());

        DateFormat df = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT
        );
        timeView.setText(df.format(new Date(notif.getTimestamp())));

        if (selectionMode) {
            // Selection mode: show checkbox, clicking row toggles selection
            checkboxSelect.setVisibility(View.VISIBLE);
            checkboxSelect.setChecked(isSelected);

            // Full row click toggles selected state
            itemView.setOnClickListener(v -> {
                boolean newSelected = !checkboxSelect.isChecked();
                checkboxSelect.setChecked(newSelected);
                if (handler != null) {
                    handler.onToggleSelection(newSelected);
                }
            });

            // Direct checkbox tap also toggles selection
            checkboxSelect.setOnClickListener(v -> {
                boolean newSelected = checkboxSelect.isChecked();
                if (handler != null) {
                    handler.onToggleSelection(newSelected);
                }
            });

        } else {
            // Normal mode: hide checkbox, click opens the notification
            checkboxSelect.setVisibility(View.GONE);
            checkboxSelect.setOnClickListener(null);
            checkboxSelect.setChecked(false);

            itemView.setOnClickListener(v -> {
                if (handler != null) {
                    handler.onNormalClick();
                }
            });
        }
    }
}
