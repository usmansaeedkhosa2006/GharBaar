package com.gharbaar.ui.customer;

import com.gharbaar.bl.domain.ListingDetails;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.SearchCriteria;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * [UI - CUSTOMER] SearchScreen   -  UC-09 Search Properties
 * Layout follows the reference design: hero + search bar, category
 * chips, and the filter sheet (price histogram, type, sector,
 * furnishing, occupancy, amenities) shown as a side panel on desktop.
 * =====================================================================
 */
public class SearchScreen extends Screen {

    // ---- filter controls ----
    private final TextField searchField = new TextField();
    private final ToggleGroup categoryGroup = new ToggleGroup();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ToggleGroup sectorGroup = new ToggleGroup();
    private final ToggleGroup furnishGroup = new ToggleGroup();
    private final ToggleGroup occupancyGroup = new ToggleGroup();
    private final List<CheckBox> amenityBoxes = new ArrayList<>();
    private final Slider minSlider = new Slider(0, 300000, 0);
    private final Slider maxSlider = new Slider(0, 300000, 300000);
    private final Label rangeLabel = UiKit.label("", "h3");
    private final HBox histogram = new HBox(3);

    // ---- results ----
    private final FlowPane grid = new FlowPane(18, 18);
    private final Label resultCount = UiKit.label("", "muted");
    private final VBox resultsBox = new VBox(14);
    private boolean syncing;

    @Override public String title() { return "Find Home"; }

    @Override
    public Node build() {
        // ---------- [HERO] title + search bar ----------
        searchField.setPromptText("Search your properties... (title, sector, address)");
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> runSearch());
        Button filterBtn = UiKit.iconButton(Icons.SLIDERS, "Apply filters", this::runSearch);
        HBox searchBar = new HBox(10, Icons.of(Icons.SEARCH, 18, "#6b7280"), searchField, filterBtn);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle("-fx-background-color: #f3f5f9; -fx-background-radius: 14; -fx-padding: 4 6 4 14;");
        searchBar.setMaxWidth(720);
        VBox hero = new VBox(14, UiKit.label("Find your home where you want to Live", "h1"), searchBar);
        hero.getStyleClass().add("hero");

