package com.myjavacardgame.presentation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.myjavacardgame.application.SoloGameController;
import com.myjavacardgame.application.SoloSessionBuilder;
import com.myjavacardgame.application.SoloSetupLoader;
import com.myjavacardgame.domain.board.BoardRules;
import com.myjavacardgame.domain.market.MarketRules;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.random.RandomSource;
import com.myjavacardgame.domain.round.DepositRules;
import com.myjavacardgame.domain.round.EndTurnCommand;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import com.myjavacardgame.domain.round.GameSessionSnapshot;
import com.myjavacardgame.domain.round.PhaseCommand;
import com.myjavacardgame.domain.round.RoundPhase;
import com.myjavacardgame.domain.round.SessionPhaseCommand;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Market and production diagnostic view for observing the normal solo bot controllers.
 *
 * <p>Bot turns are resolved synchronously by the application controller. This screen replays immutable result
 * snapshots; it does not imply that live bot execution is paused between replay steps.</p>
 */
public final class MarketBotObservationScreen {
    private static final String EVIDENCE_DIRECTORY = "production/qa/evidence/production-bot-observation";
    private static final int OBSERVATION_FRAMES = 60;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final boolean unattended = configuredBoolean("market.observation.unattended");
    private final SoloGameController controller;
    private final ProjectDefinition project;
    private final String humanId;
    private GameSessionSnapshot displayedSnapshot;
    private List<GameSessionCommandResult> replayResults = List.of();
    private int replayIndex = -1;
    private int framesAtCurrentState;
    private int capturedReplayCount;
    private boolean productionBoundary;
    private boolean observationComplete;
    private String replayLabel = "LIVE: human market turn";

