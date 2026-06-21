package com.dextrack;

import com.dextrack.api.DexcomAuth;
import com.dextrack.api.DexcomClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    @Override
    public void start(Stage loginStage) throws Exception {
        showLogin(loginStage);
    }

    public void showLogin(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dextrack/login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/com/dextrack/style.css").toExternalForm());

        LoginController ctrl = loader.getController();
        ctrl.setOnLoginSuccess((auth, client) -> {
            stage.close();
            try {
                showWidget(auth, client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        stage.setTitle("DexTrack");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    private void showWidget(DexcomAuth auth, DexcomClient client) throws Exception {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dextrack/widget.fxml"));
        Scene scene = new Scene(loader.load(), 300, 300);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("/com/dextrack/style.css").toExternalForm());

        WidgetController ctrl = loader.getController();
        ctrl.init(auth, client, stage, this);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
