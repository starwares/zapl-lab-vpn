# Auto VPN: исследование v2rayNG и точки интеграции

Исследован upstream `2dust/v2rayNG`, commit `173f60a1dabe57e822a56381931be6354f7c7f61` (11 сентября 2026), Xray AAR по gitlink `AndroidLibXrayLite`.

## Жизненный цикл туннеля в upstream

| Шаг | Где | Что происходит |
|---|---|---|
| Запуск из UI | `core/LauncherManager.kt` → `startService()` | Проверка профиля, выбор `CoreVpnService` / `CoreProxyOnlyService` / `CoreRootService`, `ContextCompat.startForegroundService()` |
| Вход в foreground | `service/CoreVpnService.kt` → `onStartCommand()` | `NotificationManager.ensureForeground()`, затем `tryLockStart()` и `setupVpnService()` |
| Создание TUN | `CoreVpnService.configureVpnService()` | `Builder()` → адреса, маршруты, DNS, per-app правила → `builder.establish()`, `isRunning = true` |
| tun2socks | `CoreVpnService.runTun2socks()` | `TProxyService` (hev), если включён соответствующий режим |
| Старт Xray | `core/CoreServiceManager.kt` → `startCoreLoop(mInterface)` → `doStartCoreLoop()` → `launchCore()` | Регистрация динамического receiver'а, `CoreController.startLoop(config, tunFd)`, notification, `NetworkMonitor` |
| Остановка | `CoreVpnService.stopService()` → `stopAllService(true)` | `unlockStart()`, `isRunning = false`, `stopTun2Socks()`, `CoreServiceManager.stopCoreLoop()`, `stopSelf()`, пауза 100 мс, `mInterface.close()` |
| Команды сервису | `helper/MessageHelper.kt` | Broadcast с `package = ANG_PACKAGE` и extra `key`; `MSG_STATE_STOP` обрабатывает `ReceiveMessageHandler` внутри `CoreServiceManager` |

Процессы: UI — основной, `CoreVpnService` / `CoreProxyOnlyService` / `CoreRootService` — `:daemon`, тестовые сервисы — `:tasks`, WorkManager — `:bg`. MMKV открыт в `MULTI_PROCESS_MODE`, поэтому настройки видны во всех процессах.

## Что переиспользовано, а не написано заново

- `LauncherManager.startService()` — единственная точка старта; Auto VPN вызывает её с новым параметром `notifyUser = false`, чтобы автоматический старт не показывал toast и не трогал буфер обмена на каждом переключении приложения.
- `MessageHelper.sendMsg2Service()` — доставка команды остановки.
- `CoreServiceManager.isRunning()` — источник истины о состоянии native-ядра. Монитор живёт в `:daemon`, поэтому имеет право его читать (правило service-гайда о процессе-владельце).
- `AppManagerUtil.loadNetworkAppList()`, `AppIconFetcher`, `AppTopBar`, `SelectListDialog`, `SettingsSwitchItem/MenuItem/ListItem` — список приложений, иконки, поиск и элементы настроек.
- `NotificationChannelType` + `NotificationHelper.startForeground()` — уведомление монитора, вместо собственного канала.

## Новые файлы

```
autoVpn/AppVpnPolicy.kt              политика пакета и её строковый ресурс
autoVpn/AutoVpnPolicyCodec.kt        формат хранения таблицы политик (покрыт regression-тестом)
autoVpn/AutoVpnPolicyRepository.kt   in-memory snapshot поверх MMKV, refresh() между процессами
autoVpn/ForegroundAppMonitor.kt      UsageEvents + SCREEN_ON/OFF/USER_PRESENT, сбор сигналов
autoVpn/AndroidForegroundFilters.kt  определение HOME-Activity и IME-пакетов
autoVpn/ForegroundHistory.kt         набор resumed-Activity: split-screen и восстановление при старте
autoVpn/PrimaryForegroundAppResolver.kt  фильтрация и стабилизация кандидата
autoVpn/AutoVpnPolicyEngine.kt       чистый reducer решений
autoVpn/VpnLifecycleController.kt    сериализация команд, generation, состояние
autoVpn/AutoVpnCoordinator.kt        связывание, idle-таймер, состояние экрана
autoVpn/AutoVpnEvent.kt              структурированные события
autoVpn/AutoVpnMetrics.kt            debug-счётчики и задержки
autoVpn/AutoVpnStatus.kt             статус для UI через MMKV
service/AutoVpnMonitorService.kt     foreground service монитора в :daemon
receiver/AutoVpnCommandReceiver.kt   обработка MSG_AUTO_VPN_STOP / MSG_AUTO_VPN_RELOAD
ui/autovpn/*                         экран настроек и экран списка приложений
```

## Изменения в upstream-файлах (минимальный diff)

| Файл | Изменение | Зачем |
|---|---|---|
| `AppConfig.kt` | `PREF_AUTO_VPN_*`, `MSG_AUTO_VPN_STOP = 9`, `MSG_AUTO_VPN_RELOAD = 91` | Новые ключи настроек и команды. Значения выбраны вне занятого диапазона `6x`: первая реализация использовала `61`, что совпадало с `MSG_MEASURE_DELAY_RESULT` |
| `core/LauncherManager.kt` | параметр `notifyUser` | Автоматический старт — не жест пользователя |
| `core/CoreServiceManager.kt` | флаг `isMessageReceiverRegistered` | Teardown теперь достижим и из `onDestroy`, receiver должен сниматься ровно один раз |
| `service/CoreVpnService.kt` | `stopCoreLoop()` в начале `onDestroy()` | `stopService()` в обход `stopAllService()` не должен оставлять живой Xray |
| `receiver/BootReceiver.kt` | `AutoVpnMonitorService.syncWithSettings()` | Восстановление монитора после reboot и обновления APK, без автоподъёма туннеля |
| `ui/main/MainActivity.kt` | `syncWithSettings()` в `onResume()` | Монитор мог быть убит системой или OEM-менеджером батареи |
| `ui/settings/SettingsActivity.kt` | пункт «Автоматическое управление VPN» | Вход в новый экран |
| `enums/NotificationChannelType.kt` | `AUTO_VPN_MONITOR` | Канал уведомления монитора |
| `AndroidManifest.xml` | `PACKAGE_USAGE_STATS`, сервис, receiver, две Activity | Без объявления разрешения приложение не появляется в списке «Доступ к статистике использования» |
| `res/values*/strings.xml` | 35 ключей `auto_vpn_*` | Локализация нового функционала |

## Ограничение upstream, которое осталось

`CoreServiceManager.stopCoreLoop()` запускает native stop в отдельной IO-корутине и сразу отправляет в UI `MSG_STATE_STOP_SUCCESS`. Поэтому UI-сообщение не является доказательством того, что TUN исчез. Auto VPN намеренно не использует это сообщение: `VpnLifecycleController` опрашивает `CoreServiceManager.isRunning()` до совпадения с ожидаемым состоянием (с ограничением сверху в 15 секунд). Полное решение — заменить UI-сообщение на completion callback и перенести корутину в scope, принадлежащий демону; это выходит за рамки Auto VPN и не сделано.

Per-app routing (`CoreVpnService.configurePerAppProxy`) меняет allowed/disallowed пакеты внутри одного TUN и не решает задачу: чувствительное приложение всё равно видит `TRANSPORT_VPN`. Xray core менять не требуется.
