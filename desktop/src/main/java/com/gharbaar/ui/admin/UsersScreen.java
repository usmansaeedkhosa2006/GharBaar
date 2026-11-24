package com.gharbaar.ui.admin;

import com.gharbaar.bl.domain.Admin;
import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.bl.domain.User;
import com.gharbaar.ui.Navigator;
import com.gharbaar.ui.common.Screen;
import com.gharbaar.ui.common.UiKit;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

/**
 * [UI - ADMIN] UsersScreen - all accounts; suspend / re-activate
 * (tackling fraudulent use of the service).
 */
public class UsersScreen extends Screen {

    private final TableView<User> table = new TableView<>();
    private List<User> all = List.of();

    @Override public String title() { return "Users"; }

    @Override
    public Node build() {
        table.getColumns().setAll(
                col("Name", u -> u.getName(), 200),
                col("Username", u -> "@" + u.getUsername(), 160),
                col("Role", u -> u.getRole().charAt(0) + u.getRole().substring(1).toLowerCase(), 110),
                col("Phone", User::getPhone, 140),
                col("Tenant status", u -> u instanceof Tenant t ? t.getVerificationStatus().label() : "-", 130),
                col("Account", u -> u.isActive() ? "Active" : "Suspended", 110),
                actionColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(600);
        table.setPlaceholder(UiKit.loading());

        TextField filter = new TextField();
        filter.setPromptText("Search by name or username");
        filter.setPrefWidth(320);
        filter.textProperty().addListener((o, a, b) -> applyFilter(b));

        UiKit.async(app.admin::getAllUsers, users -> { all = users; applyFilter(filter.getText()); });
        return new VBox(16, new HBox(UiKit.label("All accounts", "h2"), UiKit.spacer(), filter), table);
    }

    private void applyFilter(String q) {
        String s = q == null ? "" : q.toLowerCase(Locale.ROOT);
        table.getItems().setAll(all.stream().filter(u -> u.getName().toLowerCase(Locale.ROOT).contains(s)
                || u.getUsername().toLowerCase(Locale.ROOT).contains(s)).toList());
    }

    private TableColumn<User, String> col(String name, java.util.function.Function<User, String> f, double w) {
        TableColumn<User, String> c = new TableColumn<>(name);
        c.setCellValueFactory(cd -> new SimpleStringProperty(f.apply(cd.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private TableColumn<User, String> actionColumn() {
        TableColumn<User, String> c = new TableColumn<>("Action");
        c.setPrefWidth(140);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                User u = empty || getTableRow() == null ? null : getTableRow().getItem();
                if (u == null || u instanceof Admin) { setGraphic(null); return; }
                Button b = UiKit.button(u.isActive() ? "Suspend" : "Activate", u.isActive() ? "btn-danger" : "btn-success", () -> toggle(u));
                b.setStyle("-fx-padding: 4 12;");
                setGraphic(b);
            }
        });
        return c;
    }

    private void toggle(User u) {
        boolean activate = !u.isActive();
        if (!activate && !UiKit.confirm("Suspend " + u.getName() + "?", "They will not be able to log in until re-activated.")) return;
        UiKit.action(() -> app.admin.setUserActive(u.getUserID(), activate),
                activate ? "Account re-activated." : "Account suspended.", () -> Navigator.open(new UsersScreen()));
    }
}
