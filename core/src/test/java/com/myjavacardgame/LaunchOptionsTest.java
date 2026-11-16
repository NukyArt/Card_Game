package com.myjavacardgame;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LaunchOptionsTest {
    @Test void defaultsToUnchangedNormalResources() {
        var options = LaunchOptions.parse(new String[0]);
        assertEquals("data/solo-setup.json", options.setupPath());
        assertEquals("data/fantasy-city-project.json", options.projectPath());
        assertEquals("", options.label());
    }
    @Test void selectsExplicitQaResources() {
        var victory = LaunchOptions.parse(new String[] {"--qa=victory"});
        assertEquals("data/qa/solo-setup-victory.json", victory.setupPath());
        assertEquals("data/qa/victory-project.json", victory.projectPath());
        assertEquals("ТЕСТ: ПОБЕДА", victory.label());
        assertEquals("ТЕСТ: ПОРАЖЕНИЕ", LaunchOptions.parse(new String[] {"--qa=defeat"}).label());
    }
    @Test void rejectsUnknownAndRepeatedArguments() {
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[] {"--qa=typo"}));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[] {"--other"}));
        assertThrows(IllegalArgumentException.class, () -> LaunchOptions.parse(new String[] {"--qa=victory", "--qa=defeat"}));
    }
}
