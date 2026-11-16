package com.myjavacardgame.application;

import com.myjavacardgame.domain.events.DomainEvent;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Delivers committed domain events to application subscribers in event order. */
public final class DomainEventDispatcher {
    private final List<Consumer<DomainEvent>> subscribers;

    /** Creates a dispatcher with subscribers in delivery order. */
    public DomainEventDispatcher(List<Consumer<DomainEvent>> subscribers) {
        this.subscribers = List.copyOf(Objects.requireNonNull(subscribers, "subscribers"));
    }

    /**
     * Delivers events from an accepted result. A subscriber's runtime exception is collected
     * and does not stop delivery to other subscribers or later events. Rejected results are ignored.
     *
     * @param result the already completed session operation
     * @return an immutable list of subscriber failures in occurrence order
     */
    public List<RuntimeException> dispatch(GameSessionCommandResult result) {
        Objects.requireNonNull(result, "result");
        if (!result.accepted()) return List.of();
        List<RuntimeException> failures = new ArrayList<>();
        for (DomainEvent event : result.events()) {
            for (Consumer<DomainEvent> subscriber : subscribers) {
                try {
                    subscriber.accept(event);
                } catch (RuntimeException exception) {
                    failures.add(exception);
                }
            }
        }
        return List.copyOf(failures);
    }
}
