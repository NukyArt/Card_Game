# Story 002: Планшет, базы и улучшения

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

*From `design/game-brief.md` (the Player goal & fail state field + MVP feature 3), scoped to this story; detailed board rules: `design/gdd/card-game-gdd.md` §11.*

- [ ] Планшет хранит упорядоченные позиции баз и прикреплённые к каждой базе улучшения.
- [ ] На планшете размещается не более шести баз, под каждой — не более трёх улучшений; ограничения поступают из данных правил.
- [ ] Улучшение размещается только под базой того же типа и при наличии свободного места.
- [ ] Карта с разрешёнными двумя ролями размещается в выбранной роли; параметры улучшения не подменяются параметрами базы.
- [ ] Отклонённое размещение не меняет планшет.
- [ ] Снимок планшета не предоставляет изменяемые ссылки на его внутреннее состояние.

## Implementation Notes

- Использовать определения и экземпляры Story 001; ограничения получать из данных правил.
- Сохранять домен независимым от libGDX и внешних слоёв; публичные API документировать.

## Out of Scope

- Оплата покупки, продажа и разрешение эффектов.
- Story 003: извлечение пакетов и учёт активаций.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Logic  
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: Story 001 must be DONE.
- Unlocks: Story 003.

## Completion Notes

**Completed**: 2026-11-08  
**Criteria**: 6/6 passing; AC-1–AC-5 confirmed manually by the user, AC-6 verified by automated test.  
**Deviations**: None. Board limits are supplied by the caller; integration with external rule data is not implemented in this story.  
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/board/BoardSnapshotTest.java` — 1 test; `./gradlew :core:test --tests com.myjavacardgame.domain.board.BoardSnapshotTest` successful.  
**Code Review**: Skipped — solo mode.  
**Run result**: N/A — чистая доменная логика не имеет видимой игроку поверхности.
