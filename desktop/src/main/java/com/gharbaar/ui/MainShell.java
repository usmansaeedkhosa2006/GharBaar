package com.gharbaar.ui;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.domain.User;
import com.gharbaar.ui.admin.AdminDashboardScreen;
import com.gharbaar.ui.admin.AllListingsScreen;
import com.gharbaar.ui.admin.ReportsScreen;
import com.gharbaar.ui.admin.UsersScreen;
import com.gharbaar.ui.admin.VerifyListingsScreen;
import com.gharbaar.ui.admin.VerifyTenantsScreen;
import com.gharbaar.ui.agent.ListingFormScreen;
import com.gharbaar.ui.agent.MyListingsScreen;
import com.gharbaar.ui.agent.VisitRequestsScreen;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.NotificationsScreen;
import com.gharbaar.ui.common.ReportIssueScreen;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import com.gharbaar.ui.customer.MyActivityScreen;
import com.gharbaar.ui.customer.SearchScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI] MainShell - sidebar + top bar + content area.
 * The sidebar menu depends on the role (Customer / Agent / Admin).
 * =====================================================================
 */
public class MainShell {

    private final User user;
    private final AppContext app = AppContext.get();
    private final BorderPane frame = new BorderPane();
    private final StackPane root = new StackPane(frame);
    private final ScrollPane content = new ScrollPane();
    private final Label pageTitle = UiKit.label("", "h2");
    private final Label bellCount = UiKit.label("", "badge-count");
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private final Map<String, Supplier<Screen>> menu = new LinkedHashMap<>();

    public MainShell(User user) {
        this.user = user;
        buildMenu();
        frame.setLeft(sidebar());
        frame.setTop(topbar());
        content.setFitToWidth(true);
        content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        frame.setCenter(content);
        UiKit.setToastHost(root);
        refreshNotificationCount();
    }

    // ---------------------------------------------------------------
    // [MENU] role based navigation (uses User.getHomeView polymorphism)
    // ---------------------------------------------------------------
    private void buildMenu() {
        switch (user.getHomeView()) {
            case "AgentHome" -> {
                menu.put("My Listings", MyListingsScreen::new);
                menu.put("Add Listing", () -> new ListingFormScreen(null));
                menu.put("Visit Requests", VisitRequestsScreen::new);
            }
            case "AdminHome" -> {
                menu.put("Dashboard", AdminDashboardScreen::new);
                menu.put("Verify Listings", VerifyListingsScreen::new);
                menu.put("Verify Tenants", VerifyTenantsScreen::new);
                menu.put("Reports", ReportsScreen::new);
                menu.put("Users", UsersScreen::new);
                menu.put("All Listings", AllListingsScreen::new);
            }
            default -> {
                menu.put("Find Home", SearchScreen::new);
                menu.put("My Activity", MyActivityScreen::new);
            }
        }
        menu.put("Notifications", NotificationsScreen::new);
        if (!user.getHomeView().equals("AdminHome")) menu.put("Report Issue", ReportIssueScreen::new);
    }

    private String iconFor(String key) {
        return switch (key) {
            case "Find Home" -> Icons.HOME;
            case "My Activity" -> Icons.CALENDAR;
            case "My Listings", "All Listings" -> Icons.LIST;
            case "Add Listing" -> Icons.PLUS;
            case "Visit Requests" -> Icons.CALENDAR;
            case "Dashboard" -> Icons.GRID;
            case "Verify Listings" -> Icons.SHIELD;
            case "Verify Tenants" -> Icons.USER;
            case "Reports", "Report Issue" -> Icons.FLAG;
            case "Users" -> Icons.USERS;
            case "Notifications" -> Icons.BELL;
            default -> Icons.HOME;
        };
    }

    private Node sidebar() {
        VBox nav = new VBox(4);
        for (String key : menu.keySet()) {
            Button b = new Button(key);
            b.setGraphic(Icons.of(iconFor(key), 18, "#6b7280"));
            b.setGraphicTextGap(12);
            b.getStyleClass().setAll("nav-item");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> open(menu.get(key).get()));
            navButtons.put(key, b);
            nav.getChildren().add(b);
        }
        Button logout = new Button("Logout");
        logout.setGraphic(Icons.of(Icons.LOGOUT, 18, "#6b7280"));
        logout.setGraphicTextGap(12);
        logout.getStyleClass().setAll("nav-item");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> { app.auth.logout(); Navigator.showLogin(); });

        Label roleTag = UiKit.badge(user.getRole().equals("TENANT") ? "Customer · Tenant" : cap(user.getRole()) + " Portal", "blue");
        VBox side = new VBox(18, UiKit.label("GharBaar", "brand"), roleTag, nav, UiKit.spacer(), logout);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(230);
        return side;
    }

    private Node topbar() {
        Button bell = new Button();
        bell.setGraphic(Icons.of(Icons.BELL, 20, "#1f2937"));
        bell.getStyleClass().setAll("btn-ghost");
        bell.setOnAction(e -> open(new NotificationsScreen()));
        StackPane bellWrap = new StackPane(bell, bellCount);
        StackPane.setAlignment(bellCount, Pos.TOP_RIGHT);
        bellCount.setVisible(false);

        VBox who = new VBox(0, UiKit.label(user.getName(), "h3"), UiKit.label("@" + user.getUsername(), "muted", "small"));
        HBox bar = new HBox(14, pageTitle, UiKit.spacer(), bellWrap, who, UiKit.avatar(user.getName(), 40));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        return bar;
    }

    // ---------------------------------------------------------------
    // [NAVIGATION]
    // ---------------------------------------------------------------
    public void openHome() {
        open(menu.values().iterator().next().get());
    }

    public void open(Screen screen) {
        pageTitle.setText(screen.title());
        navButtons.forEach((k, b) -> {
            boolean active = k.equals(screen.navKey());
            b.getStyleClass().remove("active");
            if (active) b.getStyleClass().add("active");
            b.setGraphic(Icons.of(iconFor(k), 18, active ? "#2f80ed" : "#6b7280"));
        });
        VBox page = new VBox(screen.build());
        page.setPadding(new Insets(24, 28, 28, 28));
        content.setContent(page);
        content.setVvalue(0);
        refreshNotificationCount();
    }

    public void refreshNotificationCount() {
        UiKit.async(app.notifications::getUnreadCount, n -> {
            bellCount.setText(String.valueOf(n));
            bellCount.setVisible(n > 0);
        });
    }

    private static String cap(String s) { return s.charAt(0) + s.substring(1).toLowerCase(); }

    public Parent getRoot() { return root; }
}
