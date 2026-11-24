package com.gharbaar.ui.common;

import com.gharbaar.bl.domain.Report;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * =====================================================================
 * [UI] ReportIssueScreen  -  UC-05 Report Issue (Customer & Agent)
 * Form (type + description) and the list of my previous reports with
 * the Admin's resolution remarks (UC-08 result).
 * =====================================================================
 */
public class ReportIssueScreen extends Screen {

    private final String presetType;
    private final String presetText;
    private final VBox history = new VBox(10);

    public ReportIssueScreen() { this(null, null); }

    public ReportIssueScreen(String presetType, String presetText) {
        this.presetType = presetType;
        this.presetText = presetText;
    }

    @Override public String title() { return "Report Issue"; }

    @Override
    public Node build() {
        ComboBox<String> type = new ComboBox<>();
        type.getItems().setAll(Report.TYPES);
        type.setPromptText("Select issue type");
        type.setPrefWidth(280);
        if (presetType != null) type.setValue(presetType);
        TextArea description = new TextArea(presetText == null ? "" : presetText);
        description.setPromptText("Describe the issue (at least 10 characters)");
        description.setPrefRowCount(5);
        description.setWrapText(true);

        Button submit = UiKit.button("Submit report", "btn-primary", () ->
                UiKit.async(() -> app.reports.submitReport(type.getValue(), description.getText()), r -> {
                    UiKit.toast("Report submitted with status Open. The Admin has been informed.");
                    type.setValue(null);
                    description.clear();
                    loadHistory();
                }));

        VBox form = UiKit.card(UiKit.label("Report a problem", "h3"),
                UiKit.wrapLabel("Fake listing, fraud, wrong information or a technical problem? Tell the Admin.", "muted"),
                UiKit.field("Issue type", type), UiKit.field("Description", description), submit);
        loadHistory();
        return new VBox(20, form, UiKit.card(UiKit.label("My reports", "h3"), history));
    }

    private void loadHistory() {
        history.getChildren().setAll(UiKit.loading());
        UiKit.async(app.reports::getMyReports, list -> {
            history.getChildren().clear();
            if (list.isEmpty()) history.getChildren().add(UiKit.label("You have not reported any issues.", "muted"));
            for (Report r : list) {
                VBox text = new VBox(4, UiKit.label(r.getType() + "  ·  " + r.getDateTime().format(UiKit.DATE_TIME), "h3"),
                        UiKit.wrapLabel(r.getDescription(), "muted"));
                if (r.getResolutionRemarks() != null)
                    text.getChildren().add(UiKit.wrapLabel("Admin: " + r.getResolutionRemarks(), "field-label"));
                HBox row = new HBox(12, text, UiKit.spacer(), UiKit.statusBadge(r.getStatus()));
                row.setAlignment(Pos.CENTER_LEFT);
                history.getChildren().add(row);
            }
        });
    }
}
