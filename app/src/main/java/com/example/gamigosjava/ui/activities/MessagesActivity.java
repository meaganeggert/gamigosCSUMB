package com.example.gamigosjava.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.example.gamigosjava.R;

import javax.annotation.Nullable;

public class MessagesActivity extends BaseActivity {
    private static final String EXTRA_CONVO_ID = "EXTRA_CONVO_ID";
    private static final String EXTRA_OTHER_UID = "EXTRA_OTHER_UID";
    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_IS_GROUP = "EXTRA_IS_GROUP";

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

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        setTopTitle(title != null ? title : "Messages");

        enableBackToConversations();

        String convoId  = getIntent().getStringExtra(EXTRA_CONVO_ID);
        String otherUid = getIntent().getStringExtra(EXTRA_OTHER_UID);
        boolean isGroup = getIntent().getBooleanExtra(EXTRA_IS_GROUP, false);

        if (savedInstanceState == null) {
            Fragment fragment = MessagesFragment.newInstance(convoId, otherUid, isGroup);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}