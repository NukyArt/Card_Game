# Story 002: Сессия партии, очередь участников и исход матча

> **Epic**: Раунды и очереди
> **Status**: Complete
> **Layer**: Foundation
> **Type**: Logic
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-07

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature: соло-партия: один игрок, три бота; завершить пять этапов до 30-го раунда.

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story:*

- [ ] `GameSession` создаётся из внешнего начального определения и хранит упорядоченный неизменяемый набор одного человека и трёх ботов, первого игрока, текущего участника, номер раунда, текущую фазу и исход матча.
- [ ] В фазах рынка и производства участники получают одну очередь по фиксированному кругу, начиная с первого игрока; после завершения очереди текущим становится следующий участник.
- [ ] После депозита первого игрока следующего раунда выбирают следующим в фиксированном круге; счётчик раундов увеличивается только после завершения депозита.
- [ ] Снимок сессии неизменяем и не возвращает наружу изменяемые внутренние коллекции или сущности.
- [ ] Сессия принимает команды через единый вход, делегирует проверку текущей фазе и не оставляет частичных изменений при отказе.
- [ ] Она поддерживает немедленную фиксацию победы по внешнему доменному сигналу завершения последнего этапа.
- [ ] После депозита 30-го раунда незавершённая партия фиксируется как поражение и не переходит к 31-му раунду.
- [ ] Завершённая партия отклоняет игровые команды без изменения состояния.
- [ ] Публичные API имеют JavaDoc, не зависят от libGDX и покрыты детерминированными unit-тестами.

---

## Implementation Notes

- Использовать только чистые Java-типы и снимки состояния без изменяемых ссылок наружу.
- Сигнал завершения проекта передаётся домену явным контрактом; реализация проекта не входит в историю.

---

## Out of Scope

- Story 001: фазовые состояния и базовая допустимость команд.
- Доход и депозитная формула, таймеры, создание и прогресс проекта, конкретные команды рынка и производства, Observer-события.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

**Story Type**: Logic
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [x] Created — `core/src/test/java/com/myjavacardgame/domain/round/GameSessionTest.java`

---

## Dependencies

- Depends on: Story 001 must be DONE; `card-project-data/story-002-project-starting-party-definitions.md` must be DONE
- Unlocks: истории рынка, ресурсов, проекта, таймера и ботов

## Completion Notes

**Completed**: 2026-11-07
**Criteria**: игровая сессия, порядок ходов, снимки и исход реализованы; `./gradlew :core:test` успешно выполнен при реализации.
**Deviations**: по согласованию расширена фазовая модель Story 001 — переход из рынка к результату для немедленной победы.
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/round/GameSessionTest.java` и `RoundPhaseMachineTest.java`; при реализации успешно выполнены 36 тестов проекта.
**Code Review**: не проводился (solo mode).
**Run result**: N/A — изменения доменной модели не имеют видимой игроку поверхности.
