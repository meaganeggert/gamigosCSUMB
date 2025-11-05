
package com.example.gamigosjava.data.api;

import com.example.gamigosjava.data.model.SearchResponse;
import com.example.gamigosjava.data.model.ThingResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BGG_API {

    // Find game IDs by text
//    @GET("search")
    @GET("bggSearch")
    Call<SearchResponse> search(
            @Query("query") String query,
            @Query("type") String type // "boardgame"
    );

    // Fetch details for one or more game IDs
//    @GET("thing")
    @GET("bggThing")
    Call<ThingResponse> thing(
            @Query("id") String idsCsv, // example: "9209,13"
            @Query("stats") int stats
    );
}
