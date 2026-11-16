package com.myjavacardgame.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.myjavacardgame.GameApp;

/** Starts the desktop version of the game. */
public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    /** Creates the game window and starts the application. */
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("MyJavaCardGame");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new GameApp(com.myjavacardgame.LaunchOptions.parse(args)), config);
    }
}
