# Story 002: Сборка воспроизводимой начальной игровой сессии

> **Epic**: Подготовка партии и случайность
> **Status**: Complete
> **Layer**: Feature
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-14

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP features 1–2; рабочие требования `TR-SOLO-01`, `TR-SOLO-17`

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: Архитектурное решение не требуется на minimal; случайность внедряется через контракт, а сборка партии атомарно создаёт начальное состояние.
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**:
- Required: Принимать валидированные данные и внедряемый `RandomSource`; возвращать согласованное начальное состояние.
- Forbidden: Глобальный генератор случайности; частично созданная игровая сессия после ошибки.
- Guardrail: Полный состав карт должен сохраняться без потери или дублирования; случайность должна быть воспроизводима.

---

## Acceptance Criteria

*From `design/game-brief.md` (the **Player goal & fail state** field + the MVP feature this story implements), scoped to this story:*

- [x] Фабрика или сценарий сборки принимает валидированные данные и внедряемый `RandomSource`.
- [x] Новая сессия создаёт игрока, трёх ботов, их стартовые балансы и четыре стартовые базы каждого участника.
- [x] Колода, рынок из пяти открытых карт, сброс и пятиэтапный проект создаются из конфигурации без потери или дублирования карт.
- [x] Колода и начальный рынок используют внедрённую случайность, а не глобальный генератор.
- [x] Одинаковые входные данные и одинаковая последовательность случайности создают эквивалентное начальное состояние.
- [x] Другая допустимая последовательность случайности может менять порядок колоды или рынка, сохраняя полный состав карт.
- [x] Сессия начинает первый раунд с игроком как активным участником и фиксированным порядком дальнейших ходов.
- [x] Ошибка сборки не возвращает частично созданную сессию.

## Implementation Notes

- Использовать Story 001 как единственную точку получения проверенной конфигурации.
- Инъецировать существующий контракт случайности из домена; не создавать генератор внутри фабрики и не обращаться к статическому состоянию.
- Сначала подготовить все компоненты и проверить инварианты, затем опубликовать/вернуть сессию как единое состояние.
- Для проверки состава считать стартовые карты, карты рынка и карты колоды по стабильным идентификаторам.

## Out of Scope

*Handled by neighbouring stories — do not implement here:*

- Story 001: чтение, преобразование и валидация внешней конфигурации.
- Эффекты карт и этапов, алгоритмы ботов, интерфейс и запуск desktop-приложения.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above*

## Test Evidence

*Governed by `qa.level`: at `qa.level: minimal` tests are waived (advisory, never "must exist and pass").*

**Story Type**: Integration
**Required evidence**:
- Integration: `core/src/test/java/` integration test or documented playtest, advisory at minimal.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: Story 001 — Проверка и загрузка конфигурации соло-партии.
- Unlocks: Build order step 2 — локальный цикл рынка, планшетов, производства и этапов с ботами.

---

## Completion Notes
**Completed**: 2026-11-14
**Criteria**: 8/8 passing; acceptance scenarios confirmed manually.
**Deviations**: COMPLETE WITH NOTES — MVP participant/base/project/market counts are validation invariants in `SoloSessionBuilder`; `MarketSnapshot` does not reveal hidden deck identity or order for automated reproducibility checks.
**Test Evidence**: None required at `qa.level: minimal`; `./gradlew :core:test` passed.
**Code Review**: Skipped — solo mode
