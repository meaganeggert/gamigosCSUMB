package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.ChatMessage;
import com.example.gamigosjava.ui.viewholder.MessageViewHolder;

import java.util.ArrayList;
import java.util.List;

public class MessagesListAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private final String currentUserId;
    private final List<ChatMessage> messages = new ArrayList<>();

    public MessagesListAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        // ChatMessage has public fields; no getters needed
        return (m.senderId != null && m.senderId.equals(currentUserId))
                ? VIEW_TYPE_ME
                : VIEW_TYPE_OTHER;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        View v = inf.inflate(
                viewType == VIEW_TYPE_ME ? R.layout.row_msg_me : R.layout.row_msg_other,
                parent,
                false
        );
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage m = messages.get(position);
        holder.body.setText(m.text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void submitList(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) messages.addAll(newMessages);
        notifyDataSetChanged();
    }
}
