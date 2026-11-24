package com.gharbaar.ui.agent;

import com.gharbaar.bl.domain.VisitRequest;
import com.gharbaar.bl.domain.VisitStatus;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * [UI - AGENT] VisitRequestsScreen
 * Visit requests from customers (UC-12); the agent accepts or declines,
 * and the customer is notified.
 */
public class VisitRequestsScreen extends Screen {

    @Override public String title() { return "Visit Requests"; }

    @Override
    public Node build() {
        VBox list = new VBox(12, UiKit.loading());
        UiKit.async(app.visits::getAgentVisitRequests, items -> {
            list.getChildren().clear();
            if (items.isEmpty()) list.getChildren().add(UiKit.emptyState("No visit requests", "Customers' visit requests for your listings will appear here."));
            for (VisitRequest v : items) list.getChildren().add(row(v));
        });
        return new VBox(16, UiKit.label("Requests for your properties", "h2"), list);
    }

    private Node row(VisitRequest v) {
        VBox info = new VBox(5,
                UiKit.label(v.getListing().getTitle(), "h3"),
                UiKit.label(v.getProposedDateTime().format(UiKit.DATE_TIME), "field-label"),
                UiKit.label("Customer: " + v.getCustomer().getName() + "  ·  " + v.getCustomer().getPhone(), "muted"),
                UiKit.wrapLabel(v.getNotes().isBlank() ? "No notes" : "“" + v.getNotes() + "”", "muted", "small"));
        HBox row = new HBox(16, UiKit.imageView(v.getListing().getPhotoPath(), 120, 84, 12), info, UiKit.spacer(), UiKit.statusBadge(v.getStatus()));
        if (v.getStatus() == VisitStatus.PENDING) {
            row.getChildren().add(new VBox(8,
                    UiKit.button("Accept", "btn-success", () -> respond(v, true)),
                    UiKit.button("Decline", "btn-danger", () -> respond(v, false))));
        }
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        return row;
    }

    private void respond(VisitRequest v, boolean accept) {
        UiKit.action(() -> app.visits.respondToVisit(v.getRequestID(), accept),
                "Visit " + (accept ? "accepted" : "declined") + ". The customer has been notified.",
                () -> Navigator.open(new VisitRequestsScreen()));
    }
}
