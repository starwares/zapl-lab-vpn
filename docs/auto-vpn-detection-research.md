# Auto VPN: признаки VPN и остановка

## Что видит приложение

Приложение может вызвать `ConnectivityManager.getAllNetworks()`/`getNetworkCapabilities()` и увидеть `NetworkCapabilities.TRANSPORT_VPN`, VPN как default network, DNS/routes и изменение внешнего IP. Через `NetworkInterface.getNetworkInterfaces()` иногда виден TUN-интерфейс; имя и доступность зависят от версии Android и vendor. Foreground notification и системный VPN indicator видны пользователю, но не являются API для другого приложения. IP reputation/ASN показывают выходной сервер, а не наличие конкретного TUN.

## Что делает полный stop

Правильный порядок: запретить новые команды, остановить Xray и tun2socks, закрыть `ParcelFileDescriptor`, отменить foreground notification, `stopSelf`. После close Android удаляет интерфейс и перестаёт рекламировать `TRANSPORT_VPN`; indicator исчезает с задержкой обработки ConnectivityService. Уже установленные сокеты, DNS cache, QUIC connection и данные, прочитанные до stop, не откатываются. Между `ACTIVITY_RESUMED` запрещённого приложения и close существует race, поэтому «немедленно» означает минимизацию окна, а не атомарную гарантию.

## Как остановка сделана в этом форке

`AutoVpnCommandReceiver` — manifest receiver в процессе `:daemon`, поэтому команда остановки доходит и в
окне между `onStartCommand()` и регистрацией динамического receiver'а внутри `CoreServiceManager`, то есть
именно тогда, когда туннель поднимается, а пользователь уже открыл запрещённое приложение. Если ядро
запущено, вызывается штатный `ServiceControl.stopService()` (tun2socks → `stopCoreLoop()` → `stopSelf()` →
`close()` дескриптора); если ядро ещё не стартовало, уничтожается сам сервис, а `CoreVpnService.onDestroy()`
доводит очистку до конца.

`VpnLifecycleController` не считает состояние `OFF` по факту отправки команды: он опрашивает
`CoreServiceManager.isRunning()` до совпадения с ожидаемым значением, с ограничением сверху в 15 секунд.
UI-сообщение `MSG_STATE_STOP_SUCCESS` для этого не используется, потому что upstream отправляет его до
завершения асинхронной остановки ядра.

## Проверочный протокол

На устройстве зафиксировать `dumpsys connectivity`, `ip addr`, `ip route`, `dumpsys vpn`, `dumpsys notification` до stop, сразу после и через 1/3 секунды; из тестового приложения запросить `TRANSPORT_VPN`, default network и DNS. Повторить TCP, QUIC и DNS запросы. **Выполнено на эмуляторе (API 37):** после решения об остановке VPN-сеть переходит в `DISCONNECTED` через ~112 мс, дескриптор закрыт через ~153 мс, `tun0` исчезает из `ip link`. Пока туннель поднят, трафик «Через VPN» приложения приходит на SOCKS-сервер хоста. **Not run:** проверка из стороннего тестового приложения (`NetworkCapabilities`, `NetworkInterface`), QUIC-сессии и DNS-кеш, физические устройства.

Источники: [NetworkCapabilities](https://developer.android.com/reference/android/net/NetworkCapabilities), [VpnService](https://developer.android.com/reference/android/net/VpnService), [VPN guide](https://developer.android.com/develop/connectivity/vpn).
