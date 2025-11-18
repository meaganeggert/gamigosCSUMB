package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.FriendPickAdapter;
import com.google.android.material.imageview.ShapeableImageView;

public class FriendItemViewHolder extends RecyclerView.ViewHolder {

    private final ShapeableImageView imageFriendAvatar;
    private final TextView tvFriendName;

    public FriendItemViewHolder(@NonNull View itemView) {
        super(itemView);
        imageFriendAvatar = itemView.findViewById(R.id.imageFriendAvatar);
        tvFriendName = itemView.findViewById(R.id.tvFriendName);
    }

    public void bind(FriendItemModel friend,
                     FriendPickAdapter.OnFriendClickListener listener) {

        tvFriendName.setText(friend.displayName != null ? friend.displayName : "Unknown");

        if (friend.photoUrl != null && !friend.photoUrl.isEmpty()) {
            Glide.with(itemView.getContext())
                    .load(friend.photoUrl)
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(imageFriendAvatar);
        } else {
            imageFriendAvatar.setImageResource(R.drawable.ic_person_24);
        }

        itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFriendClick(friend);
        });
    }
}
