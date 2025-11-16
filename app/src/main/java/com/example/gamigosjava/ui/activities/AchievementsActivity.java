package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.AchievementSummary;
import com.example.gamigosjava.data.model.EventSummary;
import com.example.gamigosjava.ui.adapter.AchievementAdapter;
import com.example.gamigosjava.ui.adapter.EventAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementsActivity extends BaseActivity {

    private static final String TAG = "AchievementsActivity";
    private FirebaseFirestore db;
    private AchievementAdapter achieveAdapter;
    private RecyclerView achieveRecycler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setChildLayout(R.layout.activity_achievements);

        // Set title for NavBar
        setTopTitle("Achievements");

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Get Active User
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get All Achievements
        Task<QuerySnapshot> achievementsTask = db.collection("achievements")
                .orderBy("displayOrder", Query.Direction.ASCENDING)
                .get(Source.SERVER);

        // Get User Metrics
        Task<QuerySnapshot> userMetricsTask = db.collection("users")
                .document(userId)
                .collection("metrics")
                .get(Source.SERVER);

        // Get User Achievements
        Task<QuerySnapshot> userAchievementsTask = db.collection("users")
                .document(userId)
                .collection("achievements")
                .get(Source.SERVER);

        // Make sure all tasks complete and then save relevant data
        Tasks.whenAllSuccess(achievementsTask, userMetricsTask, userAchievementsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot allAchieveSnap = (QuerySnapshot) results.get(0);
                    QuerySnapshot userMetricsSnap = (QuerySnapshot) results.get(1);
                    QuerySnapshot userAchieveSnap = (QuerySnapshot) results.get(2);
                    // Summarize user results data
                    Map<String, DocumentSnapshot> userMetricsMap = new HashMap<>();
                    for (DocumentSnapshot doc : userMetricsSnap) {
                        userMetricsMap.put(doc.getId(), doc); // save each tracked metric document
                    }

                    Map<String, DocumentSnapshot> userEarnedMap = new HashMap<>();
                    for (DocumentSnapshot doc : userAchieveSnap) {
                        userEarnedMap.put(doc.getId(), doc); // save each earned achievement document
                    }

                    List<AchievementSummary> achievementList = new ArrayList<>();

                    // Summarize achievement results data
                    for (DocumentSnapshot achievement : allAchieveSnap) {
                        String id = achievement.getId();
                        String title = achievement.getString("name");
                        String description = achievement.getString("description");
                        Long goal = achievement.getLong("goal");
                        int goalInt = goal != null ? goal.intValue() : 1;

                        // Get current progress
                        // These are just hard-coded for our two example achievements
                        // TODO: alter this later to work with all achievement types, ex: counts, streaks, etc.
                        int current = 0;
                        if ("first_login".equals(id)) {
                            DocumentSnapshot loginCountSnap = userMetricsMap.get("login_count");
                            if (loginCountSnap != null) {
                                Long c = loginCountSnap.getLong("count");
                                current = (c != null) ? c.intValue() : 0;
                            }
                        } else if ("streak_3".equals(id)) {
                            DocumentSnapshot loginStreakSnap = userMetricsMap.get("login_streak");
                            if (loginStreakSnap != null) {
                                Long c = loginStreakSnap.getLong("current");
                                current = (c != null) ? c.intValue() : 0;
                            }
                        }

                        // To get "achieved" boolean
                        DocumentSnapshot achievedSnap = userEarnedMap.get(id);
                        boolean earned = false;
                        if (achievedSnap != null) {
                            Boolean b = achievedSnap.getBoolean("earned");
                            earned = (b != null) && b;
                        }

                        achievementList.add(new AchievementSummary(
                                id, title, description, "", current, goalInt, earned
                        ));
                    }
                    achieveAdapter.setItems(achievementList);
                })
                .addOnFailureListener(e->
                        Log.e(TAG, "Error loading achievements: ", e));


        // RecyclerView + Adapter
        achieveRecycler = findViewById(R.id.recyclerViewAchievements);
        achieveRecycler.setLayoutManager(new LinearLayoutManager(this));
        achieveAdapter = new AchievementAdapter();
        achieveRecycler.setAdapter(achieveAdapter);
    }

}