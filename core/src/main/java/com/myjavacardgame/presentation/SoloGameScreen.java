package com.myjavacardgame.presentation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.myjavacardgame.LaunchOptions;
import com.myjavacardgame.application.*;
import com.myjavacardgame.domain.cards.*;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.board.BoardRules;
import com.myjavacardgame.domain.market.MarketRules;
import com.myjavacardgame.domain.round.*;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/** Tabletop solo UI: snapshots drive cards, commands drive rules, animations are decorative. */
public final class SoloGameScreen {
    private static final String EVIDENCE = "production/qa/evidence/warm-fantasy-ui";
    private static final int WIDTH = TableLayout.WIDTH, HEIGHT = TableLayout.HEIGHT;
    private final SpriteBatch batch = new SpriteBatch();
    private final List<Button> buttons = new ArrayList<>();
    private final List<VisibleCard> visible = new ArrayList<>();
    private final Map<String, VisibleCard> ghosts = new HashMap<>();
    private final Map<String, Float> hover = new HashMap<>();
    private final Map<String, Float> pulses = new HashMap<>();
    private final Set<String> projectSources = new HashSet<>();
    private final Deque<String> feed = new ArrayDeque<>();
    private final TableAnimations animations = new TableAnimations();
    private final java.util.function.Consumer<Boolean> requestMainMenu;
    private final LaunchOptions options;
    private final RussianGameText words;
    private final TableStyle style;
    private final TableLayout table;
    private final CardRenderer cards;
    private SoloGameDriver driver;
    private ProjectDefinition project;
    private ProjectOverlayState projectWindows = new ProjectOverlayState();
    private GameSessionSnapshot observed;
    private int marketIndex, baseIndex, upgradeIndex, transferFrom, transferTo = 1, transferIndex;
    private int capturedFrames, captureNumber;
    private final GoldAmountInput contributionAmount = new GoldAmountInput();
    private final GoldAmountInput promptAmount = new GoldAmountInput();
    private ActivationPrompt amountPrompt;
    private String contributionContext;
    private boolean purchaseChoices;
    private String selectedBase, selectedMarket, selectedUpgrade, inspectId, hoveredId;
    private float pointerX, pointerY, hoverTime, projectPulse;
    private boolean captureRequested, motion, journal;
    private String feedback;
    private record Button(TableLayout.Rect bounds, Runnable action) { }
    private record VisibleCard(String id, CardViewModel model, TableLayout.Rect bounds, String owner,
            int index, boolean market, boolean upgrade, boolean used, int upgradeCount) { }

    /** Starts an ordinary solo match for callers without navigation. */
    public SoloGameScreen() { this(finished -> Gdx.app.exit()); }
    /** Starts an ordinary match with a navigation callback. */
    public SoloGameScreen(java.util.function.Consumer<Boolean> requestMainMenu) {
        this(requestMainMenu, LaunchOptions.parse(new String[0]));
    }
    /** Loads the explicitly selected normal or QA scenario without changing its rules. */
    public SoloGameScreen(java.util.function.Consumer<Boolean> requestMainMenu, LaunchOptions options) {
        this.requestMainMenu = Objects.requireNonNull(requestMainMenu);
        this.options = Objects.requireNonNull(options);
        try (Reader text = Gdx.files.internal("i18n/game-text.json").reader("UTF-8");
                Reader config = Gdx.files.internal("ui/table-style.json").reader("UTF-8")) {
            words = new RussianGameText(text); style = TableStyle.load(config);
        } catch (Exception exception) { throw new IllegalStateException("Unable to load UI configuration", exception); }
        table = new TableLayout(style); cards = new CardRenderer(words, style);
        motion = style.animations(); animations.setEnabled(motion);
        newGame();
    }
    private String t(String key) { return words.text(key); }
    private String f(String key, Object... pairs) { return words.format(key, pairs); }
    private String participant(String id) { return words.participant(id); }

    private void newGame() {
        try (Reader setup = Gdx.files.internal(options.setupPath()).reader("UTF-8");
                Reader catalog = Gdx.files.internal("data/card-catalog.json").reader("UTF-8");
                Reader stages = Gdx.files.internal(options.projectPath()).reader("UTF-8")) {
            var data = new SoloSetupLoader().load(setup, catalog, stages);
            project = data.project();
            driver = new SoloSessionBuilder().buildDriver(data, new Random()::nextInt,
                    new MarketRules(data.marketVisibleSlotCount(), data.marketRefreshGoldCost(),
                            new BoardRules(data.boardMaxBases(), data.boardMaxUpgradesPerBase()), data.marketSalePriceDivisor()),
                    new DepositRules(data.depositGoldPerBonus(), data.depositMaximumBonus(), data.roundIncomeGold()));
            feed.clear(); animations.clear(); ghosts.clear(); hover.clear(); pulses.clear(); projectSources.clear();
            projectWindows = new ProjectOverlayState();
            marketIndex = baseIndex = upgradeIndex = transferFrom = transferIndex = 0;
            contributionAmount.reset(0); promptAmount.reset(0); amountPrompt = null; contributionContext = null; purchaseChoices = false;
            transferTo = 1; selectedBase = selectedMarket = selectedUpgrade = inspectId = hoveredId = null;
            projectPulse = hoverTime = 0; feedback = t("started");
            observed = driver.snapshot(); synchronizeSelections(observed);
            if (motion) {
                int order = 0;
                for (var card : placements(observed).values()) if (card.market()) {
                    animations.deal(card.id(), table.deck(), card.bounds(), style.moveSeconds(), order++ * style.moveSeconds() * .2f);
                }
            }
        } catch (Exception exception) { throw new IllegalStateException("Unable to load solo setup", exception); }
    }

    private boolean humanTurn() {
        return !driver.paused() && driver.snapshot().outcome() == MatchOutcome.IN_PROGRESS
                && driver.humanId().equals(driver.snapshot().currentParticipantId());
    }

