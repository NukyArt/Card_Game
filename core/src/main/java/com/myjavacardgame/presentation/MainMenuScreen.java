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
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import static com.myjavacardgame.presentation.GameNavigation.Action.*;

/** Russian main menu, mode selection and modal return confirmation. */
public final class MainMenuScreen {
    private static final int WIDTH = 1280, HEIGHT = 720;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font;
    private final Texture pixel;
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
        ScreenUtils.clear(.035f, .047f, .075f, 1);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(-offsetX, -offsetY, virtualWidth, virtualHeight));
        buttons.clear();
        batch.begin();
        if (page == GameNavigation.Page.SCENARIO_DETAILS) rectangle(385, 30, 510, 660, .065f, .085f, .125f);
        else rectangle(385, 110, 510, 500, .065f, .085f, .125f);
        if (!scenarioLabel.isEmpty()) centered(scenarioLabel, 710, Color.GOLD);
        if (page == GameNavigation.Page.MAIN_MENU) {
            centered("Главное меню", 555, Color.WHITE);
            centered("Карточная игра", 512, Color.LIGHT_GRAY);
            button(410, "Создать лобби", true, () -> select.accept(CREATE_LOBBY));
            button(338, "Присоединиться к лобби — Скоро", false, () -> select.accept(JOIN_LOBBY));
            button(266, "Настройки — Скоро", false, () -> select.accept(SETTINGS));
            button(194, "Выход из игры", true, () -> Gdx.app.exit());
        } else if (page == GameNavigation.Page.MODE_SELECTION) {
            centered("Создать лобби", 555, Color.WHITE);
            centered("Выберите режим игры", 512, Color.LIGHT_GRAY);
            button(432, "Сетевая игра — Скоро", false, () -> select.accept(NETWORK_GAME));
            button(366, "Локальная игра — Скоро", false, () -> select.accept(LOCAL_GAME));
            button(300, "2 на 2 — Скоро", false, () -> { });
            button(234, "Играть с ботами", true, () -> select.accept(PLAY_BOTS));
            button(168, "Назад", true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.SCENARIO_SELECTION) {
            centered(scenario.getString("selectionTitle"), 555, Color.WHITE);
            batch.draw(art.texture(scenario.getString("illustrationType")), 460, 325, 360, 202.5f);
            centered(scenario.getString("illustrationCaption"), 307, Color.GRAY);
            button(230, scenarioName, true, () -> select.accept(OPEN_SCENARIO));
            button(155, scenario.getString("back"), true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.SCENARIO_DETAILS) {
            centered(scenarioName, 645, Color.WHITE);
            batch.draw(art.texture(scenario.getString("illustrationType")), 480, 390, 320, 180);
            centered(scenario.getString("illustrationCaption"), 370, Color.GRAY);
            font.getData().setScale(.65f);
            layout.setText(font, scenario.getString("description"), Color.LIGHT_GRAY, 450,
                    com.badlogic.gdx.utils.Align.center, true);
            font.draw(batch, layout, 415, 320);
            font.getData().setScale(1);
            centered(scenario.getString("difficultyTitle"), 280, Color.LIGHT_GRAY);
            buttons.add(new Button(410, 190, 150, scenario.getString("easy"), false, () -> { }));
            buttons.add(new Button(565, 190, 150, scenario.getString("normal"), false, () -> { }));
            buttons.add(new Button(720, 190, 150, scenario.getString("hard"), false, () -> { }));
            button(120, scenario.getString("play"), true, () -> select.accept(START_SCENARIO));
            button(50, scenario.getString("back"), true, () -> select.accept(BACK));
        } else if (page == GameNavigation.Page.CONFIRM_RETURN) {
            centered("Завершить текущую партию?", 525, Color.WHITE);
            centered("Прогресс не сохранится", 477, Color.LIGHT_GRAY);
            centered("Партия приостановлена", 425, Color.LIGHT_GRAY);
            button(310, "Продолжить игру", true, () -> select.accept(CANCEL_RETURN));
            button(230, "Завершить партию", true, () -> select.accept(CONFIRM_RETURN));
        }
        for (Button button : buttons) {
            boolean hovered = contains(button, mouseX, mouseY) && button.enabled();
            rectangle(button.x(), button.y(), button.width(), 56, hovered ? .16f : .10f, hovered ? .31f : .16f, hovered ? .44f : .23f);
            font.getData().setScale(button.width() < 200 ? .55f : 1);
            font.setColor(button.enabled() ? Color.WHITE : Color.GRAY);
            layout.setText(font, button.label());
            font.draw(batch, layout, button.x() + (button.width() - layout.width) / 2, button.y() + 37);
            font.getData().setScale(1);
        }
        batch.end();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            select.accept(page == GameNavigation.Page.CONFIRM_RETURN ? CANCEL_RETURN : BACK);
            return;
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            for (Button button : List.copyOf(buttons)) {
                if (button.enabled() && contains(button, mouseX, mouseY)) {
                    button.action().run();
                    break;
                }
            }
        }
        frames++;
        if (Gdx.input.isKeyJustPressed(Input.Keys.S) || frames == Integer.getInteger("ccgs.captureFrame", -1)) {
            capture(page);
            if (Boolean.getBoolean("ccgs.exitAfterCapture")) Gdx.app.exit();
        }
    }

    private boolean contains(Button button, float x, float y) {
        return x >= button.x() && x < button.x() + button.width() && y >= button.y() && y < button.y() + 56;
    }

    private void button(float y, String label, boolean enabled, Runnable action) {
        buttons.add(new Button(410, y, 460, label, enabled, action));
    }

    private void rectangle(float x, float y, float width, float height, float r, float g, float b) {
        batch.setColor(r, g, b, 1);
        batch.draw(pixel, x, y, width, height);
        batch.setColor(Color.WHITE);
    }

    private void centered(String text, float y, Color color) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, (WIDTH - layout.width) / 2, y);
    }

    private void capture(GameNavigation.Page page) {
        var directory = Gdx.files.absolute(new java.io.File("production/qa/evidence/main-menu").getAbsolutePath());
        directory.mkdirs();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(directory.child(page + "-" + System.currentTimeMillis() + ".png"), pixels, 6, true); }
        finally { pixels.dispose(); }
    }

    /** Releases menu-owned GPU resources; never releases game resources. */
    public void dispose() { batch.dispose(); font.dispose(); pixel.dispose(); art.dispose(); }
}
