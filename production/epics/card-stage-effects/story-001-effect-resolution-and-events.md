# Story 001: Разрешение эффектов и доменные события

> **Epic**: Эффекты карт и этапов
> **Status**: Complete
> **Layer**: Core
> **Type**: Logic
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-10

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 3 и 4 — эффекты карт и этапов.

**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; detailed agreed rules: `design/gdd/card-game-gdd.md` §11.*

- [x] Доменные эффекты задаются структурированными данными; текст описания не используется как исполняемое правило.
- [ ] Явно различаются немедленные, постоянные и отложенные эффекты и условия их действия.
- [x] Постоянные эффекты применяются в порядке их наложения: ранее наложенный раньше более позднего.
- [x] Отклонённая команда не оставляет частично изменённого игрового состояния.
- [ ] События выдаются после успешной операции в порядке возникновения; наблюдатели не владеют состоянием и не отменяют выполненную операцию.
- [ ] Выбор участника и источник случайности передаются явно; случайность воспроизводима.
- [ ] Домен не зависит от libGDX и внешних слоёв; публичные API документированы JavaDoc.

## Implementation Notes

- Сохранять игровые значения во внешних данных; использовать существующий доменный источник случайности.
- Поддержать интеграцию с существующими событиями завершения этапа, не считая доставку наблюдателям механизмом изменения состояния.

## Out of Scope

- Story 002: конкретные эффекты активации карт.
- Story 003: конкретные эффекты этапов и рыночные модификаторы.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

## Test Evidence

**Story Type**: Logic  
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` не блокируют закрытие.  
**Status**: [x] `./gradlew :core:test` — успешно, 2026-11-10. Проверены начисление золота, атомарный отказ, порядок постоянных эффектов, доставка событий и завершение этапа. Тесты: `core/src/test/java/com/myjavacardgame/domain/round/GameSessionEffectsTest.java`, `core/src/test/java/com/myjavacardgame/application/DomainEventDispatcherTest.java`.

## Completion Notes

**Completed**: 2026-11-10 — закрыто с оговорками по согласованию пользователя.

- Реализованы структурированные данные эффектов и начисление золота; проверено сохранение порядка постоянных эффектов и атомарный отказ при неизвестном получателе или переполнении.
- Сроки представлены явно, но поддерживается только условие `ALWAYS`; дополнительные условия и жизненный цикл отложенных эффектов реализуются вместе с конкретными эффектами.
- События выдаются после операции и передаются подписчикам через диспетчер при явном вызове; автоматическое подключение диспетчера к игровому циклу остаётся для интеграции. Событие завершения этапа проверено сквозным тестом.
- Выбор и `RandomSource` передаются явно; воспроизводимость источника покрыта существующими тестами. Случайная операция и её воспроизводимость остаются для Story 002.
- Транзитивная проверка изоляции домена и полнота JavaDoc публичных API отдельно не подтверждены.
- Дополнительно исправлен переход последнего депозита непосредственно в `RESULT`; ранее существовавший тест теперь проходит.
- Запуск игры: N/A — история не добавляет самостоятельную видимую поверхность.

## Dependencies

- Depends on: завершённые истории данных карт и проектов, планшета, проекта и рынка.
- Unlocks: Story 002 и Story 003.
