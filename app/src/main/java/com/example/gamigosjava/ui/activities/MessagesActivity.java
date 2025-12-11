package com.example.gamigosjava.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.gamigosjava.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.annotation.NonNull;
import javax.annotation.Nullable;

public class MessagesActivity extends BaseActivity {
    private static final String EXTRA_CONVO_ID = "EXTRA_CONVO_ID";
    private static final String EXTRA_OTHER_UID = "EXTRA_OTHER_UID";
    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_IS_GROUP = "EXTRA_IS_GROUP";
    private MessagesFragment messagesFragment;

    public static Intent newIntent(Context ctx,
                                   String conversationId,
                                   String title,
                                   @Nullable String otherUid,
                                   boolean isGroup) {
        Intent intent = new Intent(ctx, MessagesActivity.class);
        intent.putExtra(EXTRA_CONVO_ID, conversationId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_OTHER_UID, otherUid);
        intent.putExtra(EXTRA_IS_GROUP, isGroup);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_messages);

        String convoTitle = getIntent().getStringExtra(EXTRA_TITLE);

        enableBackToConversations();

        String convoId  = getIntent().getStringExtra(EXTRA_CONVO_ID);
        String otherUid = getIntent().getStringExtra(EXTRA_OTHER_UID);
        boolean isGroup = getIntent().getBooleanExtra(EXTRA_IS_GROUP, false);

        // 1) Use custom centered header instead of default toolbar title
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ShapeableImageView userAvatar = findViewById(R.id.imageAvatar);
        if (userAvatar != null) {
            userAvatar.setVisibility(View.GONE); // hide current-user avatar only here
        }

        LinearLayout centerHeader = findViewById(R.id.centerConversationHeader);
        ImageView convoAvatar     = findViewById(R.id.ivConversationPhoto);
        TextView convoTitleView   = findViewById(R.id.tvConversationTitle);

        if (centerHeader != null) {
            centerHeader.setVisibility(View.VISIBLE);
        }
        if (convoTitleView != null) {
            convoTitleView.setText(convoTitle);
        }
        if (convoAvatar != null) {
            convoAvatar.setVisibility(View.VISIBLE);
            loadConversationPhotoInto(convoAvatar, convoId, otherUid, isGroup);
        }

        if (savedInstanceState == null) {
            messagesFragment = MessagesFragment.newInstance(convoId, otherUid, isGroup);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, messagesFragment, "messages_fragment")
                    .commit();
        } else {
            messagesFragment = (MessagesFragment) getSupportFragmentManager()
                    .findFragmentByTag("messages_fragment");
        }
    }

    private void loadConversationPhotoInto(ImageView imageView,
                                           String convoId,
                                           String otherUid,
                                           boolean isGroup) {

        if (isGroup && convoId != null) {
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("group_photos")
                    .child(convoId);

            ref.getDownloadUrl()
                    .addOnSuccessListener(uri ->
                            Glide.with(this)
                                    .load(uri)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_friends_24)
                                    .into(imageView))
                    .addOnFailureListener(e -> {
                        imageView.setImageResource(R.drawable.ic_friends_24);
                        imageView.setColorFilter(
                                getColor(android.R.color.white),
                                android.graphics.PorterDuff.Mode.SRC_IN
                        );
                    });

        } else if (otherUid != null) {

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(otherUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String photoUrl = doc.getString("photoUrl");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_person_24)
                                    .into(imageView);
                        } else {
                            imageView.setImageResource(R.drawable.ic_person_24);
                            imageView.setColorFilter(
                                    getColor(android.R.color.white),
                                    android.graphics.PorterDuff.Mode.SRC_IN
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        imageView.setImageResource(R.drawable.ic_person_24);
                        imageView.setColorFilter(
                                getColor(android.R.color.white),
                                android.graphics.PorterDuff.Mode.SRC_IN
                        );
                    });

        } else {
            imageView.setImageResource(R.drawable.ic_person_24);
            imageView.setColorFilter(
                    getColor(android.R.color.white),
                    android.graphics.PorterDuff.Mode.SRC_IN
            );
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_messages_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_conversation_menu) {
            if (messagesFragment == null) {
                messagesFragment = (MessagesFragment) getSupportFragmentManager()
                        .findFragmentByTag("messages_fragment");
            }

            if (messagesFragment != null) {
                messagesFragment.openConversationMenuFromToolbar();
            }
            return true;
        } else if (id == android.R.id.home) {
            // Toolbar back arrow: behave like "back to conversations"
            getOnBackPressedDispatcher().onBackPressed();   // or finish();
            return true;
        }

        // No call to super here because BaseActivity's version is abstract
        return super.onOptionsItemSelected(item);
    }
}