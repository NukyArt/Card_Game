# Epic: Эффекты карт и этапов

> **Layer**: Core
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: domain.effects; domain.events
> **Status**: Ready
> **Stories**: 3 stories

## Overview

Активации карт, выплаты и выборы, немедленные, постоянные и отложенные эффекты карт и этапов. Доменные события передаются через application наблюдателям в порядке возникновения; наблюдатели не владеют состоянием и не отменяют выполненную операцию.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

Рабочие идентификаторы из `docs/architecture/architecture.md`; формальный TR-реестр отсутствует.

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-08 | Активации, выборы, выплаты и улучшения | N/A — minimal |
| TR-SOLO-10 | Завершение этапов, цепочки, победа и поражение | N/A — minimal |
| TR-SOLO-12 | Немедленные, постоянные и отложенные эффекты | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты; соответствующие согласованные правила §11 проверены.
- Выплаты, выборы, сроки эффектов, цепочки и доставка событий проверены детерминированными тестами в `core/src/test/java/`, включая интеграцию с рынком, проектом и раундом.
- Порядок постоянных эффектов соответствует времени наложения; каталог карт проверен до утверждения полноты эффектов.
- Домен не зависит от libGDX и внешних слоёв; публичные API имеют JavaDoc.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Разрешение эффектов и доменные события | Logic | Complete | N/A |
| 002 | Эффекты активации карт и выборы | Integration | Ready | N/A |
| 003 | Эффекты этапов и рыночные модификаторы | Integration | Ready | N/A |

## Next Step

Run `/dev-story production/epics/card-stage-effects/story-002-card-activation-effects.md`.
