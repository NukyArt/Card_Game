package com.myjavacardgame.presentation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static com.myjavacardgame.presentation.GameNavigation.Action.*;

/** Russian main menu, mode selection and modal return confirmation. */
public final class MainMenuScreen {
    private static final int WIDTH = 1280, HEIGHT = 720;
    private static final Color INK = Color.valueOf("4A291B");
    private static final Color SUBTLE_INK = Color.valueOf("79533A");
    private static final Color ACCENT = Color.valueOf("B96832");
    private static final Color GOLD = Color.valueOf("F7B253");
    private static final Color DISABLED = Color.valueOf("806046");
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private final Texture pixel;
    private final FantasyBackground background = new FantasyBackground();
    private final GlyphLayout layout = new GlyphLayout();
    private final List<Button> buttons = new ArrayList<>();
    private int frames;
    private record Button(float x, float y, float width, String label, boolean enabled, Runnable action) { }
    private final String scenarioLabel;
    private final CardArtProvider art = new CardArtProvider();
    private final com.badlogic.gdx.utils.JsonValue scenario;
    private final String scenarioName;

    /** Creates the ordinary menu after graphics initialization. */
    public MainMenuScreen() { this(""); }

    /** Creates the menu with an explicit optional QA scenario marker. */
    public MainMenuScreen(String scenarioLabel) {
        this.scenarioLabel = scenarioLabel;
        try (var config = Gdx.files.internal("ui/scenario-selection.json").reader("UTF-8")) {
            scenario = new com.badlogic.gdx.utils.JsonReader().parse(config);
            try (var project = Gdx.files.internal(scenario.getString("projectPath")).reader("UTF-8")) {
                scenarioName = new com.myjavacardgame.application.ProjectCatalogLoader().load(project).displayName();
            }
        } catch (Exception exception) { throw new IllegalStateException("Unable to load scenario menu", exception); }
        font = UiFonts.create(24);
        var pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        try { pixmap.setColor(Color.WHITE); pixmap.fill(); pixel = new Texture(pixmap); }
        finally { pixmap.dispose(); }
    }

