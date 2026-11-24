package com.gharbaar.ui.common;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.BusinessException;
import com.gharbaar.db.DataAccessException;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI] UiKit - small reusable UI helpers used by every screen
 *   - async(...)     run DB work off the UI thread + show errors
 *   - alerts/toasts  user feedback
 *   - chip/badge/... styled controls
 *   - loadImage(...) progressive image loading (UC-10 special req.)
 * =====================================================================
 */
public final class UiKit {

    private UiKit() { }

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();
    private static StackPane toastHost;

    // ---------------------------------------------------------------
    // [ASYNC] run work in background, then update UI; errors -> alert
    // ---------------------------------------------------------------
    public static <T> void async(Supplier<T> work, Consumer<T> onSuccess) {
        async(work, onSuccess, null);
    }

    public static <T> void async(Supplier<T> work, Consumer<T> onSuccess, Runnable onError) {
        Task<T> task = new Task<>() {
            @Override protected T call() { return work.get(); }
        };
        task.setOnSucceeded(e -> { if (onSuccess != null) onSuccess.accept(task.getValue()); });
        task.setOnFailed(e -> {
            showError(task.getException());
            if (onError != null) onError.run();
        });
        Thread t = new Thread(task, "gharbaar-worker");
        t.setDaemon(true);
        t.start();
    }

    /** Runs an action and shows a success toast. */
    public static void action(Runnable work, String successMessage, Runnable after) {
        async(() -> { work.run(); return Boolean.TRUE; }, ok -> {
            if (successMessage != null) toast(successMessage);
            if (after != null) after.run();
        });
    }

    // ---------------------------------------------------------------
    // [FEEDBACK] errors, confirmations, toasts
    // ---------------------------------------------------------------
    public static void showError(Throwable ex) {
        String msg;
        if (ex instanceof BusinessException be) msg = String.join("\n", be.getErrors());
        else if (ex instanceof DataAccessException) msg = ex.getMessage() + "\nPlease try again.";   // UC-09 ext 4b: retry
        else { msg = "Unexpected error: " + ex.getMessage(); ex.printStackTrace(); }
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(ex instanceof BusinessException ? "Please check the details" : "Something went wrong");
        style(a);
        a.showAndWait();
    }

    public static void info(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(header);
        style(a);
        a.showAndWait();
    }

