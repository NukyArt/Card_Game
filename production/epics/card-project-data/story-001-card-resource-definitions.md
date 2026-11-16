# Story 001: Чистые доменные определения карт и ресурсов

> **Epic**: Данные карт и проектов
> **Status**: Complete
> **Layer**: Foundation
> **Type**: Logic
> **Estimate**: M
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-07

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature: планшеты, базы, улучшения и эффекты карт.

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story:*

- [ ] В `com.myjavacardgame.domain` определены Java-типы ресурсов: золото, провиант, стройматериалы и свитки.
- [ ] Определение карты неизменно после создания и содержит стабильный идентификатор, имя, тип, допустимые роли, цены и описание доменного эффекта.
- [ ] Для карты, доступной в двух ролях, явно заданы независимые параметры роли базы и роли улучшения.
- [ ] Невозможно создать определение с пустым идентификатором или именем, отрицательной ценой либо без допустимой роли.
- [ ] Доменные типы не импортируют `com.badlogic.gdx.*` и не зависят от `application`, `presentation` или `desktop`.
- [ ] Публичные API имеют JavaDoc.
- [ ] Детерминированные unit-тесты покрывают валидные определения и отклонение недопустимых данных.

---

## Implementation Notes

- Использовать только Java 17 и собственные доменные типы.
- Представлять игровые значения как данные, а не литералы в правилах.

---

## Out of Scope

- Story 002: определения проекта и стартовой партии.
- Story 003: источник случайности.
- Экземпляры карт на планшете, рынок, активации и разрешение эффектов.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

**Story Type**: Logic
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [ ] Not yet created

---

## Dependencies

- Depends on: None
- Unlocks: Story 002, Story 003
