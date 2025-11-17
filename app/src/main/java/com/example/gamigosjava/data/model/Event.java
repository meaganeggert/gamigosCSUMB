package com.example.gamigosjava.data.model;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

import com.example.gamigosjava.R;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Event {
    public String id;
    public String hostId;
    public String title;
    public String visibility;
    public String status;
    public String notes;
    public Timestamp scheduledAt;
    public Timestamp createdAt;
    public Timestamp endedAt;
}
