module com.dextrack {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires jdk.httpserver;
    requires java.net.http;
    requires java.desktop;
    requires java.prefs;

    opens com.dextrack to javafx.fxml;
    opens com.dextrack.model to com.google.gson;

    exports com.dextrack;
}
