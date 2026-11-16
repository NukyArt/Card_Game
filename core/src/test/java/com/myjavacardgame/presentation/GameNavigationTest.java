package com.myjavacardgame.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameNavigationTest {
    @Test
    void startsAtMainMenuAndReturnsFromModeSelection() {
        var navigation = new GameNavigation();
        assertEquals(GameNavigation.Page.MAIN_MENU, navigation.page());
        navigation.select(GameNavigation.Action.CREATE_LOBBY);
        assertEquals(GameNavigation.Page.MODE_SELECTION, navigation.page());
        navigation.select(GameNavigation.Action.BACK);
        assertEquals(GameNavigation.Page.MAIN_MENU, navigation.page());
    }

    @Test
    void unavailableActionsNeverNavigate() {
        var navigation = new GameNavigation();
        navigation.select(GameNavigation.Action.JOIN_LOBBY);
        navigation.select(GameNavigation.Action.SETTINGS);
        navigation.select(GameNavigation.Action.PLAY_BOTS);
        assertEquals(GameNavigation.Page.MAIN_MENU, navigation.page());
        navigation.select(GameNavigation.Action.CREATE_LOBBY);
        navigation.select(GameNavigation.Action.NETWORK_GAME);
        navigation.select(GameNavigation.Action.LOCAL_GAME);
        assertEquals(GameNavigation.Page.MODE_SELECTION, navigation.page());
    }

    @Test
    void runningGameRequiresConfirmationAndCanBeResumed() {
        var navigation = startedGame();
        navigation.requestMainMenu(false);
        assertEquals(GameNavigation.Page.CONFIRM_RETURN, navigation.page());
        navigation.select(GameNavigation.Action.CANCEL_RETURN);
        assertEquals(GameNavigation.Page.GAME, navigation.page());
        navigation.requestMainMenu(false);
        navigation.select(GameNavigation.Action.CONFIRM_RETURN);
        assertEquals(GameNavigation.Page.MAIN_MENU, navigation.page());
    }

    @Test
    void finishedGameReturnsDirectlyAndCanStartAgain() {
        var navigation = startedGame();
        navigation.requestMainMenu(true);
        assertEquals(GameNavigation.Page.MAIN_MENU, navigation.page());
        navigation.select(GameNavigation.Action.CREATE_LOBBY);
        navigation.select(GameNavigation.Action.PLAY_BOTS);
        navigation.select(GameNavigation.Action.OPEN_SCENARIO);
        navigation.select(GameNavigation.Action.START_SCENARIO);
        assertEquals(GameNavigation.Page.GAME, navigation.page());
    }

    @Test
    void scenarioBrowsingDoesNotStartAMatchAndBackReturnsOneStep() {
        var navigation = new GameNavigation();
        navigation.select(GameNavigation.Action.CREATE_LOBBY);
        navigation.select(GameNavigation.Action.PLAY_BOTS);
        assertEquals(GameNavigation.Page.SCENARIO_SELECTION, navigation.page());
        navigation.select(GameNavigation.Action.START_SCENARIO);
        assertEquals(GameNavigation.Page.SCENARIO_SELECTION, navigation.page());
        navigation.select(GameNavigation.Action.OPEN_SCENARIO);
        assertEquals(GameNavigation.Page.SCENARIO_DETAILS, navigation.page());
        navigation.select(GameNavigation.Action.BACK);
        assertEquals(GameNavigation.Page.SCENARIO_SELECTION, navigation.page());
        navigation.select(GameNavigation.Action.BACK);
        assertEquals(GameNavigation.Page.MODE_SELECTION, navigation.page());
    }

    @Test
    void unrelatedActionsCannotDismissConfirmationOrNavigateDuringGame() {
        var navigation = startedGame();
        navigation.select(GameNavigation.Action.BACK);
        assertEquals(GameNavigation.Page.GAME, navigation.page());
        navigation.requestMainMenu(false);
        navigation.select(GameNavigation.Action.PLAY_BOTS);
        navigation.select(GameNavigation.Action.BACK);
        navigation.requestMainMenu(true);
        assertEquals(GameNavigation.Page.CONFIRM_RETURN, navigation.page());
    }

    private GameNavigation startedGame() {
        var navigation = new GameNavigation();
        navigation.select(GameNavigation.Action.CREATE_LOBBY);
        navigation.select(GameNavigation.Action.PLAY_BOTS);
        navigation.select(GameNavigation.Action.OPEN_SCENARIO);
        navigation.select(GameNavigation.Action.START_SCENARIO);
        return navigation;
    }
}
