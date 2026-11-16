# Epic: Боты-союзники

> **Layer**: Feature
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: application.bot
> **Status**: Ready
## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | [Рыночная стратегия бота](story-001-market-strategy.md) | Integration | Ready | N/A — minimal |
| 002 | [Производство и вклад бота в проект](story-002-production-contribution.md) | Integration | Ready | N/A — minimal |

## Overview

Решения трёх ботов-союзников в рыночной и производственной очередях по алгоритму §11. Боты используют те же команды и доменные проверки, что и человек; действия доступны представлению через результаты и события.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

Рабочие идентификаторы из `docs/architecture/architecture.md`; формальный TR-реестр отсутствует.

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-13 | Действия и стратегия ботов | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты; согласованный алгоритм ботов из §11 проверен.
- Приоритеты, разрешение равенства, выборы и передача команд покрыты детерминированными тестами в `core/src/test/java/`; взаимодействие с игровыми областями проверено интеграционно.
- Поведение при отсутствии допустимых действий уточнено до реализации зависящей от него истории; риск застревания на требованиях карт проверен в игровой партии.
- Публичные API имеют JavaDoc; игровые правила не дублируются в представлении.

## Next Step

Run `/create-stories ally-bots`.
