package com.myjavacardgame.application;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.domain.project.StageEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProjectCatalogLoaderTest {
    @Test
    void loadsFantasyCityStagesAndEffects() {
        var stream = getClass().getResourceAsStream("/data/fantasy-city-project.json");
        assertNotNull(stream);
        var project = new ProjectCatalogLoader().load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        assertEquals(5, project.stages().size());
        assertEquals(new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2),
                project.stages().get(0).stageEffects().get(0));
        assertEquals(-1, project.stages().get(1).stageEffects().get(0).amount());
        assertEquals(6, project.stages().get(3).requirements().requiredCardCount());
    }

    @Test
    void rejectsMissingStages() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectCatalogLoader().load(new StringReader("{\"id\":\"p\",\"name\":\"P\"}")));
    }
}
