package com.myjavacardgame.application;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.project.ProjectStageRequirements;
import com.myjavacardgame.domain.project.StageEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Reads a data-defined project without introducing framework dependencies into the domain. */
public final class ProjectCatalogLoader {
    /** Creates a project loader. */
    public ProjectCatalogLoader() { }

    /** Reads and validates one project without closing the provided reader. */
    public ProjectDefinition load(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        try {
            JsonValue root = new JsonReader().parse(reader);
            JsonValue stages = required(root, "stages");
            if (!stages.isArray()) throw new IllegalArgumentException("Stages must be an array");
            List<ProjectStageDefinition> definitions = new ArrayList<>();
            for (JsonValue stage = stages.child; stage != null; stage = stage.next) {
                JsonValue requirements = required(stage, "requirements");
                if (!requirements.isObject()) throw new IllegalArgumentException("Requirements must be an object");
                Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
                for (JsonValue value = requirements.child; value != null; value = value.next) {
                    if (amounts.put(ResourceType.valueOf(value.name), integer(value, value.name)) != null) {
                        throw new IllegalArgumentException("Duplicate resource requirement");
                    }
                }
                List<StageEffect> effects = new ArrayList<>();
                JsonValue entries = required(stage, "effects");
                if (!entries.isArray()) throw new IllegalArgumentException("Effects must be an array");
                for (JsonValue effect = entries.child; effect != null; effect = effect.next) {
                    effects.add(new StageEffect(StageEffect.Kind.valueOf(text(effect, "kind")),
                            ResourceType.valueOf(text(effect, "resource")), integer(required(effect, "amount"), "amount")));
                }
                definitions.add(new ProjectStageDefinition(text(stage, "id"), text(stage, "name"),
                        new ProjectStageRequirements(amounts, integer(required(stage, "cards"), "cards")),
                        text(stage, "description"), List.of(), effects));
            }
            return new ProjectDefinition(text(root, "id"), text(root, "name"), definitions);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid project data", exception);
        }
    }

    private static JsonValue required(JsonValue object, String key) {
        JsonValue value = object.get(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key);
        return value;
    }

    private static String text(JsonValue object, String key) {
        JsonValue value = required(object, key);
        if (!value.isString() || value.asString().isBlank()) throw new IllegalArgumentException("Invalid text: " + key);
        return value.asString();
    }

    private static int integer(JsonValue value, String key) {
        if (!value.isNumber()) throw new IllegalArgumentException("Invalid integer: " + key);
        double number = value.asDouble();
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid integer: " + key);
        }
        return (int) number;
    }
}
