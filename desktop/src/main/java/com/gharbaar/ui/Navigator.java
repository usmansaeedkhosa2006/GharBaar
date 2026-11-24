package com.gharbaar.ui;

import com.gharbaar.bl.service.Session;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * =====================================================================
 * [UI] Navigator - switches between the Login page and the main shell
 * and opens screens inside the shell.
 * =====================================================================
 */
public final class Navigator {

    private static Stage stage;
    private static MainShell shell;

    private Navigator() { }

    public static void init(Stage s) {
        stage = s;
        stage.setTitle("GharBaar - Rental Management System");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
    }

    private static void setRoot(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1320, 820);
            scene.getStylesheets().add(UiKit.stylesheet());
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
    }

    public static void showLogin() {
        shell = null;
        setRoot(new LoginView().build());
    }

    /** After login: open the home view chosen by User.getHomeView() (polymorphism). */
    public static void showHome() {
        shell = new MainShell(Session.getInstance().getCurrentUser());
        setRoot(shell.getRoot());
        shell.openHome();
    }

    public static void open(Screen screen) {
        if (shell != null) shell.open(screen);
    }

    public static void refreshBadge() {
        if (shell != null) shell.refreshNotificationCount();
    }

    public static Stage stage() { return stage; }
}
