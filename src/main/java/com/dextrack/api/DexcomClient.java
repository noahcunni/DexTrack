package com.dextrack.api;

import com.dextrack.model.GlucoseReading;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DexcomClient {

    private static final DateTimeFormatter ISO = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private final DexcomAuth auth;
    private final HttpClient http = HttpClient.newHttpClient();

    public DexcomClient(DexcomAuth auth) {
        this.auth = auth;
    }

    public List<GlucoseReading> getReadings(Instant startTime, Instant endTime) throws Exception {
        String token = auth.getValidAccessToken();
        String url = auth.getBaseUrl() + "/v3/users/self/egvs"
                + "?startDate=" + ISO.format(startTime)
                + "&endDate=" + ISO.format(endTime);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException();
        }
        if (resp.statusCode() != 200) {
            throw new RuntimeException("API error " + resp.statusCode() + ": " + resp.body());
        }

        return parseReadings(resp.body());
    }

    private List<GlucoseReading> parseReadings(String json) {
        List<GlucoseReading> readings = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray records = root.getAsJsonArray("records");
        if (records == null) return readings;

        for (var el : records) {
            JsonObject obj = el.getAsJsonObject();
            int value = obj.get("value").getAsInt();
            Instant ts = Instant.parse(obj.get("systemTime").getAsString());
            String trendStr = obj.has("trend") ? obj.get("trend").getAsString() : "NONE";
            GlucoseReading.Trend trend = parseTrend(trendStr);
            readings.add(new GlucoseReading(value, ts, trend));
        }
        return readings;
    }

    private GlucoseReading.Trend parseTrend(String s) {
        try {
            return GlucoseReading.Trend.valueOf(s);
        } catch (IllegalArgumentException e) {
            return GlucoseReading.Trend.NONE;
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException() { super("Session expired. Please log in again."); }
    }
}
