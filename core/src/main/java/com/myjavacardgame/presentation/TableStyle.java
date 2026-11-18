package com.myjavacardgame.presentation;

import com.badlogic.gdx.utils.JsonReader;
import java.io.Reader;

/** Validated presentation tunables, unrelated to gameplay balance. */
public record TableStyle(float cardAspect, float cardHeight, float gap, float hoverLift, float hoverScale,
        float hoverSeconds, float moveSeconds, float effectSeconds, boolean animations,
        String tableColor, String panelColor, String accentColor) {
    /** High-contrast ink shared by light UI surfaces. */
    public static final String INK = "4B2D1C";
    /** Secondary ink for supporting labels. */
    public static final String MUTED_INK = "735137";
    /** Dark amber for headings on parchment. */
    public static final String HEADING = "8A431C";

    /** Rejects configurations that cannot fit the six-base table. */
    public TableStyle {
        for (float value : new float[]{cardAspect, cardHeight, gap, hoverLift, hoverScale, hoverSeconds, moveSeconds, effectSeconds}) {
            if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid table style value");
        }
        if (cardAspect < .5f || cardAspect > .8f || cardHeight < 120 || cardHeight > 160 || gap < 2 || gap > 12
                || hoverScale < 1 || hoverScale > 1.1f || hoverLift > 16) throw new IllegalArgumentException("Table style exceeds available space");
        for (String color : new String[]{tableColor, panelColor, accentColor}) {
            if (color == null || !color.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected RGB color");
        }
    }
    /** Returns reference-resolution defaults suitable for injected test fixtures. */
    public static TableStyle defaults() {
        return new TableStyle(.64f, 154, 10, 12, 1.05f, .15f, .45f, .65f, true, "EFC98D", "FFF0CE", "F7B253");
    }
    /** Loads the complete style from a caller-owned reader. */
    public static TableStyle load(Reader reader) {
        var root = new JsonReader().parse(reader);
        return new TableStyle(root.getFloat("cardAspect"), root.getFloat("cardHeight"), root.getFloat("gap"), root.getFloat("hoverLift"),
                root.getFloat("hoverScale"), root.getFloat("hoverSeconds"), root.getFloat("moveSeconds"), root.getFloat("effectSeconds"),
                root.getBoolean("animations"), root.getString("tableColor"), root.getString("panelColor"), root.getString("accentColor"));
    }
}
