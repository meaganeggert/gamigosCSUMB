package com.example.gamigosjava.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


import androidx.fragment.app.Fragment;

import com.example.gamigosjava.R;

public class MessagesActivity extends BaseActivity {
    private static final String EXTRA_CONVO_ID = "EXTRA_CONVO_ID";
    private static final String EXTRA_OTHER_UID = "EXTRA_OTHER_UID";
    private static final String EXTRA_TITLE = "EXTRA_TITLE";

    public static Intent newIntent(Context ctx, String conversationId, String title, String otherUid) {
        Intent intent = new Intent(ctx, MessagesActivity.class);
        intent.putExtra(EXTRA_CONVO_ID, conversationId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_OTHER_UID, otherUid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_messages);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        setTopTitle(title != null ? title : "Messages");

        String convoId = getIntent().getStringExtra(EXTRA_CONVO_ID);
        String otherUid = getIntent().getStringExtra(EXTRA_OTHER_UID);

        if (savedInstanceState == null) {
            Fragment fragment = MessagesFragment.newInstance(convoId, otherUid);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }


}