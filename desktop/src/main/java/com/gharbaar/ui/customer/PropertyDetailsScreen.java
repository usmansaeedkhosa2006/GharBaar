package com.gharbaar.ui.customer;

import com.gharbaar.bl.domain.Facility;
import com.gharbaar.bl.domain.FacilityConfirmation;
import com.gharbaar.bl.domain.FacilityReview;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.ReportIssueScreen;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import java.util.List;

/**
 * =====================================================================
 * [UI - CUSTOMER] PropertyDetailsScreen
 *   UC-10 View Property Details (photos, rent, facilities, location,
 *         furnishing, occupancy, status, lister contact)
 *   UC-11 View Tenant History        (extends from here)
 *   UC-12 Request Property Visit     (extends from here)
 *   UC-13 Submit Tenancy Claim / UC-14 Submit Facility Review
 * =====================================================================
 */
public class PropertyDetailsScreen extends Screen {

    private final int listingId;
    private final VBox root = new VBox();
    private final VBox historyBox = new VBox(12);

    public PropertyDetailsScreen(int listingId) { this.listingId = listingId; }

    @Override public String title() { return "Property Details"; }
    @Override public String navKey() { return "Find Home"; }

    @Override
    public Node build() {
        root.getChildren().setAll(UiKit.loading());
        UiKit.async(() -> app.search.getPropertyDetails(listingId), this::render,
                () -> Navigator.open(new SearchScreen()));                                 // ext. 1a: back to results
        return root;
    }

    private void render(PropertyListing l) {
        // ---------- [GALLERY] big photo + thumbnails ----------
        ImageView[] mainRef = new ImageView[1];
        Node mainPhoto = UiKit.fluidImage(l.getPhotoPath(), 400, 20, mainRef);
        ImageView main = mainRef[0];
        FlowPane thumbs = new FlowPane(10, 10);
        for (String p : l.getPhotos()) {
            ImageView t = UiKit.imageView(p, 96, 70, 10);
            t.getStyleClass().add("thumb");
            t.setOnMouseClicked(e -> UiKit.setImage(main, p, 1080));
            thumbs.getChildren().add(t);
        }
        Button back = UiKit.button("Back to results", "btn-ghost", () -> Navigator.open(new SearchScreen()));
        back.setGraphic(Icons.of(Icons.BACK, 16, "#1f2937"));

        // ---------- [INFO] title, address, description ----------
        HBox addr = new HBox(6, Icons.of(Icons.PIN, 16, "#9ca3af"), UiKit.label(l.getAddress() + ", Islamabad", "muted"));
        addr.setAlignment(Pos.CENTER_LEFT);
        VBox info = UiKit.card(
                new HBox(10, UiKit.label(l.getTitle(), "h1"), UiKit.spacer(), UiKit.statusBadge(l.getPropertyStatus())),
                addr,
                UiKit.wrapLabel(l.getDescription(), "muted"));

        // ---------- [AMENITIES] 2-column grid with icons ----------
        GridPane amen = new GridPane();
        amen.setHgap(40);
        amen.setVgap(4);
        List<Facility> fac = l.getAdvertisedFacilities();
        for (int i = 0; i < fac.size(); i++) {
            StackPane ic = new StackPane(Icons.of(Icons.forFacility(fac.get(i).getName()), 16, "#16a34a"));
            ic.getStyleClass().add("amenity-icon");
            HBox row = new HBox(10, ic, UiKit.label(fac.get(i).getName()));
            row.getStyleClass().add("amenity");
            amen.add(row, i % 2, i / 2);
        }
        VBox amenities = UiKit.card(UiKit.label("Amenities", "h3"), amen);

        // ---------- [UC-11] tenant history ----------
        Button showHistory = UiKit.button("View Tenant History", "btn-outline", this::loadHistory);
        showHistory.setGraphic(Icons.of(Icons.HISTORY, 16, "#2f80ed"));
        VBox history = UiKit.card(
                new HBox(UiKit.label("Tenant History", "h3"), UiKit.spacer(), showHistory),
                UiKit.wrapLabel("Feedback from previous tenants with a verified tenancy: were the advertised facilities actually provided?", "muted", "small"),
                historyBox);

        VBox left = new VBox(16, back, mainPhoto, thumbs, info, amenities, history);
        left.setMinWidth(0);
        left.setPrefWidth(200);
        HBox.setHgrow(left, Priority.ALWAYS);

        // ---------- [RIGHT COLUMN] price, location, agent, actions ----------
        VBox price = UiKit.card(
                UiKit.label("Monthly rent", "muted"),
                UiKit.label(UiKit.pkr(l.getRent()), "price-big"),
                facts(l));

        StackPane map = new StackPane();
        map.getStyleClass().add("map-card");
        map.setPrefHeight(150);
        for (int i = 0; i < 6; i++) {                       // simple street lines for a map look
            Line h = new Line(0, 0, 300, 0); h.setStroke(javafx.scene.paint.Color.web("#ffffff")); h.setStrokeWidth(3);
            StackPane.setAlignment(h, Pos.TOP_LEFT); StackPane.setMargin(h, new Insets(20 + i * 24, 0, 0, 0));
            map.getChildren().add(h);
        }
        Label pin = UiKit.badge("Sector " + l.getCdaSector() + ", ISLAMABAD", "red");
        VBox pinBox = new VBox(4, Icons.of(Icons.PIN, 30, "#f97316"), pin);
        pinBox.setAlignment(Pos.CENTER);
        map.getChildren().add(pinBox);
        VBox location = UiKit.card(UiKit.label("Location", "h3"), map);

        HBox agent = new HBox(12, UiKit.avatar(l.getOwner().getName(), 46),
                new VBox(2, UiKit.label(l.getOwner().getName(), "h3"), UiKit.label("Agent / Owner", "muted", "small")),
                UiKit.spacer(),
                UiKit.iconButton(Icons.MESSAGE, "Message agent", () -> contact(l)),
                UiKit.iconButton(Icons.PHONE, "Call agent", () -> contact(l)));
        agent.setAlignment(Pos.CENTER_LEFT);
        VBox details = UiKit.card(UiKit.label("For Details", "h3"), agent);

        Button visit = wide(UiKit.button("Request Visit", "btn-primary", () -> new VisitRequestDialog(l).show()));
        visit.setGraphic(Icons.of(Icons.CALENDAR, 16, "#ffffff"));
        visit.setDisable(!l.isAvailable());
        Button claim = wide(UiKit.button("I lived here - Submit Tenancy Claim", "btn-outline",
                () -> new TenancyClaimDialog(l).show()));
        Button review = wide(UiKit.button("Write Facility Review", "btn-outline", () -> startReview(l)));
        review.setGraphic(Icons.of(Icons.STAR, 16, "#2f80ed"));
        Button report = wide(UiKit.button("Report this listing", "btn-ghost",
                () -> Navigator.open(new ReportIssueScreen("Fake Listing",
                        "About listing #" + l.getListingID() + " \"" + l.getTitle() + "\": "))));
        report.setGraphic(Icons.of(Icons.FLAG, 16, "#6b7280"));
        VBox actions = UiKit.card(UiKit.label("Actions", "h3"), visit);
        if (!l.isAvailable())
            actions.getChildren().add(UiKit.wrapLabel("Visits can only be requested while the property is available.", "muted", "small"));
        actions.getChildren().addAll(claim, review, report);

        VBox right = new VBox(16, price, details, actions, location);
        right.setPrefWidth(340);
        right.setMinWidth(340);
        right.setMaxWidth(340);
        right.setPadding(new Insets(50, 0, 0, 0));

        root.getChildren().setAll(new HBox(22, left, right));
    }

