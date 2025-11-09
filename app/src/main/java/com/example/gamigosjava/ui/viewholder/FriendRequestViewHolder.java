package com.example.gamigosjava.ui.viewholder;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;

public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
    public ImageView ivAvatar;
    public TextView tvName;
    public Button btnAccept, btnDecline, btnPending;
    public FriendRequestViewHolder(@NonNull View itemView) {
        super(itemView);
        ivAvatar = itemView.findViewById(R.id.ivAvatar);
        tvName = itemView.findViewById(R.id.tvName);
        btnAccept = itemView.findViewById(R.id.btnAccept);
        btnDecline = itemView.findViewById(R.id.btnDecline);
        btnPending = itemView.findViewById(R.id.btnPending);
    }
}
