# Story 002: Неизменяемые определения проекта и стартовой партии

> **Epic**: Данные карт и проектов
> **Status**: Complete
> **Layer**: Foundation
> **Type**: Logic
> **Estimate**: M
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-07

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature: один пятиэтапный проект, требования ресурсов и карт, эффекты этапов и исход партии.

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story:*

- [x] Можно выразить требования этапа по четырём ресурсам и числу передаваемых карт без изменяемых ссылок наружу.
- [x] Определение этапа включает стабильный идентификатор, отображаемое имя, требования и описание эффекта завершения.
- [x] Определение проекта хранит упорядоченную неизменяемую последовательность этапов и не допускает пустую последовательность или дублирующиеся идентификаторы.
- [x] Внешний прикладной слой может собрать определение стартовой партии для человека и трёх ботов с 10 золотыми и четырьмя стартовыми базами на участника, не выполняя загрузку файлов в домене.
- [x] Значения нормальной сложности, включая пять этапов первого проекта, могут быть переданы как данные, а не заданы литералами в правилах домена.
- [x] Невалидные значения требований — отрицательные количества, невалидные участники или неизвестные карты — отклоняются без частично созданного определения.
- [x] Детерминированные unit-тесты проверяют неизменяемость, порядок этапов и валидацию.
- [x] Типы не импортируют libGDX или внешние слои.

---

## Implementation Notes

- Не выполнять файловый ввод-вывод из домена.
- Ссылки на определения карт передавать через явные доменные идентификаторы или ссылки, проверяемые на границе построения данных.

---

## Out of Scope

- Story 001: определения карт и ресурсов.
- Story 003: источник случайности.
- Прогресс этапа, внесение ресурсов, завершение этапа, победа и загрузка `game.docx`.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

**Story Type**: Logic
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [x] Созданы и пройдены — `./gradlew :core:test` (2026-11-07): 16 тестов, ошибок нет; из них 15 новых тестов истории.

- `core/src/test/java/com/myjavacardgame/domain/project/ProjectStageDefinitionTest.java`
- `core/src/test/java/com/myjavacardgame/domain/project/ProjectDefinitionTest.java`
- `core/src/test/java/com/myjavacardgame/domain/party/StartingPartyDefinitionTest.java`

---

## Dependencies

- Depends on: Story 001 must be DONE
- Unlocks: истории о прогрессе проекта, рынке и подготовке партии
