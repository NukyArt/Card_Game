# Epic: Проект и этапы

> **Layer**: Core
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: domain.project
> **Status**: Ready
> **Stories**: 1 story

## Overview

Требования и прогресс этапов, приём ресурсов и пакетов карт, обработка избытка и немедленное завершение этапов и проекта. Эффекты завершения исполняются областью эффектов; результат матча координируется с моделью раунда.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

Рабочие идентификаторы из `docs/architecture/architecture.md`; формальный TR-реестр отсутствует.

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-07 | Передача пакета карт этапу и избыток | N/A — minimal |
| TR-SOLO-09 | Личный баланс и ресурсы проекта | N/A — minimal |
| TR-SOLO-10 | Завершение этапов, цепочки, победа и поражение | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты; соответствующие согласованные правила §11 проверены.
- Требования, вклад, избыток и завершение проверены детерминированными тестами в `core/src/test/java/`; цепочки эффектов и исход партии проверены интеграционно.
- Баланс и достижимость проекта проверены игровой партией; неподтверждённые значения явно отмечены.
- Домен не зависит от libGDX и внешних слоёв; публичные API имеют JavaDoc.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Вклады в этапы и завершение проекта | Integration | Ready | N/A (minimal) |

## Next Step

Run `/create-stories project-stages`.
