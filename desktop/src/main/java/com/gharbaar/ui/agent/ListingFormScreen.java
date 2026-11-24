package com.gharbaar.ui.agent;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.ListingDetails;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.PropertyStatus;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [UI - AGENT] ListingFormScreen
 *   listingId == null -> UC-03 Create Property Listing
 *   listingId != null -> UC-04 Edit Property Listing (or delete)
 * =====================================================================
 */
public class ListingFormScreen extends Screen {

    private final Integer listingId;

    // ---- form controls ----
    private final TextField title = new TextField();
    private final TextField address = new TextField();
    private final TextField rent = new TextField();
    private final ComboBox<String> sector = new ComboBox<>();
    private final TextArea description = new TextArea();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ToggleGroup occupancyGroup = new ToggleGroup();
    private final ToggleGroup furnishGroup = new ToggleGroup();
    private final ToggleGroup statusGroup = new ToggleGroup();
    private final FlowPane facilityPane = new FlowPane(16, 10);
    private final List<CheckBox> facilityBoxes = new ArrayList<>();
    private final TextField customFacility = new TextField();
    private final FlowPane photoPreview = new FlowPane(10, 10);
    private final List<File> photos = new ArrayList<>();
    private final VBox root = new VBox(18);

    public ListingFormScreen(Integer listingId) { this.listingId = listingId; }

    @Override public String title() { return listingId == null ? "Create Property Listing" : "Edit Property Listing"; }
    @Override public String navKey() { return listingId == null ? "Add Listing" : "My Listings"; }

    @Override
    public Node build() {
        buildForm();
        if (listingId != null) {
            root.setDisable(true);
            UiKit.async(() -> app.listings.getListing(listingId), l -> { fill(l); root.setDisable(false); },
                    () -> Navigator.open(new MyListingsScreen()));
        }
        return root;
    }

    // ---------------------------------------------------------------
    // [FORM LAYOUT] step 2: "System shows the property listing form"
    // ---------------------------------------------------------------
    private void buildForm() {
        title.setPromptText("e.g. Single sunny room apartment");
        address.setPromptText("House / street / block");
        rent.setPromptText("e.g. 45000");
        sector.getItems().setAll(ListingDetails.CDA_SECTORS);
        sector.setPromptText("Select sector");
        sector.setMaxWidth(Double.MAX_VALUE);
        description.setPromptText("Describe the property (at least 20 characters)");
        description.setPrefRowCount(4);
        description.setWrapText(true);

        for (String f : ListingDetails.COMMON_FACILITIES) addFacilityBox(f, false);
        customFacility.setPromptText("Other facility");
        Button addCustom = UiKit.button("Add", "btn-outline", () -> {
            String f = customFacility.getText().trim();
            if (!f.isEmpty()) { addFacilityBox(f, true); customFacility.clear(); }
        });

        Button choosePhotos = UiKit.button(listingId == null ? "Choose photos..." : "Replace photos...", "btn-outline", this::choosePhotos);
        choosePhotos.setGraphic(Icons.of(Icons.PLUS, 15, "#2f80ed"));

        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(14);
        g.add(UiKit.field("Title", title), 0, 0, 2, 1);
        g.add(UiKit.field("Address", address), 0, 1);
        g.add(UiKit.field("CDA Sector", sector), 1, 1);
        g.add(UiKit.field("Monthly rent (PKR)", rent), 0, 2);
        g.add(UiKit.field("Property type", chips(ListingDetails.PROPERTY_TYPES, typeGroup)), 1, 2);
        g.add(UiKit.field("Occupancy", chips(ListingDetails.OCCUPANCY_TYPES, occupancyGroup)), 0, 3);
        g.add(UiKit.field("Furnishing", chips(List.of("Furnished", "Unfurnished"), furnishGroup)), 1, 3);
        for (javafx.scene.Node n : g.getChildren()) GridPane.setHgrow(n, Priority.ALWAYS);

        VBox basics = UiKit.card(UiKit.label("Property details", "h3"), g, UiKit.field("Description", description));
        VBox facilities = UiKit.card(UiKit.label("Facilities advertised", "h3"),
                UiKit.wrapLabel("Tenants will later confirm whether these were really provided.", "muted", "small"),
                facilityPane, new HBox(10, customFacility, addCustom));
        VBox photosCard = UiKit.card(UiKit.label("Photos", "h3"),
                UiKit.wrapLabel("JPG, PNG or WEBP, max 4 MB each. The first photo is the cover.", "muted", "small"),
                choosePhotos, photoPreview);

        root.getChildren().setAll(basics, facilities, photosCard);

        if (listingId != null) {
            VBox statusCard = UiKit.card(UiKit.label("Current property status", "h3"),
                    chips(List.of("AVAILABLE", "OCCUPIED", "UNDER_MAINTENANCE"), statusGroup));
            root.getChildren().add(statusCard);
        }

        Button save = UiKit.button(listingId == null ? "Submit for verification" : "Save changes", "btn-primary", this::save);
        Button cancel = UiKit.button("Cancel", "btn-ghost", () -> Navigator.open(new MyListingsScreen()));
        HBox actions = new HBox(10, cancel, UiKit.spacer(), save);
        if (listingId != null) {
            Button delete = UiKit.button("Delete listing", "btn-danger", this::delete);
            delete.setGraphic(Icons.of(Icons.TRASH, 15, "#b91c1c"));
            actions.getChildren().add(1, delete);
        }
        actions.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().add(actions);
        root.setMaxWidth(980);
    }

