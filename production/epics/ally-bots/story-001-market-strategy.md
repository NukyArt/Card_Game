# Story 001: Рыночная стратегия бота

> **Epic**: Боты-союзники
> **Status**: Complete
> **Layer**: Feature
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-14

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP feature 3  
**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)  
**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)  
**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; algorithm details: `design/gdd/card-game-gdd.md` §11.*

- [x] Бот рассматривает покупку доступной базы, производящей нужный текущему этапу ресурс, только при наличии свободного слота.
- [x] Если подходящей базы нет, бот рассматривает подходящее совместимое улучшение.
- [x] Среди подходящих вариантов выбирается наименьшая цена, при равной цене — крайняя левая позиция рынка.
- [x] Оценка учитывает базовую выплату, фиксированные дополнительные ресурсы и допустимый выбор Ростовщика.
- [x] Случайные ресурсы, платные обмены и условные бонусы при оценке покупки не учитываются.
- [x] Бот повторяет допустимые покупки и затем пасует; продажу и обновление рынка не использует.
- [x] При отсутствии допустимых действий бот заканчивает ход, который передаётся следующему участнику.
- [x] Действия выполняются через те же команды и доменные проверки, что и действия игрока.

## Implementation Notes

- Логика решений принадлежит application.bot; результаты действий доступны представлению через результаты и события.
- Использовать согласованный алгоритм из §11; не дублировать доменные правила.

## Out of Scope

- Story 002: производственные решения и вклад в проект.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above*

## Test Evidence

**Story Type**: Integration  
**Evidence**: интеграционные тесты в `core/src/test/java/` рекомендуются; при `qa.level: minimal` автоматические тесты advisory, не блокируют закрытие.  
**Status**: [x] Наблюдаемый запуск и evidence сохранены в `production/qa/evidence/market-bot-observation/observation.md`; отдельные тесты контроллеров не созданы (qa.level: minimal).

## Dependencies

- Depends on: подготовка партии, рынок, планшеты и проектные этапы.
- Unlocks: Story 002.

## Completion Notes

**Completed**: 2026-11-14  
**Verdict**: COMPLETE WITH NOTES  
**Criteria**: 8/8 passing — все восемь критериев подтверждены пользователем при /story-done; отложенных критериев нет.  
**Deviations**: Блокирующих отклонений не обнаружено. Минимальный экран наблюдения реализован отдельно в рамках `production/epics/game-ui-result/story-001-game-screen-controls-and-result.md`; UI-история остаётся In Progress. Производственные решения ботов и вклад в проект в эту историю не включены.  
**Test Evidence**: Автоматические тесты истории не обязательны при qa.level: minimal; отдельных тестов MarketBotController/SoloGameController нет, рекомендуются как follow-up. Предшествующий свежий прогон `./gradlew :core:test --rerun-tasks --no-daemon` успешен: 131 тест, 0 failures/errors/skipped. /story-done повторно тесты не запускал.  
**Code Review**: Skipped — solo mode; QL-TEST-COVERAGE skipped — qa.level minimal.  
**Evidence**: `production/qa/evidence/market-bot-observation/observation.md`, 15 retained PNGs: `00-initial.png`, `01-replay.png`–`13-replay.png`, `14-production-boundary.png`.  
**Run result:** OBSERVED — normal solo session, все три бота покупают и пасуют в последовательном просмотре неизменяемых снимков, затем выполнен переход в PRODUCTION; evidence в `production/qa/evidence/market-bot-observation/`.  
**Observation limitations**: Seed 0; просмотр результатов уже синхронно выполненных команд, не пауза между действиями. Диагностический экран использует ASCII-идентификаторы карт и каталожные цены без отображения effect-modified quotes. Один наблюдённый сценарий не доказывает все пограничные случаи выбора; AC-1–AC-8 дополнительно подтверждены пользователем. Полный UI, таймеры, пауза и экран результата этим срезом не реализованы.  
**Closure approval**: Пользователь явно разрешил завершение истории после отчёта COMPLETE WITH NOTES.
