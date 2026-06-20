package com.tubedownload.shazamapi.shazam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tubedownload.shazamapi.signature.DecodedMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ShazamClient {
    private static final String API_URL_TEMPLATE = "https://amp.shazam.com/discovery/v5/%s/%s/iphone/-/tag/%s/%s";
    private static final Map<String, String> PARAMS = Map.ofEntries(
            Map.entry("sync", "true"),
            Map.entry("webv3", "true"),
            Map.entry("sampling", "true"),
            Map.entry("connected", ""),
            Map.entry("shazamapiversion", "v3"),
            Map.entry("sharehub", "true"),
            Map.entry("hubv5minorversion", "v5.1"),
            Map.entry("hidelb", "true"),
            Map.entry("video", "v3")
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ShazamConfig config;

    public JsonNode recognize(DecodedMessage signature) throws IOException, InterruptedException {
        Map<String, Object> signatureBody = new LinkedHashMap<>();
        signatureBody.put("uri", signature.encodeToUri());
        signatureBody.put("samplems", (int) (signature.numberSamples() / (double) signature.sampleRateHz() * 1000));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timezone", config.timezone());
        body.put("signature", signatureBody);
        body.put("timestamp", Instant.now().toEpochMilli());
        body.put("context", Map.of());
        body.put("geolocation", Map.of());

        HttpRequest request = HttpRequest.newBuilder(uri())
                .header("X-Shazam-Platform", "IPHONE")
                .header("X-Shazam-AppVersion", "14.1.0")
                .header("Accept", "*/*")

                //QUANDO DER ERRO 429 ALTERAR SHAZAM/XXXX E CFNETWORK/XXXX
                .header("User-Agent", "Shazam/3688 CFNetwork/1199 Darwin/20.0.0")
                .header("Accept-Language", config.lang())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Shazam returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private URI uri() {
        String url = API_URL_TEMPLATE.formatted(
                config.lang(),
                config.region(),
                UUID.randomUUID().toString().toUpperCase(),
                UUID.randomUUID().toString().toUpperCase()
        );
        return URI.create(url + "?" + queryString());
    }

    private String queryString() {
        StringBuilder builder = new StringBuilder();
        PARAMS.forEach((key, value) -> {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encode(key)).append('=').append(encode(value));
        });
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
