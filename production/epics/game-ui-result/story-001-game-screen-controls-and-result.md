# Story 001: Экран партии, управление временем и результат

> **Epic**: Интерфейс партии и результат
> **Status**: Complete
> **Layer**: Presentation
> **Type**: UI
> **Estimate**: Fill before sprint planning
> **Manifest Version**: N/A (minimal — no control manifest)
> **Last Updated**: 2026-11-14

## Context

**GDD**: `design/game-brief.md`
**Requirement**: Brief MVP feature 5 — Экран партии, видимые действия ботов, пауза и экран результата

**ADR Governing Implementation**: N/A (minimal — no ADRs)
**ADR Decision Summary**: N/A (minimal — no ADRs)
**ADR Version**: N/A (minimal — no ADRs)

**Engine**: libGDX 1.14.2 | **Risk**: NOT ASSESSED (no VERSION.md risk rating)
**Engine Notes**: none (no ADR engine-compatibility analysis at minimal)

**Control Manifest Rules (this layer)**: N/A (minimal — no control manifest)

---

## Acceptance Criteria

*From `design/game-brief.md` (the **Player goal & fail state** field + the MVP feature this story implements), scoped to this story:*

- [x] Игрок может запустить соло-партию с одним игроком и тремя ботами на экране партии.
- [x] Экран показывает состояние партии, необходимое для управления золотом, ресурсами, рынком, планшетом, проектом и текущим раундом.
- [x] Игрок может вводить доступные действия партии; представление передаёт команды application-слою и не реализует игровые правила.
- [x] Действия трёх ботов отображаются игроку по мере их выполнения.
- [x] Рыночные и производственные очереди отображают таймеры и истечение времени.
- [x] Игрок может поставить партию на паузу и продолжить её без нарушения состояния сессии.
- [x] После завершения пяти этапов до 30-го раунда показывается экран победы; если проект не завершён после 30-го раунда — экран поражения.
- [x] Для экранов партии и результата сохранены наблюдённые скриншоты в `production/qa/evidence/`.

---

## Implementation Notes

- Presentation отображает состояние и направляет пользовательские действия в application; правила партии остаются в domain/application.
- Подключить bootstrap сессии, ввод, драйвер фаз и рендеринг до наблюдаемого запуска desktop-приложения.
- Проверить конкретные API libGDX 1.14.2 по официальной документации до реализации.

---

## Out of Scope

- Игровые правила, расчёт ресурсов, рынок, проект и эффекты карт.
- Алгоритм принятия решений ботов.
- Сетевые режимы, иные размеры команд, другие сложности, косметический магазин и мета-прогрессия.

---

## QA Test Cases

*N/A — no qa-lead specs at this tier; implement against the Acceptance Criteria above.*

---

## Test Evidence

*Governed by `qa.level`: at `qa.level: minimal` tests are **waived** (advisory, never "must exist and pass"), but a Visual/Feel or UI story's retained screenshot is not.*

**Story Type**: UI
**Required evidence**:
- UI: retained screenshots of each screen touched in `production/qa/evidence/`.

**Status**: [x] Retained screenshots present; reviewed during /story-done.

---

## Dependencies

- Depends on: базовый цикл партии и действия ботов должны быть доступны application-слою.
- Unlocks: None

## Completion Notes

**Completed**: 2026-11-14
**Verdict**: COMPLETE — закрытие явно разрешено пользователем после отчёта /story-done.
**Criteria**: 8/8 passing; отложенных критериев нет. Пользователь подтвердил запуск соло-партии, действия игрока, отображение действий трёх ботов, таймеры и истечение времени, паузу/продолжение и оба исхода партии.
**Deviations**: Блокирующих отклонений не выявлено. GDD/ADR-проверки пропущены при workflow: minimal; control manifest не заявлен. Полная сверка изменённых файлов со scope не выполнена: история не содержит перечня files to create/modify, Git не использовался.
**Test Evidence**: UI — сохранённые PNG в `production/qa/evidence/game-screen-controls-and-result/`. Во время /story-done открыты `01-IN_PROGRESS-1794662695031.png` (экран партии), `03-IN_PROGRESS-1794661762200.png` (пауза), `05-IN_PROGRESS-1794661806440.png` (вопрос активации), `02-VICTORY-1794662698060.png` (победа, пять завершённых этапов). Скриншот поражения `17-DEFEAT-1794662164258.png` присутствует; его наблюдение записано в предыдущем /dev-story checkpoint. Оба исхода дополнительно подтверждены пользователем.
**Run result**: OBSERVED — экран партии и экран победы просмотрены при /story-done; пути выше. Победный сценарий не является доказательством баланса или достижимости победы в обычной партии.
**Tests**: При qa.level: minimal автоматические тесты не требуются; /story-done тесты не запускал. Предыдущий checkpoint сообщает о 131 тесте без failures/errors; это не новый прогон проверки закрытия.
**Code Review**: Skipped — Solo mode. QL-TEST-COVERAGE skipped — qa.level minimal.

### Test-Criterion Traceability

| Criterion | Test / evidence | Status |
|-----------|-----------------|--------|
| AC-1: Запуск соло-партии с тремя ботами | Ручное подтверждение пользователя | COVERED |
| AC-2: Состояние партии на экране | `production/qa/evidence/game-screen-controls-and-result/01-IN_PROGRESS-1794662695031.png` | COVERED |
| AC-3: Доступные действия игрока | Ручное подтверждение пользователя; архитектурная часть критерия принята в отчёте закрытия | COVERED |
| AC-4: Отображение действий трёх ботов | Ручное подтверждение пользователя | COVERED |
| AC-5: Таймеры и истечение времени | Ручное подтверждение пользователя | COVERED |
| AC-6: Пауза и продолжение | Ручное подтверждение пользователя; `production/qa/evidence/game-screen-controls-and-result/03-IN_PROGRESS-1794661762200.png` | COVERED |
| AC-7: Победа и поражение | Ручное подтверждение обоих исходов; `production/qa/evidence/game-screen-controls-and-result/02-VICTORY-1794662698060.png` | COVERED |
| AC-8: Сохранённые скриншоты экранов | PNG в `production/qa/evidence/game-screen-controls-and-result/` | COVERED |

