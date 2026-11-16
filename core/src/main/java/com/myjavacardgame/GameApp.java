package com.myjavacardgame;

import com.badlogic.gdx.ApplicationAdapter;
import com.myjavacardgame.presentation.GameNavigation;
import com.myjavacardgame.presentation.MainMenuScreen;
import com.myjavacardgame.presentation.SoloGameScreen;

/** Entry point for the game shared across desktop launchers. */
public class GameApp extends ApplicationAdapter {
    private final GameNavigation navigation = new GameNavigation();
    private final LaunchOptions options;

    /** Creates an ordinary application without accessing graphics resources. */
    public GameApp() { this(LaunchOptions.parse(new String[0])); }

    /** Creates an application configured by explicit launch options. */
    public GameApp(LaunchOptions options) { this.options = java.util.Objects.requireNonNull(options); }
    private MainMenuScreen menu;
    private SoloGameScreen gameScreen;

    /** Creates the main menu without starting a match. */
    @Override
    public void create() { menu = new MainMenuScreen(options.label()); }

    /** Updates only the active page, then safely creates or releases a match. */
    @Override
    public void render() {
        if (navigation.page() == GameNavigation.Page.GAME) gameScreen.render();
        else menu.render(navigation.page(), navigation::select);
        if (navigation.page() == GameNavigation.Page.GAME && gameScreen == null) {
            gameScreen = new SoloGameScreen(navigation::requestMainMenu, options);
        } else if (navigation.page() == GameNavigation.Page.MAIN_MENU && gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
    }

    /** Releases menu and any still-active match resources. */
    @Override
    public void dispose() {
        if (gameScreen != null) gameScreen.dispose();
        if (menu != null) menu.dispose();
    }
}
