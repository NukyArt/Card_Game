# Story 001: Экземпляры карт и закрепление роли

> **Epic**: Карты и планшет
> **Status**: Complete
> **Layer**: Core
> **Type**: Logic
> **Estimate**: M
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

*From `design/game-brief.md` (the Player goal & fail state field + MVP feature 3), scoped to this story; detailed role rules: `design/gdd/card-game-gdd.md` §11.*

- [ ] Экземпляр карты ссылается на существующее определение и отличается от других экземпляров того же определения.
- [ ] При размещении выбирается одна из ролей, разрешённых определением карты; недопустимая роль отклоняется.
- [ ] Роль размещённой карты остаётся неизменной, пока карта находится на планшете.
- [ ] Для выбранной роли доступны только соответствующие параметры роли из определения.
- [ ] Создание и проверка экземпляров не зависят от libGDX или файлового ввода-вывода; публичный API документирован.

## Implementation Notes

- Переиспользовать существующие определения и параметры ролей в `domain.cards`.
- Использовать чистые Java-типы; игровые данные не зашивать в код.

## Out of Scope

- Story 002: размещение на планшете.
- Покупка и конкретное исполнение эффектов карт.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Logic  
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: существующие определения карт из `card-project-data`.
- Unlocks: Story 002.

## Completion Notes

**Completed**: 2026-11-08  
**Criteria**: 5/5 passing.  
**Deviations**: None. Роль закреплена за конкретным размещением, поэтому один экземпляр можно позднее разместить в другой допустимой роли. Глобальную уникальность идентификаторов обеспечивает будущий владелец коллекции.  
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/cards/CardInstanceTest.java` — 5 tests; `./gradlew :core:test` successful.  
**Code Review**: Skipped — solo mode.  
**Run result**: N/A — чистая доменная логика не имеет видимой игроку поверхности.
