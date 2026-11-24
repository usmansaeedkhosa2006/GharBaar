package com.gharbaar.ui.common;

import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.Locale;

/**
 * [UI] Icons - outline icons drawn with SVG paths (24x24 grid).
 */
public final class Icons {

    private Icons() { }

    public static final String HOME = "M3 10l9-7 9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10";
    public static final String SEARCH = "M11 3a8 8 0 1 0 0 16a8 8 0 1 0 0-16z M21 21l-4.35-4.35";
    public static final String BELL = "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9 M10.3 21a1.94 1.94 0 0 0 3.4 0";
    public static final String PLUS = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M12 8v8 M8 12h8";
    public static final String USER = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8z";
    public static final String USERS = "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 3a4 4 0 1 0 0 8a4 4 0 1 0 0-8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75";
    public static final String FLAG = "M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z M4 22v-7";
    public static final String LIST = "M8 6h13 M8 12h13 M8 18h13 M3 6h.01 M3 12h.01 M3 18h.01";
    public static final String CALENDAR = "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M16 2v4 M8 2v4 M3 10h18";
    public static final String LOGOUT = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9";
    public static final String SHIELD = "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4";
    public static final String GRID = "M3 3h7v7H3z M14 3h7v7h-7z M14 14h7v7h-7z M3 14h7v7H3z";
    public static final String PIN = "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M12 7a3 3 0 1 0 0 6a3 3 0 1 0 0-6z";
    public static final String SLIDERS = "M4 21v-7 M4 10V3 M12 21v-9 M12 8V3 M20 21v-5 M20 12V3 M1 14h6 M9 8h6 M17 16h6";
    public static final String PHONE = "M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.9.6 2.8.7a2 2 0 0 1 1.7 2z";
    public static final String MESSAGE = "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z";
    public static final String CHECK = "M20 6L9 17l-5-5";
    public static final String X = "M18 6L6 18 M6 6l12 12";
    public static final String HISTORY = "M3 12a9 9 0 1 0 3-6.7L3 8 M3 3v5h5 M12 7v5l4 2";
    public static final String FILE = "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M9 15l2 2 4-4";
    public static final String STAR = "M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8-6.2-3.2-6.2 3.2 1.2-6.8-5-4.9 6.9-1z";
    public static final String BACK = "M19 12H5 M12 19l-7-7 7-7";
    public static final String EDIT = "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.1 2.1 0 0 1 3 3L12 15l-4 1 1-4z";
    public static final String TRASH = "M3 6h18 M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6 M10 11v6 M14 11v6 M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2";
    public static final String WIFI = "M5 12.55a11 11 0 0 1 14.08 0 M1.42 9a16 16 0 0 1 21.16 0 M8.53 16.11a6 6 0 0 1 6.95 0 M12 20h.01";
    public static final String TV = "M4 7h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M17 2l-5 5-5-5";
    public static final String SOFA = "M20 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v3 M2 11v5a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-5a2 2 0 0 0-4 0v2H6v-2a2 2 0 0 0-4 0z M4 18v2 M20 18v2";
    public static final String ELEVATOR = "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M9 10l3-3 3 3 M9 14l3 3 3-3";
    public static final String SNOW = "M12 2v20 M4.9 7l14.2 10 M19.1 7L4.9 17 M9 4l3 2 3-2 M9 20l3-2 3 2";
    public static final String BOLT = "M13 2L3 14h9l-1 8 10-12h-9z";
    public static final String CAR = "M5 17h14 M3 13l2-6h14l2 6v5H3z M7 17v2 M17 17v2 M6.5 13.5h.01 M17.5 13.5h.01";
    public static final String FLAME = "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.4-.5-2-1-3-1.1-2.1-.2-4.1 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.2.4-2.3 1-3.1 0 1.4 1.1 2.6 2.5 2.6z";
    public static final String DUMBBELL = "M6.5 6.5v11 M17.5 6.5v11 M3 9v6 M21 9v6 M6.5 12h11";
    public static final String UTENSILS = "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2 M7 2v20 M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3z M18 15v7";

    /** Stroke-style icon */
    public static Node of(String path, double size, String color) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(color));
        p.setStrokeWidth(2);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        double scale = size / 24.0;
        p.setScaleX(scale);
        p.setScaleY(scale);
        Group g = new Group(p);
        g.setAutoSizeChildren(false);
        return new javafx.scene.layout.StackPane(g) {{
            setMinSize(size, size); setPrefSize(size, size); setMaxSize(size, size);
        }};
    }

    public static Node of(String path) { return of(path, 18, "#6b7280"); }

    /** Picks an icon for a facility name (amenities grid on details page). */
    public static String forFacility(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("wi")) return WIFI;
        if (n.contains("tv")) return TV;
        if (n.contains("furn")) return SOFA;
        if (n.contains("elev") || n.contains("lift")) return ELEVATOR;
        if (n.contains("air") || n.contains("ac")) return SNOW;
        if (n.contains("gener") || n.contains("ups") || n.contains("solar")) return BOLT;
        if (n.contains("park")) return CAR;
        if (n.contains("gas")) return FLAME;
        if (n.contains("gym")) return DUMBBELL;
        if (n.contains("meal") || n.contains("food")) return UTENSILS;
        if (n.contains("secur")) return SHIELD;
        return CHECK;
    }
}
