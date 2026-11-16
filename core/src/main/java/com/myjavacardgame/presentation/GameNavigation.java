package com.myjavacardgame.presentation;

/** Backend-independent navigation; unavailable actions never change the page. */
public final class GameNavigation {
    /** Presentation pages, including the modal return confirmation. */
    public enum Page { MAIN_MENU, MODE_SELECTION, SCENARIO_SELECTION, SCENARIO_DETAILS, GAME, CONFIRM_RETURN }

    /** Menu choices; future modes are deliberately unavailable. */
    public enum Action {
        CREATE_LOBBY, JOIN_LOBBY, SETTINGS, NETWORK_GAME, LOCAL_GAME,
        PLAY_BOTS, OPEN_SCENARIO, START_SCENARIO, BACK, CONFIRM_RETURN, CANCEL_RETURN
    }

    private Page page = Page.MAIN_MENU;

    /** Returns the current page without requiring a graphics backend. */
    public Page page() { return page; }

    /** Applies only choices available on the current page. */
    public void select(Action action) {
        page = switch (page) {
            case MAIN_MENU -> action == Action.CREATE_LOBBY ? Page.MODE_SELECTION : page;
            case MODE_SELECTION -> switch (action) {
                case PLAY_BOTS -> Page.SCENARIO_SELECTION;
                case BACK -> Page.MAIN_MENU;
                default -> page;
            };
            case SCENARIO_SELECTION -> switch (action) {
                case OPEN_SCENARIO -> Page.SCENARIO_DETAILS;
                case BACK -> Page.MODE_SELECTION;
                default -> page;
            };
            case SCENARIO_DETAILS -> switch (action) {
                case START_SCENARIO -> Page.GAME;
                case BACK -> Page.SCENARIO_SELECTION;
                default -> page;
            };
            case CONFIRM_RETURN -> switch (action) {
                case CONFIRM_RETURN -> Page.MAIN_MENU;
                case CANCEL_RETURN -> Page.GAME;
                default -> page;
            };
            case GAME -> page;
        };
    }

    /** Requests confirmation unless the current match has already ended. */
    public void requestMainMenu(boolean finished) {
        if (page == Page.GAME) page = finished ? Page.MAIN_MENU : Page.CONFIRM_RETURN;
    }
}
