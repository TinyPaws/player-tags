
package com.playertags;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.inject.Inject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SharedTagService
{
    private static final String SUPABASE_URL =
            "https://susdisuddoacxdazxhne.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ODJQK7U9lRNx6bv7bBkjpA_GibgA3ib";

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http;

    @Inject
    public SharedTagService(OkHttpClient http)
    {
        this.http = http;
    }

    /**
     * Retrieves a player's shared tag and pronouns.
     */
    public JsonObject getPlayerTag(String playerName) throws Exception
    {
        if (playerName == null || playerName.trim().isEmpty())
        {
            return null;
        }

        String name = URLEncoder.encode(
                playerName.trim().toLowerCase(Locale.ROOT),
                StandardCharsets.UTF_8.name());

        String url = SUPABASE_URL + "/rest/v1/player_tags"
                + "?select=player_name,tag,pronouns"
                + "&player_name=eq." + name;

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .get()
                .build();

        try (Response response = http.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                throw new IllegalStateException(
                        "Shared lookup failed: HTTP "
                                + response.code());
            }

            if (response.body() == null)
            {
                return null;
            }

            JsonArray results =
                    new JsonParser()
                            .parse(response.body().string())
                            .getAsJsonArray();

            if (results.size() == 0)
            {
                return null;
            }

            return results.get(0).getAsJsonObject();
        }
    }

    /**
     * Publishes or updates a player's shared tag and pronouns.
     */
    public void publishPlayerTag(
            String playerName,
            String tag,
            String pronouns) throws Exception
    {
        if (playerName == null || playerName.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                    "Player name is required");
        }

        playerName = playerName.trim().toLowerCase(Locale.ROOT);
        tag = tag == null ? "" : tag.trim();
        pronouns = pronouns == null ? "" : pronouns.trim();

        if (playerName.length() > 12
                || tag.length() > 50
                || pronouns.length() > 30)
        {
            throw new IllegalArgumentException(
                    "Player tag is too long");
        }

        JsonObject body = new JsonObject();
        body.addProperty("player_name", playerName);
        body.addProperty("tag", tag);
        body.addProperty("pronouns", pronouns);

        String url = SUPABASE_URL
                + "/rest/v1/player_tags?on_conflict=player_name";

        RequestBody requestBody =
                RequestBody.create(JSON, body.toString());

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header(
                        "Prefer",
                        "resolution=merge-duplicates,return=minimal")
                .post(requestBody)
                .build();

        try (Response response = http.newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                String error = response.body() == null
                        ? ""
                        : response.body().string();

                throw new IllegalStateException(
                        "Publishing failed: HTTP "
                                + response.code()
                                + " - " + error);
            }
        }
    }
}
