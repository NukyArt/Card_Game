# Story 001: Учёт золота и депозита

> **Epic**: Золото и депозит
> **Status**: Complete
> **Layer**: Core
> **Type**: Logic
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-08

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 2 — раунды, очереди, таймеры, золото и депозит.

**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*Из `design/game-brief.md` (цель игрока и MVP feature 2); правило бонуса уточнено в `design/gdd/card-game-gdd.md` §11.*

- [ ] У каждого участника в состоянии партии есть личный баланс, инициализированный его `startingGold`.
- [ ] Успешное получение золота увеличивает баланс соответствующего участника.
- [ ] В конце раунда каждому участнику начисляется бонус `min(потолок, floor(остаток / шаг))`, рассчитанный по его остатку до начисления; исходное золото сохраняется и бонус повторно в расчёт не входит.
- [ ] Шаг и потолок бонуса передаются извне как данные правил; текущие значения — 3 и 5.
- [ ] Бонус не начисляется повторно за тот же раунд.
- [ ] Балансы доступны для отображения через неизменяемое состояние.
- [ ] Некорректная операция, включая отрицательную сумму или отсутствующего участника, отклоняется без частичного изменения состояния.
- [ ] Источники дохода, траты, эффекты и прогресс проекта остаются за соседними историями.

## Implementation Notes

- Сохранять домен независимым от libGDX и внешних слоёв; публичные API документировать.
- Начислять бонус в конце раунда до перехода к следующему раунду или поражению; учитывать его не более одного раза за раунд.
- Использовать правила бонуса, переданные извне, без зашитых игровых значений.

## Out of Scope

- Источники дохода, расходы золота, пороги проекта, выплаты и финальный подсчёт.
- Рынок, эффекты и интерфейс партии.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Logic  
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [ ] Not yet created

## Dependencies

- Depends on: завершённые истории эпиков `rounds-turns` и `cards-board`.
- Unlocks: истории рынка, проекта, эффектов и результата партии.

## Completion Notes

**Completed**: 2026-11-08  
**Criteria**: 8/8 — подтверждены пользователем.  
**Deviations**: нет; отдельный депозитный счёт заменён согласованным бонусом за остаток до реализации.  
**Test Evidence**: отдельные тесты механики не написаны; при `qa.level: minimal` они не обязательны. Ранее `./gradlew :core:test` завершился успешно, но не подтверждает покрытие новых сценариев.  
**Code Review**: пропущен — режим solo.  
**Run result**: N/A — изменения доменной модели не имеют видимой игроку поверхности.
