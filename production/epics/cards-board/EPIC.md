# Epic: Карты и планшет

> **Layer**: Core
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: domain.cards/board
> **Status**: Ready
> **Stories**: 3 stories — see `## Stories`

## Overview

Модель экземпляров карт и планшета: позиции баз, совместимые улучшения, роли и ограничения размещения. Удаление и передача пакета сохраняют состав карт; расчёты сделок, прогресс проекта и разрешение эффектов принадлежат соответствующим областям.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

Рабочие идентификаторы из `docs/architecture/architecture.md`; формальный TR-реестр отсутствует.

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-05 | Базы, улучшения, типы, роли и пределы планшета | N/A — minimal |
| TR-SOLO-07 | Передача пакета карт этапу и избыток | N/A — minimal |
| TR-SOLO-08 | Активации, выборы, выплаты и улучшения | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты; соответствующие согласованные правила §11 проверены.
- Роли, совместимость, пределы и состав пакетов проверены детерминированными unit-тестами в `core/src/test/java/`; взаимодействия с проектом и рынком проверены интеграционно.
- Недопустимые операции не изменяют состояние; домен не зависит от libGDX и внешних слоёв.
- Публичные API имеют JavaDoc.

## Next Step

Run `/dev-story production/epics/cards-board/story-001-card-instances-and-roles.md`.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Экземпляры карт и закрепление роли | Logic | Ready | N/A — minimal |
| 002 | Планшет, базы и улучшения | Logic | Ready | N/A — minimal |
| 003 | Пакеты карт и учёт активаций | Logic | Ready | N/A — minimal |
