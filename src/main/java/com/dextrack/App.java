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
        // Loader knows where fxml is
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/dextrack/login.fxml"));

        // Parses the xml and makes scene object with it.
        /*
        Loader takes 5 steps...
            1. Instantiate the controller by fx:controller
            2. Parse fxml components as javaFX objects
            3. Inject new objects into controller by fx:id
            4. Binds event handlers through event="#..."
            5. Runs initialize          
         */
        Scene scene = new Scene(loader.load());

        // Style the scene
        scene.getStylesheets().add(getClass().getResource("/com/dextrack/style.css").toExternalForm());

        // loader has reference to controller from fx:controller step
        LoginController ctrl = loader.getController();

        
        ctrl.setOnLoginSuccess((auth, client) -> {
            stage.close();
            try {
                showWidget(auth, client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // configure stage
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
