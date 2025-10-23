package com.example.gamigosjava.data.api;

import com.example.gamigosjava.data.model.ThingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BGG_API {
    @GET("thing")
    Call<ThingResponse> thing(
            @Query("id") String idsCsv,   // e.g., "13,9209"
            @Query("stats") int stats     // 0 or 1 — we don't need stats for these fields
    );
}
