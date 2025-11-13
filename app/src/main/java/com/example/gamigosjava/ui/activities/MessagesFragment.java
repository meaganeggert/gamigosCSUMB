package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.ui.adapter.MessagesListAdapter;
import com.example.gamigosjava.ui.viewholder.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class MessagesFragment extends Fragment {
    private static final String ARG_CONVO_ID = "ARG_CONVO_ID";
    private static final String ARG_OTHER_UID = "ARG_OTHER_UID";

    public static MessagesFragment newInstance(@Nullable String convoId, @Nullable String otherUid) {
        MessagesFragment f = new MessagesFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CONVO_ID, convoId);
        b.putString(ARG_OTHER_UID, otherUid);
        f.setArguments(b);
        return f;
    }

    private ChatViewModel vm;
    private MessagesListAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        // Views
        RecyclerView rv = v.findViewById(R.id.rvMessages);
        EditText input = v.findViewById(R.id.etMessage);
        ImageButton send = v.findViewById(R.id.btnSend);

        // Recycler
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesListAdapter(FirebaseAuth.getInstance().getUid());
        rv.setAdapter(adapter);

        // ViewModel
        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        String convoId = getArguments() != null ? getArguments().getString(ARG_CONVO_ID) : null;
        String otherUid = getArguments() != null ? getArguments().getString(ARG_OTHER_UID) : null;
        vm.start(convoId, otherUid);

        // Observe messages
        vm.messages.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            if (list != null && !list.isEmpty()) {
                rv.scrollToPosition(list.size() - 1);
            }
        });

        // Mark read when we have a convo id
        vm.conversationId.observe(getViewLifecycleOwner(), id -> {
            if (id != null) vm.markRead();
        });

        // Send click
        send.setOnClickListener(view -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                vm.sendMessage(text);
                input.setText("");
            }
        });
    }
}