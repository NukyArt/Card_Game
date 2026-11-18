package com.myjavacardgame.presentation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * A reusable, procedurally painted warm fantasy city backdrop for menu screens.
 * The generated texture is owned by this object and must be disposed with it.
 */
public final class FantasyBackground {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private final Texture texture;

    /** Creates the city illustration after a libGDX graphics context is available. */
    public FantasyBackground() {
        var pixmap = new Pixmap(WIDTH, HEIGHT, Pixmap.Format.RGBA8888);
        try {
            paint(pixmap);
            texture = new Texture(pixmap);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } finally {
            pixmap.dispose();
        }
    }

    /** Draws the illustration to cover the menu's virtual design area. */
    public void draw(SpriteBatch batch) {
        batch.setColor(Color.WHITE);
        batch.draw(texture, 0, 0, WIDTH, HEIGHT, 0, 0, WIDTH, HEIGHT, false, true);
    }

    private void paint(Pixmap p) {
        for (int y = 0; y < HEIGHT; y++) {
            float t = y / (float) (HEIGHT - 1);
            p.setColor(lerp("FFF1C9", "F7C77D", t));
            p.drawLine(0, y, WIDTH, y);
        }

        // Soft morning sun and the pale glow kept behind menu lettering.
        p.setColor(Color.valueOf("FFF8D8")); p.fillCircle(1010, 560, 104);
        p.setColor(Color.valueOf("FFF3C1")); p.fillCircle(1010, 560, 81);
        p.setColor(Color.valueOf("FFE9B0"));
        p.setColor(Color.valueOf("FCE0A8")); p.fillRectangle(0, 0, WIDTH, 180);
        p.setColor(Color.valueOf("F6D499")); p.fillRectangle(0, 180, WIDTH, 48);

        // Distant ochre skyline, then a richer foreground city on the edges.
        city(p, 0, 206, 76, 112, "E6B96E", "C9854B");
        city(p, 830, 206, 58, 118, "E5B56A", "B96F3D");
        tower(p, 72, 185, 96, 235, "E6BD78", "B96B3A");
        tower(p, 190, 214, 70, 164, "F0C782", "C67643");
        tower(p, 1038, 178, 108, 247, "E7B66C", "B96235");
        tower(p, 1155, 229, 72, 151, "F2C982", "C87743");
        for (int y = 0; y < 110; y++) {
            p.setColor(lerp("DDA85E", "FCE0A8", y / 109f));
            p.drawLine(0, y, WIDTH - 1, y);
        }

        // A feathered wash behind lettering, without a rectangular panel edge.
        for (int x = 200; x < 1080; x++) {
            float strength = Math.max(0, 1 - Math.abs(x - 640) / 440f);
            p.setColor(1f, .96f, .82f, .5f * strength * strength);
            p.drawLine(x, 0, x, HEIGHT - 1);
        }
    }

    private void city(Pixmap p, int startX, int ground, int step, int maxHeight, String wall, String roof) {
        for (int i = 0; i < 7; i++) {
            int x = startX + i * step;
            int width = step - 8;
            int height = 48 + (i * 29 % maxHeight);
            p.setColor(Color.valueOf(wall)); p.fillRectangle(x, ground, width, height);
            p.setColor(Color.valueOf(roof)); p.fillTriangle(x - 8, ground + height, x + width / 2, ground + height + 34, x + width + 8, ground + height);
            p.setColor(Color.valueOf("9B6039")); p.fillRectangle(x + width / 2 - 5, ground, 10, 25);
            p.setColor(Color.valueOf("FFF0BB"));
            p.fillRectangle(x + 12, ground + height / 2, 7, 12);
            p.fillRectangle(x + width - 19, ground + height / 2, 7, 12);
        }
    }

    private void tower(Pixmap p, int x, int ground, int width, int height, String wall, String roof) {
        p.setColor(Color.valueOf(wall)); p.fillRectangle(x, ground, width, height);
        p.setColor(Color.valueOf(roof)); p.fillTriangle(x - 13, ground + height, x + width / 2, ground + height + 70, x + width + 13, ground + height);
        p.setColor(Color.valueOf("A4653B")); p.fillRectangle(x + width / 2 - 9, ground, 18, 38);
        p.setColor(Color.valueOf("FFF0B9"));
        for (int y = ground + 55; y < ground + height - 20; y += 43) {
            p.fillRectangle(x + 17, y, 10, 17);
            p.fillRectangle(x + width - 27, y, 10, 17);
        }
        p.setColor(Color.valueOf("C68348")); p.drawRectangle(x, ground, width, height);
    }

    private Color lerp(String lower, String upper, float progress) {
        return new Color(Color.valueOf(lower)).lerp(Color.valueOf(upper), progress);
    }

    /** Releases the generated texture owned by this backdrop. */
    public void dispose() { texture.dispose(); }
}
