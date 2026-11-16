# Story 001: Вклады в этапы и завершение проекта

> **Epic**: Проект и этапы
> **Status**: Complete
> **Layer**: Core
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-08

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 4 — один пятиэтапный проект, требования ресурсов и карт, эффекты этапов и исход партии.

**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; detailed agreed rules: `design/gdd/card-game-gdd.md` §11.*

- [ ] Проект отслеживает упорядоченную последовательность этапов и принимает вклады только в текущий этап.
- [ ] Вклад личного золота не превышает баланс участника и оставшуюся потребность этапа; неиспользованное золото остаётся на личном балансе.
- [ ] Вклад провианта, стройматериалов и свитков засчитывается в требования текущего этапа; избыток отдельной выплаты не переносится.
- [ ] Передача базы вместе с её улучшениями засчитывает каждую карту пакета в требование карт этапа; избыток карт пакета сгорает.
- [ ] Недопустимый вклад — неизвестного участника, отрицательное значение, вклад в отсутствующий этап или значение сверх разрешённого правила — отклоняется без частичных изменений проекта или баланса.
- [ ] При выполнении всех требований текущий этап завершается немедленно, открывая следующий этап и формируя событие завершения для системы эффектов.
- [ ] Последующие вклады в той же очереди применяются уже к следующему этапу, поэтому эффекты и последовательные выплаты могут образовывать цепочку завершений.
- [ ] Завершение пятого этапа фиксирует победу команды и не допускает дальнейших вкладов.
- [ ] Доменная реализация не импортирует libGDX и остаётся тестируемой через внедряемые зависимости.

## Implementation Notes

- Использовать определения проекта и этапов, созданные в `card-project-data`.
- Согласованные подробности вкладов и порядка завершения описаны в `design/gdd/card-game-gdd.md` §11; параметры проекта передаются как данные.
- Система эффектов получает завершение этапа после открытия следующего; реализация конкретных эффектов относится к отдельному epic.

## Out of Scope

- Конкретные эффекты карт и этапов: `card-stage-effects`.
- Рынок, колода и сброс: `market-deck`.
- Алгоритм ботов, подготовка партии и интерфейс: соответствующие epics.
- Правила начисления депозита и порядок поражения после 30-го раунда: существующие истории `gold-deposit` и `rounds-turns`.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Integration  
**Evidence**: интеграционные проверки в `core/src/test/java/` или документированный playtest; при `qa.level: minimal` evidence является advisory и не блокирует закрытие истории.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: `card-project-data/story-002-project-starting-party-definitions.md` — Complete; `gold-deposit/story-001-gold-balance-and-deposit.md` — Complete.
- Unlocks: эффекты этапов, алгоритм ботов, подготовка партии и игровой результат.

## Completion Notes

**Completed**: 2026-11-08  
**Criteria**: 9/9 — подтверждены пользователем вручную. Проверка очереди также подтверждена пользователем после исправления.  
**Deviations**: нет заявленных отклонений.  
**Test Evidence**: отдельные интеграционные тесты не создавались при `qa.level: minimal`; компиляция `./gradlew :core:compileJava` успешна, но не является поведенческим тестом.  
**Code Review**: пропущен — режим solo.  
**Run result**: N/A — изменения доменной модели не имеют отдельной видимой игроку поверхности.
