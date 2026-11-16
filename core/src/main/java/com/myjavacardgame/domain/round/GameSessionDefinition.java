package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.market.MarketDeck;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.party.StartingPartyDefinition;
import com.myjavacardgame.domain.party.StartingPartyParticipantDefinition;
import com.myjavacardgame.domain.project.ProjectDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable external configuration from which a solo game session is created. */
public record GameSessionDefinition(
        StartingPartyDefinition startingParty,
        int roundLimit,
        DepositRules depositRules,
        Optional<ProjectDefinition> project,
        Map<String, BoardSnapshot> startingBoards,
        Optional<MarketDeck> marketDeck,
        Map<String, Boolean> freeRefreshEntitlements) {
    /** Retains compatibility for round/deposit-only sessions. */
    public GameSessionDefinition(StartingPartyDefinition startingParty, int roundLimit, DepositRules depositRules) {
        this(startingParty, roundLimit, depositRules, Optional.empty(), Map.of(), Optional.empty(), Map.of());
    }

    /** Creates a project-enabled session with one immutable owned board per participant. */
    public GameSessionDefinition(StartingPartyDefinition startingParty, int roundLimit, DepositRules depositRules,
            ProjectDefinition project, Map<String, BoardSnapshot> startingBoards) {
        this(startingParty, roundLimit, depositRules, Optional.of(Objects.requireNonNull(project, "project")),
                startingBoards, Optional.empty(), Map.of());
    }

    /**
     * Creates a market-enabled session. Free-refresh entitlements are supplied by an external
     * effects system and are consumed by the first refresh in the current market phase.
     */
    public GameSessionDefinition(StartingPartyDefinition startingParty, int roundLimit, DepositRules depositRules,
            ProjectDefinition project, Map<String, BoardSnapshot> startingBoards, MarketDeck marketDeck,
            Map<String, Boolean> freeRefreshEntitlements) {
        this(startingParty, roundLimit, depositRules, Optional.of(Objects.requireNonNull(project, "project")),
                startingBoards, Optional.of(Objects.requireNonNull(marketDeck, "marketDeck")), freeRefreshEntitlements);
    }

    /** Validates MVP composition and project-board-market ownership data. */
    public GameSessionDefinition {
        Objects.requireNonNull(startingParty, "startingParty");
        Objects.requireNonNull(depositRules, "depositRules");
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(startingBoards, "startingBoards");
        Objects.requireNonNull(marketDeck, "marketDeck");
        Objects.requireNonNull(freeRefreshEntitlements, "freeRefreshEntitlements");
        if (roundLimit < 1) throw new IllegalArgumentException("Round limit must be positive");
        List<StartingPartyParticipantDefinition> participants = startingParty.participants();
        long humans = participants.stream().filter(p -> p.type() == ParticipantType.HUMAN).count();
        long bots = participants.stream().filter(p -> p.type() == ParticipantType.BOT).count();
        if (participants.size() != 4 || humans != 1 || bots != 3) throw new IllegalArgumentException("A game session requires exactly one human and three bots");
        startingBoards = Map.copyOf(startingBoards);
        Set<String> participantIds = participants.stream().map(StartingPartyParticipantDefinition::id).collect(Collectors.toSet());
        if (project.isPresent() && !startingBoards.keySet().equals(participantIds)) {
            throw new IllegalArgumentException("Project sessions require exactly one board for every participant");
        }
        if (project.isEmpty() && !startingBoards.isEmpty()) throw new IllegalArgumentException("Boards require a project");
        marketDeck = marketDeck.map(MarketDeck::copy);
        freeRefreshEntitlements = Map.copyOf(new LinkedHashMap<>(freeRefreshEntitlements));
        for (Map.Entry<String, Boolean> entry : freeRefreshEntitlements.entrySet()) {
            if (!participantIds.contains(entry.getKey()) || entry.getValue() == null) {
                throw new IllegalArgumentException("Free refresh entitlements must identify known participants");
            }
        }
        if (marketDeck.isEmpty() && !freeRefreshEntitlements.isEmpty()) {
            throw new IllegalArgumentException("Free refresh entitlements require a configured market");
        }
    }

    /** Returns a defensive copy so definition state cannot be mutated by a session caller. */
    @Override
    public Optional<MarketDeck> marketDeck() {
        return marketDeck.map(MarketDeck::copy);
    }
}
