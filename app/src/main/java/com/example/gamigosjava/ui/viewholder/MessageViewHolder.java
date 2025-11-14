package com.example.gamigosjava.ui.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;

public class MessageViewHolder extends RecyclerView.ViewHolder {
    public final TextView body;

    public MessageViewHolder(@NonNull View itemView) {
        super(itemView);
        body = itemView.findViewById(R.id.tvBody);
    }
}
