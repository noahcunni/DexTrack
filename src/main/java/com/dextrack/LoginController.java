package com.dextrack;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.dextrack.api.CallbackServer;
import com.dextrack.api.DexcomAuth;
import com.dextrack.api.DexcomClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField clientIdField;
    @FXML private PasswordField clientSecretField;
    @FXML private TextField clientSecretVisible;
    @FXML private Button toggleBtn;
    @FXML private Label statusLabel;

    private boolean showingSecret = false;
    private BiConsumer<DexcomAuth, DexcomClient> onLoginSuccess;

    @FXML
    public void initialize() {
        clientSecretField.textProperty().addListener((obs, o, n) -> {
            if (!showingSecret) clientSecretVisible.setText(n);
        });
        clientSecretVisible.textProperty().addListener((obs, o, n) -> {
            if (showingSecret) clientSecretField.setText(n);
        });
    }

    public void setOnLoginSuccess(BiConsumer<DexcomAuth, DexcomClient> cb) {
        this.onLoginSuccess = cb;
    }

    @FXML
    private void onToggleSecret() {
        showingSecret = !showingSecret;
        if (showingSecret) {
            clientSecretVisible.setText(clientSecretField.getText());
            clientSecretVisible.setVisible(true);  clientSecretVisible.setManaged(true);
            clientSecretField.setVisible(false);    clientSecretField.setManaged(false);
            toggleBtn.setText("Hide");
            clientSecretVisible.requestFocus();
        } else {
            clientSecretField.setText(clientSecretVisible.getText());
            clientSecretField.setVisible(true);    clientSecretField.setManaged(true);
            clientSecretVisible.setVisible(false); clientSecretVisible.setManaged(false);
            toggleBtn.setText("Show");
            clientSecretField.requestFocus();
        }
    }

    @FXML
    private void onLogin() {
        String clientId     = clientIdField.getText().trim();
        String clientSecret = showingSecret ? clientSecretVisible.getText() : clientSecretField.getText();

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            setStatus("Enter your Client ID and Client Secret.", true);
            return;
        }

        DexcomAuth auth   = new DexcomAuth(clientId, clientSecret);
        DexcomClient client = new DexcomClient(auth);

        if (auth.hasStoredCredentials()) {
            onLoginSuccess.accept(auth, client);
            return;
        }

        setStatus("Opening browser for Dexcom login…", false);
        new Thread(() -> {
            CallbackServer cb = new CallbackServer();
            try {
                var codeFuture = cb.start();
                Desktop.getDesktop().browse(new URI(auth.getAuthorizationUrl()));
                String code = codeFuture.get(2, TimeUnit.MINUTES);
                auth.exchangeCode(code);
                Platform.runLater(() -> onLoginSuccess.accept(auth, client));
            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Login failed: " + e.getMessage(), true));
            } finally {
                cb.stop();
            }
        }).start();
    }

    private void setStatus(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #888;");
    }
}
