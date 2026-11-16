# Epic: Данные карт и проектов

> **Layer**: Foundation
> **GDD**: design/game-brief.md; design/gdd/card-game-gdd.md (§11)
> **Architecture Module**: domain.cards, domain.project
> **Status**: Ready
> **Stories**: 3 stories — see `## Stories`

## Overview

Определить чистую Java-модель данных карт, ресурсов, этапов и проекта, передаваемую в домен извне; включить проверку неизменяемых определений и воспроизводимую случайность. Загрузка файлов, рынок, планшет и UI не входят.

## Governing ADRs

Нет. При `workflow: minimal` ADR не обязателен.

## GDD Requirements

| TR-ID | Requirement | ADR Coverage |
|---|---|---|
| TR-SOLO-01 | Данные стартового состава партии | N/A — minimal |
| TR-SOLO-05 | Определения баз, улучшений, ролей и пределов | N/A — minimal |
| TR-SOLO-15 | Внешние данные проекта, правил и каталога | N/A — minimal |
| TR-SOLO-17 | Воспроизводимая случайность | N/A — minimal |

## Definition of Done

- Все дочерние истории реализованы и закрыты.
- Доменные типы не импортируют `com.badlogic.gdx.*`.
- Карты и требования этапов передаются извне и неизменяемы после создания.
- Есть детерминированные unit-тесты в `core/src/test/java/`.

## Next Step

Run `/create-stories card-project-data`.

## Stories

| # | Story | Type | Status | ADR |
|---|-------|------|--------|-----|
| 001 | Чистые доменные определения карт и ресурсов | Logic | Ready | N/A — minimal |
| 002 | Неизменяемые определения проекта и стартовой партии | Logic | Ready | N/A — minimal |
| 003 | Воспроизводимый источник случайности для домена | Logic | Ready | N/A — minimal |
