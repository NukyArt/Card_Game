package com.myjavacardgame.presentation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import java.util.HashMap;
import java.util.Map;

/** Cached fantasy-city illustrations; replace texture creation here to use authored art. */
public final class CardArtProvider {
    private final Map<String, Texture> textures = new HashMap<>();

    /** Returns a lazily created illustration for a catalog type. Requires a graphics context. */
    public Texture texture(String type) { return textures.computeIfAbsent(type, this::create); }

    /** Returns the stable type accent, with a neutral fallback for future types. */
    public Color accent(String type) {
        return Color.valueOf(switch (type) {
            case "gold-power" -> "D7B770";
            case "fields-feasts" -> "8FB77D";
            case "craft-stone" -> "C39275";
            case "arcana-mysteries" -> "A49AD6";
            default -> "89AFBA";
        });
    }

    private Texture create(String type) {
        var p = new Pixmap(256, 144, Pixmap.Format.RGBA8888);
        try {
            Color accent = accent(type);
            p.setColor(.055f, .12f, .16f, 1); p.fill();
            p.setColor(accent.r * .25f, accent.g * .25f, accent.b * .25f, 1);
            p.fillCircle(186, 38, 48);
            p.setColor(accent.r * .45f, accent.g * .45f, accent.b * .45f, 1);
            for (int i = 0; i < 8; i++) {
                int x = i * 36 - 10, h = 22 + (i * 17 % 34);
                p.fillRectangle(x, 128 - h, 29, h);
                p.fillTriangle(x - 3, 128 - h, x + 14, 113 - h, x + 32, 128 - h);
            }
            p.setColor(accent);
            if (type.equals("fields-feasts")) {
                for (int i = 0; i < 5; i++) {
                    int x = 71 + i * 27;
                    p.drawLine(x, 113, x + 11, 34 + Math.abs(i - 2) * 6);
                    for (int j = 0; j < 4; j++) {
                        p.fillTriangle(x + 4, 51 + j * 12, x - 9, 40 + j * 12, x + 4, 67 + j * 12);
                        p.fillTriangle(x + 5, 54 + j * 12, x + 19, 42 + j * 12, x + 5, 71 + j * 12);
                    }
                }
            } else if (type.equals("craft-stone")) {
                p.fillRectangle(67, 59, 122, 57);
                p.fillTriangle(57, 59, 127, 20, 199, 59);
                p.setColor(.1f, .16f, .19f, 1);
                for (int i = 0; i < 4; i++) p.fillRectangle(78 + i * 27, 65, 16, 9);
                p.fillRectangle(115, 85, 26, 31);
                p.drawLine(67, 81, 188, 81);
            } else if (type.equals("arcana-mysteries")) {
                p.fillTriangle(128, 15, 91, 65, 165, 65);
                p.fillRectangle(101, 65, 54, 57);
                p.fillCircle(190, 33, 15);
                p.setColor(.1f, .14f, .23f, 1); p.fillCircle(196, 28, 13);
                p.fillRectangle(119, 78, 18, 18);
                p.setColor(accent); p.drawCircle(128, 77, 56);
                p.drawLine(39, 40, 49, 40); p.drawLine(44, 35, 44, 45);
            } else {
                p.fillCircle(128, 72, 47);
                p.setColor(.14f, .2f, .21f, 1); p.fillCircle(128, 72, 39);
                p.setColor(accent);
                p.fillRectangle(101, 69, 54, 26);
                p.fillTriangle(101, 69, 92, 46, 119, 63);
                p.fillTriangle(119, 69, 128, 35, 139, 69);
                p.fillTriangle(137, 69, 164, 46, 155, 76);
                p.fillRectangle(102, 101, 52, 4);
            }
            p.setColor(accent.r * .5f, accent.g * .5f, accent.b * .5f, 1);
            p.drawLine(8, 134, 248, 134); p.drawRectangle(5, 5, 246, 134);
            Texture texture = new Texture(p);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return texture;
        } finally { p.dispose(); }
    }

    /** Releases only illustrations owned by this provider. */
    public void dispose() { textures.values().forEach(Texture::dispose); textures.clear(); }
}
