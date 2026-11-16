# Story 001: Фазовая модель раунда и допустимость команд

> **Epic**: Раунды и очереди
> **Status**: Complete
> **Layer**: Foundation
> **Type**: Logic
> **Estimate**: M
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-07

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature: раунды, очереди, таймеры, золото и депозит.

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story:*

- [ ] В чистом Java-пакете `domain.round` определены фазы дохода, рынка, производства, депозита и результата матча.
- [ ] Фазы реализуют согласованный контракт `PhaseState`, а не ветвление UI или libGDX-классов.
- [ ] Переходы разрешены только в последовательности: доход → рынок → производство → депозит → следующий раунд либо результат.
- [ ] Каждая фаза знает допустимые для неё доменные команды; команда не из текущей фазы отклоняется без изменения состояния.
- [ ] Контракт результата команды различает принятую и отклонённую команду, содержит причину отказа и не требует libGDX-типов.
- [ ] Фазовая модель не читает системное время и не зависит от `application`, `presentation` или `desktop`.
- [ ] Публичные API документированы JavaDoc.
- [ ] Детерминированные unit-тесты проверяют допустимые переходы, запрет неверного перехода и неизменность состояния при отказе.

---

## Implementation Notes

- Использовать State Pattern для разрешения команд и переходов между фазами.
- Таймер и конкретные игровые действия остаются за границей этой истории.

---

## Out of Scope

- Story 002: сессия, порядок участников и исход матча.
- Конкретные рыночные, производственные и ресурсные действия; запуск таймера; проверка требований проекта.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

**Story Type**: Logic
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [x] Created — `core/src/test/java/com/myjavacardgame/domain/round/RoundPhaseMachineTest.java`

---

## Dependencies

- Depends on: None
- Unlocks: Story 002

## Completion Notes

**Completed**: 2026-11-07
**Criteria**: фазовая модель и тесты реализованы; `./gradlew :core:test` успешно выполнен при реализации.
**Deviations**: по согласованию добавлен переход из производства к результату для будущей немедленной победы; определение исхода остаётся вне этой истории.
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/round/RoundPhaseMachineTest.java` — 5 тестов.
**Code Review**: не проводился (solo mode).
**Run result**: N/A — фазовая модель не меняет видимую игроку часть игры.
