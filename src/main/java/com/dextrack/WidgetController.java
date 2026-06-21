package com.dextrack;

import com.dextrack.api.DexcomAuth;
import com.dextrack.api.DexcomClient;
import com.dextrack.api.DexcomClient.UnauthorizedException;
import com.dextrack.model.GlucoseReading;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WidgetController {

    @FXML private StackPane rootPane;
    @FXML private Label glucoseLabel;
    @FXML private Label trendLabel;
    @FXML private Label tirLabel;
    @FXML private Label timestampLabel;
    @FXML private Region glowRegion;

    private DexcomAuth auth;
    private DexcomClient client;
    private Stage stage;
    private App app;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private double dragOffsetX, dragOffsetY;

    private static final int LOW  = 70;
    private static final int HIGH = 180;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault());

    public void init(DexcomAuth auth, DexcomClient client, Stage stage, App app) {
        this.auth   = auth;
        this.client = client;
        this.stage  = stage;
        this.app    = app;

        setupDrag();
        setupContextMenu();

        glucoseLabel.setText("...");
        tirLabel.setText("Fetching…");

        fetchAndUpdate();
        scheduler.scheduleAtFixedRate(this::fetchAndUpdate, 5, 5, TimeUnit.MINUTES);
    }

    private void setupDrag() {
        rootPane.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        rootPane.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
    }

    private void setupContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem refresh = new MenuItem("↻  Refresh");
        refresh.setOnAction(e -> fetchAndUpdate());

        MenuItem logout = new MenuItem("Logout");
        logout.setStyle("-fx-text-fill: #e74c3c;");
        logout.setOnAction(e -> {
            scheduler.shutdownNow();
            auth.clearCredentials();
            stage.close();
            try { app.showLogin(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
        });

        menu.getItems().addAll(refresh, new SeparatorMenuItem(), logout);
        rootPane.setOnContextMenuRequested(e ->
                menu.show(rootPane, e.getScreenX(), e.getScreenY()));
    }

    private void fetchAndUpdate() {
        new Thread(() -> {
            try {
                Instant now = Instant.now();
                List<GlucoseReading> readings = client.getReadings(now.minus(24, ChronoUnit.HOURS), now);

                if (readings.isEmpty()) {
                    Platform.runLater(() -> showError("No recent readings found."));
                    return;
                }

                GlucoseReading latest = readings.get(readings.size() - 1);
                double tir = computeTir(readings);
                Platform.runLater(() -> applyReading(latest, tir));

            } catch (UnauthorizedException e) {
                Platform.runLater(this::forceReLogin);
            } catch (Throwable e) {
                Platform.runLater(() -> showError(e.getMessage() != null ? e.getMessage() : e.toString()));
            }
        }).start();
    }

    private void applyReading(GlucoseReading reading, double tir) {
        int val = reading.getValue();
        glucoseLabel.setText(String.valueOf(val));
        trendLabel.setText(reading.getTrend().arrow);
        tirLabel.setText(String.format("TIR %.0f%%", tir));
        timestampLabel.setText(TIME_FMT.format(reading.getTimestamp()));

        String color, glowColor, glowFill;
        if (val < LOW) {
            color     = "#e74c3c";
            glowColor = "rgba(231,76,60,0.9)";
            glowFill  = "rgba(231,76,60,0.08)";
        } else if (val > HIGH) {
            color     = "#f39c12";
            glowColor = "rgba(243,156,18,0.9)";
            glowFill  = "rgba(243,156,18,0.08)";
        } else {
            color     = "#2ecc71";
            glowColor = "rgba(46,204,113,0.85)";
            glowFill  = "rgba(46,204,113,0.08)";
        }

        glucoseLabel.setStyle("-fx-text-fill: " + color + ";");
        trendLabel.setStyle("-fx-text-fill: " + color + ";");
        glowRegion.setStyle(
            "-fx-background-color: " + glowFill + ";" +
            "-fx-background-radius: 108;" +
            "-fx-effect: dropshadow(gaussian, " + glowColor + ", 50, 0.7, 0, 0);"
        );
    }

    private void showError(String msg) {
        glucoseLabel.setText("!");
        glucoseLabel.setStyle("-fx-text-fill: #e74c3c;");
        trendLabel.setText("");
        tirLabel.setText("Error");
        String display = msg != null && msg.length() > 60 ? msg.substring(0, 57) + "…" : msg;
        timestampLabel.setText(display);
        timestampLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 9px;");
    }

    private void forceReLogin() {
        scheduler.shutdownNow();
        auth.clearCredentials();
        stage.close();
        try { app.showLogin(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
    }

    private double computeTir(List<GlucoseReading> readings) {
        long inRange = readings.stream()
                .filter(r -> r.getValue() >= LOW && r.getValue() <= HIGH)
                .count();
        return readings.isEmpty() ? 0 : (inRange * 100.0 / readings.size());
    }
}
