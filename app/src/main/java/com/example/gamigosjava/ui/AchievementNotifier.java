package com.example.gamigosjava.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.example.gamigosjava.ui.activities.AchievementsActivity;
import com.google.android.material.snackbar.Snackbar;

public class AchievementNotifier {

    private final Context context;
    private final View view;

    public AchievementNotifier(Context context, View view) {
        this.context = context;
        this.view = view;
    }

    // Determine which achievement notify to you
    // Display Achievement Unlocked banner if there is a Snackbar view
    // Display Achievement Unlocked toast if not
    public void pickAchievementBanner (String achieveName, String iconURL) {
//        if (view != null) {
//            showAchievementSnackbar(achieveName);
//        } else {
            showAchievementToast(achieveName, iconURL);
//        }
    }

    // Display Achievement Banner (Snackbar)
    private void showAchievementSnackbar(String achievementName) {
        Snackbar snackbar = Snackbar.make(view, "Achievement Unlocked: " + achievementName, Snackbar.LENGTH_LONG);

        // Make snackbar clickable
        snackbar.setAction("View", v-> {
            Intent intent = new Intent(context, AchievementsActivity.class);
            context.startActivity(intent);
        });

        snackbar.show();
    }

    // Display Achievement Toast (back-up)
    private void showAchievementToast(String achievementName, String iconUrl) {
        // TODO: Modify this so the toast has a custom layout
        Toast.makeText(context, "\uD83C\uDFC6 Achievement Unlocked: " + achievementName + "!", Toast.LENGTH_SHORT).show();
    }
}
