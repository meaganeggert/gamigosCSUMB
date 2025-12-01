package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.Match;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.example.gamigosjava.ui.adapter.ScoresAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuickPlayActivity extends ViewMatchActivity {
    private static final String TAG = "Quick Play";
    BGG_API api;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = auth.getCurrentUser();
    List<Friend> inviteeList = new ArrayList<>();
    ArrayAdapter<Friend> inviteeAdapter;

    Match matchItem = new Match();
    LinearLayout matchFormContainerHandle;
    private ArrayAdapter<GameSummary> userGameAdapter;
    private ArrayAdapter<GameSummary> apiGameAdapter;
    private List<GameSummary> userGameList;
    private List<GameSummary> apiGameList;

    private RecyclerView recyclerView;
    private ScoresAdapter scoresAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_quick_play);

        setTopTitle("Quick Play");
    }

}