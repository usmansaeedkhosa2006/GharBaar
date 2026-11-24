package com.gharbaar.ui.customer;

import com.gharbaar.bl.AppContext;
import com.gharbaar.bl.domain.PropertyListing;
import com.gharbaar.bl.domain.Tenant;
import com.gharbaar.bl.service.Session;
import com.gharbaar.ui.common.FormDialog;
import com.gharbaar.ui.common.UiKit;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * [UI - CUSTOMER] TenancyClaimDialog  -  UC-13 Submit Tenancy Claim
 * Tenancy start/end dates, CNIC and proof upload (rent agreement / receipt).
 */
public class TenancyClaimDialog extends FormDialog {

    private final PropertyListing listing;
    private final DatePicker start = new DatePicker();
    private final DatePicker end = new DatePicker();
    private final TextField cnic = new TextField();
    private final Label fileLabel = UiKit.label("No file chosen", "muted");
    private File proof;

    public TenancyClaimDialog(PropertyListing listing) { this.listing = listing; }

    @Override protected String heading() { return "Submit tenancy claim for \"" + listing.getTitle() + "\""; }
    @Override protected String submitText() { return "Submit claim"; }

    @Override
    protected Node content() {
        cnic.setPromptText("12345-1234567-1");
        if (Session.getInstance().getCurrentUser() instanceof Tenant t && t.getCnic() != null) cnic.setText(t.getCnic());
        Button choose = UiKit.button("Upload proof...", "btn-outline", () -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Rent agreement or receipt");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF or image", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
            File f = fc.showOpenDialog(dialog.getOwner());
            if (f != null) { proof = f; fileLabel.setText(f.getName()); }
        });
        return new VBox(14,
                new HBox(12, UiKit.field("Tenancy start", start), UiKit.field("Tenancy end", end)),
                UiKit.field("Your CNIC", cnic),
                UiKit.field("Proof document (PDF, JPG, PNG - max 4 MB)", new HBox(12, choose, fileLabel)),
                UiKit.wrapLabel("Your proof is only visible to the GharBaar Admin. Once verified you can review the facilities.", "muted", "small"));
    }

    @Override
    protected void submit() {
        UiKit.async(() -> AppContext.get().tenancy.submitClaim(listing.getListingID(), start.getValue(), end.getValue(), proof, cnic.getText().trim()),
                c -> done(c.isFlagged()
                        ? "Claim submitted. It overlaps another claim, so the Admin will review it closely."
                        : "Claim submitted with status Pending. The Admin will verify it."));
    }
}
