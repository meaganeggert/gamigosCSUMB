package com.example.gamigosjava.data.api;

import com.tickaroo.tikxml.TikXml;
import com.tickaroo.tikxml.retrofit.TikXmlConverterFactory;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class BGGService {
    private static final String BASE_URL = "https://boardgamegeek.com/xmlapi2/";
    private static BGG_API api;

    public static BGG_API getInstance() {
        if (api == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    // you can add logging, caching, etc. here later
                    .build();

            // TikXml parser configuration
            TikXml tikXml = new TikXml.Builder()
                    .exceptionOnUnreadXml(false) // ignore unknown tags instead of crashing
                    .build();

            // Retrofit setup using TikXml
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(TikXmlConverterFactory.create(tikXml))
                    .client(client)
                    .build();

            api = retrofit.create(BGG_API.class);
        }
        return api;
    }
}
