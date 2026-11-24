package com.gharbaar.ui.common;

import com.gharbaar.bl.domain.Notification;
import com.gharbaar.ui.Navigator;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * [UI] NotificationsScreen - messages sent by the system via Notifier
 * (listing verified/rejected, visit requested/answered, report resolved...).
 */
public class NotificationsScreen extends Screen {

    @Override public String title() { return "Notifications"; }

    @Override
    public Node build() {
        VBox list = new VBox(10, UiKit.loading());
        UiKit.async(app.notifications::getMyNotifications, items -> {
            list.getChildren().clear();
            if (items.isEmpty()) list.getChildren().add(UiKit.emptyState("You're all caught up", "No notifications yet."));
            for (Notification n : items) {
                StackPane dot = new StackPane(Icons.of(Icons.BELL, 16, n.isRead() ? "#9ca3af" : "#2f80ed"));
                dot.getStyleClass().add("amenity-icon");
                dot.setStyle(n.isRead() ? "-fx-background-color: #f3f4f6;" : "-fx-background-color: #eaf2fd;");
                HBox row = new HBox(12, dot,
                        new VBox(3, UiKit.wrapLabel(n.getMessage(), n.isRead() ? "muted" : "field-label"),
                                UiKit.label(n.getCreatedAt().format(UiKit.DATE_TIME), "muted", "small")));
                row.setAlignment(Pos.CENTER_LEFT);
                list.getChildren().add(row);
            }
            // mark as read once shown
            UiKit.async(() -> { app.notifications.markAllRead(); return true; }, ok -> Navigator.refreshBadge());
        });
        return UiKit.card(UiKit.label("Recent notifications", "h3"), list);
    }
}
