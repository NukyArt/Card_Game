# Main menu verification — 2026-11-14

Run result: OBSERVED — desktop application launched, driven with real Windows mouse input, screenshots retained and representative images reviewed with Read.

## Automated
- Test-first attempt: new GameNavigationTest failed compilation before GameNavigation existed (expected red).
- Initial full compilation exposed Windows-1251 default encoding; core JavaCompile now explicitly uses UTF-8.
- `./gradlew :core:test :desktop:classes`: PASS.
- JUnit XML totals: 136 tests, 0 failures/errors, 0 skipped. Includes 5 new navigation tests.
- Tests cover initial page, mode selection/back, disabled and out-of-context actions, running-match confirmation/cancellation, direct finished-match return and repeat navigation into a match. These are model tests, not GPU/resource tests.

## Observed desktop behavior
- Main menu appears instead of immediately starting a match; Russian glyphs render correctly, labels fit.
- Create lobby opens mode selection; Join lobby and Network clicks leave their page unchanged.
- Bots launches an ordinary configured match; Main menu opens confirmation.
- Cancellation returns to the same match; confirmation discards it and returns to main menu.
- A subsequent Bots launch creates a new initial match, including a newly randomized market.
- Existing refresh-market and end-turn controls remain functional; accepted command feedback and bot progress observed.
- Manual pause survives opening confirmation, waiting three seconds, and cancelling: game remains PAUSED and same state visible. Game render/update and keyboard handling are not invoked while confirmation is active (code inspection); exhaustive live timer/keyboard stress testing was not performed.
- Resized client 984×761: centered layout with preserved aspect ratio, matching mouse coordinates, disabled Local/Settings choices, Back, and Exit verified with DPI-aware Windows capture/input.
- Exit button closes window; second `:desktop:run` completed with exit code 0.

## Retained evidence
First interaction sequence, 1280×720:
- `1794666217977-01-main-menu.png` — reviewed.
- `1794666217977-02-disabled-join.png`.
- `1794666217977-03-mode-selection.png` — reviewed.
- `1794666217977-04-disabled-network.png`.
- `1794666217977-05-game.png`.
- `1794666217977-06-confirm-return.png` — reviewed.
- `1794666217977-07-confirm-after-wait.png`.
- `1794666217977-08-resumed-game.png` — reviewed.
- `1794666217977-09-returned-menu.png`.
- `1794666217977-10-new-game.png` — reviewed; fresh initial state and different market.

Command and pause checks:
- `1794666288924-11-market-command.png`.
- `1794666288924-12-turn-command.png`.
- `1794666288924-13-manual-pause.png`.
- `1794666288924-14-pause-preserved.png` — reviewed.

Successful DPI-aware resize sequence:
- `1794666356886-20-resized-main-menu.png`.
- `1794666356886-21-resized-mode-selection.png` — reviewed.
- `1794666356886-22-disabled-local.png`.
- `1794666356886-23-back-to-menu.png`.
- `1794666356886-24-disabled-settings.png`.

## Limitations and discarded observations
- First background launch was stopped by its 120-second task time limit. Relaunched with a longer limit for final resize/exit checks.
- `1794666288924-15-...` through `1794666288924-19-...` are NOT accepted resize/exit evidence: the initial screenshot driver was not DPI-aware after moving/resizing the window, producing incorrect desktop capture coordinates and clicks. Retained but superseded by the 20–24 sequence. No application defect inferred from these captures.
- Victory/defeat was not reached in this run. Direct return from completed match is covered by navigation unit test and inspected outcome callback wiring, NOT a retained screenshot of an actual result. Needs a later result-screen playtest; no story is marked Done here.
- Existing English gameplay screen is intentionally not fully translated or visually redesigned.

## Font provenance
- Font: https://raw.githubusercontent.com/notofonts/noto-fonts/main/hinted/ttf/NotoSans/NotoSans-Regular.ttf
- License: https://raw.githubusercontent.com/notofonts/noto-fonts/main/LICENSE
- Downloaded unchanged; SIL OFL 1.1 retained at `assets/fonts/OFL.txt`.

No Git commands or new worktrees used during implementation/verification. No domain/application or game balance changes.
