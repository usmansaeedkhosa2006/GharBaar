package com.gharbaar.ui.admin;

import com.gharbaar.bl.controller.AdminController.DashboardStats;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

/**
 * [UI - ADMIN] AdminDashboardScreen - overview + shortcuts to the work queues.
 */
public class AdminDashboardScreen extends Screen {

    @Override public String title() { return "Dashboard"; }

    @Override
    public Node build() {
        FlowPane cards = new FlowPane(16, 16);
        cards.getChildren().add(UiKit.loading());
        UiKit.async(app.admin::getDashboardStats, s -> cards.getChildren().setAll(statCards(s)));
        return new VBox(20,
                new VBox(4, UiKit.label("Admin panel", "h1"),
                        UiKit.label("Verify listings and tenants, resolve reports and manage accounts.", "muted")),
                cards);
    }

    private java.util.List<Node> statCards(DashboardStats s) {
        return java.util.List.of(
                card("Listings waiting for verification", s.pendingListings(), Icons.SHIELD, "#d97706", VerifyListingsScreen::new),
                card("Tenants waiting for verification", s.pendingTenants(), Icons.USER, "#d97706", VerifyTenantsScreen::new),
                card("Open reports", s.openReports(), Icons.FLAG, "#dc2626", ReportsScreen::new),
                card("Verified listings", s.verifiedListings(), Icons.HOME, "#16a34a", AllListingsScreen::new),
                card("Rejected listings", s.rejectedListings(), Icons.X, "#6b7280", AllListingsScreen::new),
                card("Customers", s.customers(), Icons.USERS, "#2f80ed", UsersScreen::new),
                card("Agents", s.agents(), Icons.USERS, "#2f80ed", UsersScreen::new),
                card("Pending tenancy claims", s.pendingClaims(), Icons.FILE, "#d97706", VerifyTenantsScreen::new));
    }

    private Node card(String label, int value, String icon, String color, Supplier<Screen> target) {
        StackPane ic = new StackPane(Icons.of(icon, 20, color));
        ic.getStyleClass().add("amenity-icon");
        ic.setStyle("-fx-background-color: " + color + "1a;");
        VBox v = new VBox(10, new HBox(ic), UiKit.label(String.valueOf(value), "stat-value"), UiKit.wrapLabel(label, "muted"));
        v.getStyleClass().add("stat-card");
        v.setPrefWidth(250);
        v.setAlignment(Pos.TOP_LEFT);
        v.setStyle("-fx-cursor: hand;");
        v.setOnMouseClicked(e -> Navigator.open(target.get()));
        return v;
    }
}
