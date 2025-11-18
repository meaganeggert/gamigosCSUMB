package com.example.gamigosjava.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;

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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

public class MessagesFragment extends Fragment {
    private static final String ARG_CONVO_ID = "ARG_CONVO_ID";
    private static final String ARG_OTHER_UID = "ARG_OTHER_UID";
    private static final String ARG_IS_GROUP = "ARG_IS_GROUP";

    public static MessagesFragment newInstance(@Nullable String convoId,
                                               @Nullable String otherUid,
                                               boolean isGroup) {
        MessagesFragment f = new MessagesFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CONVO_ID, convoId);
        b.putString(ARG_OTHER_UID, otherUid);
        b.putBoolean(ARG_IS_GROUP, isGroup);
        f.setArguments(b);
        return f;
    }

    private ChatViewModel vm;
    private MessagesListAdapter adapter;

    // 🔹 add these fields
    private String convoId;
    private String otherUid;
    private boolean isGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = v.findViewById(R.id.rvMessages);
        EditText input = v.findViewById(R.id.etMessage);
        ImageButton send = v.findViewById(R.id.btnSend);
        ImageButton menu = v.findViewById(R.id.btnMessageMenu);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MessagesListAdapter(FirebaseAuth.getInstance().getUid());
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ChatViewModel.class);

        // 🔹 read args into the fields
        Bundle args = getArguments();
        if (args != null) {
            convoId = args.getString(ARG_CONVO_ID);
            otherUid = args.getString(ARG_OTHER_UID);
            isGroup = args.getBoolean(ARG_IS_GROUP, false);
        }

        vm.start(convoId, otherUid);

        vm.messages.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            if (list != null && !list.isEmpty()) {
                rv.scrollToPosition(list.size() - 1);
            }
        });

        vm.conversationId.observe(getViewLifecycleOwner(), id -> {
            if (id != null) vm.markRead();
        });

        send.setOnClickListener(view -> {
            String text = input.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                vm.sendMessage(text);
                input.setText("");
            }
        });

        menu.setOnClickListener(this::showConversationMenu);
    }

    private void showConversationMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(
                isGroup ? R.menu.menu_group_conversation : R.menu.menu_dm_conversation,
                popup.getMenu()
        );

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_delete_conversation) {
                confirmDeleteConversation();
                return true;
            } else if (!isGroup && id == R.id.action_add_participants) {
                openEditParticipants();
                return true;
            } else if (isGroup && id == R.id.action_change_title) {
                showChangeTitleDialog();
                return true;
            } else if (isGroup && id == R.id.action_change_photo) {
                requireActivity().startActivity(
                        new Intent(requireContext(), NewConversationActivity.class)
                                .putExtra("EDIT_CONVO_ID", convoId)
                                .putExtra("EDIT_MODE", "photo")
                );
                return true;
            } else if (isGroup && id == R.id.action_edit_participants) {
                openEditParticipants();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void confirmDeleteConversation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete conversation")
                .setMessage("This will delete the conversation for you. Continue?")
                .setPositiveButton("Delete", (d, which) -> deleteConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteConversation() {
        if (convoId == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference convoRef = db.collection("conversations").document(convoId);

        StorageReference photoRef = FirebaseStorage.getInstance()
                .getReference("group_photos")
                .child(convoId);

        photoRef.delete()
                .addOnSuccessListener(unused ->
                        Log.d("MessagesFragment", "Group photo deleted for convo " + convoId))
                .addOnFailureListener(e -> {
                    // It's okay if there was no photo; ignore "object not found" errors
                    if (e instanceof com.google.firebase.storage.StorageException
                            && ((StorageException) e).getErrorCode()
                            == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        Log.d("MessagesFragment", "No group photo to delete for convo " + convoId);
                    } else {
                        Log.w("MessagesFragment", "Failed to delete group photo for convo " + convoId, e);
                    }
                });

        convoRef.collection("messages").get()
                .addOnSuccessListener(msgSnap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : msgSnap.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    convoRef.collection("participantsData").get()
                            .addOnSuccessListener(partSnap -> {
                                for (DocumentSnapshot doc : partSnap.getDocuments()) {
                                    batch.delete(doc.getReference());
                                }
                                batch.commit()
                                        .addOnSuccessListener(unused -> convoRef.delete()
                                                .addOnSuccessListener(u -> requireActivity().finish()));
                            });
                });
    }


    private void showChangeTitleDialog() {
        if (!isGroup || convoId == null) return;

        final EditText input = new EditText(requireContext());
        input.setHint("Group title");

        new AlertDialog.Builder(requireContext())
                .setTitle("Change group title")
                .setView(input)
                .setPositiveButton("Save", (d, which) -> {
                    String newTitle = input.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        FirebaseFirestore.getInstance()
                                .collection("conversations")
                                .document(convoId)
                                .update("titleOverride", newTitle);

                        // Also update toolbar title
                        if (getActivity() instanceof BaseActivity) {
                            ((BaseActivity) getActivity()).setTopTitle(newTitle);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openEditParticipants() {
        if (convoId == null) return;
        Intent intent = new Intent(requireContext(), NewConversationActivity.class);
        intent.putExtra(NewConversationActivity.EXTRA_EDIT_CONVO_ID, convoId);
        startActivity(intent);
    }
}