package com.myjavacardgame.presentation;

/** Pure reference-resolution geometry shared by rendering and pointer input. */
public final class TableLayout {
    public static final int WIDTH = 1280, HEIGHT = 720;
    private final TableStyle style;

    /** Creates geometry from validated external visual tuning. */
    public TableLayout(TableStyle style) { this.style = style; }

    /** Immutable bottom-left rectangle with half-open pointer boundaries. */
    public record Rect(float x, float y, float width, float height) {
        /** Returns the right edge. */
        public float right() { return x + width; }
        /** Returns the top edge. */
        public float top() { return y + height; }
        /** Tests a pointer without claiming an adjacent rectangle's boundary. */
        public boolean contains(float px, float py) { return px >= x && px < right() && py >= y && py < top(); }
        /** Tests positive-area intersection. */
        public boolean overlaps(Rect other) { return x < other.right() && right() > other.x && y < other.top() && top() > other.y; }
        /** Scales around the center and raises the card. */
        public Rect lifted(float lift, float scale) {
            return new Rect(x - width * (scale - 1) / 2, y + lift - height * (scale - 1) / 2, width * scale, height * scale);
        }
    }
    /** Uniform physical-to-logical mapping; physical pointer Y begins at the top. */
    public record Viewport(float scale, float offsetX, float offsetY) {
        /** Converts physical X. */
        public float x(float physicalX) { return (physicalX - offsetX) / scale; }
        /** Converts physical top-origin Y. */
        public float y(float physicalY) { return HEIGHT - (physicalY - offsetY) / scale; }
    }
    /** Fits the table without stretching card proportions. */
    public static Viewport viewport(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Window must be positive");
        float scale = Math.min(width / (float) WIDTH, height / (float) HEIGHT);
        return new Viewport(scale, (width - WIDTH * scale) / 2, (height - HEIGHT * scale) / 2);
    }
    /** Returns a stable base slot: human bottom, bots top/left/right. */
    public Rect board(int seat, int index) {
        if (seat < 0 || seat > 3 || index < 0 || index >= 6) throw new IllegalArgumentException("Unsupported table seat/slot");
        float h = seat == 0 ? style.cardHeight() : seat == 1 ? 104 : 112;
        float w = h * style.cardAspect();
        if (seat <= 1) return new Rect((WIDTH - (w * 6 + style.gap() * 5)) / 2 + index * (w + style.gap()), seat == 0 ? 35 : 568, w, h);
        return new Rect((seat == 2 ? 25 : WIDTH - 25 - w * 2 - style.gap()) + (index % 2) * (w + style.gap()),
                435 - (index / 2) * (h + 22), w, h);
    }
    /** Returns a centered market slot; empty slots retain their location. */
    public Rect market(int index, int count) {
        if (count <= 0 || index < 0 || index >= count) throw new IllegalArgumentException("Invalid market index");
        float h = Math.min(style.cardHeight(), (700 - style.gap() * (count - 1)) / count / style.cardAspect());
        float w = h * style.cardAspect();
        return new Rect((WIDTH - count * w - (count - 1) * style.gap()) / 2 + index * (w + style.gap()), 277, w, h);
    }
    /** Returns the central project panel. */
    public Rect project() { return new Rect(274, 461, 732, 91); }
    /** Returns the destination for discarded market cards. */
    public Rect discard() { return new Rect(945, 284, 45, 70); }
}
