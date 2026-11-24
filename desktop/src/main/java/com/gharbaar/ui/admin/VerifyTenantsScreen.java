package com.gharbaar.ui.admin;

import com.gharbaar.App;
import com.gharbaar.bl.domain.TenancyClaim;
import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.ui.common.Icons;
import com.gharbaar.ui.common.UiKit;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

/**
 * =====================================================================
 * [UI - ADMIN] VerifyTenantsScreen  -  UC-07 Verify Tenant
 * Unverified tenants -> their info (CNIC, phone) + tenancy claims with
 * proof documents (+ overlap flag) -> Approve or Reject with reason.
 * =====================================================================
 */
public class VerifyTenantsScreen extends MasterDetailScreen<Tenant> {

    @Override public String title() { return "Verify Tenants"; }
    @Override protected String listHeading() { return "Tenants waiting for verification"; }
    @Override protected String emptyTitle() { return "No tenants waiting"; }

    @Override protected Supplier<List<Tenant>> load() { return app.verification::getUnverifiedTenants; }

    @Override
    protected Node rowFor(Tenant t) {
        HBox row = new HBox(12, UiKit.avatar(t.getName(), 40),
                new VBox(3, UiKit.label(t.getName(), "h3"), UiKit.label("@" + t.getUsername() + "  ·  CNIC " + t.getCnic(), "muted", "small")));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    @Override
    protected Node detailsFor(Tenant t) {
        VBox claimsBox = new VBox(10, UiKit.loading());
        UiKit.async(() -> app.verification.getTenantClaims(t.getUserID()), claims -> {
            claimsBox.getChildren().clear();
            for (TenancyClaim c : claims) claimsBox.getChildren().add(claimCard(c));
        });

        GridPane facts = new GridPane();
        facts.setHgap(24);
        facts.setVgap(8);
        String[][] rows = {{"Full name", t.getName()}, {"Username", t.getUsername()}, {"Phone", t.getPhone()},
                {"CNIC", t.getCnic()}, {"Status", t.getVerificationStatus().label()}};
        for (int i = 0; i < rows.length; i++) {
            facts.add(UiKit.label(rows[i][0], "muted"), 0, i);
            facts.add(UiKit.label(rows[i][1], "field-label"), 1, i);
        }

        Button approve = UiKit.button("Confirm tenant as valid", "btn-success", () ->
                UiKit.action(() -> app.verification.approveTenant(t.getUserID()),
                        "Tenant verified and informed.", this::afterAction));
        approve.setGraphic(Icons.of(Icons.CHECK, 16, "#ffffff"));
        Button reject = UiKit.button("Reject", "btn-danger", () ->
                UiKit.askReason("Reject tenant " + t.getName(), "Reason (sent to the tenant):").ifPresent(reason ->
                        UiKit.action(() -> app.verification.rejectTenant(t.getUserID(), reason),
                                "Tenant rejected and informed.", this::afterAction)));
        reject.setGraphic(Icons.of(Icons.X, 16, "#b91c1c"));

        return new VBox(14, new HBox(12, UiKit.avatar(t.getName(), 52), UiKit.label(t.getName(), "h2")),
                facts, UiKit.label("Tenancy claims", "h3"), claimsBox, new HBox(10, approve, reject));
    }

    private Node claimCard(TenancyClaim c) {
        Button proof = UiKit.button("View proof", "btn-outline", () -> openProof(c.getProofDocument()));
        proof.setGraphic(Icons.of(Icons.FILE, 15, "#2f80ed"));
        HBox head = new HBox(10, UiKit.label(c.getListing().getTitle(), "h3"), UiKit.spacer(), UiKit.statusBadge(c.getStatus()));
        if (c.isFlagged()) head.getChildren().add(2, UiKit.badge("FLAGGED: overlaps another claim", "amber"));
        VBox v = new VBox(8, head,
                UiKit.label(c.getStartDate().format(UiKit.DATE) + "  to  " + c.getEndDate().format(UiKit.DATE)
                        + "  ·  Agent: " + c.getListing().getOwner().getName(), "muted"),
                proof);
        v.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-padding: 12;");
        return v;
    }

    /** Proof is fetched through the controller (admin-only) and opened in the default viewer. */
    private void openProof(String path) {
        UiKit.async(() -> app.verification.getProofDocument(path), f -> {
            try {
                String ext = f.contentType().equals("application/pdf") ? ".pdf" : f.contentType().endsWith("png") ? ".png" : ".jpg";
                File tmp = File.createTempFile("gharbaar-proof-", ext);
                tmp.deleteOnExit();
                Files.write(tmp.toPath(), f.data());
                App.hostServices().showDocument(tmp.toURI().toString());
            } catch (Exception ex) {
                UiKit.showError(ex);
            }
        });
    }
}
