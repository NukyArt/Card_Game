package com.myjavacardgame.application.bot;

import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectStageRequirements;
import com.myjavacardgame.domain.random.RandomSource;
import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.round.ActivateBaseCommand;
import com.myjavacardgame.domain.round.ActivationDecisionProvider;
import com.myjavacardgame.domain.round.ContributeBoardPackageCommand;
import com.myjavacardgame.domain.round.ContributeGoldCommand;
import com.myjavacardgame.domain.round.EndTurnCommand;
import com.myjavacardgame.domain.round.GameSession;
import com.myjavacardgame.domain.round.GameSessionCommand;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import com.myjavacardgame.domain.round.GameSessionSnapshot;
import com.myjavacardgame.domain.round.MatchOutcome;
import com.myjavacardgame.domain.round.ResolveRandomGoldCommand;
import com.myjavacardgame.domain.round.RoundPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Executes the GDD §11 production policy through ordinary, domain-validated session commands. */
public final class ProductionBotController implements ActivationDecisionProvider {
    private static final List<ResourceType> PRIORITY = List.of(ResourceType.BUILDING_MATERIALS,
            ResourceType.PROVISIONS, ResourceType.SCROLLS, ResourceType.GOLD);
    private final ProjectDefinition project;
    private final RandomSource randomSource;

    /** Creates a production policy with externally supplied project requirements and randomness. */
    public ProductionBotController(ProjectDefinition project, RandomSource randomSource) {
        this.project = Objects.requireNonNull(project, "project");
        this.randomSource = Objects.requireNonNull(randomSource, "randomSource");
    }

    /** Executes one production decision for the current bot, preserving the configured priority. */
    public GameSessionCommandResult executeNextProductionAction(GameSession session, String botParticipantId) {
        Objects.requireNonNull(session, "session"); Objects.requireNonNull(botParticipantId, "botParticipantId");
        if (!isCurrentBot(session.snapshot(), botParticipantId)) return null;
        var snapshot = session.snapshot();
        if (snapshot.pendingRandomGold().isPresent()) { var pending=snapshot.pendingRandomGold().orElseThrow(); return session.execute(new ResolveRandomGoldCommand(botParticipantId, Math.min(pending.amount(), pending.remainingStageGold()))); }
        var slots=snapshot.boards().get(botParticipantId).slots();
        for (int i=0;i<slots.size();i++) if (slots.get(i).lastActivatedRound().orElse(0)!=snapshot.roundNumber()) return session.execute(new ActivateBaseCommand(botParticipantId,i,randomSource,this));
        if (remainingCards(snapshot)>0) for (int i=0;i<slots.size();i++) if (slots.get(i).upgrades().isEmpty()) return session.execute(new ContributeBoardPackageCommand(botParticipantId,i));
        int amount=Math.min(snapshot.goldBalances().get(botParticipantId),remainingGold(snapshot));
        if (amount>0) return session.execute(new ContributeGoldCommand(botParticipantId,amount));
        return session.execute(new EndTurnCommand(botParticipantId));
    }

