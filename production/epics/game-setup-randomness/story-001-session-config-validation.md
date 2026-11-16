# Story 001: Проверка и загрузка конфигурации соло-партии

> **Epic**: Подготовка партии и случайность
> **Status**: Complete
> **Layer**: Feature
> **Type**: Integration
> **Estimate**: M
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-13

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP features 1–2; рабочие требования `TR-SOLO-01`, `TR-SOLO-15`

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: Архитектурное решение не требуется на minimal; данные должны загружаться извне и передаваться домену через проверенный контракт.
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**:
- Required: Проверять внешние данные до создания игровой сессии; использовать существующие доменные определения.
- Forbidden: Файловый ввод-вывод в домене; зависимость домена от libGDX и внешних слоёв.
- Guardrail: Некорректная конфигурация не должна приводить к частично созданной партии.

---

## Acceptance Criteria

*From `design/game-brief.md` (the **Player goal & fail state** field + the MVP feature this story implements), scoped to this story:*

- [x] Конфигурация задаёт ровно одного игрока и трёх ботов-союзников для нормальной сложности.
- [x] Каталог задаёт 20 наименований карт, их роли, цены и эффекты, не подменяя доменные определения.
- [x] Данные старта задают каждому участнику 10 золотых и по одной базе каждого из четырёх типов.
- [x] Распределение задаёт 34 карты в рыночной колоде; вместе со стартовыми картами состав получается из 50 экземпляров.
- [x] Конфигурация проекта задаёт пять этапов фэнтезийного города с требованиями ресурсов, карт и эффектами из согласованной таблицы.
- [x] Некорректные, неполные или противоречивые данные отклоняются с диагностикой до создания игровой сессии.
- [x] Загрузка данных выполняется во внешнем/application-слое; домен не выполняет файловый ввод-вывод.

## Implementation Notes

- Представить внешние данные отдельными DTO/конфигурационными объектами и преобразовать их в доменные определения после валидации.
- Проверять количество участников, стартовые значения, состав колоды, пять этапов и обязательные поля каталога до передачи данных фабрике сессии.
- Ошибка должна содержать причину и не оставлять частично построенное состояние.

## Out of Scope

*Handled by neighbouring stories — do not implement here:*

- Story 002: создание начальной игровой сессии, перемешивание и открытие рынка.
- Выбор сложности, сетевые режимы и другие размеры команды.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above*

## Test Evidence

*Governed by `qa.level`: at `qa.level: minimal` tests are waived (advisory, never "must exist and pass").*

**Story Type**: Integration
**Required evidence**:
- Integration: integration test under `core/src/test/java/` or documented playtest, advisory at minimal.

**Status**: [x] Created and verified

## Dependencies

- Depends on: Existing card, project, resource and randomness domain definitions.
- Unlocks: Story 002 — Сборка воспроизводимой начальной игровой сессии.

## Completion Notes
**Completed**: 2026-11-13
**Criteria**: 7/7 passing
**Deviations**: COMPLETE WITH NOTES — `SoloSetupLoader` contains MVP validation constants for catalog, market, and project sizes; story template does not enumerate implementation files.
**Test Evidence**: Integration test at `core/src/test/java/com/myjavacardgame/application/SoloSetupLoaderTest.java`; `./gradlew :core:test` passed.
**Code Review**: Skipped — solo mode
