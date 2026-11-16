# Story 003: Эффекты этапов и рыночные модификаторы

> **Epic**: Эффекты карт и этапов
> **Status**: Complete
> **Layer**: Core
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-11

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 3 и 4 — эффекты карт, пятиэтапный проект и его эффекты.

**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; detailed agreed rules: `design/gdd/card-game-gdd.md` §11.*

- [ ] Завершение этапа открывает следующий этап, затем разрешает эффект завершённого; эффект первого этапа немедленно даёт каждому участнику 2 золота.
- [ ] До конца проекта второй этап снижает на 1 итоговые стройматериалы каждой активации производящей их базы после улучшений, минимум до 0; четвёртый добавляет 1 свиток каждой активации производящей свитки базы.
- [ ] Третий этап немедленно вносит по 3 провианта на участника в следующий этап; избыток отдельной выплаты сгорает и возможны цепочки завершений. Пятый этап немедленно завершает партию победой.
- [ ] Постоянные эффекты применяются в порядке наложения: первый наложенный срабатывает первым, второй — вторым и так далее.
- [ ] Мытарь даёт владельцу +1 к каждой продаже в следующем раунде за каждую активацию; бонусы складываются, каждый применяется один раз к сделке пакетом и сохраняется после удаления карты.
- [ ] Обсерватория даёт одно бесплатное обновление в следующей рыночной фазе без увеличения лимита; право сохраняется после удаления карты и исчезает после фазы.
- [ ] Конюшни дают в следующей рыночной фазе скидку 1 на каждую покупку карты своего типа; скидки складываются до минимальной цены 1 и сохраняются после удаления карты.
- [ ] Каждая Каменоломня-база даёт 2 стройматериала при последующей покупке владельцем карты её типа, включая улучшение; собственная покупка её не запускает, несколько Каменоломен складываются и штраф второго этапа к этой выплате не применяется.
- [ ] Эффекты и сроки проверяются при переходе фаз и раундов, а последовательность событий сохраняется при цепочке завершений.

## Implementation Notes

- Использовать данные проекта и каталога; не привязывать доменные правила к отображаемым названиям.
- Проверить каталог `design/gdd/game.docx` до утверждения полноты эффектов.

## Out of Scope

- Story 001: общий механизм разрешения эффектов и доставка событий.
- Story 002: выплаты активаций, выборы, обмены и передача баз.
- Пассивный доход: источник и объём остаются открытыми.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Integration  
**Required evidence**: advisory интеграционные тесты в `core/src/test/java/` либо документированный плейтест; при `qa.level: minimal` не блокируют закрытие.  
**Status**: [ ] Not yet created

## Dependencies

- Depends on: Story 001; существующие истории проекта, рынка и раундов. Для сквозной проверки активаций также Story 002.
- Unlocks: истории ботов и проверка полного цикла партии.

## Completion Notes

**Completed**: 2026-11-11  
**Criteria**: 9/9 passing; все критерии подтверждены ручной проверкой и сопоставлены с интеграционными тестами.  
**Deviations**: None.  
**Test Evidence**: Integration tests at `core/src/test/java/com/myjavacardgame/domain/round/GameSessionStageMarketEffectsTest.java` (19 test methods; evidence not required at `qa.level: minimal`).  
**Code Review**: Skipped — solo review mode.
