package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.EventForm;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UpdateEventActivity extends BaseActivity {
    FirebaseUser currentUser;
    FirebaseAuth auth;

    // TODO: set the navbar and such
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_update_event);
        setTopTitle("Update Event");

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        String eventId = getIntent().getStringExtra("selectedEventId");
        EventForm eventForm= new EventForm(findViewById(R.id.updateEventRoot), this, R.layout.fragment_event_form, R.id.eventFormContainer);
        eventForm.getFriends(currentUser);
        eventForm.getEvent(eventId, currentUser);

    }


}