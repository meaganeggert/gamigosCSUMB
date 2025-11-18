package com.example.gamigosjava.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.viewholder.ConversationViewHolder;
import com.example.gamigosjava.ui.viewholder.ConversationsModel;
import java.util.ArrayList;
import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(ConversationsModel conversation);
    }

    private List<ConversationsModel> conversations = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationsAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<ConversationsModel> list) {
        this.conversations = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        ConversationsModel c = conversations.get(position);
        holder.bind(c, listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }
}