    /** Resolves one current bot's production turn, returning ordered immutable results and halting on rejection. */
    public List<GameSessionCommandResult> executeProductionTurn(GameSession session, String botParticipantId) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(botParticipantId, "botParticipantId");
        if (botParticipantId.isBlank()) throw new IllegalArgumentException("Bot participant id must not be blank");
        List<GameSessionCommandResult> results = new ArrayList<>();
        if (!isCurrentBot(session.snapshot(), botParticipantId)) return List.of();
        if (!resolvePending(session, botParticipantId, results)) return List.copyOf(results);
        int baseIndex = 0;
        while (isCurrentBot(session.snapshot(), botParticipantId)) {
            var snapshot = session.snapshot();
            var slots = snapshot.boards().get(botParticipantId).slots();
            if (baseIndex >= slots.size()) break;
            var slot = slots.get(baseIndex);
            if (slot.lastActivatedRound().orElse(0) != snapshot.roundNumber()) {
                if (!submit(session, new ActivateBaseCommand(botParticipantId, baseIndex, randomSource, this), results)
                        || !resolvePending(session, botParticipantId, results)) return List.copyOf(results);
            }
            baseIndex++;
        }
        baseIndex = 0;
        while (isCurrentBot(session.snapshot(), botParticipantId) && remainingCards(session.snapshot()) > 0) {
            var slots = session.snapshot().boards().get(botParticipantId).slots();
            if (baseIndex >= slots.size()) break;
            if (!slots.get(baseIndex).upgrades().isEmpty()) {
                baseIndex++;
                continue;
            }
            if (!submit(session, new ContributeBoardPackageCommand(botParticipantId, baseIndex), results)) {
                return List.copyOf(results);
            }
        }
        while (isCurrentBot(session.snapshot(), botParticipantId)) {
            var snapshot = session.snapshot();
            int amount = Math.min(snapshot.goldBalances().get(botParticipantId), remainingGold(snapshot));
            if (amount == 0) break;
            if (!submit(session, new ContributeGoldCommand(botParticipantId, amount), results)) return List.copyOf(results);
        }
        if (isCurrentBot(session.snapshot(), botParticipantId)) submit(session, new EndTurnCommand(botParticipantId), results);
        return List.copyOf(results);
    }

    /** Selects needed allowed resources in GDD order; mine replacement has its explicit materials/gold policy. */
    @Override
    public Optional<ResourceType> chooseResource(ProductionEffect effect, Context context, boolean mandatory) {
        if (effect.kind() == ProductionEffect.Kind.REPLACE_BASE) {
            ResourceType resource = context.remainingResources().get(ResourceType.BUILDING_MATERIALS) > 0
                    ? ResourceType.BUILDING_MATERIALS : ResourceType.GOLD;
            return Optional.of(resource);
        }
        Optional<ResourceType> needed = PRIORITY.stream().filter(effect.resources()::contains)
                .filter(resource -> context.remainingResources().get(resource) > 0).findFirst();
        return needed.isPresent() || !mandatory ? needed : effect.resources().stream().findFirst();
    }

    /** Offers produced gold to the current stage before retaining its excess on the personal balance. */
    @Override
    public int goldToProject(int payout, Context context) {
        return Math.min(payout, context.remainingResources().get(ResourceType.GOLD));
    }

    private boolean resolvePending(GameSession session, String botId, List<GameSessionCommandResult> results) {
        while (isCurrentBot(session.snapshot(), botId) && session.snapshot().pendingRandomGold().isPresent()) {
            var pending = session.snapshot().pendingRandomGold().orElseThrow();
            if (!submit(session, new ResolveRandomGoldCommand(botId,
                    Math.min(pending.amount(), pending.remainingStageGold())), results)) return false;
        }
        return true;
    }

    private ProjectStageRequirements requirements(GameSessionSnapshot snapshot) {
        String stageId = snapshot.projectProgress().orElseThrow().currentStageId().orElseThrow();
        return project.stages().stream().filter(stage -> stage.id().equals(stageId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project stage: " + stageId)).requirements();
    }

    private int remainingGold(GameSessionSnapshot snapshot) {
        return requirements(snapshot).resources().get(ResourceType.GOLD)
                - snapshot.projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD);
    }

    private int remainingCards(GameSessionSnapshot snapshot) {
        return requirements(snapshot).requiredCardCount() - snapshot.projectProgress().orElseThrow().contributedCardCount();
    }

    private static boolean submit(GameSession session, GameSessionCommand command, List<GameSessionCommandResult> results) {
        GameSessionCommandResult result = session.execute(command);
        results.add(result);
        return result.accepted();
    }

    private static boolean isCurrentBot(GameSessionSnapshot snapshot, String botId) {
        return snapshot.outcome() == MatchOutcome.IN_PROGRESS && snapshot.currentPhase() == RoundPhase.PRODUCTION
                && snapshot.currentParticipantId().equals(botId) && snapshot.projectProgress().isPresent()
                && snapshot.participants().stream().anyMatch(participant -> participant.id().equals(botId)
                        && participant.type() == ParticipantType.BOT);
    }
}
