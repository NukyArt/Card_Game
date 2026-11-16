package com.myjavacardgame.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myjavacardgame.domain.events.DomainEvent;
import com.myjavacardgame.domain.events.GoldCreditedEvent;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import com.myjavacardgame.domain.round.GameSessionSnapshot;
import com.myjavacardgame.domain.round.MatchOutcome;
import com.myjavacardgame.domain.round.RoundPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DomainEventDispatcherTest {
    private static final DomainEvent FIRST = new GoldCreditedEvent("first", 1);
    private static final DomainEvent SECOND = new GoldCreditedEvent("second", 2);

    @Test
    void deliversEventsToSubscribersInEventOrder() {
        List<DomainEvent> firstSubscriber = new ArrayList<>();
        List<DomainEvent> secondSubscriber = new ArrayList<>();
        DomainEventDispatcher dispatcher = new DomainEventDispatcher(List.of(firstSubscriber::add, secondSubscriber::add));

        assertTrue(dispatcher.dispatch(accepted()).isEmpty());
        assertEquals(List.of(FIRST, SECOND), firstSubscriber);
        assertEquals(List.of(FIRST, SECOND), secondSubscriber);
    }

    @Test
    void subscriberFailureDoesNotStopOtherSubscribersOrLaterEvents() {
        RuntimeException failure = new IllegalStateException("observer failure");
        List<DomainEvent> received = new ArrayList<>();
        DomainEventDispatcher dispatcher = new DomainEventDispatcher(List.of(event -> { throw failure; }, received::add));
        GameSessionCommandResult result = accepted();

        List<RuntimeException> failures = dispatcher.dispatch(result);

        assertEquals(List.of(FIRST, SECOND), received);
        assertEquals(2, failures.size());
        assertSame(failure, failures.get(0));
        assertThrows(UnsupportedOperationException.class, failures::clear);
        assertTrue(result.accepted());
        assertEquals(List.of(FIRST, SECOND), result.events());
    }

    @Test
    void rejectedResultIsNotDelivered() {
        List<DomainEvent> received = new ArrayList<>();
        DomainEventDispatcher dispatcher = new DomainEventDispatcher(List.of(received::add));
        GameSessionCommandResult rejected = GameSessionCommandResult.reject("rejected", snapshot());

        assertTrue(dispatcher.dispatch(rejected).isEmpty());
        assertTrue(received.isEmpty());
    }

    private static GameSessionCommandResult accepted() {
        return new GameSessionCommandResult(true, Optional.empty(), snapshot(), List.of(FIRST, SECOND));
    }

    private static GameSessionSnapshot snapshot() {
        return new GameSessionSnapshot(List.of(), "first", "first", 1, RoundPhase.INCOME,
                MatchOutcome.IN_PROGRESS, Map.of());
    }
}
