
package com.playertags;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class SharedTagService
{
    private static final String SUPABASE_URL =
            "https://susdisuddoacxdazxhne.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ODJQK7U9lRNx6bv7bBkjpA_GibgA3ib";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
                StandardCharsets.UTF_8);

        String url = SUPABASE_URL + "/rest/v1/player_tags"
                + "?select=player_name,tag,pronouns"
                + "&player_name=eq." + name;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .GET()
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
        {
            throw new IllegalStateException(
                    "Shared lookup failed: HTTP "
                            + response.statusCode()
                            + " - " + response.body());
        }

        JsonArray results =
                new JsonParser().parse(response.body()).getAsJsonArray();

        if (results.size() == 0)
        {
            return null;
        }

        return results.get(0).getAsJsonObject();
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header(
                        "Prefer",
                        "resolution=merge-duplicates,return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(
                        body.toString(),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200
                && response.statusCode() != 201
                && response.statusCode() != 204)
        {
            throw new IllegalStateException(
                    "Publishing failed: HTTP "
                            + response.statusCode()
                            + " - " + response.body());
        }
    }
}
