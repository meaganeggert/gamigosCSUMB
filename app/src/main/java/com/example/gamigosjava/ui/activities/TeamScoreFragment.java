package com.example.gamigosjava.ui.activities;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.model.Friend;
import com.example.gamigosjava.data.model.Player;
import com.example.gamigosjava.ui.adapter.ScoresAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TeamScoreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TeamScoreFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_MATCH_ID = "param1";
    private static final String ARG_WIN_RULE = "param2";

    // TODO: Rename and change types of parameters
    private String matchId;
    private String winRule;
    private Integer teamNumber;

    private List<Player> players = new ArrayList<>();
    public ScoresAdapter scoresAdapter;

    Context context;
    private EditText teamPlacement;
    private Switch playerTypeToggle;
    private Spinner inviteeDropDown;
    private EditText customPlayerInput;
    private Button addPlayer;
    private RecyclerView playerScores;
    private TextView placementLabel;
    private TextView scoreLabel;

    public ArrayAdapter<Friend> inviteeAdapter;
    private List<Friend> inviteeList = new ArrayList<>();



    public TeamScoreFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param matchId Parameter 1.
     * @param winRule Parameter 2.
     * @return A new instance of fragment TeamScoreFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TeamScoreFragment newInstance(String matchId, String winRule) {
        TeamScoreFragment fragment = new TeamScoreFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_ID, matchId);
        args.putString(ARG_WIN_RULE, winRule);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId = getArguments().getString(ARG_MATCH_ID);
            winRule = getArguments().getString(ARG_WIN_RULE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_team_score, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstance) {
        context = v.getContext();
        teamPlacement = v.findViewById(R.id.editText_teamPlacement);
        playerTypeToggle = v.findViewById(R.id.switch_customPlayerToggle);
        inviteeDropDown = v.findViewById(R.id.dropdown_invitees);
        customPlayerInput = v.findViewById(R.id.editText_customPlayer);
        addPlayer = v.findViewById(R.id.button_addPlayer);
        playerScores = v.findViewById(R.id.recyclerViewPlayerScores);
        placementLabel = v.findViewById(R.id.textView_playerPlacementLabel);
        scoreLabel = v.findViewById(R.id.textView_playerScoreLabel);


        playerScores.setLayoutManager(new LinearLayoutManager(context));
        scoresAdapter = new ScoresAdapter();
        scoresAdapter.setItems(players);
        playerScores.setAdapter(scoresAdapter);

        scoresAdapter.winRule = winRule;

        inviteeAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                inviteeList
        );
        inviteeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inviteeDropDown.setAdapter(inviteeAdapter);
        inviteeAdapter.notifyDataSetChanged();

        teamPlacement.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String placementText = teamPlacement.getText().toString();
                for (Player p: scoresAdapter.playerList) {
                    p.setPlacement(placementText);
                }
                scoresAdapter.notifyDataSetChanged();
            }
        });


        if (playerTypeToggle != null) {
            playerTypeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    inviteeDropDown.setVisibility(Spinner.GONE);
                    customPlayerInput.setVisibility(Button.VISIBLE);
                    addPlayerFromText();
                } else {
                    inviteeDropDown.setVisibility(Spinner.VISIBLE);
                    customPlayerInput.setVisibility(Button.GONE);
                    addPlayerFromSpinner();
                }
            });
        }
        addPlayerFromSpinner();
    }

    private void addPlayerFromSpinner() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                Player newPlayer = new Player();
                newPlayer.friend = (Friend) inviteeDropDown.getSelectedItem();

                if (newPlayer.friend == null) return;
                if (newPlayer.friend.id == null) return;

                boolean inList = false;
                for (int i = 0; i < scoresAdapter.getItemCount(); i++) {
                    Player player = scoresAdapter.playerList.get(i);
                    if (player.friend.id == null) continue;

                    if (player.friend.id.equals(newPlayer.friend.id)) {
                        inList = true;
                        break;
                    }
                }
                if (inList) {
                    Toast.makeText(context, "User is already a player in this match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(context, "Adding player: " + newPlayer.friend.displayName, Toast.LENGTH_SHORT).show();
                scoresAdapter.playerList.add(newPlayer);
                scoresAdapter.notifyDataSetChanged();
            });
        }
    }

    private void addPlayerFromText() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                String customName = customPlayerInput.getText().toString();
                if (customName.isEmpty()) return;

                Player newPlayer = new Player();
                newPlayer.friend = new Friend();
                newPlayer.friend.displayName = customName;

                boolean inList = false;
                for (int i = 0; i < scoresAdapter.getItemCount(); i++) {
                    if (scoresAdapter.playerList.get(i).friend.displayName.equals(newPlayer.friend.displayName)) {
                        inList = true;
                        break;
                    }
                }
                if (inList) {
                    Toast.makeText(context, "User is already a player in this match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                scoresAdapter.playerList.add(newPlayer);
                scoresAdapter.notifyDataSetChanged();
            });
        }
    }

    public void setScoresAdapter(ScoresAdapter adapter) {
        scoresAdapter = adapter;
    }

    public void setInviteeList(List<Friend> inviteeList) {
        this.inviteeList = inviteeList;

        if (inviteeAdapter != null) {
            inviteeAdapter.notifyDataSetChanged();
        }
    }

    public void setTeamNumber(int teamNumber) {
        this.teamNumber = teamNumber;
    }

    public void setWinRule(String winRule) {
        if (scoreLabel != null) {
            scoreLabel.setVisibility(TextView.INVISIBLE);
        }
        if (placementLabel != null) {
            placementLabel.setVisibility(TextView.INVISIBLE);
        }

        scoresAdapter.winRule = winRule;
        scoresAdapter.notifyDataSetChanged();
    }

    public void setPlayerList(List<Player> playerList) {
        this.players = playerList;

        if (scoresAdapter != null) {
            scoresAdapter.notifyDataSetChanged();
        }
    }
}