    private Button wide(Button b) { b.setMaxWidth(Double.MAX_VALUE); return b; }

    private Node facts(PropertyListing l) {
        GridPane g = new GridPane();
        g.setHgap(18);
        g.setVgap(8);
        String[][] rows = {
                {"Type", l.getPropertyType()}, {"CDA Sector", l.getCdaSector()},
                {"Furnishing", l.isFurnishing() ? "Furnished" : "Unfurnished"},
                {"Occupancy", l.getOccupancyType()}, {"Status", l.getPropertyStatus().label()}};
        for (int i = 0; i < rows.length; i++) {
            g.add(UiKit.label(rows[i][0], "muted"), 0, i);
            g.add(UiKit.label(rows[i][1], "field-label"), 1, i);
        }
        return g;
    }

    private void contact(PropertyListing l) {
        UiKit.info("Contact " + l.getOwner().getName(),
                "Phone / WhatsApp: " + l.getOwner().getPhone() + "\n\nMention GharBaar listing #" + l.getListingID() + ".");
    }

    // ---------------------------------------------------------------
    // [UC-14] check eligibility (ext 1a / 1b) then open review dialog
    // ---------------------------------------------------------------
    private void startReview(PropertyListing l) {
        UiKit.async(() -> app.tenancy.startFacilityReview(l.getListingID()),
                facilities -> new FacilityReviewDialog(l, facilities, this::loadHistory).show());
    }

    // ---------------------------------------------------------------
    // [UC-11] View Tenant History
    // ---------------------------------------------------------------
    private void loadHistory() {
        historyBox.getChildren().setAll(UiKit.loading());
        UiKit.async(() -> app.search.getTenantHistory(listingId), reviews -> {
            if (reviews.isEmpty()) {                                                       // ext. 2a
                historyBox.getChildren().setAll(UiKit.label("No tenant history available yet.", "muted"));
                return;
            }
            historyBox.getChildren().clear();
            for (FacilityReview r : reviews) historyBox.getChildren().add(reviewCard(r));
        });
    }

    static Node reviewCard(FacilityReview r) {
        FlowPane chips = new FlowPane(8, 8);
        for (FacilityConfirmation fc : r.getConfirmations()) {
            Label c = UiKit.badge((fc.isConfirmed() ? "✓ " : "✗ ") + fc.getFacility().getName(),
                    fc.isConfirmed() ? "green" : "red");
            chips.getChildren().add(c);
        }
        String period = r.getTenancyStart() == null ? "" :
                "Lived here " + r.getTenancyStart().format(UiKit.DATE) + " - " + r.getTenancyEnd().format(UiKit.DATE);
        HBox head = new HBox(10, UiKit.avatar(r.getTenantName().isBlank() ? "T" : r.getTenantName(), 34),
                new VBox(2, new HBox(8, UiKit.label(r.getTenantName(), "h3"), UiKit.badge("Verified tenant", "green")),
                        UiKit.label(period, "muted", "small")),
                UiKit.spacer(),
                UiKit.label(r.confirmedCount() + "/" + r.getConfirmations().size() + " facilities confirmed", "field-label"));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox v = new VBox(10, head, chips);
        if (!r.getComments().isBlank()) v.getChildren().add(UiKit.wrapLabel("“" + r.getComments() + "”", "muted"));
        v.getChildren().add(UiKit.label("Submitted " + r.getSubmittedDate().format(UiKit.DATE), "muted", "small"));
        v.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-padding: 14;");
        return v;
    }
}