    /** Renders one menu frame and dispatches at most one enabled choice. */
    public void render(GameNavigation.Page page, Consumer<GameNavigation.Action> select) {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        if (width <= 0 || height <= 0) return;
        float scale = Math.min(width / (float) WIDTH, height / (float) HEIGHT);
        float virtualWidth = width / scale, virtualHeight = height / scale;
        float offsetX = (virtualWidth - WIDTH) / 2, offsetY = (virtualHeight - HEIGHT) / 2;
        float mouseX = Gdx.input.getX() / scale - offsetX;
        float mouseY = (height - Gdx.input.getY()) / scale - offsetY;
        ScreenUtils.clear(.96f, .82f, .56f, 1);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(-offsetX, -offsetY, virtualWidth, virtualHeight));
        buttons.clear();
        batch.begin();
        background.draw(batch);
        if (!scenarioLabel.isEmpty()) centered(scenarioLabel, 690, GOLD);
        if (page == GameNavigation.Page.MAIN_MENU) {
            centered("Главное меню", 575, INK); centered("Карточная игра", 532, SUBTLE_INK);
            button(420, "Создать лобби", true, () -> select.accept(CREATE_LOBBY));
            button(338, "Присоединиться к лобби — Скоро", false, () -> select.accept(JOIN_LOBBY));
            button(256, "Настройки — Скоро", false, () -> select.accept(SETTINGS));
            button(174, "Выход из игры", true, () -> Gdx.app.exit());
        } else if (page == GameNavigation.Page.MODE_SELECTION) {
            centered("Создать лобби", 575, INK); centered("Выберите режим игры", 532, SUBTLE_INK);
            button(448, "Сетевая игра — Скоро", false, () -> select.accept(NETWORK_GAME));
            button(374, "Локальная игра — Скоро", false, () -> select.accept(LOCAL_GAME));
            button(300, "2 на 2 — Скоро", false, () -> { });
            button(226, "Играть с ботами", true, () -> select.accept(PLAY_BOTS)); button(152, "Назад", true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.SCENARIO_SELECTION) {
            centered(scenario.getString("selectionTitle"), 575, INK);
            batch.draw(art.texture(scenario.getString("illustrationType")), 460, 325, 360, 202.5f);
            centered(scenario.getString("illustrationCaption"), 307, SUBTLE_INK);
            button(230, scenarioName, true, () -> select.accept(OPEN_SCENARIO)); button(155, scenario.getString("back"), true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.SCENARIO_DETAILS) {
            centered(scenarioName, 645, INK); batch.draw(art.texture(scenario.getString("illustrationType")), 480, 390, 320, 180);
            centered(scenario.getString("illustrationCaption"), 370, SUBTLE_INK);
            font.getData().setScale(.65f); layout.setText(font, scenario.getString("description"), SUBTLE_INK, 450, Align.center, true); font.draw(batch, layout, 415, 320); font.getData().setScale(1);
            centered(scenario.getString("difficultyTitle"), 280, SUBTLE_INK);
            buttons.add(new Button(410, 190, 150, scenario.getString("easy"), false, () -> { }));
            buttons.add(new Button(565, 190, 150, scenario.getString("normal"), false, () -> { }));
            buttons.add(new Button(720, 190, 150, scenario.getString("hard"), false, () -> { }));
            button(120, scenario.getString("play"), true, () -> select.accept(START_SCENARIO)); button(50, scenario.getString("back"), true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.CONFIRM_RETURN) {
            centered("Завершить текущую партию?", 545, INK); centered("Прогресс не сохранится", 497, SUBTLE_INK); centered("Партия приостановлена", 445, SUBTLE_INK);
            button(310, "Продолжить игру", true, () -> select.accept(CANCEL_RETURN)); button(230, "Завершить партию", true, () -> select.accept(CONFIRM_RETURN));
        }
        for (Button button : buttons) drawButton(button, mouseX, mouseY);
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { select.accept(page == GameNavigation.Page.CONFIRM_RETURN ? CANCEL_RETURN : BACK); return; }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) for (Button button : List.copyOf(buttons)) if (button.enabled() && contains(button, mouseX, mouseY)) { button.action().run(); break; }
        frames++;
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || frames == Integer.getInteger("ccgs.captureFrame", -1)) { capture(page); if (Boolean.getBoolean("ccgs.exitAfterCapture")) Gdx.app.exit(); }
    }

    private void drawButton(Button button, float mouseX, float mouseY) {
        boolean hovered = contains(button, mouseX, mouseY) && button.enabled();
        font.getData().setScale(button.width() < 200 ? .55f : 1); font.setColor(button.enabled() ? (hovered ? ACCENT : INK) : DISABLED);
        layout.setText(font, button.label()); float textX = button.x() + (button.width() - layout.width) / 2; font.draw(batch, layout, textX, button.y() + 37);
        batch.setColor(button.enabled() ? (hovered ? GOLD : Color.valueOf("C88448")) : DISABLED);
        float lineWidth = button.enabled() && hovered ? layout.width + 24 : Math.min(button.enabled() ? 42 : 32, layout.width);
        batch.draw(pixel, button.x() + (button.width() - lineWidth) / 2, button.y() + 11, lineWidth, hovered ? 3 : 1);
        batch.setColor(Color.WHITE); font.getData().setScale(1);
    }

    private boolean contains(Button button, float x, float y) { return x >= button.x() && x < button.x() + button.width() && y >= button.y() && y < button.y() + 56; }
    private void button(float y, String label, boolean enabled, Runnable action) { buttons.add(new Button(410, y, 460, label, enabled, action)); }
    private void centered(String text, float y, Color color) { font.setColor(color); layout.setText(font, text); font.draw(batch, text, (WIDTH - layout.width) / 2, y); }
    private void capture(GameNavigation.Page page) {
        var directory = Gdx.files.absolute(new java.io.File("production/qa/evidence/warm-fantasy-ui").getAbsolutePath()); directory.mkdirs();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(directory.child(page + "-" + System.currentTimeMillis() + ".png"), pixels, 6, true); } finally { pixels.dispose(); }
    }
    /** Releases menu-owned GPU resources; never releases game resources. */
    public void dispose() { batch.dispose(); font.dispose(); pixel.dispose(); background.dispose(); art.dispose(); }
}
