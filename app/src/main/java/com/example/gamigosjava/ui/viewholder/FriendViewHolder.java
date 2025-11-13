package com.example.gamigosjava.ui.viewholder;

import androidx.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;

public class FriendViewHolder extends RecyclerView.ViewHolder {
    public TextView tvName;
    public ImageView ivPhoto;
    public Button btnMessage;

    public FriendViewHolder(@NonNull View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvName);
        ivPhoto = itemView.findViewById(R.id.ivPhoto);
        btnMessage = itemView.findViewById(R.id.btnMessageFriend);
    }
}
