package com.gharbaar.ui.admin;

import com.gharbaar.bl.domain.Report;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI - ADMIN] ReportsScreen  -  UC-08 Resolve Report
 * Open reports -> details -> resolution remarks -> Resolved (reporter notified)
 * =====================================================================
 */
public class ReportsScreen extends MasterDetailScreen<Report> {

    private boolean showResolved;

    @Override public String title() { return "Reports"; }
    @Override protected String emptyTitle() { return showResolved ? "No resolved reports" : "No open reports"; }

    @Override
    protected String listHeading() { return showResolved ? "Resolved reports" : "Open reports"; }

    @Override
    public Node build() {
        ToggleGroup g = new ToggleGroup();
        ToggleButton open = UiKit.chip("Open");
        ToggleButton resolved = UiKit.chip("Resolved");
        open.setToggleGroup(g);
        resolved.setToggleGroup(g);
        open.setSelected(true);
        g.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) { a.setSelected(true); return; }
            showResolved = b == resolved;
            detailBox.getChildren().setAll(UiKit.emptyState("Nothing selected", "Select a report."));
            listBox.getChildren().setAll(UiKit.loading());
            reload();
        });
        return new VBox(16, new HBox(8, open, resolved), super.build());
    }

    @Override
    protected Supplier<List<Report>> load() {
        return showResolved ? app.reports::getResolvedReports : app.reports::getOpenReports;
    }

    @Override
    protected Node rowFor(Report r) {
        HBox row = new HBox(12, new VBox(3, UiKit.label(r.getType(), "h3"),
                UiKit.label(r.getReporter().getName() + " (" + r.getReporter().getRole().toLowerCase() + ")  ·  "
                        + r.getDateTime().format(UiKit.DATE), "muted", "small")),
                UiKit.spacer(), UiKit.statusBadge(r.getStatus()));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    protected Node detailsFor(Report r) {
        VBox v = new VBox(12,
                new HBox(10, UiKit.label(r.getType(), "h2"), UiKit.spacer(), UiKit.statusBadge(r.getStatus())),
                UiKit.label("Reported by " + r.getReporter().getName() + "  ·  @" + r.getReporter().getUsername()
                        + "  ·  " + r.getReporter().getPhone(), "muted"),
                UiKit.label(r.getDateTime().format(UiKit.DATE_TIME), "muted", "small"),
                UiKit.label("Description", "field-label"),
                UiKit.wrapLabel(r.getDescription(), ""));
        if (r.isOpen()) {
            TextArea remarks = new TextArea();
            remarks.setPromptText("Action taken / resolution remarks (required)");
            remarks.setPrefRowCount(4);
            remarks.setWrapText(true);
            Button resolve = UiKit.button("Mark as resolved", "btn-success", () ->
                    UiKit.action(() -> app.reports.resolveReport(r.getReportID(), remarks.getText()),
                            "Report resolved. The reporter has been notified.", this::afterAction));
            resolve.setGraphic(Icons.of(Icons.CHECK, 16, "#ffffff"));
            v.getChildren().addAll(UiKit.field("Resolution remarks", remarks), resolve);
        } else {
            v.getChildren().addAll(UiKit.label("Resolution remarks", "field-label"), UiKit.wrapLabel(r.getResolutionRemarks(), "muted"));
        }
        return v;
    }
}
