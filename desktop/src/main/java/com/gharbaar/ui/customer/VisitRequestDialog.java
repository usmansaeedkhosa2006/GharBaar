package com.gharbaar.ui.customer;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.FormDialog;
import com.gharbaar.ui.common.UiKit;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * [UI - CUSTOMER] VisitRequestDialog  -  UC-12 Request Property Visit
 * Form: proposed date + time + optional notes.
 */
public class VisitRequestDialog extends FormDialog {

    private final PropertyListing listing;
    private final DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
    private final ComboBox<LocalTime> time = new ComboBox<>();
    private final TextArea notes = new TextArea();

    public VisitRequestDialog(PropertyListing listing) { this.listing = listing; }

    @Override protected String heading() { return "Request a visit to \"" + listing.getTitle() + "\""; }
    @Override protected String submitText() { return "Send request"; }

    @Override
    protected Node content() {
        List<LocalTime> slots = new ArrayList<>();
        for (int h = 9; h <= 20; h++) { slots.add(LocalTime.of(h, 0)); slots.add(LocalTime.of(h, 30)); }
        time.getItems().setAll(slots);
        time.setValue(LocalTime.of(17, 0));
        notes.setPromptText("Optional notes for the agent (e.g. preferred contact time)");
        notes.setPrefRowCount(3);
        notes.setWrapText(true);
        return new VBox(14,
                new HBox(12, UiKit.field("Proposed date", date), UiKit.field("Time", time)),
                UiKit.field("Notes (optional)", notes),
                UiKit.wrapLabel("The agent will be notified immediately and can accept or decline.", "muted", "small"));
    }

    @Override
    protected void submit() {
        LocalDateTime when = date.getValue() == null || time.getValue() == null ? null : LocalDateTime.of(date.getValue(), time.getValue());
        UiKit.async(() -> AppContext.get().visits.requestVisit(listing.getListingID(), when, notes.getText()),
                v -> { done("Visit request sent to " + listing.getOwner().getName() + "."); Navigator.refreshBadge(); });
    }
}
