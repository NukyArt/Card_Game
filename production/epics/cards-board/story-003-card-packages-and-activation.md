# Story 003: Пакеты карт и учёт активаций

> **Epic**: Карты и планшет
> **Status**: Complete
> **Layer**: Core
> **Type**: Logic
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-08

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

*From `design/game-brief.md` (the Player goal & fail state field + MVP feature 3), scoped to this story; detailed package and production rules: `design/gdd/card-game-gdd.md` §11.*

- [ ] Извлечение базы возвращает её вместе со всеми прикреплёнными улучшениями и освобождает позицию.
- [ ] Отдельное улучшение можно извлечь для продажи, не удаляя базу и остальные улучшения.
- [ ] Передача базы на этап предоставляет весь пакет; каждая карта пакета учитывается отдельно. Сам приём этапом и обработка избытка остаются за эпиком проекта.
- [ ] База активируется не более одного раза за раунд; неиспользованная активация не переносится на следующий раунд.
- [ ] При передаче базы другому участнику уже использованная в текущем раунде активация сохраняется.
- [ ] Недопустимое извлечение или повторная активация не изменяет состояние планшета.

## Implementation Notes

- Сохранять состав пакета и состояние активации при перемещении карты; не реализовывать игровые эффекты.
- Сохранять домен независимым от libGDX и внешних слоёв; публичные API документировать.

## Out of Scope

- Выручка от продажи, приём и прогресс проекта, выплаты и конкретный эффект передачи Инспектора.
- Передача отдельного улучшения на этап не утверждена и не входит в историю.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Logic  
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: Story 002 must be DONE; существующая модель раунда из `rounds-turns`.
- Unlocks: истории рынка, проекта и эффектов.

## Completion Notes

**Completed**: 2026-11-08  
**Criteria**: 6/6 — подтверждены пользователем; отдельные автоматические тесты для истории не написаны.  
**Deviations**: нет; приём этапом, эффекты и подключение текущего номера раунда остаются вне этой истории.  
**Test Evidence**: при `qa.level: minimal` отдельные тесты не требуются; ранее выполнен `./gradlew :core:test` — успешно, но покрытие новых сценариев этим не подтверждено.  
**Code Review**: пропущен — режим solo.  
**Run result**: N/A — изменения доменной модели не имеют видимой игроку поверхности.
