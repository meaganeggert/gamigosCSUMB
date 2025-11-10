package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;

public class UserViewHolder extends RecyclerView.ViewHolder {
    public TextView tvName;
    public ImageView ivPhoto;
    public Button btnAddFriend, btnDenyFriend;

    public UserViewHolder(View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tvName);
        ivPhoto = itemView.findViewById(R.id.ivPhoto);
        btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        btnDenyFriend = itemView.findViewById(R.id.btnDenyFriend);
    }
}