    private FlowPane chips(List<String> values, ToggleGroup group) {
        FlowPane f = new FlowPane(8, 8);
        for (String v : values) {
            boolean isStatus = group == statusGroup;
            ToggleButton c = UiKit.chip(isStatus ? PropertyStatus.valueOf(v).label() : v);
            c.setUserData(v);
            c.setToggleGroup(group);
            f.getChildren().add(c);
        }
        group.selectedToggleProperty().addListener((o, a, b) -> { if (b == null && a != null) a.setSelected(true); });
        return f;
    }

    private void select(ToggleGroup g, String value) {
        for (Toggle t : g.getToggles()) if (t.getUserData().equals(value)) t.setSelected(true);
    }

    private String selected(ToggleGroup g) {
        return g.getSelectedToggle() == null ? null : g.getSelectedToggle().getUserData().toString();
    }

    private void addFacilityBox(String name, boolean checked) {
        for (CheckBox c : facilityBoxes) if (c.getText().equalsIgnoreCase(name)) { c.setSelected(true); return; }
        CheckBox cb = new CheckBox(name);
        cb.setSelected(checked);
        cb.setPrefWidth(160);
        facilityBoxes.add(cb);
        facilityPane.getChildren().add(cb);
    }

    private void choosePhotos() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose property photos");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        List<File> files = fc.showOpenMultipleDialog(Navigator.stage());
        if (files == null) return;
        photos.clear();
        photos.addAll(files);
        photoPreview.getChildren().clear();
        for (File f : files) {
            ImageView iv = new ImageView(new Image(f.toURI().toString(), 140, 95, false, true, true));
            photoPreview.getChildren().add(iv);
        }
    }

    // ---------------------------------------------------------------
    // [EDIT] fill form with current details (UC-04 step 2)
    // ---------------------------------------------------------------
    private void fill(PropertyListing l) {
        title.setText(l.getTitle());
        address.setText(l.getAddress());
        rent.setText(String.valueOf((long) l.getRent()));
        sector.setValue(l.getCdaSector());
        description.setText(l.getDescription());
        select(typeGroup, l.getPropertyType());
        select(occupancyGroup, l.getOccupancyType());
        select(furnishGroup, l.isFurnishing() ? "Furnished" : "Unfurnished");
        select(statusGroup, l.getPropertyStatus().name());
        for (String f : l.getFacilityNames()) addFacilityBox(f, true);
        photoPreview.getChildren().clear();
        for (String p : l.getPhotos()) photoPreview.getChildren().add(UiKit.imageView(p, 140, 95, 10));
        if (l.getRejectionReason() != null)
            root.getChildren().add(0, UiKit.card(UiKit.wrapLabel("Admin rejected this listing: " + l.getRejectionReason()
                    + "\nFix the details and save to resubmit it for verification.", "error-text")));
    }

    // ---------------------------------------------------------------
    // [SAVE] UC-03 steps 3-5 / UC-04 steps 3-5
    // ---------------------------------------------------------------
    private void save() {
        double rentValue;
        try {
            rentValue = Double.parseDouble(rent.getText().trim().replace(",", ""));
        } catch (NumberFormatException e) {
            UiKit.showError(new BusinessException("Rent must be a number, e.g. 45000."));
            return;
        }
        String status = selected(statusGroup);
        ListingDetails d = new ListingDetails(title.getText(), address.getText(), selected(typeGroup), sector.getValue(),
                rentValue, description.getText(), "Furnished".equals(selected(furnishGroup)), selected(occupancyGroup),
                photos, facilityBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).toList(),
                status == null ? PropertyStatus.AVAILABLE : PropertyStatus.valueOf(status));
        if (furnishGroup.getSelectedToggle() == null) {
            UiKit.showError(new BusinessException("Select whether the property is furnished."));
            return;
        }
        root.setDisable(true);
        if (listingId == null) {
            UiKit.async(() -> app.listings.createListing(d), l -> {
                UiKit.info("Listing submitted", "\"" + l.getTitle() + "\" was saved as Pending Verification.\n"
                        + "The Admin will verify it before it becomes public.");
                Navigator.open(new MyListingsScreen());
            }, () -> root.setDisable(false));
        } else {
            UiKit.async(() -> { app.listings.updateListing(listingId, d); return true; }, ok -> {
                UiKit.toast("Listing updated.");
                Navigator.open(new MyListingsScreen());
            }, () -> root.setDisable(false));
        }
    }

    private void delete() {
        UiKit.askReason("Delete this listing?", "Reason for deleting:").ifPresent(reason ->
                UiKit.action(() -> app.listings.deleteListing(listingId, reason), "Listing deleted.",
                        () -> Navigator.open(new MyListingsScreen())));
    }
}
