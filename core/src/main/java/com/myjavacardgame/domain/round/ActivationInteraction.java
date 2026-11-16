package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Result of starting or continuing a nonblocking human activation. */
public sealed interface ActivationInteraction permits ActivationInteraction.Rejected,
        ActivationInteraction.Prompt, ActivationInteraction.Completed {
    /** An invalid request; any retained instruction remains available from the session. */
    record Rejected(GameSessionCommandResult result) implements ActivationInteraction {
        /** Requires an immutable command result. */
        public Rejected { Objects.requireNonNull(result, "result"); }
    }

    /** A question awaiting the next answer before any candidate changes are published. */
    record Prompt(ActivationPrompt prompt) implements ActivationInteraction {
        /** Requires an immutable question. */
        public Prompt { Objects.requireNonNull(prompt, "prompt"); }
    }

    /** The complete activation was atomically applied to the session. */
    record Completed(GameSessionCommandResult result) implements ActivationInteraction {
        /** Requires the published command result. */
        public Completed { Objects.requireNonNull(result, "result"); }
    }
}
