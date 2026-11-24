package com.gharbaar;

import com.gharbaar.ui.Navigator;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;

/**
 * =====================================================================
 * GharBaar Desktop - JavaFX entry point
 * Layers:
 *   com.gharbaar.ui  -> UI layer (JavaFX screens)
 *   com.gharbaar.bl  -> Business Logic (domain, controllers, services)
 *   com.gharbaar.db  -> Database layer (JDBC repositories)
 * =====================================================================
 */
public class App extends Application {

    private static HostServices hostServices;

    /** Used to open proof documents in the system viewer. */
    public static HostServices hostServices() { return hostServices; }

    @Override
    public void start(Stage stage) {
        hostServices = getHostServices();
        Navigator.init(stage);
        Navigator.showLogin();
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
