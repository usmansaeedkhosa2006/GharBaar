package com.gharbaar.ui.customer;

import com.gharbaar.bl.domain.TenancyClaim;
import com.gharbaar.bl.domain.VisitRequest;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * =====================================================================
 * [UI - CUSTOMER] MyActivityScreen
 *   - my visit requests (UC-12) with the agent's answer
 *   - my tenancy claims (UC-13) and "Write review" when verified (UC-14)
 * =====================================================================
 */
public class MyActivityScreen extends Screen {

    private final VBox visits = new VBox(10);
    private final VBox claims = new VBox(10);

    @Override public String title() { return "My Activity"; }

    @Override
    public Node build() {
        visits.getChildren().setAll(UiKit.loading());
        claims.getChildren().setAll(UiKit.loading());
        UiKit.async(app.visits::getMyVisits, list -> {
            visits.getChildren().clear();
            if (list.isEmpty()) visits.getChildren().add(UiKit.label("You have not requested any visits yet.", "muted"));
            for (VisitRequest v : list) visits.getChildren().add(visitRow(v));
        });
        loadClaims();
        return new VBox(20,
                UiKit.card(UiKit.label("My visit requests", "h3"), visits),
                UiKit.card(UiKit.label("My tenancy claims & reviews", "h3"),
                        UiKit.wrapLabel("Lived in a property listed on GharBaar? Open the property and choose \"Submit Tenancy Claim\". "
                                + "After the Admin verifies it, you can review its facilities here.", "muted", "small"),
                        claims));
    }

    private void loadClaims() {
        UiKit.async(app.tenancy::getMyClaims, list -> {
            claims.getChildren().clear();
            if (list.isEmpty()) claims.getChildren().add(UiKit.label("No tenancy claims yet.", "muted"));
            for (TenancyClaim c : list) claims.getChildren().add(claimRow(c));
        });
    }

    private Node visitRow(VisitRequest v) {
        HBox row = new HBox(14,
                UiKit.imageView(v.getListing().getPhotoPath(), 90, 64, 10),
                new VBox(4, UiKit.label(v.getListing().getTitle(), "h3"),
                        UiKit.label(v.getProposedDateTime().format(UiKit.DATE_TIME) + "  ·  Agent: " + v.getListing().getOwner().getName(), "muted"),
                        UiKit.label(v.getNotes().isBlank() ? "No notes" : "Notes: " + v.getNotes(), "muted", "small")),
                UiKit.spacer(), UiKit.statusBadge(v.getStatus()));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> Navigator.open(new PropertyDetailsScreen(v.getListing().getListingID())));
        row.setStyle("-fx-cursor: hand;");
        return row;
    }

    private Node claimRow(TenancyClaim c) {
        VBox info = new VBox(4, UiKit.label(c.getListing().getTitle(), "h3"),
                UiKit.label(c.getStartDate().format(UiKit.DATE) + " - " + c.getEndDate().format(UiKit.DATE), "muted"));
        if (c.getRejectionReason() != null) info.getChildren().add(UiKit.wrapLabel("Reason: " + c.getRejectionReason(), "error-text", "small"));
        HBox row = new HBox(14, UiKit.imageView(c.getListing().getPhotoPath(), 90, 64, 10), info, UiKit.spacer());
        if (c.isFlagged()) row.getChildren().add(UiKit.badge("Flagged: overlapping dates", "amber"));
        row.getChildren().add(UiKit.statusBadge(c.getStatus()));
        if (c.isVerified() && !c.hasReview()) {
            Button review = UiKit.button("Write review", "btn-primary", () ->
                    UiKit.async(() -> app.tenancy.startFacilityReview(c.getListing().getListingID()),
                            f -> new FacilityReviewDialog(c.getListing(), f, this::loadClaims).show()));
            review.setGraphic(Icons.of(Icons.STAR, 16, "#ffffff"));
            row.getChildren().add(review);
        } else if (c.hasReview()) {
            row.getChildren().add(UiKit.badge("Reviewed", "blue"));
        }
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
