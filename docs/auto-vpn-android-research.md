# Auto VPN: исследование Android

## Вердикт

**IMPLEMENT WITH LIMITATIONS.** Режим можно реализовать без root и без Accessibility, используя `UsageStatsManager.queryEvents()` и события `ACTIVITY_RESUMED`. Android выдаёт события только приложению с разрешением `PACKAGE_USAGE_STATS` (пользователь включает его на системном экране «Доступ к статистике использования»). Это разрешение не является runtime permission и может быть отключено пользователем.

`ACTIVITY_RESUMED` означает переход Activity в foreground, но не даёт универсального callback «пользователь коснулся этого окна». Начиная с Android 10 несколько окон split-screen могут быть одновременно `RESUMED`; Android рекомендует `onTopResumedActivityChanged()` только Activity, которой владеет само приложение. Поэтому для чужих приложений используется последний стабилизированный `ACTIVITY_RESUMED` и явно документируется fallback.

## Сигналы и выбор

| Сигнал | Root | Accessibility | Задержка/энергия | Решение |
|---|---:|---:|---|---|
| `UsageEvents.ACTIVITY_RESUMED` | нет | нет | 0.5–2 с при интервале 500 мс; умеренный | основной сигнал |
| `ACTIVITY_PAUSED/STOPPED` | нет | нет | event history, без callback | фильтрация переходов |
| `UsageStats` | нет | нет | агрегированный, не фокус | не использовать для realtime |
| `ActivityManager.getRunningTasks` | нет | нет | ограничен Android 5+, чужой top task недоступен | не использовать |
| Accessibility events | нет | да, специальный доступ | быстро, но сервис постоянно активен | резервный opt-in не включён |
| `onTopResumedActivityChanged` | нет | нет | доступен только собственной Activity | неприменим к чужим окнам |
| polling 50–100 мс | нет | нет | дорогой CPU/wakeup | запрещён |

Монитор опрашивает историю `UsageEvents` раз в 500 мс только когда экран включён и не заблокирован. Состояние экрана приходит из `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` / `ACTION_USER_PRESENT` плюс `PowerManager.isInteractive` и `KeyguardManager.isKeyguardLocked`; корутина монитора приостанавливается на `StateFlow` состояния экрана, поэтому при выключенном экране опроса и wakeup'ов нет вообще. `PrimaryForegroundAppResolver` удерживает кандидата 250 мс и отбрасывает собственный пакет, System UI, клавиатуру и permission controller. Launcher, notification shade, recent apps и системные overlay не становятся новой пользовательской политикой.

При выдаче доступа монитор запускается как foreground service, поскольку процесс должен пережить отсутствие UI. На Android 8+ запуск foreground service из видимого Settings Activity разрешён; запуск из background может быть запрещён системой. Производители могут остановить сервис: Xiaomi документирует autostart control, Huawei — ручное разрешение «Run in background». Пользователь должен снять battery restrictions для надёжной работы.

## Крайние случаи

- **Split-screen:** на Android 10+ оба приложения resumed. Проверено на эмуляторе API 37: касание другой панели меняет фокус окна, но `UsageEvents` молчит. Применяется правило видимых панелей: «Всегда отключать VPN» среди видимых → стоп, иначе «Через VPN» среди видимых → туннель удерживается.
- **PiP:** PiP activity не должна удерживать VPN, когда fullscreen app получил новый resumed event. При одном PiP и launcher состояние удерживается до idle timeout.
- **Home/Recent Apps:** HOME-Activity (по компоненту, `priority ≥ 0` — системные Settings содержат служебную `FallbackHome` с `priority=-1000`) трактуется как `NO_ACTIVE_APP`; обычный «Без VPN» останавливает VPN через 10 секунд, «Всегда отключать VPN» — только при реальном событии этого пакета.
- **Lock screen:** VPN не включается из-за фонового воспроизведения. Опция «сохранять VPN при выключенном экране» оставлена для последующей версии: без видимого Activity нельзя надёжно отличить playback от фонового sync.
- **Doze/process death/reboot:** UsageEvents недоступен в locked direct-boot состоянии и требует разблокированного пользователя. После reboot Auto VPN не поднимает VPN автоматически; настройки остаются в MMKV. Foreground service может быть перезапущен ОС после process death, но должен заново проверить разрешение и настройку.

## VPN и утечки

`VpnService.Builder.establish()` создаёт TUN и публикует `TRANSPORT_VPN`. Полная остановка требует остановить Xray, tun2socks, закрыть `ParcelFileDescriptor`, затем `stopSelf()`. После закрытия descriptor Android удаляет VPN network и indicator асинхронно; уже установленные TCP/QUIC соединения могут коротко пережить переход, DNS cache остаётся у приложения. Без Android Lockdown приложение, открытое в момент `STARTING`, может успеть отправить прямой пакет до создания TUN; абсолютной гарантии kill-switch для обычного приложения нет.

## Разрешение и Play

`PACKAGE_USAGE_STATS` выдаётся через Settings, а не диалог приложения. Accessibility намеренно не используется: Google Play требует декларацию и проверку core functionality для AccessibilityService. VPN foreground service уже имеет отдельный Android service type; новый монитор использует `specialUse` и должен быть объяснён в Play Console.

## Battery plan

Измерение должно сравнить: постоянно включённый VPN, Auto VPN с монитором 500 мс и агрессивные 100 мс. Команды: `adb shell dumpsys batterystats`, `dumpsys power`, `dumpsys deviceidle`; дополнительно Perfetto CPU wakeups и Android Studio Profiler. **Выполнено на эмуляторе (API 37):** CPU-время процесса `:daemon` — 140 мс за 120 с при включённом экране и 0 мс за 120 с при выключенном. **Not run:** замеры батареи, wakeup'ов и wakelock на физическом устройстве, сравнение сценариев A/B/C/D, поведение OEM-прошивок.

## Ответы на вопросы ТЗ

Сводная таблица ответов на все 24 вопроса раздела 50 вынесена в [`auto-vpn-qa.md`](auto-vpn-qa.md).
Состояние реализации, перечень выполненных проверок и список неподтверждённых пунктов — в
[`auto-vpn-status.md`](auto-vpn-status.md).

## Источники

1. [UsageStatsManager API](https://developer.android.com/reference/android/app/usage/UsageStatsManager) и [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event).
2. [Multi-window и multi-resume](https://developer.android.com/develop/ui/views/layout/support-multi-window-mode).
3. [VpnService и остановка туннеля](https://developer.android.com/develop/connectivity/vpn) и [`VpnService.Builder.establish`](https://developer.android.com/reference/android/net/VpnService.Builder#establish()).
4. [Foreground service restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).
5. [Google Play policy для AccessibilityService](https://support.google.com/googleplay/android-developer/answer/10964491).
6. [Xiaomi autostart](https://www.mi.com/global/support/faq/details/KA-497677/) и [Huawei background launch](https://consumer.huawei.com/ca/support/content/en-us15850065/).
