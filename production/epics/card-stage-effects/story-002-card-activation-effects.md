# Story 002: Эффекты активации карт и выборы

> **Epic**: Эффекты карт и этапов
> **Status**: Complete
> **Layer**: Core
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-11

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 3 — планшеты, базы, улучшения и эффекты карт.

**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; detailed agreed rules: `design/gdd/card-game-gdd.md` §11 and card catalogue.*

- [x] При активации база и её улучшения выполняют базовую выплату с применимыми эффектами до дополнительных выплат в порядке карты; база активируется не более раза за раунд.
- [x] Произведённое золото делится участником целыми монетами между текущим этапом и личным балансом; избыток золота остаётся на балансе, избыток других ресурсов сгорает.
- [x] Ростовщик, Винодельня и Кузница дают дополнительные выплаты согласно каталогу; Шахта позволяет выбрать базовые 2 стройматериала или 2 золота, сохраняя производство улучшений.
- [x] Овца-база и Шар-база получают бонус за каждую другую соответствующую базу владельца; Шар-улучшение этот бонус не получает.
- [x] Охотничий домик и Лаборатория позволяют не более одного необязательного обмена при достаточном личном балансе после базовой выплаты, включая применение обмена к новому этапу.
- [x] Цитадель после базовой выплаты производит две единицы ресурсов, тип каждой выбирается независимо и равновероятно из четырёх через инъецированную случайность.
- [x] Инспектор может необязательно передать одну базу со всеми улучшениями другому участнику при свободном слоте; перенос не является продажей или вкладом и сохраняет историю активации.
- [x] Последовательные выплаты могут завершать разные этапы; переполнение отдельной выплаты не переносится, а после победы дальнейшие действия не выполняются.

## Implementation Notes

- Использовать согласованные уточнения `design/gdd/card-game-gdd.md` §11 и проверить каталог `design/gdd/game.docx` для полноты конкретных выплат.
- Значения правил брать из данных, а не зашивать в доменную логику.

## Out of Scope

- Story 001: общий механизм и доставка событий.
- Story 003: эффекты завершения этапов, рыночные бонусы и скидки.
- Алгоритм выбора действий ботов.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Integration  
**Required evidence**: advisory интеграционные тесты в `core/src/test/java/` либо документированный плейтест; при `qa.level: minimal` не блокируют закрытие.  
**Status**: [x] Интеграционные тесты: `core/src/test/java/com/myjavacardgame/domain/round/GameSessionActivationTest.java` и `GameSessionStageMarketEffectsTest.java`; последний запуск `./gradlew test --rerun-tasks` — успешно, 123 теста core, desktop скомпилирован.

## Dependencies

- Depends on: Story 001; существующие истории планшета, проекта, рынка и источника случайности.
- Unlocks: истории ботов и интеграционная проверка эффектов.

## Completion Notes

**Completed**: 2026-11-11  
**Criteria**: 8/8 — подтверждены пользователем и покрыты интеграционными тестами. Равновероятность и независимость результатов зависят от реализации инъецированного `RandomSource`; статистическая равномерность отдельно не проверялась.  
**Deviations**: совместно реализованы эффекты этапов и рыночные права Story 003; Story 003 остаётся In Progress и требует отдельной проверки и закрытия. Каталог и команды домена ещё не подключены к экрану партии.  
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/round/GameSessionActivationTest.java`, `GameSessionStageMarketEffectsTest.java`; ранее выполнен `./gradlew test --rerun-tasks` — 123 теста core прошли, desktop скомпилирован. При `qa.level: minimal` evidence-gate не требовался.  
**Code Review**: пропущен — solo review mode.  
**Run result**: N/A — история не добавляет самостоятельную видимую поверхность; экран партии и подключение игровой сессии ещё не реализованы.

