package com.myjavacardgame.application;

import com.myjavacardgame.domain.board.BoardPlacement;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.market.MarketDeck;
import com.myjavacardgame.domain.market.MarketRules;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.random.RandomSource;
import com.myjavacardgame.domain.round.DepositRules;
import com.myjavacardgame.domain.round.GameSession;
import com.myjavacardgame.domain.round.GameSessionDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Builds a complete solo session from loaded setup data and externally supplied rules. */
public final class SoloSessionBuilder {
    /** Creates a session builder. */
    public SoloSessionBuilder() { }

    /**
     * Builds a session without exposing any of its partially constructed components.
     *
     * <p>Market entries are ordered by their definition identifier before shuffling so that
     * equivalent inputs and random sequences yield the same card order. The supplied random
     * source is also retained by the market for later discard recycling.</p>
     *
     * @param data setup data produced by {@link SoloSetupLoader}
     * @param randomSource source of bounded random values
     * @param marketRules externally configured market and board rules
     * @param depositRules externally configured deposit rules
     * @return a fully constructed session at the start of its first round
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the inputs cannot form the configured solo session
     * @throws IllegalStateException if the random source returns an out-of-range value
     */
    public GameSession build(SoloSetupLoader.Data data, RandomSource randomSource,
            MarketRules marketRules, DepositRules depositRules) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(randomSource, "randomSource");
        Objects.requireNonNull(marketRules, "marketRules");
        Objects.requireNonNull(depositRules, "depositRules");
        if (marketRules.visibleSlotCount() != 5) {
            throw new IllegalArgumentException("Solo sessions require five market slots");
        }
        var participants = data.party().participants();
        if (participants.size() != 4 || participants.get(0).type() != ParticipantType.HUMAN
                || participants.stream().filter(p -> p.type() == ParticipantType.BOT).count() != 3) {
            throw new IllegalArgumentException("Solo sessions require a human followed by three bots");
        }
        if (data.project().stages().size() != 5) {
            throw new IllegalArgumentException("Solo sessions require five project stages");
        }
        var catalog = data.cards().catalog();
        List<String> startingCards = data.cards().startingCards();
        if (startingCards.size() != 4 || new HashSet<>(startingCards).size() != 4) {
            throw new IllegalArgumentException("Solo sessions require four distinct starting bases");
        }
        Set<String> instanceIds = new HashSet<>();
        Map<String, BoardSnapshot> boards = new LinkedHashMap<>();
        int nextId = 0;
        for (var participant : participants) {
            List<String> participantCards = participant.startingBaseCards().stream().map(card -> card.id()).toList();
            if (!participantCards.equals(startingCards)) {
                throw new IllegalArgumentException("Participant starting bases do not match setup data");
            }
            BoardSnapshot board = BoardSnapshot.empty();
            for (String cardId : participantCards) {
                String instanceId = "card-" + nextId++;
                if (!instanceIds.add(instanceId)) throw new IllegalStateException("Duplicate card instance id");
                board = BoardPlacement.placeBase(board, CardInstance.fromCatalog(instanceId, cardId, catalog),
                        marketRules.boardRules());
            }
            boards.put(participant.id(), board);
        }
        List<CardInstance> marketCards = new ArrayList<>();
        for (String cardId : data.cards().marketCounts().keySet().stream().sorted().toList()) {
            int count = data.cards().marketCounts().get(cardId);
            if (count < 0) throw new IllegalArgumentException("Negative market count: " + cardId);
            catalog.requireCard(cardId);
            for (int index = 0; index < count; index++) {
                String instanceId = "card-" + nextId++;
                if (!instanceIds.add(instanceId)) throw new IllegalStateException("Duplicate card instance id");
                marketCards.add(CardInstance.fromCatalog(instanceId, cardId, catalog));
            }
        }
        if (marketCards.size() < marketRules.visibleSlotCount()) {
            throw new IllegalArgumentException("Market deck cannot fill all visible slots");
        }
        for (int index = marketCards.size() - 1; index > 0; index--) {
            int swapIndex = randomSource.nextInt(index + 1);
            if (swapIndex < 0 || swapIndex > index) {
                throw new IllegalStateException("Random source returned an invalid value");
            }
            CardInstance card = marketCards.get(index);
            marketCards.set(index, marketCards.get(swapIndex));
            marketCards.set(swapIndex, card);
        }
        MarketDeck market = new MarketDeck(marketRules, marketCards, randomSource);
        GameSessionDefinition definition = new GameSessionDefinition(data.party(), data.roundLimit(), depositRules,
                data.project(), boards, market, Map.of());
        return new GameSession(definition);
    }

    /**
     * Builds a solo session controller that automatically resolves consecutive market and production bot turns.
     *
     * @param data setup data produced by {@link SoloSetupLoader}
     * @param randomSource source of bounded random values
     * @param marketRules externally configured market and board rules
     * @param depositRules externally configured deposit rules
     * @return a controller for a fully constructed solo session
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the inputs cannot form the configured solo session
     * @throws IllegalStateException if the random source returns an out-of-range value
     */
    public SoloGameController buildController(SoloSetupLoader.Data data, RandomSource randomSource,
            MarketRules marketRules, DepositRules depositRules) {
        RandomSource sharedRandom = randomSource instanceof com.myjavacardgame.domain.random.TransactionalRandomSource
                ? randomSource : new com.myjavacardgame.domain.random.TransactionalRandomSource(randomSource);
        GameSession session = build(data, sharedRandom, marketRules, depositRules);
        return new SoloGameController(session, data.project(), sharedRandom);
    }
    /** Builds a timer-driven controller using configured turn durations. */
    public SoloGameDriver buildDriver(SoloSetupLoader.Data data, RandomSource randomSource, MarketRules marketRules, DepositRules depositRules) {
        RandomSource sharedRandom = randomSource instanceof com.myjavacardgame.domain.random.TransactionalRandomSource
                ? randomSource : new com.myjavacardgame.domain.random.TransactionalRandomSource(randomSource);
        return new SoloGameDriver(buildController(data, sharedRandom, marketRules, depositRules),
                data.marketTurnSeconds(), data.productionTurnSeconds());
    }
}
