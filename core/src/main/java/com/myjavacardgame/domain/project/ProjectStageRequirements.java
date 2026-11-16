package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable resource and card requirements for a project stage.
 *
 * @param resources required amount for each of the four resource types
 * @param requiredCardCount nonnegative number of cards required by the stage
 */
public record ProjectStageRequirements(Map<ResourceType, Integer> resources, int requiredCardCount) {
    /**
     * Creates complete requirements for all supported resource types.
     *
     * @param resources non-null, complete mapping of resource types to nonnegative required amounts
     * @param requiredCardCount nonnegative number of required cards
     * @throws NullPointerException if {@code resources}, a resource type, or an amount is null
     * @throws IllegalArgumentException if the resource mapping is incomplete, has unsupported keys,
     *                                  or an amount or card count is negative
     */
    public ProjectStageRequirements {
        Objects.requireNonNull(resources, "resources");
        resources = Map.copyOf(resources);
        if (!resources.keySet().equals(EnumSet.allOf(ResourceType.class))) {
            throw new IllegalArgumentException("Requirements must specify every resource type exactly once");
        }
        for (int amount : resources.values()) {
            if (amount < 0) {
                throw new IllegalArgumentException("Resource requirements must not be negative");
            }
        }
        if (requiredCardCount < 0) {
            throw new IllegalArgumentException("Required card count must not be negative");
        }
    }
}
