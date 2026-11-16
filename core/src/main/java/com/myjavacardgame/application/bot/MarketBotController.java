package com.myjavacardgame.application.bot;

import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.market.MarketSnapshot;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectProgressSnapshot;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.round.BuyMarketCardCommand;
import com.myjavacardgame.domain.round.EndTurnCommand;
import com.myjavacardgame.domain.round.GameSession;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import com.myjavacardgame.domain.round.GameSessionSnapshot;
import com.myjavacardgame.domain.round.MarketPurchaseQuote;
import com.myjavacardgame.domain.round.RoundPhase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes the GDD §11 market-purchase policy for one bot turn through {@link GameSession} commands. */
public final class MarketBotController {
    private final ProjectDefinition project;

    /**
     * Creates a controller using the immutable project definition whose current-stage requirements guide purchases.
     *
     * @param project configured project for the controlled session
     */
    public MarketBotController(ProjectDefinition project) {
        this.project = Objects.requireNonNull(project, "project");
    }

    /** Executes one market decision for the current bot, preserving the existing selection policy. */
    public GameSessionCommandResult executeNextMarketAction(GameSession session, String botParticipantId) {
        Objects.requireNonNull(session, "session"); Objects.requireNonNull(botParticipantId, "botParticipantId");
        if (!isCurrentMarketBot(session.snapshot(), botParticipantId)) return null;
        Optional<PurchaseTarget> purchase = bestPurchase(session, session.snapshot(), botParticipantId);
        if (purchase.isEmpty()) return session.execute(new EndTurnCommand(botParticipantId));
        PurchaseTarget target = purchase.orElseThrow();
        GameSessionCommandResult result = session.execute(new BuyMarketCardCommand(botParticipantId, target.quote().marketSlot(), target.quote().role(), target.quote().baseIndex()));
        return result.accepted() ? result : session.execute(new EndTurnCommand(botParticipantId));
    }

    /**
     * Repeatedly buys the best currently quoted eligible card for the specified current bot, then ends its turn.
     *
     * <p>The controller does not sell or refresh. Every mutation is submitted to the supplied session as an
     * existing command, so session validation remains authoritative.</p>
     *
     * @param session session receiving purchase and end-turn commands
     * @param botParticipantId bot expected to own the current market turn
     * @return ordered results for all submitted commands, or an empty list when this is not that bot's market turn
     */
    public List<GameSessionCommandResult> executeMarketTurn(GameSession session, String botParticipantId) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(botParticipantId, "botParticipantId");
        if (botParticipantId.isBlank()) {
            throw new IllegalArgumentException("Bot participant id must not be blank");
        }
        List<GameSessionCommandResult> results = new ArrayList<>();
        while (isCurrentMarketBot(session.snapshot(), botParticipantId)) {
            GameSessionCommandResult result = executeNextMarketAction(session, botParticipantId);
            if (result == null) break;
            results.add(result);
            if (!result.accepted()) break;
        }
        return List.copyOf(results);
    }

    private Optional<PurchaseTarget> bestPurchase(GameSession session, GameSessionSnapshot snapshot, String botParticipantId) {
        EnumSet<ResourceType> neededResources = neededResources(snapshot);
        if (neededResources.isEmpty() || snapshot.market().isEmpty()) {
            return Optional.empty();
        }
        MarketSnapshot market = snapshot.market().orElseThrow();
        List<PurchaseTarget> bases = new ArrayList<>();
        List<PurchaseTarget> upgrades = new ArrayList<>();
        int boardSize = snapshot.boards().get(botParticipantId).slots().size();
        for (int marketSlot = 0; marketSlot < market.visibleCards().size(); marketSlot++) {
            Optional<CardInstance> visibleCard = market.visibleCards().get(marketSlot);
            if (visibleCard.isEmpty()) {
                continue;
            }
            CardInstance card = visibleCard.orElseThrow();
            if (producesNeededResource(card, CardRole.BASE, neededResources)) {
                quote(session, botParticipantId, marketSlot, CardRole.BASE, -1)
                        .map(PurchaseTarget::new).ifPresent(bases::add);
            }
            if (producesNeededResource(card, CardRole.UPGRADE, neededResources)) {
                for (int baseIndex = 0; baseIndex < boardSize; baseIndex++) {
                    quote(session, botParticipantId, marketSlot, CardRole.UPGRADE, baseIndex)
                            .map(PurchaseTarget::new).ifPresent(upgrades::add);
                }
            }
        }
        return selectCheapest(bases).or(() -> selectCheapest(upgrades));
    }

    private Optional<MarketPurchaseQuote> quote(
            GameSession session, String participantId, int marketSlot, CardRole role, int baseIndex) {
        try {
            return session.quoteMarketPurchase(participantId, marketSlot, role, baseIndex);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private EnumSet<ResourceType> neededResources(GameSessionSnapshot snapshot) {
        Optional<ProjectProgressSnapshot> progress = snapshot.projectProgress();
        if (progress.isEmpty() || progress.orElseThrow().currentStageId().isEmpty()) {
            return EnumSet.noneOf(ResourceType.class);
        }
        String stageId = progress.orElseThrow().currentStageId().orElseThrow();
        ProjectStageDefinition stage = project.stages().stream().filter(candidate -> candidate.id().equals(stageId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Session stage is not in the configured project: " + stageId));
        EnumSet<ResourceType> needed = EnumSet.noneOf(ResourceType.class);
        for (Map.Entry<ResourceType, Integer> requirement : stage.requirements().resources().entrySet()) {
            if (progress.orElseThrow().contributedResources().get(requirement.getKey()) < requirement.getValue()) {
                needed.add(requirement.getKey());
            }
        }
        return needed;
    }

    private static boolean producesNeededResource(CardInstance card, CardRole role, EnumSet<ResourceType> neededResources) {
        if (!card.definition().allowedRoles().contains(role)) {
            return false;
        }
        var production = card.definition().roleParameters().get(role).production();
        if (production.amounts().entrySet().stream()
                .anyMatch(entry -> entry.getValue() > 0 && neededResources.contains(entry.getKey()))) {
            return true;
        }
        return production.effects().stream().anyMatch(effect -> producesNeededResource(effect, neededResources));
    }

    private static boolean producesNeededResource(ProductionEffect effect, EnumSet<ResourceType> neededResources) {
        return (effect.kind() == ProductionEffect.Kind.FIXED || effect.kind() == ProductionEffect.Kind.CHOICE)
                && effect.amount() > 0 && effect.resources().stream().anyMatch(neededResources::contains);
    }

    private static Optional<PurchaseTarget> selectCheapest(List<PurchaseTarget> targets) {
        return targets.stream().min(Comparator.comparingInt((PurchaseTarget target) -> target.quote().effectiveGoldCost())
                .thenComparingInt(target -> target.quote().marketSlot())
                .thenComparingInt(target -> target.quote().baseIndex()));
    }

    private static boolean isCurrentMarketBot(GameSessionSnapshot snapshot, String botParticipantId) {
        return snapshot.currentPhase() == RoundPhase.MARKET && snapshot.currentParticipantId().equals(botParticipantId)
                && snapshot.participants().stream().anyMatch(participant -> participant.id().equals(botParticipantId)
                        && participant.type() == ParticipantType.BOT);
    }

    private record PurchaseTarget(MarketPurchaseQuote quote) { }
}
