package com.gharbaar.ui.admin;

import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI - ADMIN] MasterDetailScreen<T> (abstract, generic)
 * Template Method: list of items on the left, details of the selected
 * item on the right. Used by Verify Listings, Verify Tenants, Reports.
 * Subclasses only define: load(), rowFor(item), detailsFor(item).
 * =====================================================================
 */
public abstract class MasterDetailScreen<T> extends Screen {

    protected final VBox listBox = new VBox(10);
    protected final VBox detailBox = new VBox(14);

    protected abstract Supplier<List<T>> load();
    protected abstract Node rowFor(T item);
    protected abstract Node detailsFor(T item);
    protected abstract String emptyTitle();
    protected String listHeading() { return title(); }

    @Override
    public Node build() {
        listBox.getChildren().setAll(UiKit.loading());
        detailBox.getChildren().setAll(UiKit.emptyState("Nothing selected", "Select an item from the list to review its details."));
        reload();
        VBox left = UiKit.card(UiKit.label(listHeading(), "h3"), listBox);
        left.setPrefWidth(400);
        left.setMinWidth(380);
        VBox right = UiKit.card(detailBox);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox root = new HBox(20, left, right);
        root.setPadding(new Insets(0));
        return root;
    }

    protected void reload() {
        UiKit.async(load(), items -> {
            listBox.getChildren().clear();
            if (items.isEmpty()) listBox.getChildren().add(UiKit.emptyState(emptyTitle(), "The queue is empty."));
            for (T item : items) {
                Node row = rowFor(item);
                row.setStyle("-fx-cursor: hand; -fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-padding: 12;");
                row.setOnMouseClicked(e -> select(item, row));
                listBox.getChildren().add(row);
            }
        });
    }

    protected void select(T item, Node row) {
        listBox.getChildren().forEach(n -> n.setStyle(n.getStyle().replace("-fx-border-color: #2f80ed; -fx-border-radius: 14;", "")));
        row.setStyle(row.getStyle() + "-fx-border-color: #2f80ed; -fx-border-radius: 14;");
        detailBox.getChildren().setAll(detailsFor(item));
    }

    /** After an approve / reject / resolve: refresh list and clear details. */
    protected void afterAction() {
        detailBox.getChildren().setAll(UiKit.emptyState("Done", "Select the next item from the list."));
        reload();
    }
}
