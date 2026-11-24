package com.gharbaar.ui;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.BusinessException;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * =====================================================================
 * [UI] LoginView  -  UC-01 Register, UC-02 Login
 * Register offers TWO separate account types: Customer and Agent.
 * =====================================================================
 */
public class LoginView {

    private final AppContext app = AppContext.get();
    private final StackPane formHost = new StackPane();

    public Parent build() {
        // ---------- [LEFT] brand panel ----------
        ImageView hero = UiKit.imageView("https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=900&q=80", 420, 260, 22);
        VBox left = new VBox(18,
                UiKit.label("GharBaar", "login-title"),
                UiKit.wrapLabel("Find your home where you want to live.\nVerified rentals across Islamabad's CDA sectors.", "login-sub"),
                hero,
                bullet("Every listing verified by our Admin team"),
                bullet("Real tenant history: were the facilities really provided?"),
                bullet("Request visits and talk to agents directly"));
        left.getStyleClass().add("login-left");
        left.setPadding(new Insets(60, 50, 50, 60));
        left.setPrefWidth(560);
        left.setAlignment(Pos.CENTER_LEFT);

        // ---------- [RIGHT] login / register tabs ----------
        ToggleGroup tabs = new ToggleGroup();
        ToggleButton loginTab = tab("Login", tabs);
        ToggleButton registerTab = tab("Register", tabs);
        HBox tabBar = new HBox(4, loginTab, registerTab);
        tabBar.getStyleClass().add("tab-switch");
        tabBar.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        loginTab.setSelected(true);
        formHost.getChildren().setAll(loginForm());
        tabs.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            formHost.getChildren().setAll(b == loginTab ? loginForm() : registerForm());
        });

        VBox right = new VBox(26, tabBar, formHost);
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(40));
        right.setMaxWidth(460);
        StackPane rightWrap = new StackPane(right);
        HBox.setHgrow(rightWrap, Priority.ALWAYS);

        HBox root = new HBox(left, rightWrap);
        StackPane toastHost = new StackPane(root);
        UiKit.setToastHost(toastHost);
        return toastHost;
    }

    private HBox bullet(String text) {
        HBox h = new HBox(10, Icons.of(Icons.CHECK, 18, "#ffffff"), UiKit.label(text, "login-sub"));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private ToggleButton tab(String text, ToggleGroup g) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().setAll("tab-btn");
        b.setToggleGroup(g);
        return b;
    }

    // ---------------------------------------------------------------
    // [UC-02] Login form
    // ---------------------------------------------------------------
    private VBox loginForm() {
        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        Label error = UiKit.wrapLabel("", "error-text");

        Button login = new Button("Login");
        login.getStyleClass().add("btn-primary");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);
        login.setOnAction(e -> {
            error.setText("");
            login.setDisable(true);
            UiKit.async(() -> app.auth.login(username.getText(), password.getText()),
                    home -> Navigator.showHome(),
                    () -> login.setDisable(false));
        });

        VBox v = new VBox(16,
                UiKit.label("Welcome back", "h1"),
                UiKit.label("Log in as a Customer, Agent or Admin.", "muted"),
                UiKit.field("Username", username),
                UiKit.field("Password", password),
                error, login,
                UiKit.wrapLabel("Demo: customer_sara / agent_mahmud / tenant_bilal  (password Demo@1234)", "muted", "small"));
        v.getStyleClass().add("card");
        v.setPadding(new Insets(30));
        return v;
    }

    // ---------------------------------------------------------------
    // [UC-01] Register form - Customer OR Agent account
    // ---------------------------------------------------------------
    private VBox registerForm() {
        ToggleGroup roleGroup = new ToggleGroup();
        ToggleButton customer = roleCard("Customer", "Search, visit and rent homes", Icons.SEARCH, roleGroup);
        ToggleButton agent = roleCard("Agent", "List your properties for rent", Icons.HOME, roleGroup);
        customer.setSelected(true);
        roleGroup.selectedToggleProperty().addListener((o, a, b) -> { if (b == null) a.setSelected(true); });
        HBox roles = new HBox(12, customer, agent);
        HBox.setHgrow(customer, Priority.ALWAYS);
        HBox.setHgrow(agent, Priority.ALWAYS);

        TextField name = new TextField();      name.setPromptText("e.g. Ali Khan");
        TextField username = new TextField();  username.setPromptText("letters, digits, _");
        TextField phone = new TextField();     phone.setPromptText("03001234567");
        PasswordField password = new PasswordField(); password.setPromptText("min 8 chars, letters + numbers");
        PasswordField confirm = new PasswordField();  confirm.setPromptText("re-enter password");

        Button register = new Button("Create account");
        register.getStyleClass().add("btn-primary");
        register.setMaxWidth(Double.MAX_VALUE);
        register.setOnAction(e -> {
            if (!password.getText().equals(confirm.getText())) {
                UiKit.showError(new BusinessException("Passwords do not match."));
                return;
            }
            String role = customer.isSelected() ? "CUSTOMER" : "AGENT";
            register.setDisable(true);
            UiKit.async(() -> app.auth.register(username.getText(), password.getText(), name.getText(), phone.getText(), role),
                    u -> {
                        UiKit.info("Account created", "Your " + (role.equals("AGENT") ? "Agent" : "Customer")
                                + " account is ready. You can now log in as " + u.getUsername() + ".");
                        formHost.getChildren().setAll(loginForm());
                    },
                    () -> register.setDisable(false));
        });

        HBox row1 = new HBox(12, grow(UiKit.field("Full name", name)), grow(UiKit.field("Phone", phone)));
        HBox row2 = new HBox(12, grow(UiKit.field("Password", password)), grow(UiKit.field("Confirm", confirm)));
        VBox v = new VBox(14,
                UiKit.label("Create your account", "h1"),
                UiKit.label("Choose the type of account you want:", "muted"),
                roles, row1, UiKit.field("Username", username), row2, register);
        v.getStyleClass().add("card");
        v.setPadding(new Insets(28));
        return v;
    }

    private VBox grow(VBox v) { HBox.setHgrow(v, Priority.ALWAYS); return v; }

    private ToggleButton roleCard(String title, String sub, String icon, ToggleGroup g) {
        VBox content = new VBox(6, Icons.of(icon, 22, "#2f80ed"), UiKit.label(title, "h3"), UiKit.wrapLabel(sub, "muted", "small"));
        ToggleButton b = new ToggleButton();
        b.setGraphic(content);
        b.getStyleClass().setAll("role-card");
        b.setToggleGroup(g);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.TOP_LEFT);
        return b;
    }
}
