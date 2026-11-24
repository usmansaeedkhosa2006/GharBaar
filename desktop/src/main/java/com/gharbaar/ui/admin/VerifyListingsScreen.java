package com.gharbaar.ui.admin;

import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI - ADMIN] VerifyListingsScreen  -  UC-06 Verify Listing
 * Pending listings -> review full details -> Approve or Reject (reason)
 * =====================================================================
 */
public class VerifyListingsScreen extends MasterDetailScreen<PropertyListing> {

    @Override public String title() { return "Verify Listings"; }
    @Override protected String listHeading() { return "Pending Verification"; }
    @Override protected String emptyTitle() { return "No listings waiting"; }

    @Override protected Supplier<List<PropertyListing>> load() { return app.verification::getPendingListings; }

    @Override
    protected Node rowFor(PropertyListing l) {
        HBox row = new HBox(12, UiKit.imageView(l.getPhotoPath(), 80, 58, 10),
                new VBox(3, UiKit.label(l.getTitle(), "h3"),
                        UiKit.label(l.getOwner().getName() + "  ·  " + l.getCdaSector(), "muted", "small"),
                        UiKit.label(UiKit.pkr(l.getRent()), "field-label")));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    protected Node detailsFor(PropertyListing l) {
        // ---- step 4: complete details of the selected listing ----
        FlowPane photos = new FlowPane(10, 10);
        for (String p : l.getPhotos()) photos.getChildren().add(UiKit.imageView(p, 200, 130, 12));
        FlowPane facilities = new FlowPane(8, 8);
        l.getAdvertisedFacilities().forEach(f -> facilities.getChildren().add(UiKit.badge(f.getName(), "blue")));

        GridPane facts = new GridPane();
        facts.setHgap(24);
        facts.setVgap(8);
        String[][] rows = {
                {"Agent", l.getOwner().getName() + " (" + l.getOwner().getPhone() + ")"},
                {"Address", l.getAddress()}, {"CDA Sector", l.getCdaSector()}, {"Type", l.getPropertyType()},
                {"Rent", UiKit.pkr(l.getRent()) + " /month"}, {"Furnishing", l.isFurnishing() ? "Furnished" : "Unfurnished"},
                {"Occupancy", l.getOccupancyType()}, {"Submitted", l.getCreatedAt().format(UiKit.DATE_TIME)}};
        for (int i = 0; i < rows.length; i++) {
            facts.add(UiKit.label(rows[i][0], "muted"), 0, i);
            facts.add(UiKit.wrapLabel(rows[i][1], "field-label"), 1, i);
        }

        // ---- step 5: approve  /  ext. 5a: reject with reason ----
        Button approve = UiKit.button("Approve listing", "btn-success", () ->
                UiKit.action(() -> app.verification.approveListing(l.getListingID()),
                        "Listing verified. The agent has been informed.", this::afterAction));
        approve.setGraphic(Icons.of(Icons.CHECK, 16, "#ffffff"));
        Button reject = UiKit.button("Reject", "btn-danger", () ->
                UiKit.askReason("Reject \"" + l.getTitle() + "\"", "Reason (sent to the agent):").ifPresent(reason ->
                        UiKit.action(() -> app.verification.rejectListing(l.getListingID(), reason),
                                "Listing rejected. The agent has been informed.", this::afterAction)));
        reject.setGraphic(Icons.of(Icons.X, 16, "#b91c1c"));

        return new VBox(14,
                new HBox(10, UiKit.label(l.getTitle(), "h2"), UiKit.spacer(), UiKit.statusBadge(l.getListingStatus())),
                photos, facts,
                UiKit.label("Description", "field-label"), UiKit.wrapLabel(l.getDescription(), "muted"),
                UiKit.label("Advertised facilities", "field-label"), facilities,
                new HBox(10, approve, reject));
    }
}
