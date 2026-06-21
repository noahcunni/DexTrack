package com.dextrack.api;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class CallbackServer {

    private HttpServer server;
    private final CompletableFuture<String> codeFuture = new CompletableFuture<>();

    public CompletableFuture<String> start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(9000), 0);
        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = null;
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("code=")) {
                        code = param.substring(5);
                        break;
                    }
                }
            }
            String html = "<html><body><h2>Login successful! You can close this tab.</h2></body></html>";
            byte[] bytes = html.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().close();

            if (code != null) codeFuture.complete(code);
            else codeFuture.completeExceptionally(new RuntimeException("No code in callback"));

            server.stop(1);
        });
        server.start();
        return codeFuture;
    }

    public void stop() {
        if (server != null) {
            try { server.stop(0); } catch (Exception ignored) {}
            server = null;
        }
    }
}
