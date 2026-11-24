package com.gharbaar.ui.common;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;

/**
 * [UI] FormDialog (abstract) - base for all pop-up forms.
 * Template Method (GoF): show() builds the dialog and calls the
 * subclass hooks content() and submit(); the dialog stays open while
 * submit() reports an error so the user can correct the input.
 */
public abstract class FormDialog {

    protected final Dialog<ButtonType> dialog = new Dialog<>();

    protected abstract String heading();
    protected abstract String submitText();
    protected abstract Node content();

    /** Runs the use case. Call done() when it succeeds. */
    protected abstract void submit();

    public void show() {
        dialog.setTitle("GharBaar");
        dialog.setHeaderText(heading());
        dialog.getDialogPane().getStylesheets().add(UiKit.stylesheet());
        ButtonType ok = new ButtonType(submitText(), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ok).getStyleClass().add("btn-primary");
        VBox body = new VBox(14, content());
        body.setPadding(new Insets(10, 4, 4, 4));
        body.setPrefWidth(500);
        dialog.getDialogPane().setContent(body);
        dialog.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            e.consume();            // keep open; submit() closes it via done()
            submit();
        });
        dialog.show();
    }

    protected void done(String toast) {
        dialog.close();
        if (toast != null) UiKit.toast(toast);
    }
}
