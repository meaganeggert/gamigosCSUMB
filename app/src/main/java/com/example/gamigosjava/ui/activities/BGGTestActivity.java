package com.example.gamigosjava.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gamigosjava.R;
import com.example.gamigosjava.data.api.BGGService;
import com.example.gamigosjava.data.api.BGG_API;
import com.example.gamigosjava.data.api.BGGMappers;
import com.example.gamigosjava.data.model.BGGItem;
import com.example.gamigosjava.data.model.GameSummary;
import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;
import com.example.gamigosjava.ui.adapter.GameAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BGGTestActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GameAdapter gameAdapter;
    private BGG_API api;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bggtest);

        // RecyclerView + Adapter
        recyclerView = findViewById(R.id.recyclerViewGames);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        gameAdapter = new GameAdapter();
        recyclerView.setAdapter(gameAdapter);

        // Fake Item to Debug RecyclerView
//        List<GameSummary> fake = new ArrayList<>();
//        fake.add(new GameSummary("test","Fake Game","",2,4,45));
//        gameAdapter.setItems(fake);


        // API
        api = BGGService.getInstance();

        // Test: search → thing → map → display
        fetchGamesForQuery("catan"); // should return various Catan games
//        fetchGamesForQuery("azul"); // should return various Azul games
//        fetchThingDetails("9209"); // returns Ticket to Ride

    }

    private void fetchGamesForQuery(String query) {
        api.search(query, "boardgame").enqueue(new Callback<SearchResponse>() {
            @Override public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().items == null || resp.body().items.isEmpty()) {
                    Toast.makeText(BGGTestActivity.this, "No search results", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Build CSV of a few IDs to batch the /thing call
                StringBuilder ids = new StringBuilder();
                int max = Math.min(10, resp.body().items.size());
                for (int i = 0; i < max; i++) {
                    if (i > 0) ids.append(',');
                    ids.append(resp.body().items.get(i).id);
                }
                fetchThingDetails(ids.toString());
            }

            @Override public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                Toast.makeText(BGGTestActivity.this, "Search failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchThingDetails(String idsCsv) {
        api.thing(idsCsv, 0).enqueue(new Callback<ThingResponse>() {
            @Override public void onResponse(Call<ThingResponse> call, Response<ThingResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().items == null) {
                    Toast.makeText(BGGTestActivity.this, "No details", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<GameSummary> list = new ArrayList<>();
                for (BGGItem it : resp.body().items) {
                    list.add(BGGMappers.toSummary(it));
                }

                // 🟢 Add this to confirm data size
                Toast.makeText(BGGTestActivity.this, "Loaded " + list.size() + " games", Toast.LENGTH_SHORT).show();
                Log.d("BGG", "Loaded " + list.size() + " game(s)"); // debug

                gameAdapter.setItems(list);
            }

            @Override public void onFailure(Call<ThingResponse> call, Throwable t) {
                Toast.makeText(BGGTestActivity.this, "Thing failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