        // ---------- [CATEGORIES] quick property-type chips ----------
        HBox categories = new HBox(10);
        for (String t : withAll(ListingDetails.PROPERTY_TYPES)) {
            ToggleButton c = UiKit.chip(t);
            c.setUserData(t);
            c.setToggleGroup(categoryGroup);
            categories.getChildren().add(c);
        }
        categoryGroup.getToggles().get(0).setSelected(true);
        keepOneSelected(categoryGroup);
        categoryGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (syncing || b == null) return;
            syncing = true;
            selectByData(typeGroup, b.getUserData());
            syncing = false;
            runSearch();
        });
        HBox catHeader = new HBox(UiKit.label("Categories", "h3"), UiKit.spacer());

        // ---------- [RESULTS] ----------
        HBox resultsHeader = new HBox(10, UiKit.label("Properties for rent", "h3"), resultCount, UiKit.spacer());
        resultsHeader.setAlignment(Pos.CENTER_LEFT);
        resultsBox.getChildren().setAll(resultsHeader, grid);
        HBox.setHgrow(resultsBox, Priority.ALWAYS);

        HBox body = new HBox(22, filterPanel(), resultsBox);
        VBox page = new VBox(20, hero, new VBox(10, catHeader, categories), body);

        loadHistogramThenSearch();
        return page;
    }

    // ---------------------------------------------------------------
    // [FILTER PANEL] the "Filters" sheet from the design, as a sidebar
    // ---------------------------------------------------------------
    private Node filterPanel() {
        // --- price range + histogram ---
        minSlider.valueProperty().addListener((o, a, b) -> { if (b.doubleValue() > maxSlider.getValue()) maxSlider.setValue(b.doubleValue()); updateRangeLabel(); });
        maxSlider.valueProperty().addListener((o, a, b) -> { if (b.doubleValue() < minSlider.getValue()) minSlider.setValue(b.doubleValue()); updateRangeLabel(); });
        minSlider.setBlockIncrement(5000);
        maxSlider.setBlockIncrement(5000);
        histogram.setAlignment(Pos.BOTTOM_LEFT);
        histogram.setPrefHeight(56);
        histogram.setMinHeight(56);
        updateRangeLabel();
        VBox price = new VBox(8, UiKit.label("Price range (monthly rent)", "field-label"), rangeLabel,
                histogram, minSlider, maxSlider);

        // --- chips ---
        FlowPane types = chips(withAll(ListingDetails.PROPERTY_TYPES), typeGroup);
        typeGroup.selectedToggleProperty().addListener((o, a, b) -> {
            if (syncing || b == null) return;
            syncing = true;
            selectByData(categoryGroup, b.getUserData());
            syncing = false;
        });
        FlowPane sectors = chips(withAll(ListingDetails.CDA_SECTORS), sectorGroup);
        FlowPane furnish = chips(List.of("Any", "Furnished", "Unfurnished"), furnishGroup);
        FlowPane occupancy = chips(withAll(ListingDetails.OCCUPANCY_TYPES), occupancyGroup);

        // --- amenities (2 columns, "view more") ---
        GridPane amen = new GridPane();
        amen.setHgap(16);
        amen.setVgap(10);
        List<String> all = ListingDetails.COMMON_FACILITIES;
        for (int i = 0; i < all.size(); i++) {
            CheckBox cb = new CheckBox(all.get(i));
            amenityBoxes.add(cb);
            amen.add(cb, i % 2, i / 2);
            if (i >= 6) { cb.setVisible(false); cb.setManaged(false); }
        }
        Label viewMore = UiKit.label("View more", "link");
        viewMore.setOnMouseClicked(e -> {
            boolean show = viewMore.getText().equals("View more");
            for (int i = 6; i < amenityBoxes.size(); i++) { amenityBoxes.get(i).setVisible(show); amenityBoxes.get(i).setManaged(show); }
            viewMore.setText(show ? "View less" : "View more");
        });
        HBox amenHeader = new HBox(UiKit.label("Home amenities", "field-label"), UiKit.spacer(), viewMore);

        // --- buttons ---
        Button clear = UiKit.button("Clear all", "btn-outline", this::clearFilters);
        Button show = UiKit.button("Show result", "btn-primary", this::runSearch);
        clear.setMaxWidth(Double.MAX_VALUE);
        show.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clear, Priority.ALWAYS);
        HBox.setHgrow(show, Priority.ALWAYS);

        VBox panel = new VBox(18,
                new HBox(UiKit.label("Filters", "h3"), UiKit.spacer()),
                price,
                section("Property Type", types),
                section("CDA Sector", sectors),
                section("Furnishing", furnish),
                section("Occupancy", occupancy),
                new VBox(10, amenHeader, amen),
                new HBox(10, clear, show));
        panel.getStyleClass().add("card");
        panel.setPrefWidth(320);
        panel.setMinWidth(320);
        panel.setMaxWidth(320);
        return panel;
    }

    private VBox section(String title, Node content) { return new VBox(8, UiKit.label(title, "field-label"), content); }

    private FlowPane chips(List<String> values, ToggleGroup group) {
        FlowPane f = new FlowPane(8, 8);
        for (String v : values) {
            ToggleButton c = UiKit.chip(v);
            if (v.length() <= 4) c.getStyleClass().add("chip-square");
            c.setUserData(v);
            c.setToggleGroup(group);
            f.getChildren().add(c);
        }
        group.getToggles().get(0).setSelected(true);
        keepOneSelected(group);
        return f;
    }

    private static List<String> withAll(List<String> values) {
        List<String> l = new ArrayList<>();
        l.add(values.size() > 3 && !values.get(0).contains("-") ? "All" : "Any");
        l.addAll(values);
        return l;
    }

    private static void keepOneSelected(ToggleGroup g) {
        g.selectedToggleProperty().addListener((o, a, b) -> { if (b == null && a != null) a.setSelected(true); });
    }

    private static void selectByData(ToggleGroup g, Object data) {
        for (Toggle t : g.getToggles()) if (t.getUserData().equals(data)) { t.setSelected(true); return; }
    }

    private static String selected(ToggleGroup g) {
        Object v = g.getSelectedToggle() == null ? null : g.getSelectedToggle().getUserData();
        return v == null || v.equals("All") || v.equals("Any") ? null : v.toString();
    }

    private void updateRangeLabel() {
        rangeLabel.setText(UiKit.pkr(minSlider.getValue()) + "  -  " + UiKit.pkr(maxSlider.getValue()));
    }

    private void clearFilters() {
        searchField.clear();
        for (ToggleGroup g : List.of(typeGroup, sectorGroup, furnishGroup, occupancyGroup, categoryGroup))
            g.getToggles().get(0).setSelected(true);
        amenityBoxes.forEach(c -> c.setSelected(false));
        minSlider.setValue(0);
        maxSlider.setValue(maxSlider.getMax());
        runSearch();
    }

    // ---------------------------------------------------------------
    // [UC-09] build SearchCriteria from the UI and call the controller
    // ---------------------------------------------------------------
    private SearchCriteria criteria() {
        String furn = selected(furnishGroup);
        SearchCriteria c = new SearchCriteria()
                .setKeyword(searchField.getText())
                .setPropertyType(selected(typeGroup))
                .setCdaSector(selected(sectorGroup))
                .setOccupancyType(selected(occupancyGroup))
                .setFurnishing(furn == null ? null : furn.equals("Furnished"))
                .setFacilities(amenityBoxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).toList());
        if (minSlider.getValue() > 0) c.setMinRent(minSlider.getValue());
        if (maxSlider.getValue() < maxSlider.getMax()) c.setMaxRent(maxSlider.getValue());
        return c;
    }

    private void runSearch() {
        grid.getChildren().setAll(UiKit.loading());
        SearchCriteria c = criteria();
        UiKit.async(() -> app.search.search(c), this::showResults,
                () -> grid.getChildren().setAll(retryState()));
    }

    /** UC-09 ext. 4b: connectivity error -> allow retry */
    private Node retryState() {
        Button retry = UiKit.button("Retry", "btn-primary", this::runSearch);
        VBox v = new VBox(10, UiKit.label("Could not load properties.", "h3"), retry);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(40));
        return v;
    }

    private void showResults(List<PropertyListing> results) {
        resultCount.setText(results.size() + " found");
        if (results.isEmpty()) {                                                         // ext. 4a
            grid.getChildren().setAll(UiKit.emptyState("No results found",
                    "Try broadening your filters: increase the price range or remove some amenities."));
            return;
        }
        grid.getChildren().setAll(results.stream().map(PropertyCard::new).map(PropertyCard::node).toList());
    }

    /** Draws the rent histogram (like the design) from all verified listings. */
    private void loadHistogramThenSearch() {
        UiKit.async(() -> app.search.search(new SearchCriteria()), all -> {
            double max = all.stream().mapToDouble(PropertyListing::getRent).max().orElse(300000);
            max = Math.ceil(max / 50000.0) * 50000;
            minSlider.setMax(max);
            maxSlider.setMax(max);
            maxSlider.setValue(max);
            int bins = 24;
            int[] counts = new int[bins];
            for (PropertyListing l : all) counts[(int) Math.min(bins - 1, l.getRent() / max * bins)]++;
            int top = 1;
            for (int n : counts) top = Math.max(top, n);
            histogram.getChildren().clear();
            for (int n : counts) {
                Rectangle r = new Rectangle(8, 6 + 46.0 * n / top);
                r.setArcWidth(4);
                r.setArcHeight(4);
                r.setFill(Color.web(n == 0 ? "#e6eefb" : "#a9c9f5"));
                histogram.getChildren().add(r);
            }
            showResults(all);
        }, () -> grid.getChildren().setAll(retryState()));
    }

    // ---------------------------------------------------------------
    // [CARD] property card like "Nearby Properties" in the design
    // ---------------------------------------------------------------
    static class PropertyCard {
        private final PropertyListing l;
        PropertyCard(PropertyListing l) { this.l = l; }

        Node node() {
            StackPane img = new StackPane(UiKit.imageView(l.getPhotoPath(), 268, 170, 14));
            Label type = UiKit.badge(l.getPropertyType(), "blue");
            StackPane.setAlignment(type, Pos.TOP_LEFT);
            StackPane.setMargin(type, new Insets(10));
            img.getChildren().add(type);
            if (!l.isAvailable()) {
                Label st = UiKit.statusBadge(l.getPropertyStatus());
                StackPane.setAlignment(st, Pos.TOP_RIGHT);
                StackPane.setMargin(st, new Insets(10));
                img.getChildren().add(st);
            }

            HBox owner = new HBox(8, UiKit.avatar(l.getOwner().getName(), 24),
                    UiKit.label(l.getOwner().getName(), "small"), UiKit.label("/Agent", "muted", "small"));
            owner.setAlignment(Pos.CENTER_LEFT);
            Label title = UiKit.label(l.getTitle(), "h3");
            title.setMaxWidth(268);
            HBox addr = new HBox(4, Icons.of(Icons.PIN, 14, "#9ca3af"), UiKit.label(l.getAddress(), "muted", "small"));
            addr.setAlignment(Pos.CENTER_LEFT);
            Label view = UiKit.label("View details  >", "link");
            HBox bottom = new HBox(UiKit.label(UiKit.pkr(l.getRent()), "price"), UiKit.label(" /month", "muted", "small"),
                    UiKit.spacer(), view);
            bottom.setAlignment(Pos.CENTER_LEFT);

            VBox card = new VBox(10, img, owner, title, addr, bottom);
            card.getStyleClass().add("property-card");
            card.setPadding(new Insets(10, 10, 14, 10));
            card.setPrefWidth(290);
            card.setOnMouseClicked(e -> Navigator.open(new PropertyDetailsScreen(l.getListingID())));
            return card;
        }
    }
}
