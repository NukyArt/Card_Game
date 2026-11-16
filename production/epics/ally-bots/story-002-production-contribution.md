# Story 002: Производство и вклад бота в проект

> **Epic**: Боты-союзники
> **Status**: Complete
> **Layer**: Feature
> **Type**: Integration
> **Estimate**: L
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-14

## Context

**GDD**: `design/game-brief.md`  
**Requirement**: Brief MVP features 3–4  
**ADR Governing Implementation**: N/A (minimal — no ADRs)  
**ADR Decision Summary**: N/A (minimal — no ADRs)  
**ADR Version**: N/A (minimal — no ADRs)  
**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)  
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)  
**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story; algorithm details: `design/gdd/card-game-gdd.md` §11.*

- [ ] В производстве бот активирует базы слева направо.
- [ ] Затем бот передаёт нужному этапу подходящие базы без улучшений слева направо.
- [ ] Произведённое золото бот сначала направляет на этап, затем вносит максимально доступное золото в пределах потребности.
- [ ] При выборе ресурса бот приоритизирует нужные и допустимые: стройматериалы, провиант, свитки, золото.
- [ ] При отсутствии подходящего необязательный выбор пропускается; при обязательном выбирается первый допустимый вариант.
- [ ] Шахта выбирает стройматериалы, если они нужны этапу, иначе золото.
- [ ] Платный обмен используется при достаточных средствах и потребности; передачу Инспектора бот не использует.
- [ ] При отсутствии допустимых действий бот заканчивает ход, который передаётся следующему участнику.
- [ ] Действия выполняются через те же команды и доменные проверки, что и действия игрока.

## Implementation Notes

- Использовать согласованный алгоритм из `design/gdd/card-game-gdd.md` §11; не дублировать доменные правила.
- Проверить в игровой партии риск застревания на требовании карт.

## Out of Scope

- Story 001: рыночная стратегия бота.

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above*

## Test Evidence

**Story Type**: Integration  
**Evidence**: интеграционные тесты в `core/src/test/java/` и игровая проверка риска застревания рекомендуются; при `qa.level: minimal` автоматические тесты advisory, не блокируют закрытие.  
**Status**: [ ] Not yet created

## Dependencies

- Depends on: Story 001; производство и проектные этапы.
- Unlocks: None.

## Completion Notes

**Completed**: 2026-11-14  
**Criteria**: 9/9 passing — все девять критериев подтверждены пользователем при `/story-done`; отложенных и непроверенных критериев нет.  
**Verdict**: COMPLETE WITH NOTES  
**Deviations**: Отклонений от согласованных правил не выявлено. В истории отсутствует список файлов реализации, поэтому точное сравнение изменений с файловым scope недоступно; диагностический экран и evidence были согласованы отдельно.  
**Test Evidence**: `production/qa/evidence/production-bot-observation/observation.md` и 40 сохранённых PNG. Новые автоматические тесты не требовались при `qa.level: minimal`; по сохранённому отчёту предыдущей сессии свежий Gradle-прогон `:core:test :desktop:classes --rerun-tasks` завершился успешно: 131 тест, 0 failures/errors/skips. Во время `/story-done` тесты повторно не запускались.  
**Code Review**: Skipped — Solo mode; QL-TEST-COVERAGE skipped — qa.level minimal.

Run result: OBSERVED — просмотрен сохранённый кадр `production/qa/evidence/production-bot-observation/39-production-complete.png`: принятый переход в DEPOSIT после производства трёх ботов. Диагностический экран воспроизводит неизменяемые результаты команд, а не живое выполнение с паузой.

### Test-Criterion Traceability

| Criterion | Test | Status |
|-----------|------|--------|
| AC-1: активация баз слева направо | Подтверждение пользователя при /story-done | COVERED |
| AC-2: передача баз без улучшений слева направо | Подтверждение пользователя при /story-done | COVERED |
| AC-3: произведённое и личное золото в пределах потребности | Подтверждение пользователя при /story-done | COVERED |
| AC-4: приоритет нужных и допустимых ресурсов | Подтверждение пользователя при /story-done | COVERED |
| AC-5: пропуск необязательного / первый допустимый обязательный выбор | Подтверждение пользователя при /story-done | COVERED |
| AC-6: выбор Шахты | Подтверждение пользователя при /story-done | COVERED |
| AC-7: платный обмен и отказ от передачи Инспектора | Подтверждение пользователя при /story-done | COVERED |
| AC-8: завершение и передача хода | Подтверждение пользователя при /story-done | COVERED |
| AC-9: общие команды и доменные проверки | Подтверждение пользователя при /story-done; ProductionBotController использует GameSession.execute | COVERED |

### Remaining Risks

- В записанном невизуальном 30-раундовом прогоне seed 0 требования карт выполнены в раундах 5 и 28, отклонений команд и зацикливания очереди не было. Партия завершилась DEFEAT на этапе mage-tower в раунде 30. Это не доказательство достижимости победы, баланса или отсутствия блокировки на картах в других раскладах.
- Старый двухаргументный SoloGameController остаётся market-only; конструктор с RandomSource и SoloSessionBuilder.buildController автоматизируют обе фазы.
