package com.myjavacardgame;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class GameAppTest {
    @Test
    void createsApplicationWithoutDesktopBackend() {
        assertNotNull(new GameApp());
    }
}
