package com.tubedownload.apiyoutubeoficial;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class YouTubeOfficialClient {

    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/playlistItems";
    private static final int MAX_RESULTS = 50;

    private final HttpClient httpClient;

    @Inject
    public YouTubeOfficialClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<String> listVideoUrlsFromPlaylist(String playlistUrl) throws IOException, InterruptedException {
        String playlistId = extractPlaylistId(playlistUrl);
        ArrayList<String> videoUrls = new ArrayList<>();
        String pageToken = null;

        do {
            JSONObject response = fetchPlaylistItemsPage(playlistId, pageToken);
            JSONArray items = response.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject snippet = item.optJSONObject("snippet");
                    if (snippet == null) {
                        continue;
                    }
                    JSONObject resourceId = snippet.optJSONObject("resourceId");
                    if (resourceId == null) {
                        continue;
                    }
                    String videoId = resourceId.optString("videoId", null);
                    if (videoId != null && !videoId.isBlank()) {
                        videoUrls.add("https://www.youtube.com/watch?v=" + videoId);
                    }
                }
            }
            pageToken = response.optString("nextPageToken", null);
            if (pageToken != null && pageToken.isBlank()) {
                pageToken = null;
            }
        } while (pageToken != null);

        return videoUrls;
    }

    private JSONObject fetchPlaylistItemsPage(String playlistId, String pageToken) throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?part=snippet")
                .append("&maxResults=").append(MAX_RESULTS)
                .append("&playlistId=").append(encode(playlistId))
                .append("&key=").append(encode(requireApiKey()));
        if (pageToken != null && !pageToken.isBlank()) {
            url.append("&pageToken=").append(encode(pageToken));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url.toString()))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("YouTube API request failed with HTTP " + response.statusCode() + ": " + response.body());
        }
        return new JSONObject(response.body());
    }

    private static String extractPlaylistId(String playlistUrl) {
        int idx = playlistUrl.indexOf("list=");
        if (idx < 0) {
            throw new IllegalArgumentException("URL de playlist invalida: " + playlistUrl);
        }
        String playlistId = playlistUrl.substring(idx + 5);
        int amp = playlistId.indexOf('&');
        if (amp >= 0) {
            playlistId = playlistId.substring(0, amp);
        }
        if (playlistId.isBlank()) {
            throw new IllegalArgumentException("URL de playlist sem list=: " + playlistUrl);
        }
        return playlistId;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String requireApiKey() {
        String apiKey = ConfigProvider.getConfig().getOptionalValue("app.api.key", String.class).orElse("");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("API_KEY_YOUTUBE_OFICIAL");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("API_KEY_YOUTUBE_OFICIAL");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Chave da API nao configurada. Defina app.api.key ou API_KEY_YOUTUBE_OFICIAL.");
        }
        return apiKey;
    }
}
