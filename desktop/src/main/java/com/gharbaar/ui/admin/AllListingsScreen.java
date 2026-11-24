package com.gharbaar.ui.admin;

import com.gharbaar.bl.domain.ListingStatus;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * [UI - ADMIN] AllListingsScreen - every listing on the platform with a
 * status filter; the Admin can remove fraudulent listings (with reason).
 */
public class AllListingsScreen extends Screen {

    private final VBox list = new VBox(12);
    private List<PropertyListing> all = List.of();
    private ListingStatus filter;

    @Override public String title() { return "All Listings"; }

    @Override
    public Node build() {
        ToggleGroup g = new ToggleGroup();
        HBox chips = new HBox(8);
        for (String s : List.of("All", "VERIFIED", "PENDING_VERIFICATION", "REJECTED")) {
            ToggleButton c = UiKit.chip(s.equals("All") ? "All" : ListingStatus.valueOf(s).label());
            c.setUserData(s);
            c.setToggleGroup(g);
            chips.getChildren().add(c);
        }
        g.getToggles().get(0).setSelected(true);
        g.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            filter = b.getUserData().equals("All") ? null : ListingStatus.valueOf(b.getUserData().toString());
            render();
        });
        list.getChildren().setAll(UiKit.loading());
        UiKit.async(app.admin::getAllListings, l -> { all = l; render(); });
        return new VBox(16, chips, list);
    }

    private void render() {
        list.getChildren().clear();
        List<PropertyListing> shown = all.stream().filter(l -> filter == null || l.getListingStatus() == filter).toList();
        if (shown.isEmpty()) list.getChildren().add(UiKit.emptyState("No listings", "Nothing matches this filter."));
        for (PropertyListing l : shown) {
            Button remove = UiKit.button("Remove", "btn-danger", () ->
                    UiKit.askReason("Remove \"" + l.getTitle() + "\" from GharBaar?", "Reason (sent to the agent):").ifPresent(r ->
                            UiKit.action(() -> app.admin.removeListing(l.getListingID(), r), "Listing removed.",
                                    () -> Navigator.open(new AllListingsScreen()))));
            remove.setGraphic(Icons.of(Icons.TRASH, 15, "#b91c1c"));
            HBox row = new HBox(14, UiKit.imageView(l.getPhotoPath(), 120, 80, 12),
                    new VBox(4, UiKit.label(l.getTitle(), "h3"),
                            UiKit.label(l.getAddress() + "  ·  " + l.getCdaSector() + "  ·  Agent: " + l.getOwner().getName(), "muted"),
                            UiKit.label(UiKit.pkr(l.getRent()) + " /month", "field-label")),
                    UiKit.spacer(), UiKit.statusBadge(l.getListingStatus()), UiKit.statusBadge(l.getPropertyStatus()), remove);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("card");
            list.getChildren().add(row);
        }
    }
}
