package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.ConversationsAdapter;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.Timestamp;

import java.text.DateFormat;

public class ConversationViewHolder extends RecyclerView.ViewHolder {

    ShapeableImageView imageIcon;
    TextView tvTitle;
    TextView tvLastMessage;
    TextView tvLastUpdated;

    public ConversationViewHolder(@NonNull View itemView) {
        super(itemView);
        imageIcon     = itemView.findViewById(R.id.imageOtherAvatar);
        tvTitle       = itemView.findViewById(R.id.tvOtherName);
        tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        tvLastUpdated = itemView.findViewById(R.id.tvLastUpdated);
    }

    public void bind(ConversationsModel c,
                     ConversationsAdapter.OnConversationClickListener listener) {
        // --- ICON / AVATAR ---
        if (c.isGroup()) {
            String groupPhotoUrl = c.getGroupPhotoUrl();
            if (groupPhotoUrl != null && !groupPhotoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(groupPhotoUrl)
                        .placeholder(R.drawable.ic_friends_24)
                        .error(R.drawable.ic_friends_24)
                        .into(imageIcon);
            } else {
                imageIcon.setImageResource(R.drawable.ic_friends_24);
            }
        } else {
            String photoUrl = c.getOtherPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person_24)
                        .error(R.drawable.ic_person_24)
                        .into(imageIcon);
            } else {
                // no avatar → default icon
                imageIcon.setImageResource(R.drawable.ic_person_24);
            }
        }

        // --- TITLE ---
        String title;
        if (c.isGroup()) {
            if (c.getTitleOverride() != null && !c.getTitleOverride().isEmpty()) {
                title = c.getTitleOverride();
            } else {
                title = "Group chat";
            }
        } else {
            if (c.getOtherName() != null && !c.getOtherName().isEmpty()) {
                title = c.getOtherName();
            } else {
                title = "Direct message";
            }
        }
        tvTitle.setText(title);

        // --- LAST MESSAGE ---
        tvLastMessage.setText(c.getLastMessage() != null ? c.getLastMessage() : "");

        // --- TIME ---
        Timestamp ts = c.getLastMessageAt();
        if (ts != null) {
            String timeStr = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT, DateFormat.SHORT
            ).format(ts.toDate());
            tvLastUpdated.setText(timeStr);
        } else {
            tvLastUpdated.setText("");
        }

        itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(c);
        });
    }
}
