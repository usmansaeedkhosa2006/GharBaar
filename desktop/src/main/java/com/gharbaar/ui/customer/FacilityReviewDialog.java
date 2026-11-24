package com.gharbaar.ui.customer;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.domain.Facility;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.common.FormDialog;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * =====================================================================
 * [UI - CUSTOMER] FacilityReviewDialog  -  UC-14 Submit Facility Review
 * The (verified) tenant confirms or denies EACH advertised facility and
 * adds optional comments. One review per verified tenancy.
 * =====================================================================
 */
public class FacilityReviewDialog extends FormDialog {

    private final PropertyListing listing;
    private final List<Facility> facilities;
    private final Runnable onSaved;
    private final Map<Facility, ToggleGroup> answers = new LinkedHashMap<>();
    private final TextArea comments = new TextArea();

    public FacilityReviewDialog(PropertyListing listing, List<Facility> facilities, Runnable onSaved) {
        this.listing = listing;
        this.facilities = facilities;
        this.onSaved = onSaved;
    }

    @Override protected String heading() { return "Review the facilities of \"" + listing.getTitle() + "\""; }
    @Override protected String submitText() { return "Submit review"; }

    @Override
    protected Node content() {
        VBox rows = new VBox(10);
        for (Facility f : facilities) {                                                  // step 2
            ToggleGroup g = new ToggleGroup();
            ToggleButton yes = UiKit.chip("Provided");
            ToggleButton no = UiKit.chip("Not provided");
            yes.setUserData(Boolean.TRUE);
            no.setUserData(Boolean.FALSE);
            yes.setToggleGroup(g);
            no.setToggleGroup(g);
            StackPane ic = new StackPane(Icons.of(Icons.forFacility(f.getName()), 16, "#16a34a"));
            ic.getStyleClass().add("amenity-icon");
            HBox row = new HBox(10, ic, UiKit.label(f.getName(), "field-label"), UiKit.spacer(), yes, no);
            row.setAlignment(Pos.CENTER_LEFT);
            answers.put(f, g);
            rows.getChildren().add(row);
        }
        comments.setPromptText("Optional comments for future tenants");
        comments.setPrefRowCount(3);
        comments.setWrapText(true);
        return new VBox(14,
                UiKit.wrapLabel("Was each facility advertised by the agent actually provided during your tenancy?", "muted"),
                rows, UiKit.field("Comments (optional)", comments),
                UiKit.wrapLabel("Note: reviews cannot be edited or deleted after submission.", "muted", "small"));
    }

    @Override
    protected void submit() {
        Map<Facility, Boolean> confirmations = new LinkedHashMap<>();
        for (var e : answers.entrySet()) {
            if (e.getValue().getSelectedToggle() == null) {
                UiKit.showError(new com.gharbaar.bl.BusinessException("Please confirm or deny every facility (" + e.getKey().getName() + ")."));
                return;
            }
            confirmations.put(e.getKey(), (Boolean) e.getValue().getSelectedToggle().getUserData());
        }
        UiKit.async(() -> AppContext.get().tenancy.submitReview(listing.getListingID(), confirmations, comments.getText()),
                r -> { done("Thank you! Your facility review is now part of the tenant history."); if (onSaved != null) onSaved.run(); });
    }
}
