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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesListAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private final boolean isGroup;
    private final String currentUserId;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, String> senderNames = new HashMap<>();

    public MessagesListAdapter(String currentUserId, boolean isGroup) {
        this.currentUserId = currentUserId;
        this.isGroup = isGroup;
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

        if (holder.senderName != null) {
            boolean isMine = m.senderId != null && m.senderId.equals(currentUserId);

            if (isGroup && !isMine) {
                holder.senderName.setVisibility(View.VISIBLE);

                String name = senderNames.get(m.senderId);
                if (name == null || name.trim().isEmpty()) {
                    name = "Unknown";
                }
                holder.senderName.setText(name);
            } else {
                holder.senderName.setVisibility(View.GONE);
            }
        }
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

    public void setSenderNames(Map<String, String> map) {
        senderNames.clear();
        if (map != null) {
            senderNames.putAll(map);
        }
        notifyDataSetChanged();
    }

    public void setSenderName(String userId, String displayName) {
        if (userId == null) return;
        if (displayName == null) displayName = "";
        senderNames.put(userId, displayName);
        notifyDataSetChanged();
    }
}
