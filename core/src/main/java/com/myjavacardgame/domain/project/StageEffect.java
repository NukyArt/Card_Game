package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Objects;

/** Data-defined effect applied when a project stage completes. */
public record StageEffect(Kind kind, ResourceType resource, int amount) {
    /** Supported stage completion effect types. */
    public enum Kind { CREDIT_EACH, CONTRIBUTE_EACH, MODIFY_PRODUCTION }

    /** Validates the effect parameters. */
    public StageEffect {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(resource, "resource");
        if (kind != Kind.MODIFY_PRODUCTION && amount < 0) {
            throw new IllegalArgumentException("Immediate stage effect amounts must not be negative");
        }
        if (kind == Kind.CREDIT_EACH && resource != ResourceType.GOLD) {
            throw new IllegalArgumentException("Only gold can be credited to personal balances");
        }
    }
}
