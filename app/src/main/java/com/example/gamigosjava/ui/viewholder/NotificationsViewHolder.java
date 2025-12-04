package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.NotificationsAdapter;

import java.text.DateFormat;
import java.util.Date;

public class NotificationsViewHolder extends RecyclerView.ViewHolder {
    TextView titleView, bodyView, timeView;

    public NotificationsViewHolder(@NonNull View itemView) {
        super(itemView);
        titleView = itemView.findViewById(R.id.textTitle);
        bodyView = itemView.findViewById(R.id.textBody);
        timeView = itemView.findViewById(R.id.textTime);
    }

    public void bind(AppNotificationModel notif, NotificationsAdapter.OnNotificationClickListener listener) {
        titleView.setText(notif.getTitle());
        bodyView.setText(notif.getBody());

        DateFormat df = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT
        );
        timeView.setText(df.format(new Date(notif.getTimestamp())));

        itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notif);
        });
    }
}
