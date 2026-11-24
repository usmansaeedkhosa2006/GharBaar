package com.gharbaar.ui.common;

import com.gharbaar.bl.AppContext;
import javafx.scene.Node;

/**
 * [UI] Screen (abstract) - every page shown inside the main window.
 * Polymorphism: MainShell just calls build() on whatever screen it opens.
 */
public abstract class Screen {

    /** Shortcut to the controllers (UI talks ONLY to controllers). */
    protected final AppContext app = AppContext.get();

    /** Title shown in the top bar. */
    public abstract String title();

    /** Which sidebar item to highlight. */
    public String navKey() { return title(); }

    /** Builds the page content. */
    public abstract Node build();
}
