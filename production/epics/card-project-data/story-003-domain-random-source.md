# Story 003: Воспроизводимый источник случайности для домена

> **Epic**: Данные карт и проектов
> **Status**: Complete
> **Layer**: Foundation
> **Type**: Logic
> **Estimate**: S
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-07

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature: рынок и колода из 50 экземпляров; случайные перемешивания и эффекты должны быть воспроизводимы в тестах.

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the Player goal & fail state field + the MVP feature this story implements), scoped to this story:*

- [ ] Домен получает случайность через небольшой Java-контракт, не обращаясь к глобальному генератору или системному времени.
- [ ] Контракт позволяет получить ограниченное случайное целое число для будущих перемешиваний и эффектов Цитадели.
- [ ] Недопустимая верхняя граница случайного выбора отклоняется понятной ошибкой.
- [ ] Тестовая реализация возвращает предсказуемую последовательность значений.
- [ ] Доменная модель не содержит API libGDX и не требует файлового ввода-вывода для генерации случайности.
- [ ] Публичный контракт документирован JavaDoc.
- [ ] Unit-тесты подтверждают границы и воспроизводимость.

---

## Implementation Notes

- Инъецировать источник случайности в точки, где он потребуется; не хранить глобальный генератор в домене.
- Реальные генераторы и загрузка конфигурации остаются за границей домена.

---

## Out of Scope

- Story 001: определения карт и ресурсов.
- Story 002: определения проекта и стартовой партии.
- Алгоритм перемешивания колоды, рынок, конкретное разрешение случайного эффекта и загрузка данных.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

**Story Type**: Logic
**Required evidence**: advisory unit-тесты в `core/src/test/java/`; при `qa.level: minimal` они не блокируют закрытие истории.

**Status**: [x] Created — `core/src/test/java/com/myjavacardgame/domain/random/SequenceRandomSourceTest.java`

---

## Dependencies

- Depends on: Story 001 must be DONE
- Unlocks: истории о рынке и случайных эффектах

## Completion Notes

**Completed**: 2026-11-07
**Criteria**: 7/7 verified
**Deviations**: None
**Test Evidence**: `core/src/test/java/com/myjavacardgame/domain/random/SequenceRandomSourceTest.java` — 6 tests; `./gradlew :core:test` succeeded during implementation.
**Code Review**: Skipped — solo mode.
**Run result**: N/A — изменения доменной модели не имеют видимой игроку поверхности.