    public static boolean confirm(String header, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(header);
        style(a);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    /** Asks for a reason text (reject / delete). Returns empty if cancelled. */
    public static Optional<String> askReason(String header, String prompt) {
        TextInputDialog d = new TextInputDialog();
        d.setHeaderText(header);
        d.setContentText(prompt);
        d.setTitle("GharBaar");
        d.getDialogPane().getStylesheets().add(stylesheet());
        d.getEditor().setPrefWidth(360);
        return d.showAndWait().map(String::trim);
    }

    public static void style(Alert a) {
        a.setTitle("GharBaar");
        a.getDialogPane().getStylesheets().add(stylesheet());
        a.getDialogPane().setMinWidth(420);
    }

    public static String stylesheet() {
        return UiKit.class.getResource("/com/gharbaar/ui/styles.css").toExternalForm();
    }

    public static void setToastHost(StackPane host) { toastHost = host; }

    public static void toast(String message) {
        if (toastHost == null) { info("Done", message); return; }
        Label l = new Label(message);
        l.getStyleClass().add("toast");
        l.setStyle("-fx-text-fill: white;");
        StackPane.setAlignment(l, Pos.BOTTOM_CENTER);
        StackPane.setMargin(l, new Insets(0, 0, 30, 0));
        toastHost.getChildren().add(l);
        PauseTransition p = new PauseTransition(Duration.seconds(3));
        p.setOnFinished(e -> toastHost.getChildren().remove(l));
        p.play();
    }

    // ---------------------------------------------------------------
    // [CONTROLS] labels, chips, badges, buttons
    // ---------------------------------------------------------------
    public static Label label(String text, String... styles) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styles);
        return l;
    }

    public static Label wrapLabel(String text, String... styles) {
        Label l = label(text, styles);
        l.setWrapText(true);
        return l;
    }

    /** Squircle filter chip (toggle). */
    public static ToggleButton chip(String text) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().setAll("chip");
        b.setFocusTraversable(false);
        return b;
    }

    public static Button button(String text, String style, Runnable onClick) {
        Button b = new Button(text);
        b.getStyleClass().add(style);
        b.setOnAction(e -> onClick.run());
        return b;
    }

    public static Button iconButton(String icon, String tooltip, Runnable onClick) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, 18, "#2f80ed"));
        b.getStyleClass().setAll("icon-btn");
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        b.setOnAction(e -> onClick.run());
        return b;
    }

    public static Label badge(String text, String color) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", "badge-" + color);
        return l;
    }

    /** Colour for every status enum in the system. */
    public static Label statusBadge(Enum<?> status) {
        String c = switch (status.name()) {
            case "VERIFIED", "AVAILABLE", "ACCEPTED", "RESOLVED" -> "green";
            case "PENDING", "PENDING_VERIFICATION", "OPEN", "UNDER_MAINTENANCE" -> "amber";
            case "REJECTED", "DECLINED" -> "red";
            case "OCCUPIED" -> "gray";
            default -> "blue";
        };
        String text = status.name().equals("PENDING_VERIFICATION") ? "Pending Verification"
                : status.name().replace('_', ' ').charAt(0) + status.name().replace('_', ' ').substring(1).toLowerCase();
        return badge(text, c);
    }

    public static VBox field(String label, Node input) {
        VBox v = new VBox(6, label(label, "field-label"), input);
        return v;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static VBox card(Node... children) {
        VBox v = new VBox(12, children);
        v.getStyleClass().add("card");
        return v;
    }

    public static Node loading() {
        ProgressIndicator p = new ProgressIndicator();
        p.setMaxSize(42, 42);
        StackPane s = new StackPane(p);
        s.setPadding(new Insets(40));
        return s;
    }

    public static Node emptyState(String title, String sub) {
        VBox v = new VBox(6, label(title, "h3"), wrapLabel(sub, "muted"));
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(50));
        return v;
    }

    public static String pkr(double amount) {
        return "PKR " + NumberFormat.getIntegerInstance(Locale.US).format(Math.round(amount));
    }

    public static String initials(String name) {
        String[] p = name.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    public static Label avatar(String name, double size) {
        Label a = new Label(initials(name));
        a.getStyleClass().add("avatar");
        a.setMinSize(size, size);
        a.setPrefSize(size, size);
        a.setStyle("-fx-background-radius: " + size / 2 + "; -fx-text-fill: white;");
        return a;
    }

    // ---------------------------------------------------------------
    // [IMAGES] progressive loading (web URL or "db:<id>" stored file)
    // ---------------------------------------------------------------
    public static ImageView imageView(String path, double w, double h, double radius) {
        ImageView iv = new ImageView();
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        iv.setClip(clip);
        setImage(iv, path, w * 1.5);
        return iv;
    }

    /** Image that stretches to the width of its parent (used for the big gallery photo). */
    public static Region fluidImage(String path, double height, double radius, ImageView[] out) {
        ImageView iv = new ImageView();
        iv.setPreserveRatio(false);
        iv.setSmooth(true);
        iv.setManaged(false);
        iv.setFitHeight(height);
        Rectangle clip = new Rectangle(0, height);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        iv.setClip(clip);
        javafx.scene.layout.Pane holder = new javafx.scene.layout.Pane(iv);
        holder.setMinWidth(0);
        holder.setPrefWidth(200);
        holder.setMinHeight(height);
        holder.setPrefHeight(height);
        iv.fitWidthProperty().bind(holder.widthProperty());
        clip.widthProperty().bind(holder.widthProperty());
        setImage(iv, path, 1100);
        if (out != null && out.length > 0) out[0] = iv;
        return holder;
    }

    public static void setImage(ImageView iv, String path, double reqWidth) {
        if (path == null) return;
        Image cached = IMAGE_CACHE.get(path);
        if (cached != null) { iv.setImage(cached); return; }
        if (path.startsWith("http")) {
            String url = path.contains("unsplash.com") ? path.replaceAll("w=\\d+", "w=" + (int) Math.max(400, reqWidth)) : path;
            Image img = new Image(url, true);               // background loading = progressive
            IMAGE_CACHE.put(path, img);
            iv.setImage(img);
        } else {
            async(() -> AppContext.get().listings.loadImage(path), bytes -> {
                if (bytes == null) return;
                Image img = new Image(new ByteArrayInputStream(bytes));
                IMAGE_CACHE.put(path, img);
                iv.setImage(img);
            });
        }
    }

    public static void later(Runnable r) { Platform.runLater(r); }

    public static Window activeWindow() {
        return Window.getWindows().stream().filter(Window::isFocused).findFirst().orElse(null);
    }
}
