package com.myjavacardgame.presentation;

import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.Align;
import com.myjavacardgame.domain.cards.*;
import java.util.stream.Collectors;

/** SpriteBatch card widget shared by market, boards and enlarged inspection. */
public final class CardRenderer {
    private final Texture pixel;
    private final BitmapFont font = UiFonts.create(24);
    private final GlyphLayout glyphs = new GlyphLayout();
    private final CardArtProvider art = new CardArtProvider();
    private final RussianGameText words;
    private final TableStyle style;

    /** Creates screen-owned GPU resources after graphics initialization. */
    public CardRenderer(RussianGameText words, TableStyle style) {
        this.words = words; this.style = style;
        font.setUseIntegerPositions(false);
        for (var region : font.getRegions()) region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        var image = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try { image.setColor(Color.WHITE); image.fill(); pixel = new Texture(image); }
        finally { image.dispose(); }
    }
    /** Draws a filled rectangle while restoring the caller's batch tint. */
    public void fill(SpriteBatch batch, TableLayout.Rect r, Color color) {
        batch.setColor(color); batch.draw(pixel, r.x(), r.y(), r.width(), r.height()); batch.setColor(Color.WHITE);
    }
    /** Draws wrapped text in full, fitting the font to the available area without ellipses. */
    public void text(SpriteBatch batch, String value, float x, float top, float width, float height, float size, Color color) {
        if (value == null || value.isEmpty() || width <= 0 || height <= 0) return;
        float fitted = size;
        do {
            font.getData().setScale(fitted / 24f);
            glyphs.setText(font, value, color, width, Align.left, true);
            if (glyphs.height <= height && glyphs.width <= width + .1f) break;
            fitted *= .95f;
        } while (fitted > 1);
        font.setColor(color);
        font.draw(batch, glyphs, x, top);
        font.getData().setScale(1);
    }
    /** Draws the ornamental amber card back shared by the pile and dealing cards. */
    public void back(SpriteBatch batch, TableLayout.Rect r) {
        fill(batch, new TableLayout.Rect(r.x() + 3, r.y() - 4, r.width(), r.height()), new Color(.28f, .14f, .05f, .22f));
        fill(batch, r, Color.valueOf("8A542B"));
        float inset = Math.min(r.width(), r.height()) * .045f;
        fill(batch, new TableLayout.Rect(r.x() + inset, r.y() + inset, r.width() - inset * 2, r.height() - inset * 2), Color.valueOf("FFF0CE"));
        fill(batch, new TableLayout.Rect(r.x() + inset * 2, r.y() + inset * 2, r.width() - inset * 4, r.height() - inset * 4), Color.valueOf(style.accentColor()));
        float cx = r.x() + r.width() / 2, cy = r.y() + r.height() / 2;
        for (int i = 0; i < 5; i++) {
            float y = r.y() + r.height() * (.16f + i * .17f);
            fill(batch, new TableLayout.Rect(r.x() + r.width() * .14f, y, r.width() * .72f, Math.max(1, r.height() * .008f)), Color.valueOf("D58C36"));
        }
        fill(batch, new TableLayout.Rect(cx - r.width() * .18f, cy - r.height() * .13f, r.width() * .36f, r.height() * .26f), Color.valueOf("8A542B"));
        fill(batch, new TableLayout.Rect(cx - r.width() * .14f, cy - r.height() * .10f, r.width() * .28f, r.height() * .20f), Color.valueOf("FFE7AD"));
        Color seal = Color.valueOf("8A542B");
        fill(batch, new TableLayout.Rect(cx - r.width() * .065f, cy - r.height() * .065f, r.width() * .13f, r.height() * .12f), seal);
        for (int i = 0; i < 3; i++) fill(batch, new TableLayout.Rect(cx + r.width() * (-.09f + i * .065f), cy + r.height() * .04f, r.width() * .05f, r.height() * .035f), seal);
    }
    /** Reveals a dealt card by turning its back through a narrow horizontal midpoint. */
    public void reveal(SpriteBatch batch, CardViewModel card, TableLayout.Rect r, boolean selected, boolean used, int upgrades, float progress) {
        if (progress >= 1) { draw(batch, card, r, selected, used, upgrades); return; }
        float turn = Math.max(0, Math.min(1, (progress - .3f) / .6f));
        float scale = Math.max(.025f, Math.abs(1 - 2 * turn));
        var turned = new TableLayout.Rect(r.x() + r.width() * (1 - scale) / 2, r.y(), r.width() * scale, r.height());
        if (turn < .5f) back(batch, turned);
        else if (scale < .3f) fill(batch, turned, Color.valueOf("FFF0CE"));
        else draw(batch, card, turned, selected, used, upgrades);
    }
    /** Formats all configured production values, including numeric effect parameters. */
    public String production(CardRoleParameters parameters) {
        String payout = words.amounts(parameters.production().amounts());
        String effects = parameters.production().effects().stream().map(e -> words.label("effect", e.kind())
                + (e.amount() == 0 ? "" : " " + e.amount())
                + (e.cost() == 0 ? "" : " / " + words.format("card.effectCost", "amount", e.cost()))
                + (e.resources().isEmpty() ? "" : " (" + e.resources().stream().map(r -> words.label("resource", r)).collect(Collectors.joining(", ")) + ")"))
                .collect(Collectors.joining("; "));
        return payout + (payout.isEmpty() || effects.isEmpty() ? "" : "; ") + effects;
    }
    /** Draws face-up card content; upgrade capability is separate from current affordability. */
    public void draw(SpriteBatch batch, CardViewModel card, TableLayout.Rect r, boolean selected, boolean used, int upgrades) {
        float x = r.x(), y = r.y(), w = r.width(), h = r.height();
        Color accent = art.accent(card.type());
        fill(batch, new TableLayout.Rect(x + 3, y - 5, w, h), new Color(0, 0, 0, .4f));
        fill(batch, r, selected ? Color.valueOf(style.accentColor()) : accent);
        float border = selected ? Math.max(3f, w * .035f) : 1.5f;
        fill(batch, new TableLayout.Rect(x + border, y + border, w - 2 * border, h - 2 * border), Color.valueOf("FFF5DB"));
        float p = w * .055f, inner = w - 2 * p;
        text(batch, card.name(), x + p, y + h - p, inner, h * .13f, h * .061f, Color.valueOf("4B2D1C"));
        text(batch, words.text("type." + card.type()), x + p, y + h * .825f, inner, h * .065f, h * .045f, Color.valueOf("735137"));
        batch.draw(art.texture(card.type()), x + p, y + h * .49f, inner, h * .27f);
        float strip = card.upgrade().isPresent() ? h * .16f : 0;
        String description = card.description().isBlank() ? words.text("card.activateDescription") : card.description();
        text(batch, description, x + p, y + h * .465f, inner, h * .07f, h * .044f, Color.valueOf("735137"));
        String effects = card.base().map(this::production).orElse(words.text("roleUnavailable"));
        text(batch, effects.isBlank() ? words.text("none") : effects, x + p, y + h * .378f, inner, h * .23f - strip * .3f, h * .048f, Color.valueOf("4B2D1C"));
        String price = card.base().map(b -> words.amounts(b.prices())).orElse("—");
        text(batch, words.text("card.cost") + " " + price, x + p, y + strip + h * .095f, inner, h * .068f, h * .045f, Color.valueOf("63522F"));
        card.upgrade().ifPresent(upgrade -> {
            fill(batch, new TableLayout.Rect(x + 2, y + 2, w - 4, strip), new Color(accent.r * .3f, accent.g * .3f, accent.b * .3f, 1));
            text(batch, words.text("card.upgradeShort") + " " + words.amounts(upgrade.prices()) + "\n" + production(upgrade),
                    x + p, y + strip - 1, inner, strip - 3, h * .043f, Color.valueOf("F2E9D3"));
        });
        if (selected) {
            fill(batch, new TableLayout.Rect(x + 3, y + h * .46f, w - 6, h * .065f), Color.valueOf(style.accentColor()));
            text(batch, words.text("card.selected"), x + p, y + h * .518f, inner, h * .06f, h * .041f, Color.valueOf("4B2D1C"));
        }
        if (used) {
            fill(batch, new TableLayout.Rect(x, y + h * .5f, w, h * .12f), new Color(.30f, .16f, .07f, .9f));
            text(batch, words.text("used"), x + p, y + h * .6f, inner, h * .1f, h * .052f, Color.WHITE);
        }
        if (upgrades > 0) {
            fill(batch, new TableLayout.Rect(x + w - 23, y + h - 15, 23, 15), Color.valueOf("8A542B"));
            text(batch, "+" + upgrades, x + w - 21, y + h - 3, 21, 12, 10, Color.WHITE);
        }
    }
    /** Releases textures and fonts owned by this renderer. */
    public void dispose() { pixel.dispose(); font.dispose(); art.dispose(); }
}
