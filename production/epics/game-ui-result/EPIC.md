# Epic: Интерфейс партии и результат

> **Layer**: Presentation
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: presentation; application
> **Status**: Ready

## Overview

Экран партии, ввод игрока, видимые действия ботов и экран результата; таймеры рыночных и производственных очередей и ручная пауза. Управление временем и командами находится в application, представление не владеет игровыми правилами.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

Рабочие идентификаторы из `docs/architecture/architecture.md`; формальный TR-реестр отсутствует.

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-14 | Таймеры, истечение и пауза | N/A — minimal |
| TR-SOLO-16 | Отображение партии, действий ботов и результата | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты; соответствующие согласованные правила §11 проверены.
- Таймер, пауза, истечение и обработка команд проверены детерминированными тестами в `core/src/test/java/`; взаимодействие application и представления проверено интеграционно.
- Каждая история с изменением, видимым игроку, запущена и наблюдалась; снимки каждого затронутого экрана сохранены в `production/qa/evidence/`.
- Конкретные API и интеграция потоков сверены с официальной документацией libGDX 1.14.2 до реализации; публичные API имеют JavaDoc.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Экран партии, управление временем и результат | UI | Ready | N/A — minimal |

## Next Step

Run `/dev-story production/epics/game-ui-result/story-001-game-screen-controls-and-result.md`.
