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
    /** Draws bounded wrapped text, reserving full content for inspection when space is limited. */
    public void text(SpriteBatch batch, String value, float x, float top, float width, float height, float size, Color color) {
        font.getData().setScale(size / 24f);
        font.setColor(color);
        String visible = value;
        glyphs.setText(font, visible, color, width, Align.left, true);
        if (glyphs.height > height) {
            int length = Math.min(visible.length(), Math.max(0, (int) (visible.length() * height / glyphs.height) - 3));
            visible = visible.substring(0, length).stripTrailing() + "...";
            glyphs.setText(font, visible, color, width, Align.left, true);
            while (glyphs.height > height && length > 0) {
                visible = value.substring(0, --length).stripTrailing() + "...";
                glyphs.setText(font, visible, color, width, Align.left, true);
            }
        }
        font.draw(batch, glyphs, x, top);
        font.getData().setScale(1);
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
        fill(batch, new TableLayout.Rect(x + border, y + border, w - 2 * border, h - 2 * border), Color.valueOf("E7DFC9"));
        float p = w * .055f, inner = w - 2 * p;
        text(batch, card.name(), x + p, y + h - p, inner, h * .13f, h * .061f, Color.valueOf("202E31"));
        text(batch, words.text("type." + card.type()), x + p, y + h * .825f, inner, h * .065f, h * .045f, Color.valueOf("526063"));
        batch.draw(art.texture(card.type()), x + p, y + h * .49f, inner, h * .27f);
        float strip = card.upgrade().isPresent() ? h * .16f : 0;
        String description = card.description().isBlank() ? words.text("card.activateDescription") : card.description();
        text(batch, description, x + p, y + h * .465f, inner, h * .07f, h * .044f, Color.valueOf("526063"));
        String effects = card.base().map(this::production).orElse(words.text("roleUnavailable"));
        text(batch, effects.isBlank() ? words.text("none") : effects, x + p, y + h * .378f, inner, h * .23f - strip * .3f, h * .048f, Color.valueOf("202E31"));
        String price = card.base().map(b -> words.amounts(b.prices())).orElse("—");
        text(batch, words.text("card.cost") + " " + price, x + p, y + strip + h * .095f, inner, h * .068f, h * .045f, Color.valueOf("63522F"));
        card.upgrade().ifPresent(upgrade -> {
            fill(batch, new TableLayout.Rect(x + 2, y + 2, w - 4, strip), new Color(accent.r * .3f, accent.g * .3f, accent.b * .3f, 1));
            text(batch, words.text("card.upgradeShort") + " " + words.amounts(upgrade.prices()) + "\n" + production(upgrade),
                    x + p, y + strip - 1, inner, strip - 3, h * .043f, Color.valueOf("F2E9D3"));
        });
        if (selected) {
            fill(batch, new TableLayout.Rect(x + 3, y + h * .46f, w - 6, h * .065f), Color.valueOf(style.accentColor()));
            text(batch, words.text("card.selected"), x + p, y + h * .518f, inner, h * .06f, h * .041f, Color.valueOf("202E31"));
        }
        if (used) {
            fill(batch, new TableLayout.Rect(x, y + h * .5f, w, h * .12f), new Color(.05f, .12f, .15f, .85f));
            text(batch, words.text("used"), x + p, y + h * .6f, inner, h * .1f, h * .052f, Color.WHITE);
        }
        if (upgrades > 0) {
            fill(batch, new TableLayout.Rect(x + w - 23, y + h - 15, 23, 15), Color.valueOf("364D50"));
            text(batch, "+" + upgrades, x + w - 21, y + h - 3, 21, 12, 10, Color.WHITE);
        }
    }
    /** Releases textures and fonts owned by this renderer. */
    public void dispose() { pixel.dispose(); font.dispose(); art.dispose(); }
}
