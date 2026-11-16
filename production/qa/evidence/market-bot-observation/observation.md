# Market bot observation evidence

- **Run:** `JAVA_TOOL_OPTIONS='-Dmarket.observation.unattended=true -Dmarket.observation.seed=0' ./gradlew :desktop:run --no-daemon`
- **Session:** current `solo-setup.json`, `NORMAL`, one human and three bots.
- **Observation mode:** captured each rendered state after at least 60 frames. It used the actual `SoloGameController` result snapshots; bot turns were already resolved synchronously and then replayed.

## Observed output

- `00-initial.png` shows Round 1, MARKET, player current, the five market positions with ASCII card IDs and listed role prices, every participant's gold and board summary, and the current stage's remaining requirements.
- `01-replay.png` records the human player's accepted pass.
- `02-replay.png` through `13-replay.png` record the ordered accepted bot actions. The final replay image visibly records bot-3's accepted pass; the board summaries show bot-1 added four cards, bot-2 added three cards, and bot-3 added two cards during this market replay.
- `14-production-boundary.png` shows the explicit, accepted `COMPLETE_MARKET` application command after the replay: Round 1 is in PRODUCTION with player current. The observation intentionally stops there and exposes no production controls.

## Limitations

The built-in libGDX font cannot render the catalog's Cyrillic display names. The evidence therefore uses stable ASCII card IDs as the readable diagnostic fallback. `listed` prices are role price data from the displayed card definition; this observation slice does not present a quote for an effect-modified effective price. This is not the full game UI, timer UI, pause UI, or result UI.
