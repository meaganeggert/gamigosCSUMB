package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import com.example.gamigosjava.ui.activities.BaseActivity;

// Firebase
import com.example.gamigosjava.R;
import com.google.firebase.auth.FirebaseAuth;

// Credential Manager (AndroidX)
import androidx.credentials.CredentialManager;

public class LandingActivity extends BaseActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_landing);

        setChildLayout(R.layout.activity_landing);

        // Set title for NavBar
        setTopTitle("Gamigos");

    }
}