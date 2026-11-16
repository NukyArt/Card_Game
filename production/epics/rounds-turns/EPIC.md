# Epic: Раунды и очереди

> **Layer**: Foundation
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: domain.round
> **Status**: Ready
> **Stories**: 2 stories — see `## Stories`

## Overview

Создать чистую Java-модель сессии и фаз с State Pattern: порядок участников, первый игрок, номер раунда, допустимость команд и результат матча. Правила рынка, ресурсов, эффектов, реальный таймер и UI не входят.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-02 | Фазы, очередь, первый игрок и доход | N/A — minimal |
| TR-SOLO-10 | Границы завершения проекта и исхода | N/A — minimal |
| TR-SOLO-11 | Депозит и граница последнего раунда | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты.
- Переходы фаз и смена очереди покрыты детерминированными unit-тестами.
- Недопустимые команды не меняют состояние.
- Фазы не используют часы, libGDX или UI-объекты.
- Публичные API имеют JavaDoc.

## Next Step

Run `/create-stories rounds-turns`.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Фазовая модель раунда и допустимость команд | Logic | Ready | N/A — minimal |
| 002 | Сессия партии, очередь участников и исход матча | Logic | Ready | N/A — minimal |
