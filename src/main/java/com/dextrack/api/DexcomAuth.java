package com.dextrack.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

import com.dextrack.api.DexcomClient.UnauthorizedException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DexcomAuth {

    // Set SANDBOX = false and fill in real credentials for production use
    public static final boolean SANDBOX = false;

    private static final String BASE_URL = SANDBOX
            ? "https://sandbox-api.dexcom.com"
            : "https://api.dexcom.com";

    private static final String AUTH_URL = BASE_URL + "/v2/oauth2/login";
    private static final String TOKEN_URL = BASE_URL + "/v2/oauth2/token";
    private static final String REDIRECT_URI = "http://localhost:9000/callback";

    private static final Preferences PREFS = Preferences.userNodeForPackage(DexcomAuth.class);

    private final String clientId;
    private final String clientSecret;
    private final HttpClient http = HttpClient.newHttpClient();

    private String accessToken;
    private String refreshToken;
    private long tokenExpiryEpoch;

    public DexcomAuth(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = PREFS.get("refresh_token", null);
        this.accessToken = PREFS.get("access_token", null);
        this.tokenExpiryEpoch = PREFS.getLong("token_expiry", 0);
    }

    public String getAuthorizationUrl() {
        return AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(REDIRECT_URI)
                + "&response_type=code"
                + "&scope=offline_access";
    }

    public void exchangeCode(String code) throws Exception {
        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&code=" + encode(code)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(REDIRECT_URI);
        fetchTokens(body);
    }

    public String getValidAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryEpoch - 60_000) {
            return accessToken;
        }
        if (refreshToken != null) {
            refresh();
            return accessToken;
        }
        throw new IllegalStateException("Not authenticated. Please log in.");
    }

    private void refresh() throws Exception {
        String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken)
                + "&grant_type=refresh_token"
                + "&redirect_uri=" + encode(REDIRECT_URI);
        fetchTokens(body);
    }

    private void fetchTokens(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            clearCredentials();
            throw new UnauthorizedException();
        }
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Token request failed: " + resp.body());
        }

        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
        accessToken = json.get("access_token").getAsString();
        refreshToken = json.get("refresh_token").getAsString();
        long expiresIn = json.get("expires_in").getAsLong();
        tokenExpiryEpoch = System.currentTimeMillis() + expiresIn * 1000;

        PREFS.put("access_token", accessToken);
        PREFS.put("refresh_token", refreshToken);
        PREFS.putLong("token_expiry", tokenExpiryEpoch);
    }

    public boolean hasStoredCredentials() {
        return refreshToken != null;
    }

    public void clearCredentials() {
        PREFS.remove("access_token");
        PREFS.remove("refresh_token");
        PREFS.remove("token_expiry");
        accessToken = null;
        refreshToken = null;
        tokenExpiryEpoch = 0;
    }

    public String getBaseUrl() { return BASE_URL; }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