    private GoldAmountInput activeAmount() {
        return driver.activationPrompt().isPresent() ? promptAmount : contributionAmount;
    }

    private boolean goldPrompt(ActivationPrompt prompt) {
        return prompt instanceof ActivationPrompt.GoldAllocation || prompt instanceof ActivationPrompt.RandomGoldAllocation;
    }

    private void confirmGold() {
        if (!humanTurn()) return;
        var prompt = driver.activationPrompt();
        if (prompt.isPresent()) {
            if (goldPrompt(prompt.orElseThrow())) answer(new ActivationAnswer.Gold(prompt.orElseThrow().activationId(), promptAmount.value()));
        } else if (driver.snapshot().currentPhase() == RoundPhase.PRODUCTION) {
            submit(new ContributeGoldCommand(driver.humanId(), contributionAmount.value()));
        }
        activeAmount().blur();
    }

    private void handleKeyboard() {
        if (projectWindows.blocking()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) closeProjectWindow();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (activeAmount().focused()) activeAmount().blur();
            else if (driver.activationPrompt().isPresent()) driver.cancelActivation();
            else if (inspectId != null) inspectId = null;
            else if (purchaseChoices) purchaseChoices = false;
            else returnToMenu();
            return;
        }
        if (inspectId != null || purchaseChoices || !humanTurn()) return;
        var input = activeAmount();
        if (input.focused()) {
            for (int i = 0; i < 10; i++) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + i) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + i)) input.digit(i);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)) input.backspace();
            if (Gdx.input.isKeyJustPressed(Input.Keys.FORWARD_DEL)) input.clear();
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.F)) confirmGold();
            return;
        }
        if (driver.activationPrompt().isPresent()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) confirmGold();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && driver.snapshot().currentPhase() == RoundPhase.PRODUCTION && selectedBase != null) driver.beginActivation(baseIndex);
        else if (Gdx.input.isKeyJustPressed(Input.Keys.F)) confirmGold();
        else if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && driver.snapshot().currentPhase() == RoundPhase.MARKET) submit(new RefreshMarketCommand(driver.humanId()));
        else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) endTurn();
    }

    private void syncContributionMaximum(GameSessionSnapshot state) {
        int balance = state.goldBalances().getOrDefault(driver.humanId(), 0);
        int remaining = state.projectProgress().map(p -> p.currentStageId().flatMap(id -> project.stages().stream()
                .filter(s -> s.id().equals(id)).findFirst()).map(s -> Math.max(0,
                        s.requirements().resources().get(com.myjavacardgame.domain.resources.ResourceType.GOLD)
                        - p.contributedResources().get(com.myjavacardgame.domain.resources.ResourceType.GOLD))).orElse(0)).orElse(0);
        int maximum = Math.min(balance, remaining);
        String context = state.roundNumber() + ":" + state.currentPhase() + ":" + balance + ":" + state.projectProgress();
        if (!Objects.equals(context, contributionContext)) { contributionAmount.reset(maximum); contributionContext = context; }
        else contributionAmount.limit(maximum);
    }

    private void syncPromptAmount(ActivationPrompt prompt) {
        if (amountPrompt != prompt) {
            int maximum = prompt instanceof ActivationPrompt.GoldAllocation gold ? gold.maximumProjectGold()
                    : prompt instanceof ActivationPrompt.RandomGoldAllocation gold ? gold.maximumProjectGold() : 0;
            promptAmount.reset(maximum);
            contributionAmount.blur();
            amountPrompt = prompt;
        }
    }

    /** Advances the session and decorative time, renders the table and dispatches at most one click. */
    public void render() {
        int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
        if (width <= 0 || height <= 0) return;
        consumeResults();
        synchronizeSelections(driver.snapshot());
        syncContributionMaximum(driver.snapshot());
        driver.activationPrompt().ifPresent(this::syncPromptAmount);
        handleKeyboard();
        if (!projectWindows.blocking() && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) driver.setPaused(!driver.paused());
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) captureRequested = true;
        float delta = Gdx.graphics.getDeltaTime();
        driver.update(delta);
        consumeResults();
        if (!driver.paused()) {
            animations.update(delta);
            projectPulse = Math.max(0, projectPulse - delta);
            pulses.replaceAll((id, time) -> Math.max(0, time - delta));
            pulses.values().removeIf(time -> time == 0);
            projectSources.retainAll(pulses.keySet());
        }
        ghosts.keySet().removeIf(id -> !animations.moving(id));
        var viewport = TableLayout.viewport(width, height);
        pointerX = viewport.x(Gdx.input.getX()); pointerY = viewport.y(Gdx.input.getY());
        float offsetX = viewport.offsetX() / viewport.scale(), offsetY = viewport.offsetY() / viewport.scale();
        ScreenUtils.clear(.94f, .79f, .55f, 1);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(-offsetX, -offsetY, width / viewport.scale(), height / viewport.scale()));
        buttons.clear(); visible.clear();
        var state = driver.snapshot(); synchronizeSelections(state); syncContributionMaximum(state);
        batch.begin();
        drawTable(); drawHeader(state);
        if (state.outcome() == MatchOutcome.IN_PROGRESS) drawGame(state); else drawResult(state);
        if (driver.activationPrompt().isPresent() && state.outcome() == MatchOutcome.IN_PROGRESS) drawPrompt(state, driver.activationPrompt().orElseThrow());
        else if (purchaseChoices && state.outcome() == MatchOutcome.IN_PROGRESS) drawPurchaseChoices(state);
        else if (inspectId != null && state.outcome() == MatchOutcome.IN_PROGRESS) drawInspection(state);
        else if (state.outcome() == MatchOutcome.IN_PROGRESS) drawHover(delta);
        drawFooter();
        if (projectWindows.blocking()) drawProjectWindow(state);
        batch.end();
        if (!projectWindows.blocking() && Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT) && driver.activationPrompt().isEmpty() && !purchaseChoices && !journal) {
            if (inspectId == null) visible.stream().filter(c -> (!c.market() || !animations.moving(c.id())) && c.bounds().contains(pointerX, pointerY)).findFirst().ifPresent(c -> inspectId = c.id());
            else {
                var selected = placements(state).get(inspectId);
                if (selected != null && !selected.market()) state.boards().get(selected.owner()).slots().stream()
                        .filter(s -> s.base().instance().instanceId().equals(selected.id()) || s.upgrades().stream().anyMatch(u -> u.instance().instanceId().equals(selected.id())))
                        .findFirst().ifPresent(s -> {
                            var ids = new ArrayList<String>(); ids.add(s.base().instance().instanceId());
                            s.upgrades().forEach(u -> ids.add(u.instance().instanceId()));
                            inspectId = ids.get((ids.indexOf(inspectId) + 1) % ids.size());
                        });
            }
        }
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            for (int i = buttons.size() - 1; i >= 0; i--) {
                var button = buttons.get(i);
                if (button.bounds().contains(pointerX, pointerY)) { invoke(button.action()); break; }
            }
        }
        capturedFrames++;
        if (captureRequested || capturedFrames == Integer.getInteger("ccgs.captureFrame", -1)) {
            captureRequested = false; capture();
            if (Boolean.getBoolean("ccgs.exitAfterCapture")) Gdx.app.exit();
        }
    }

    private void consumeResults() {
        for (GameSessionCommandResult result : driver.drainResults()) {
            if (!result.accepted()) feedback = rejection(result.rejectionReason().orElse(""));
            else {
                projectWindows.enqueue(result.snapshot().stageCompletedEvents(), driver.paused());
                if (projectWindows.blocking()) driver.setPaused(true);
                observeChanges(observed, result.snapshot()); observed = result.snapshot();
                String balances = observed.participants().stream().map(p -> participant(p.id()) + ": " + observed.goldBalances().get(p.id())).collect(Collectors.joining(", "));
                feed.addLast(f("feed", "round", observed.roundNumber(), "phase", words.label("phase", observed.currentPhase()), "player", participant(observed.currentParticipantId()), "gold", balances));
                while (feed.size() > 5) feed.removeFirst();
            }
        }
    }

    private void synchronizeSelections(GameSessionSnapshot state) {
        var bases = state.boards().get(driver.humanId()).slots();
        baseIndex = 0;
        for (int i = 0; i < bases.size(); i++) if (bases.get(i).base().instance().instanceId().equals(selectedBase)) baseIndex = i;
        selectedBase = bases.isEmpty() ? null : bases.get(baseIndex).base().instance().instanceId();
        var upgrades = bases.isEmpty() ? List.<PlacedCard>of() : bases.get(baseIndex).upgrades();
        upgradeIndex = 0;
        for (int i = 0; i < upgrades.size(); i++) if (upgrades.get(i).instance().instanceId().equals(selectedUpgrade)) upgradeIndex = i;
        selectedUpgrade = upgrades.isEmpty() ? null : upgrades.get(upgradeIndex).instance().instanceId();
        state.market().ifPresent(market -> {
            marketIndex = -1;
            for (int i = 0; i < market.visibleCards().size(); i++) if (market.visibleCards().get(i).map(c -> c.instanceId().equals(selectedMarket)).orElse(false)) marketIndex = i;
            if (marketIndex < 0) for (int i = 0; i < market.visibleCards().size(); i++) if (market.visibleCards().get(i).isPresent()) { marketIndex = i; break; }
            selectedMarket = marketIndex < 0 ? null : market.visibleCards().get(marketIndex).orElseThrow().instanceId();
        });
    }

    private List<String> seats(GameSessionSnapshot state) {
        var out = new ArrayList<String>(); out.add(driver.humanId());
        state.participants().stream().map(SessionParticipantSnapshot::id).filter(id -> !id.equals(driver.humanId())).forEach(out::add);
        return out;
    }
    private Map<String, VisibleCard> placements(GameSessionSnapshot state) {
        var out = new LinkedHashMap<String, VisibleCard>();
        var owners = seats(state);
        for (int seat = 0; seat < Math.min(4, owners.size()); seat++) {
            String owner = owners.get(seat); var slots = state.boards().get(owner).slots();
            for (int i = 0; i < Math.min(6, slots.size()); i++) {
                var slot = slots.get(i); var instance = slot.base().instance(); var rect = table.board(seat, i);
                boolean used = slot.lastActivatedRound().orElse(-1) == state.roundNumber();
                out.put(instance.instanceId(), new VisibleCard(instance.instanceId(), CardViewModel.from(instance.definition()), rect, owner, i, false, false, used, slot.upgrades().size()));
                for (int u = 0; u < slot.upgrades().size(); u++) {
                    var upgrade = slot.upgrades().get(u).instance();
                    out.put(upgrade.instanceId(), new VisibleCard(upgrade.instanceId(), CardViewModel.from(upgrade.definition()), rect, owner, u, false, true, used, 0));
                }
            }
        }
        state.market().ifPresent(m -> {
            for (int i = 0; i < m.visibleCards().size(); i++) if (m.visibleCards().get(i).isPresent()) {
                var c = m.visibleCards().get(i).orElseThrow();
                out.put(c.instanceId(), new VisibleCard(c.instanceId(), CardViewModel.from(c.definition()), table.market(i, m.visibleCards().size()), "", i, true, false, false, 0));
            }
        });
        return out;
    }
    private void observeChanges(GameSessionSnapshot before, GameSessionSnapshot after) {
        if (!motion) return;
        var old = placements(before); var next = placements(after);
        boolean projectChanged = !before.projectProgress().equals(after.projectProgress());
        int dealOrder = 0;
        for (var card : next.values()) {
            ghosts.remove(card.id());
            var previous = old.get(card.id());
            if (previous == null) {
                if (card.market()) animations.deal(card.id(), table.deck(), card.bounds(), style.moveSeconds(), dealOrder++ * style.moveSeconds() * .2f);
            } else if (!previous.bounds().equals(card.bounds()) || previous.market() != card.market()) {
                ghosts.remove(card.id());
                animations.move(card.id(), previous.bounds(), card.bounds(), style.moveSeconds(), false);
            }
            if (previous != null && !previous.used() && card.used() && !card.upgrade()) {
                pulses.put(card.id(), style.effectSeconds());
                if (projectChanged) projectSources.add(card.id()); else projectSources.remove(card.id());
            }
        }
        for (var card : old.values()) if (!next.containsKey(card.id()) && card.market()) {
            ghosts.put(card.id(), card); animations.move(card.id(), card.bounds(), table.discard(), style.moveSeconds(), true);
        }
        if (projectChanged) projectPulse = style.effectSeconds();
        for (String owner : seats(after)) if (!Objects.equals(before.goldBalances().get(owner), after.goldBalances().get(owner))) pulses.put(owner, style.effectSeconds());
    }

    private void drawTable() {
        fill(0, 0, WIDTH, HEIGHT, style.tableColor());
        fill(10, 26, 1260, 655, "BD8747");
        fill(16, 32, 1248, 643, style.tableColor());
        fill(262, 263, 756, 297, "F9DBA4");
        fill(0, 686, WIDTH, 34, style.accentColor());
    }
    private void drawHeader(GameSessionSnapshot state) {
        label(16, 712, 590, f("header", "round", state.roundNumber(), "phase", words.label("phase", state.currentPhase()), "player", participant(state.currentParticipantId())), 13, Color.valueOf(TableStyle.INK));
        label(600, 711, 140, f("timer", "seconds", (int) Math.ceil(driver.remainingSeconds())), 12, Color.valueOf(TableStyle.HEADING));
        button(750, 692, 140, 23, t(driver.paused() ? "resume" : "pause"), () -> driver.setPaused(!driver.paused()));
        button(896, 692, 137, 23, t(motion ? "table.motionOn" : "table.motionOff"), this::toggleMotion);
        button(1039, 692, 86, 23, t("screenshot"), () -> captureRequested = true);
        button(1131, 692, 137, 23, t("menu"), this::returnToMenu);
        if (!options.label().isEmpty()) label(20, 678, 290, options.label(), 12, Color.valueOf(TableStyle.HEADING));
    }
    private void toggleMotion() {
        motion = !motion; animations.setEnabled(motion); hover.clear(); pulses.clear(); projectSources.clear(); ghosts.clear(); projectPulse = 0;
    }
    private void drawGame(GameSessionSnapshot state) {
        var owners = seats(state);
        for (int seat = 0; seat < Math.min(4, owners.size()); seat++) {
            String owner = owners.get(seat);
            float x = seat <= 1 ? 352 : seat == 2 ? 24 : 1080;
            float y = seat == 0 ? 207 : seat == 1 ? 683 : 566;
            String name = (seat == 0 ? t("table.yourBoard") + " / " : "") + f("balance", "player", participant(owner), "gold", state.goldBalances().get(owner));
            label(x, y, seat <= 1 ? 580 : 180, name, 12, pulses.containsKey(owner) ? Color.valueOf(TableStyle.HEADING) : Color.valueOf(TableStyle.INK));
        }
        drawProject(state);
        label(282, 452, 350, t("market"), 12, Color.valueOf(TableStyle.HEADING));
        state.market().ifPresent(m -> {
            var deck = table.deck();
            label(deck.x() - 10, 451, 147, f("deck", "deck", m.drawPileCount(), "discard", m.discardPileCount()), 11, Color.valueOf(TableStyle.MUTED_INK));
            if (m.drawPileCount() > 0) {
                for (int layer = 3; layer >= 0; layer--) cards.back(batch,
                        new TableLayout.Rect(deck.x() + layer * 3, deck.y() + layer * 2, deck.width(), deck.height()));
            } else {
                cards.fill(batch, deck, Color.valueOf("E8C48B"));
                cards.text(batch, t("empty"), deck.x() + 10, deck.y() + deck.height() / 2, deck.width() - 20, 35, 13, Color.valueOf(TableStyle.MUTED_INK));
            }
            if (m.discardPileCount() > 0) cards.back(batch, table.discard());
            for (int i = 0; i < m.visibleCards().size(); i++) if (m.visibleCards().get(i).isEmpty()) {
                var rect = table.market(i, m.visibleCards().size()); cards.fill(batch, rect, Color.valueOf("E8C48B"));
                label(rect.x() + 10, rect.y() + rect.height() / 2, rect.width() - 20, t("empty"), 12, Color.valueOf(TableStyle.MUTED_INK));
            }
        });
        var placed = placements(state);
        visible.addAll(placed.values().stream().filter(c -> !c.upgrade()).toList());
        VisibleCard raised = null;
        for (var card : visible) {
            if (!animations.moving(card.id()) && card.bounds().contains(pointerX, pointerY) && driver.activationPrompt().isEmpty() && inspectId == null) raised = card;
            else drawCard(card, false);
            if (!card.market() || !animations.moving(card.id())) buttons.add(new Button(card.bounds(), () -> selectCard(card)));
        }
        if (raised != null) drawCard(raised, true);
        for (var ghost : ghosts.values()) {
            cards.draw(batch, ghost.model(), animations.position(ghost.id(), table.discard()), false, false, 0);
        }
        // An incoming attached upgrade has no independent board slot, but still flies to its parent.
        for (var card : placed.values()) if (card.upgrade() && animations.moving(card.id())) {
            cards.draw(batch, card.model(), animations.position(card.id(), card.bounds()), false, false, 0);
        }
        drawActions(state);
    }
    private void drawCard(VisibleCard card, boolean over) {
        float current = hover.getOrDefault(card.id(), 0f);
        float target = over && motion ? 1 : 0;
        float step = style.hoverSeconds() == 0 ? 1 : Math.min(1, Gdx.graphics.getDeltaTime() / style.hoverSeconds());
        current += Math.signum(target - current) * Math.min(Math.abs(target - current), step);
        if (current == 0) hover.remove(card.id()); else hover.put(card.id(), current);
        var rect = animations.position(card.id(), card.bounds()).lifted(style.hoverLift() * current, 1 + (style.hoverScale() - 1) * current);
        boolean selected = card.id().equals(selectedBase) || card.id().equals(selectedMarket);
        cards.reveal(batch, card.model(), rect, selected, card.used(), card.upgradeCount(), animations.reveal(card.id()));
        Float pulse = pulses.get(card.id());
        if (pulse != null && style.effectSeconds() > 0) {
            float t = 1 - pulse / style.effectSeconds();
            cards.fill(batch, rect, new Color(.9f, .77f, .4f, .25f * (1 - t)));
            if (projectSources.contains(card.id())) {
                float x = rect.x() + (640 - rect.x()) * t, y = rect.top() + (500 - rect.top()) * t;
                cards.fill(batch, new TableLayout.Rect(x, y, 6, 6), Color.valueOf(TableStyle.HEADING));
            }
        }
    }
    private void selectCard(VisibleCard card) {
        if (card.market()) selectedMarket = card.id();
        else if (card.owner().equals(driver.humanId())) { selectedBase = card.id(); selectedUpgrade = null; }
        else inspectId = card.id();
    }

    private void drawProject(GameSessionSnapshot state) {
        var panel = table.project(); cards.fill(batch, panel, Color.valueOf(style.panelColor()));
        label(286, 541, 270, t("table.projectTitle"), 14, Color.valueOf(TableStyle.HEADING));
        state.projectProgress().ifPresent(progress -> {
            label(772, 541, 225, f("stages", "done", progress.completedStageCount(), "total", project.stages().size()), 12, Color.valueOf(TableStyle.INK));
            float step = 705f / Math.max(1, project.stages().size());
            for (int i = 0; i < project.stages().size(); i++) {
                String color = i < progress.completedStageCount() ? "A66A2C" : i == progress.completedStageCount() ? style.accentColor() : "DAB783";
                fill(286 + i * step, 510, step - 5, 7, color);
            }
            var stage = progress.currentStageId().flatMap(id -> project.stages().stream().filter(s -> s.id().equals(id)).findFirst());
            label(286, 504, 705, stage.map(s -> s.displayName()).orElse(t("complete")), 12, Color.valueOf(TableStyle.INK));
            stage.ifPresent(s -> {
                String values = Arrays.stream(com.myjavacardgame.domain.resources.ResourceType.values())
                        .map(r -> words.label("resource", r) + " " + progress.contributedResources().get(r) + "/" + s.requirements().resources().get(r))
                        .collect(Collectors.joining("   ·   "));
                label(286, 484, 705, values + "   ·   " + f("cards", "done", progress.contributedCardCount(), "total", s.requirements().requiredCardCount()), 10, Color.valueOf(TableStyle.MUTED_INK));
            });
        });
        if (state.stageModifiers().isEmpty()) label(286, 465, 705, t("stageNoModifiers"), 10, Color.valueOf(TableStyle.MUTED_INK));
        else label(286, 465, 705, f("stageModifiers", "effects", state.stageModifiers().stream()
                .map(e -> (e.amount() >= 0 ? "+" : "") + e.amount() + " " + words.label("resource", e.resource()))
                .collect(Collectors.joining("; "))), 10, Color.valueOf(TableStyle.HEADING));
        button(560, 525, 200, 24, t("project.allStages"), this::openProjectOverview);
        if (projectPulse > 0 && style.effectSeconds() > 0) cards.fill(batch, panel, new Color(.85f, .74f, .4f, .15f * projectPulse / style.effectSeconds()));
    }
    private void drawActions(GameSessionSnapshot state) {
        button(266, 239, 133, 25, t("buy"), this::openPurchaseChoices);
        button(404, 239, 140, 25, t("refresh") + " [Q]", () -> submit(new RefreshMarketCommand(driver.humanId())));
        button(549, 239, 150, 25, t("activate") + " [E]", () -> driver.beginActivation(baseIndex));
        button(704, 239, 150, 25, t("sellBase"), () -> submit(new SellBasePackageCommand(driver.humanId(), baseIndex)));
        button(859, 239, 155, 25, t("table.inspect"), () -> inspectId = selectedBase);
        drawAmountEditor(contributionAmount, 266, 209, 24);
        button(544, 209, 110, 24, f("contributeGold", "amount", contributionAmount.value()) + " [F]", this::confirmGold);
        button(659, 209, 178, 24, t("contributeBase"), () -> submit(new ContributeBoardPackageCommand(driver.humanId(), baseIndex)));
        button(842, 209, 172, 24, t("endTurn"), this::endTurn);
        label(19, 160, 200, f("table.upgradeTarget", "number", baseIndex + 1), 12, Color.valueOf(TableStyle.MUTED_INK));
        var board = state.boards().get(driver.humanId());
        if (!board.slots().isEmpty()) label(19, 139, 200, board.slots().get(baseIndex).base().instance().definition().name(), 12, Color.valueOf(TableStyle.INK));
    }

    private void drawAmountEditor(GoldAmountInput input, float x, float y, float height) {
        button(x, y, 40, height, "-10", () -> input.adjust(-10));
        button(x + 44, y, 32, height, "-", () -> input.adjust(-1));
        button(x + 80, y, 100, height, (input.focused() ? "> " : "") + input.value(), () -> { contributionAmount.blur(); promptAmount.blur(); input.focus(); });
        button(x + 184, y, 32, height, "+", () -> input.adjust(1));
        button(x + 220, y, 48, height, "+10", () -> input.adjust(10));
    }

    private List<MarketPurchaseQuote> purchaseQuotes() {
        var quotes = new ArrayList<MarketPurchaseQuote>();
        driver.quoteMarketPurchase(marketIndex, CardRole.BASE, -1).ifPresent(quotes::add);
        int count = driver.snapshot().boards().get(driver.humanId()).slots().size();
        for (int i = 0; i < count; i++) driver.quoteMarketPurchase(marketIndex, CardRole.UPGRADE, i).ifPresent(quotes::add);
        return quotes;
    }

    private void openPurchaseChoices() {
        var quotes = purchaseQuotes();
        if (quotes.size() == 1) { var q = quotes.get(0); confirmPurchase(q.role(), q.baseIndex()); }
        else if (quotes.isEmpty()) feedback = t("purchaseUnavailable");
        else { purchaseChoices = true; contributionAmount.blur(); }
    }

    private void drawPurchaseChoices(GameSessionSnapshot state) {
        if (!humanTurn() || state.currentPhase() != RoundPhase.MARKET) { purchaseChoices = false; return; }
        modal(300, 100, 680, 520);
        label(330, 590, 620, t("purchaseHow"), 18, Color.valueOf(TableStyle.HEADING));
        int y = 515;
        for (var quote : purchaseQuotes()) {
            String text = quote.role() == CardRole.BASE ? t("buyAsBase") : t("buyAsUpgrade") + " / "
                    + state.boards().get(driver.humanId()).slots().get(quote.baseIndex()).base().instance().definition().name();
            button(330, y, 620, 38, text + " / " + quote.effectiveGoldCost() + " " + words.label("resource", com.myjavacardgame.domain.resources.ResourceType.GOLD),
                    () -> confirmPurchase(quote.role(), quote.baseIndex()));
            y -= 45;
        }
        button(330, 122, 620, 34, t("table.close"), () -> purchaseChoices = false);
    }

    private void confirmPurchase(CardRole role, int target) {
        if (driver.quoteMarketPurchase(marketIndex, role, target).isEmpty()) { feedback = t("purchaseUnavailable"); return; }
        purchaseChoices = false;
        submit(new BuyMarketCardCommand(driver.humanId(), marketIndex, role, target));
    }

    private void drawHover(float delta) {
        var hovered = visible.stream().filter(c -> !animations.moving(c.id()) && c.bounds().contains(pointerX, pointerY)).findFirst().orElse(null);
        String id = hovered == null ? null : hovered.id();
        if (!Objects.equals(id, hoveredId)) { hoveredId = id; hoverTime = 0; }
        else hoverTime += delta;
        if (hovered != null && hoverTime > Math.max(.25f, style.hoverSeconds() * 2)) {
            float x = hovered.bounds().x() < 640 ? 725 : 280;
            cards.draw(batch, hovered.model(), new TableLayout.Rect(x, 258, 250, 390.625f), true, hovered.used(), hovered.upgradeCount());
        }
    }
    private void drawInspection(GameSessionSnapshot state) {
        var all = placements(state); var selected = all.get(inspectId);
        if (selected == null) { inspectId = null; return; }
        modal(216, 60, 848, 610);
        label(242, 648, 600, participant(selected.owner().isEmpty() ? driver.humanId() : selected.owner()) + " / " + selected.model().name(), 18, Color.valueOf(TableStyle.INK));
        button(933, 629, 108, 28, t("table.close"), () -> inspectId = null);
        cards.draw(batch, selected.model(), new TableLayout.Rect(244, 177, 272, 425), true, selected.used(), selected.upgradeCount());
        String content = selected.model().description();
        if (selected.model().base().isPresent()) content += "\n\n" + cards.production(selected.model().base().orElseThrow());
        if (selected.model().upgrade().isPresent()) content += "\n\n" + t("card.upgradeShort") + " " + words.amounts(selected.model().upgrade().orElseThrow().prices()) + "\n" + cards.production(selected.model().upgrade().orElseThrow());
        cards.text(batch, content.strip(), 540, 602, 495, 230, 15, Color.valueOf(TableStyle.INK));
        label(541, 391, 490, t("activationFree"), 12, Color.valueOf(TableStyle.MUTED_INK));
        label(541, 371, 490, t("inspectionHelp"), 12, Color.valueOf(TableStyle.MUTED_INK));
        if (!selected.market()) {
            var board = state.boards().get(selected.owner());
            var parent = board.slots().stream().filter(s -> s.base().instance().instanceId().equals(selected.id()) || s.upgrades().stream().anyMatch(u -> u.instance().instanceId().equals(selected.id()))).findFirst();
            parent.ifPresent(slot -> {
                label(541, 356, 490, t("upgrades").replace("{cards}", ""), 14, Color.valueOf(TableStyle.HEADING));
                for (int i = 0; i < slot.upgrades().size(); i++) {
                    var upgrade = slot.upgrades().get(i).instance();
                    button(540, 309 - i * 36, 490, 29, (upgrade.instanceId().equals(selectedUpgrade) ? "> " : "") + upgrade.definition().name(), () -> {
                        if (selected.owner().equals(driver.humanId())) selectedUpgrade = upgrade.instanceId();
                        inspectId = upgrade.instanceId();
                    });
                }
                if (slot.upgrades().isEmpty()) label(541, 326, 490, t("none"), 14, Color.valueOf(TableStyle.MUTED_INK));
                if (selected.owner().equals(driver.humanId())) {
                    button(540, 91, 230, 30, t("sellUpgrade"), () -> { submit(new SellUpgradeCommand(driver.humanId(), baseIndex, upgradeIndex)); inspectId = null; });
                }
            });
        }
    }

    private void drawPrompt(GameSessionSnapshot state, ActivationPrompt prompt) {
        inspectId = null;
        syncPromptAmount(prompt);
        modal(380, 147, 520, 465);
        label(405, 584, 470, t("activation"), 18, Color.valueOf(TableStyle.HEADING));
        drawActivationProject(state);
        if (goldPrompt(prompt)) drawAmountEditor(promptAmount, 405, 517, 29);
        if (prompt instanceof ActivationPrompt.ResourceChoice choice) {
            label(405, 495, 470, choice.mandatory() ? t("chooseResource") : f("exchange", "gold", choice.goldCost()), 14, Color.valueOf(TableStyle.INK));
            int y = 435;
            for (var resource : choice.allowed()) {
                button(405, y, 470, 30, words.label("resource", resource), () -> answer(new ActivationAnswer.Resource(prompt.activationId(), Optional.of(resource)))); y -= 38;
            }
            if (!choice.mandatory()) button(405, y, 470, 30, t("skipExchange"), () -> answer(new ActivationAnswer.Resource(prompt.activationId(), Optional.empty())));
        } else if (prompt instanceof ActivationPrompt.GoldAllocation gold) drawGoldPrompt(prompt, gold.payout(), gold.maximumProjectGold());
        else if (prompt instanceof ActivationPrompt.RandomGoldAllocation gold) drawGoldPrompt(prompt, gold.payout(), gold.maximumProjectGold());
        else if (prompt instanceof ActivationPrompt.TransferChoice) {
            var participants = state.participants();
            button(405, 454, 470, 30, f("from", "player", participant(participants.get(transferFrom).id())), () -> { transferFrom = (transferFrom + 1) % participants.size(); transferIndex = 0; });
            button(405, 416, 470, 30, f("to", "player", participant(participants.get(transferTo).id())), () -> transferTo = (transferTo + 1) % participants.size());
            button(405, 378, 470, 30, f("source", "number", transferIndex + 1), () -> {
                int count = state.boards().get(participants.get(transferFrom).id()).slots().size(); transferIndex = (transferIndex + 1) % Math.max(1, count);
            });
            button(405, 326, 470, 30, t("transfer"), () -> answer(new ActivationAnswer.Transfer(prompt.activationId(), Optional.of(new ActivateBaseCommand.Transfer(participants.get(transferFrom).id(), transferIndex, participants.get(transferTo).id())))));
            button(405, 288, 470, 30, t("skipTransfer"), () -> answer(new ActivationAnswer.Transfer(prompt.activationId(), Optional.empty())));
        }
        button(405, 170, 470, 30, t("cancelActivation"), () -> driver.cancelActivation());
    }
    private void drawGoldPrompt(ActivationPrompt prompt, int payout, int maximum) {
        label(405, 485, 470, f("payout", "gold", payout, "maximum", maximum), 15, Color.valueOf(TableStyle.INK));
        label(405, 452, 470, t("allocationHelp"), 14, Color.valueOf(TableStyle.MUTED_INK));
        button(405, 371, 470, 34, f("allocate", "gold", promptAmount.value()), () -> answer(new ActivationAnswer.Gold(prompt.activationId(), promptAmount.value())));
        button(405, 326, 470, 34, t("keep"), () -> answer(new ActivationAnswer.Gold(prompt.activationId(), 0)));
    }
    private void openProjectOverview() {
        var progress = driver.snapshot().projectProgress().orElseThrow();
        projectWindows.openOverview(Math.min(progress.completedStageCount(), project.stages().size() - 1), driver.paused());
        contributionAmount.blur(); promptAmount.blur();
        driver.setPaused(true);
    }

    private void closeProjectWindow() { driver.setPaused(projectWindows.closeTop()); }

    private void drawActivationProject(GameSessionSnapshot state) {
        var progress = driver.activationProjectProgress().or(() -> state.projectProgress());
        if (progress.isEmpty()) return;
        fill(24, 260, 330, 345, style.panelColor());
        label(40, 581, 298, t("project.activationProgress"), 15, Color.valueOf(TableStyle.HEADING));
        var rows = ProjectViewModel.stages(project, progress.orElseThrow());
        var current = rows.stream().filter(row -> row.status() == ProjectViewModel.Status.CURRENT).findFirst();
        if (current.isPresent()) drawStageDetails(current.orElseThrow(), 40, 549, 298);
        else label(40, 549, 298, t("complete"), 15, Color.valueOf(TableStyle.INK));
    }

    private void drawStageDetails(ProjectViewModel.Stage row, float x, float top, float width) {
        cards.text(batch, row.definition().displayName(), x, top, width, 52, 17, Color.valueOf(TableStyle.INK));
        label(x, top - 55, width, t("project.status." + row.status()), 13, Color.valueOf(TableStyle.HEADING));
        int index = 0;
        for (var resource : com.myjavacardgame.domain.resources.ResourceType.values()) {
            label(x, top - 85 - index++ * 25, width, words.label("resource", resource) + ": "
                    + row.contributedResources().get(resource) + " / " + row.definition().requirements().resources().get(resource), 14, Color.valueOf(TableStyle.INK));
        }
        label(x, top - 185, width, f("cards", "done", row.contributedCards(), "total", row.definition().requirements().requiredCardCount()), 14, Color.valueOf(TableStyle.INK));
        cards.text(batch, row.definition().completionEffectDescription(), x, top - 218, width, 75, 14, Color.valueOf(TableStyle.MUTED_INK));
    }

    private void drawProjectWindow(GameSessionSnapshot state) {
        modal(170, 100, 940, 520);
        buttons.add(new Button(new TableLayout.Rect(0, 0, WIDTH, HEIGHT), () -> { }));
        var progress = state.projectProgress().orElseThrow();
        if (projectWindows.overview()) {
            label(195, 593, 850, t("project.allStages"), 20, Color.valueOf(TableStyle.HEADING));
            var rows = ProjectViewModel.stages(project, progress);
            int selected = Math.min(projectWindows.selectedStage(), rows.size() - 1);
            for (int index = 0; index < rows.size(); index++) {
                int target = index;
                var row = rows.get(index);
                button(195, 515 - index * 62, 345, 52,
                        (index == selected ? "> " : "") + row.definition().displayName() + " / " + t("project.status." + row.status()),
                        () -> projectWindows.selectStage(target));
            }
            drawStageDetails(rows.get(selected), 570, 545, 500);
        } else {
            var event = projectWindows.notice().orElseThrow();
            var completed = project.stages().stream().filter(stage -> stage.id().equals(event.completedStageId())).findFirst().orElseThrow();
            label(205, 575, 870, t("project.stageCompleted"), 23, Color.valueOf(TableStyle.HEADING));
            cards.text(batch, completed.displayName(), 205, 525, 870, 65, 22, Color.valueOf(TableStyle.INK));
            label(205, 440, 870, t("project.rewardApplied"), 16, Color.valueOf(TableStyle.HEADING));
            cards.text(batch, completed.completionEffectDescription(), 205, 403, 870, 110, 20, Color.valueOf(TableStyle.INK));
            button(205, 220, 870, 38, t("project.allStages"), this::openProjectOverview);
        }
        button(205, 127, 870, 40, t("table.close") + " [Esc]", this::closeProjectWindow);
    }

    private void drawResult(GameSessionSnapshot state) {
        modal(308, 170, 664, 388);
        label(344, 519, 592, t(state.outcome() == MatchOutcome.VICTORY ? "victory" : "defeat"), 23, Color.valueOf(TableStyle.HEADING));
        label(344, 461, 592, f("finalRound", "round", state.roundNumber()), 19, Color.valueOf(TableStyle.INK));
        label(344, 420, 592, f("completedStages", "done", state.projectProgress().map(p -> p.completedStageCount()).orElse(0)), 19, Color.valueOf(TableStyle.INK));
        button(344, 288, 592, 44, t("newGame"), this::newGame);
        button(344, 230, 592, 44, t("menu"), this::returnToMenu);
    }
    private void drawFooter() {
        fill(0, 0, WIDTH, 25, style.accentColor());
        label(14, 20, 1040, feedback, 12, Color.valueOf(TableStyle.INK));
        button(1080, 2, 187, 21, t("table.log"), () -> journal = !journal);
        if (journal) {
            var logBounds = new TableLayout.Rect(20, 31, 1240, 122);
            cards.fill(batch, logBounds, Color.valueOf(style.panelColor()));
            buttons.add(new Button(logBounds, () -> { }));
            int y = 139; for (String entry : feed) { label(31, y, 1215, entry, 12, Color.valueOf(TableStyle.MUTED_INK)); y -= 21; }
        }
    }
    private void modal(float x, float y, float w, float h) {
        cards.fill(batch, new TableLayout.Rect(0, 26, WIDTH, 660), new Color(0, 0, 0, .68f));
        // The last matching region consumes clicks; only subsequently registered modal buttons win.
        buttons.add(new Button(new TableLayout.Rect(0, 26, WIDTH, 660), () -> { }));
        fill(x - 2, y - 2, w + 4, h + 4, style.accentColor()); fill(x, y, w, h, style.panelColor());
    }
    private void fill(float x, float y, float w, float h, String hex) { cards.fill(batch, new TableLayout.Rect(x, y, w, h), Color.valueOf(hex)); }
    private void label(float x, float top, float width, String value, float size, Color color) { cards.text(batch, value, x, top, width, size * 1.5f, size, color); }
    private void button(float x, float y, float width, float height, String label, Runnable action) {
        var bounds = new TableLayout.Rect(x, y, width, height);
        boolean over = bounds.contains(pointerX, pointerY);
        fill(x, y, width, height, over ? "FFD28A" : style.accentColor());
        fill(x, y, width, 1, "D69A48");
        cards.text(batch, label, x + 7, y + height - 5, width - 14, height - 8, Math.min(13, height * .5f), Color.valueOf(TableStyle.INK));
        buttons.add(new Button(bounds, action));
    }
    private void returnToMenu() { animations.clear(); ghosts.clear(); requestMainMenu.accept(driver.snapshot().outcome() != MatchOutcome.IN_PROGRESS); }
    private void endTurn() { submit(new EndTurnCommand(driver.humanId())); }
    private void submit(GameSessionCommand command) {
        var result = driver.execute(command);
        feedback = result.accepted() ? f("accepted", "action", t("action." + command.getClass().getSimpleName())) : rejection(result.rejectionReason().orElse(""));
    }
    private String rejection(String reason) { Gdx.app.log("Action rejected", reason); return words.rejection(reason); }
    private void answer(ActivationAnswer answer) { driver.answerActivation(answer); }
    private void invoke(Runnable action) { try { action.run(); } catch (IllegalArgumentException exception) { feedback = rejection(exception.getMessage()); } }
    private void capture() {
        var directory = Gdx.files.absolute(new java.io.File(EVIDENCE).getAbsolutePath()); directory.mkdirs();
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(directory.child(String.format("table-layout-%02d-%s-%d.png", ++captureNumber, driver.snapshot().outcome(), System.currentTimeMillis())), pixels, 6, true); }
        finally { pixels.dispose(); }
    }
    /** Releases resources owned only by this screen. */
    public void dispose() { batch.dispose(); cards.dispose(); animations.clear(); }
}
