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
import android.util.Log;
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
 * Use the {@link MatchResultFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MatchResultFragment extends Fragment {
    private static final String TAG = "TeamScoreFragment";
    private static final String ARG_MATCH_ID = "param1";
    private static final String ARG_WIN_RULE = "param2";
    private static final String ARG_TEAM_NUMBER = "param3";

    private String matchId;
    private String winRule;
    private Integer teamNumber;

    private List<Player> playerList = new ArrayList<>();    // Used to pass the playerList to the scoresAdapter

    public ScoresAdapter scoresAdapter;

    Context context;
    private EditText teamPlacement, customPlayerInput;
    private Switch playerTypeToggle;
    private Spinner inviteeDropDown;
    private Button addPlayer;
    private RecyclerView playerScores;
    private TextView placementLabel, scoreLabel, teamName;

    public ArrayAdapter<Friend> inviteeAdapter;
    private List<Friend> inviteeList = new ArrayList<>();




    public MatchResultFragment() {
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
    public static MatchResultFragment newInstance(String matchId, String winRule, Integer teamNumber, List<Player> playerList) {
        MatchResultFragment fragment = new MatchResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_ID, matchId);
        args.putString(ARG_WIN_RULE, winRule);
        args.putInt(ARG_TEAM_NUMBER, teamNumber);
        fragment.setArguments(args);

        // It is necessary to set the team number and player list as early as possible.
        fragment.teamNumber = teamNumber;
        fragment.playerList.addAll(playerList);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId = getArguments().getString(ARG_MATCH_ID);
            setWinRule(getArguments().getString(ARG_WIN_RULE));
            setTeamNumber(getArguments().getInt(ARG_TEAM_NUMBER));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_match_result, container, false);
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
        teamName = v.findViewById(R.id.textView_teamName);

        // Filter players
        handleCoopPlayers();

        playerScores.setLayoutManager(new LinearLayoutManager(context));
        scoresAdapter = new ScoresAdapter();
        scoresAdapter.setItems(playerList);
        playerScores.setAdapter(scoresAdapter);

        scoresAdapter.winRule = winRule;
        scoresAdapter.setTeamNumber(teamNumber);

        // Create the label for the team.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Team ");
        stringBuilder.append(teamNumber);
        teamName.setText(stringBuilder);

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

                if (placementText.isEmpty()) return;

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

        // Hide/show labels when they are necessary to the winRule
        if (winRule.equals("highest")) {
            teamName.setVisibility(TextView.GONE);
            teamPlacement.setVisibility(TextView.GONE);
            scoreLabel.setVisibility(TextView.VISIBLE);
            placementLabel.setVisibility(TextView.GONE);
        }
        else if (winRule.equals("lowest")) {
            teamName.setVisibility(TextView.GONE);
            teamPlacement.setVisibility(TextView.GONE);
            scoreLabel.setVisibility(TextView.VISIBLE);
            placementLabel.setVisibility(TextView.GONE);
        }
        else if (winRule.equals("cooperative") || winRule.equals("teams")) {
            teamName.setVisibility(TextView.VISIBLE);
            teamPlacement.setVisibility(TextView.VISIBLE);
            placementLabel.setVisibility(TextView.INVISIBLE);
            scoreLabel.setVisibility(TextView.INVISIBLE);
        }
        else {
            teamName.setVisibility(TextView.GONE);
            teamPlacement.setVisibility(TextView.GONE);
            scoreLabel.setVisibility(TextView.GONE);
            placementLabel.setVisibility(TextView.VISIBLE);
        }
    }

    // Only called when the fragment view is created.
    // Removes any players from the current fragments scoreAdapter if they
    // are not a part of the current fragments team. (Only when winRule = cooperative/teams)
    private void handleCoopPlayers() {
        if (!winRule.equals("teams")) {
            Log.d(TAG, "Couldn't handle players: win rule isn't coop or teams");
            return;
        }
        if (teamNumber == null) {
            Log.d(TAG, "Couldn't handle players: team number was null");
            return;
        }
        if (playerList.isEmpty()) {
            Log.d(TAG, "Couldn't handle players: player list is empty");
            return;
        }

        for (int i = 0; i < playerList.size(); i++) {
            Integer playerTeam = playerList.get(i).team;

            if (playerTeam == null || !playerTeam.equals(teamNumber)) {
                playerList.remove(i);
            }
        }
    }

    // Allows the user to add a player from the "Add Player" dropdown to the score adapter.
    private void addPlayerFromSpinner() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                Player newPlayer = new Player();
                newPlayer.friend = (Friend) inviteeDropDown.getSelectedItem();
                newPlayer.team = teamNumber;

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

    // Allows the user to add a player from the "Add Player" custom player text input to the score adapter.
    private void addPlayerFromText() {
        if (addPlayer != null) {
            addPlayer.setOnClickListener(v -> {
                String customName = customPlayerInput.getText().toString();
                if (customName.isEmpty()) return;

                Player newPlayer = new Player();
                newPlayer.friend = new Friend();
                newPlayer.friend.displayName = customName;
                newPlayer.team = teamNumber;

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

    // Maps the match's overall invitee List to the current fragment's invitee dropdown.
    public void setInviteeList(List<Friend> inviteeList) {
        this.inviteeList = inviteeList;

        Log.d(TAG, "SET INVITEES");

        if (inviteeAdapter != null) {
            inviteeAdapter.notifyDataSetChanged();
        }
    }

    // Sets the team number for both the fragment, and the scoresAdapter.
    public void setTeamNumber(int teamNumber) {
        this.teamNumber = teamNumber;

        if (scoresAdapter != null) {
            scoresAdapter.setTeamNumber(this.teamNumber);
        }
    }

    // Sets the win rule for the both the fraqment and the scoresAdapter.
    public void setWinRule(String winRule) {
        if (scoreLabel != null) {
            scoreLabel.setVisibility(TextView.INVISIBLE);
        }
        if (placementLabel != null) {
            placementLabel.setVisibility(TextView.INVISIBLE);
        }

        this.winRule = winRule;

        if (scoresAdapter != null) {
            scoresAdapter.winRule = winRule;
            scoresAdapter.notifyDataSetChanged();
        }
    }

    // Can be called any time after the fragment is created. Only adds players if they are in this team.
    public void setPlayerList(List<Player> playerList) {
        this.playerList.clear();

        // If the winRule is set to cooperative/teams, only add players to the current fragment's
        // scoresAdapter, otherwise, just add all players.
        if (winRule.equals("teams")) {
            for (int i = 0; i < playerList.size(); i++) {
                Player p = playerList.get(i);

                if (p.team != null && p.team.equals(teamNumber)) {
                    this.playerList.add(p);
                    teamPlacement.setText(p.placement.toString());
                }
            }
        }

        else {
            this.playerList.addAll(playerList);
        }

        if (scoresAdapter != null) {
            scoresAdapter.setItems(this.playerList);
            scoresAdapter.notifyDataSetChanged();
        }
    }
}