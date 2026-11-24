package com.gharbaar.ui.agent;

import com.gharbaar.bl.domain.ListingStatus;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * =====================================================================
 * [UI - AGENT] MyListingsScreen  -  UC-04 step 1 "Agent opens his
 * property listings and selects the listing to edit".
 * Shows verification status + rejection reason from the Admin.
 * =====================================================================
 */
public class MyListingsScreen extends Screen {

    private final VBox list = new VBox(14);
    private final HBox stats = new HBox(14);

    @Override public String title() { return "My Listings"; }

    @Override
    public Node build() {
        Button add = UiKit.button("Create Property Listing", "btn-primary", () -> Navigator.open(new ListingFormScreen(null)));
        add.setGraphic(Icons.of(Icons.PLUS, 16, "#ffffff"));
        HBox header = new HBox(UiKit.label("Your properties", "h2"), UiKit.spacer(), add);
        header.setAlignment(Pos.CENTER_LEFT);
        list.getChildren().setAll(UiKit.loading());
        UiKit.async(app.listings::getMyListings, this::render);
        return new VBox(18, header, stats, list);
    }

    private void render(List<PropertyListing> listings) {
        stats.getChildren().setAll(
                stat("Total", listings.size(), "#2f80ed"),
                stat("Verified", count(listings, ListingStatus.VERIFIED), "#16a34a"),
                stat("Pending", count(listings, ListingStatus.PENDING_VERIFICATION), "#d97706"),
                stat("Rejected", count(listings, ListingStatus.REJECTED), "#dc2626"));
        list.getChildren().clear();
        if (listings.isEmpty())
            list.getChildren().add(UiKit.emptyState("No listings yet", "Create your first property listing. The Admin will verify it before it becomes public."));
        for (PropertyListing l : listings) list.getChildren().add(row(l));
    }

    private long count(List<PropertyListing> l, ListingStatus s) { return l.stream().filter(x -> x.getListingStatus() == s).count(); }

    private Node stat(String label, long value, String color) {
        VBox v = new VBox(4, UiKit.label(label, "muted"), UiKit.label(String.valueOf(value), "stat-value"));
        v.getChildren().get(1).setStyle("-fx-text-fill: " + color + ";");
        v.getStyleClass().add("stat-card");
        v.setPrefWidth(180);
        return v;
    }

    private Node row(PropertyListing l) {
        VBox info = new VBox(6,
                new HBox(10, UiKit.label(l.getTitle(), "h3"), UiKit.statusBadge(l.getListingStatus()), UiKit.statusBadge(l.getPropertyStatus())),
                UiKit.label(l.getAddress() + "  ·  Sector " + l.getCdaSector() + "  ·  " + l.getPropertyType(), "muted"),
                UiKit.label(UiKit.pkr(l.getRent()) + " /month  ·  " + l.getAdvertisedFacilities().size() + " facilities", "field-label"));
        if (l.getListingStatus() == ListingStatus.REJECTED)
            info.getChildren().add(UiKit.wrapLabel("Rejected by Admin: " + l.getRejectionReason() + "  -  edit the listing to resubmit it.", "error-text"));
        if (l.getListingStatus() == ListingStatus.PENDING_VERIFICATION)
            info.getChildren().add(UiKit.label("Waiting for Admin verification before it becomes public.", "muted", "small"));

        Button edit = UiKit.button("Edit", "btn-outline", () -> Navigator.open(new ListingFormScreen(l.getListingID())));
        edit.setGraphic(Icons.of(Icons.EDIT, 15, "#2f80ed"));
        Button delete = UiKit.button("Delete", "btn-danger", () -> delete(l));
        delete.setGraphic(Icons.of(Icons.TRASH, 15, "#b91c1c"));

        HBox row = new HBox(16, UiKit.imageView(l.getPhotoPath(), 170, 110, 14), info, UiKit.spacer(), new VBox(8, edit, delete));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        return row;
    }

    /** UC-04 alternative: delete the property with a reason */
    private void delete(PropertyListing l) {
        UiKit.askReason("Delete \"" + l.getTitle() + "\"?", "Reason for deleting:").ifPresent(reason ->
                UiKit.action(() -> app.listings.deleteListing(l.getListingID(), reason),
                        "Listing deleted.", () -> Navigator.open(new MyListingsScreen())));
    }
}