    /** Creates the screen from the configured normal solo session. */
    public MarketBotObservationScreen() {
        SoloSetupLoader.Data data = loadSetup();
        project = data.project();
        MarketRules marketRules = new MarketRules(data.marketVisibleSlotCount(), data.marketRefreshGoldCost(),
                new BoardRules(data.boardMaxBases(), data.boardMaxUpgradesPerBase()), data.marketSalePriceDivisor());
        controller = new SoloSessionBuilder().buildController(data, seededRandom(configuredSeed()), marketRules,
                new DepositRules(data.depositGoldPerBonus(), data.depositMaximumBonus(), data.roundIncomeGold()));
        humanId = data.party().participants().get(0).id();
        GameSessionCommandResult income = controller.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).get(0);
        if (!income.accepted()) throw new IllegalStateException("Unable to begin market observation: "
                + income.rejectionReason().orElse("income transition rejected"));
        displayedSnapshot = income.snapshot();
    }

    /** Renders the diagnostic screen and processes its keyboard controls. */
    public void render() {
        draw();
        framesAtCurrentState++;
        if (unattended) advanceUnattended();
        else processInput();
    }

    /** Releases rendering resources owned by this screen. */
    public void dispose() {
        batch.dispose();
        font.dispose();
    }

    private static SoloSetupLoader.Data loadSetup() {
        try (Reader setup = Gdx.files.internal("data/solo-setup.json").reader("UTF-8");
                Reader cards = Gdx.files.internal("data/card-catalog.json").reader("UTF-8");
                Reader project = Gdx.files.internal("data/fantasy-city-project.json").reader("UTF-8")) {
            return new SoloSetupLoader().load(setup, cards, project);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load normal solo observation session", exception);
        }
    }

    private static RandomSource seededRandom(long seed) {
        Random random = new Random(seed);
        return random::nextInt;
    }

    private static long configuredSeed() {
        String value = configuredValue("market.observation.seed");
        if (value == null || value.isBlank()) return new Random().nextLong();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("market.observation.seed must be a long", exception);
        }
    }

    private static boolean configuredBoolean(String key) {
        String value = configuredValue(key);
        return value != null && Boolean.parseBoolean(value);
    }

    private static String configuredValue(String key) {
        String property = System.getProperty(key);
        return property != null ? property : System.getenv(key.toUpperCase().replace('.', '_'));
    }

    private void processInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) capture("manual");
        if (replayIndex >= 0 && Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            if (replayIndex + 1 < replayResults.size()) nextReplay();
            else if (!observationComplete) advanceBoundary();
        }
        if (replayIndex < 0 && replayResults.isEmpty() && Gdx.input.isKeyJustPressed(Input.Keys.P)) humanPass();
    }

    private void advanceUnattended() {
        if (framesAtCurrentState < OBSERVATION_FRAMES) return;
        if (replayResults.isEmpty()) {
            capture("00-initial");
            humanPass();
            return;
        }
        if (observationComplete) {
            capture(String.format("%02d-production-complete", capturedReplayCount + 1));
            Gdx.app.exit();
            return;
        }
        if (productionBoundary) {
            capture(String.format("%02d-initial-production", ++capturedReplayCount));
            productionBoundary = false;
            humanPass();
            return;
        }
        capture(String.format("%02d-replay", capturedReplayCount + 1));
        capturedReplayCount++;
        if (replayIndex + 1 < replayResults.size()) nextReplay();
        else advanceBoundary();
    }

    private void humanPass() {
        replayResults = controller.execute(new EndTurnCommand(humanId));
        replayIndex = 0;
        showReplayResult();
    }

    private void nextReplay() {
        replayIndex++;
        showReplayResult();
    }

    private void advanceBoundary() {
        if (productionBoundary) {
            productionBoundary = false;
            humanPass();
            return;
        }
        if (displayedSnapshot.outcome() != com.myjavacardgame.domain.round.MatchOutcome.IN_PROGRESS) {
            observationComplete = true;
            replayLabel = "REPLAY COMPLETE: match ended " + displayedSnapshot.outcome();
            framesAtCurrentState = 0;
            return;
        }
        if (displayedSnapshot.currentPhase() == RoundPhase.PRODUCTION) {
            GameSessionCommandResult result = controller.execute(
                    new SessionPhaseCommand(PhaseCommand.COMPLETE_PRODUCTION)).get(0);
            if (!result.accepted()) throw new IllegalStateException("Unable to complete production: "
                    + result.rejectionReason().orElse("production transition rejected"));
            displayedSnapshot = result.snapshot();
            replayLabel = "PRODUCTION REPLAY COMPLETE: accepted transition to " + displayedSnapshot.currentPhase();
            observationComplete = true;
            framesAtCurrentState = 0;
        } else {
            completeMarketObservation();
        }
    }

    private void completeMarketObservation() {
        GameSessionCommandResult result = controller.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)).get(0);
        if (!result.accepted()) throw new IllegalStateException("Unable to enter production: "
                + result.rejectionReason().orElse("market transition rejected"));
        displayedSnapshot = result.snapshot();
        replayLabel = "MARKET REPLAY COMPLETE: accepted transition to PRODUCTION; human passes next";
        productionBoundary = true;
        framesAtCurrentState = 0;
    }

    private void showReplayResult() {
        GameSessionSnapshot previous = replayIndex == 0 ? displayedSnapshot : replayResults.get(replayIndex - 1).snapshot();
        GameSessionCommandResult result = replayResults.get(replayIndex);
        displayedSnapshot = result.snapshot();
        replayLabel = replayDescription(previous, result);
        framesAtCurrentState = 0;
    }

    private String replayDescription(GameSessionSnapshot before, GameSessionCommandResult result) {
        String actor = before.currentParticipantId();
        if (!result.accepted()) return "REPLAY " + actor + ": rejected " + result.rejectionReason().orElse("command");
        boolean marketChanged = !before.market().equals(result.snapshot().market());
        boolean boardsChanged = !before.boards().equals(result.snapshot().boards());
        boolean goldChanged = !before.goldBalances().equals(result.snapshot().goldBalances());
        if (before.currentPhase() == RoundPhase.PRODUCTION) {
            String action = "accepted action";
            if (!before.currentParticipantId().equals(result.snapshot().currentParticipantId())) action = "accepted pass";
            else if (before.boards().get(actor).slots().size() > result.snapshot().boards().get(actor).slots().size()) {
                action = "accepted base contribution";
            } else if (boardsChanged) action = "accepted base activation";
            else if (before.pendingRandomGold().isPresent()) action = "accepted random gold allocation";
            else if (goldChanged) action = "accepted personal gold contribution";
            return "PRODUCTION REPLAY " + actor + ": " + action;
        }
        return "REPLAY " + actor + (marketChanged || boardsChanged || goldChanged ? ": accepted purchase" : ": accepted pass")
                + " | gold " + (goldChanged ? "changed" : "unchanged") + ", market "
                + (marketChanged ? "changed" : "unchanged") + ", board " + (boardsChanged ? "changed" : "unchanged");
    }

    private void draw() {
        ScreenUtils.clear(0.08f, 0.09f, 0.12f, 1f);
        batch.begin();
        float y = Gdx.graphics.getHeight() - 24f;
        for (String line : lines()) {
            font.draw(batch, line, 18f, y);
            y -= 20f;
        }
        batch.end();
    }

    private List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("MARKET / PRODUCTION BOT OBSERVATION - normal solo session");
        lines.add("Phase: " + displayedSnapshot.currentPhase() + " | Current: " + displayedSnapshot.currentParticipantId()
                + " | Round: " + displayedSnapshot.roundNumber());
        lines.add(replayLabel);
        lines.add(observationComplete ? "Replay complete: accepted phase boundary / outcome shown"
                : productionBoundary ? "N: pass human production turn and replay bots"
                : replayIndex >= 0 ? "N: next replay action | Replay " + (replayIndex + 1) + "/" + replayResults.size()
                : "P: human market pass | S: capture PNG");
        lines.add("Immutable command-result replay after synchronous execution; NOT live paused bot execution.");
        lines.add("Project: " + projectLine());
        lines.addAll(marketLines());
        lines.add("Participants:");
        displayedSnapshot.participants().forEach(participant -> lines.add("  " + participant.id() + " [" + participant.type()
                + "] gold=" + displayedSnapshot.goldBalances().get(participant.id()) + " board=" + boardLine(participant.id())));
        lines.add("Diagnostic one-round slice, not full game UI; later card-demand deadlock is not ruled out.");
        return lines;
    }

    private String projectLine() {
        return displayedSnapshot.projectProgress().map(progress -> {
            if (progress.currentStageId().isEmpty()) return "complete";
            String stageId = progress.currentStageId().orElseThrow();
            var stage = project.stages().stream().filter(candidate -> candidate.id().equals(stageId)).findFirst().orElseThrow();
            var required = stage.requirements();
            var contributed = progress.contributedResources();
            return "stage=" + stageId + " remaining=" + required.resources().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + (entry.getValue() - contributed.get(entry.getKey())))
                    .reduce((a, b) -> a + ", " + b).orElse("") + " cards="
                    + (required.requiredCardCount() - progress.contributedCardCount());
        }).orElse("unavailable");
    }

    private List<String> marketLines() {
        return displayedSnapshot.market().map(market -> {
            List<String> lines = new ArrayList<>();
            lines.add("Market: draw=" + market.drawPileCount() + " discard=" + market.discardPileCount());
            for (int index = 0; index < market.visibleCards().size(); index++) {
                int position = index + 1;
                lines.add("  slot " + position + ": " + market.visibleCards().get(index).map(card -> card.definition().id()
                        + " listed=" + card.definition().roleParameters().entrySet().stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue().prices())
                                .reduce((a, b) -> a + ", " + b).orElse("none")).orElse("empty"));
            }
            return lines;
        }).orElseGet(() -> List.of("Market: unavailable"));
    }

    private String boardLine(String participantId) {
        return displayedSnapshot.boards().get(participantId).slots().stream()
                .map(slot -> slot.base().instance().definition().id() + "+" + slot.upgrades().size())
                .reduce((a, b) -> a + ", " + b).orElse("empty");
    }

    private void capture(String label) {
        Gdx.files.absolute(EVIDENCE_DIRECTORY).mkdirs();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            PixmapIO.writePNG(Gdx.files.absolute(EVIDENCE_DIRECTORY + "/" + label + ".png"), pixmap, 6, true);
        } finally {
            pixmap.dispose();
        }
    }
}
