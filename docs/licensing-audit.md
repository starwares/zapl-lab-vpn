# Licensing Audit

Объект: Android-клиент Zapl Lab (модифицированный v2rayNG), ветка `release/publication-readiness`.
Дата аудита: 2026-09-13. Upstream-база: `2dust/v2rayNG` commit
`173f60a1dabe57e822a56381931be6354f7c7f61` (versionName 2.3.8). На момент аудита upstream
`master` = `a9b1242`; этот коммит не влит, лицензионно значимых файлов он не меняет.

Документ не является юридическим заключением. Пункты с пометкой **LEGAL REVIEW REQUIRED**
нельзя решить по текстам лицензий однозначно.

## Executive Summary

**Вердикт: PUBLISHABLE AFTER FIXES**

Лицензия проекта (GPL-3.0) допускает публичное и коммерческое распространение производной
работы. Большая часть технических обязательств в репозитории уже выполнена: атрибуция, тексты
лицензий внутри APK, сторонние уведомления, отдельный applicationId, updater и ссылки на
репозиторий форка, связка source ↔ binary, CI-проверка лицензий.

Публиковать сборку **нельзя**, пока не закрыты блокеры:

| # | Блокер | Тип |
|---|---|---|
| B1 | `libmmkv.so` (MMKV 1.3.16) содержит код OpenSSL 1.1.0i под OpenSSL/SSLeay License (advertising clause) | LEGAL REVIEW REQUIRED |
| B2 | Встроенные в APK `geoip.dat`/`geosite.dat` (из `libv2ray.aar`): данные MaxMind GeoLite (EULA §6), hosts Dan Pollock (только некоммерческое использование), список Peter Lowe (лицензия неизвестна) | LEGAL REVIEW REQUIRED |
| B3 | Знак «V» v2rayNG — центральный элемент новой иконки, splash и логотипа Zapl Lab; upstream-логотип также в иконках уведомлений, QS-плитки, виджета и в TV-баннере с надписью «v2rayNG» | Брендинг (нужен новый дизайн) |
| B4 | Происхождение растровой графики Zapl Lab не подтверждено (автор, AI-генерация, сток) | Требуется подтверждение прав |
| B5 | Репозиторий исходников `github.com/starwares/zapl-lab-vpn` ещё не создан; в релизе нет архива исходников | GPL-3.0 §6 |

Пока B1 и B2 отмечены `resolved: false` в `third_party/license-policy.json`, релизная сборка
(`check_licenses.py --release`) падает намеренно.

Ответ на критерий готовности (после закрытия блокеров):

- **Какой код соответствует APK:** Git-тег `v<versionName>` → commit, записанный в
  `build-info.txt` и показанный в About (`BuildConfig.SOURCE_COMMIT`); архив
  `zapl-lab-vpn-<tag>-source.tar.gz` (репозиторий + сабмодули + Go vendor), sha256
  `libv2ray.aar`. См. [releasing.md](releasing.md).
- **Какие лицензии:** раздел [Dependency Audit](#dependency-audit) и `THIRD_PARTY_NOTICES.md`.
- **Какие обязательства выполняются:** раздел [GPL Obligations](#gpl-obligations).
- **Почему можно распространять:** все компоненты кода, кроме B1, совместимы с GPL-3.0; их
  атрибуция и тексты лицензий входят в APK; исходники публикуются для каждого тега.

## Upstream License

Проверено по фактическим файлам (`173f60a` и `upstream/master` `a9b1242`):

| Источник | Результат |
|---|---|
| `LICENSE` | Полный текст GNU GPL v3 (29 June 2007). md5 `d32239bcb673463ab874e80d47fae504` совпадает в обеих ревизиях; от текущего gnu.org отличается только `http`→`https` в трёх URL |
| GitHub API `repos/2dust/v2rayNG` | `license.spdx_id = GPL-3.0` |
| README | Раздела о лицензии нет, trademark-политики нет |
| Gradle metadata (`build.gradle.kts`, `settings.gradle.kts`) | Лицензия не объявлена (приложение, не библиотека) |
| Заголовки исходников | Copyright/SPDX-заголовков нет. Единственные заголовки — Apache-2.0 в `gradlew`/`gradlew.bat` (Gradle) |
| «or any later version» | Нигде не встречается → распространяем на условиях GPL-3.0 |
| Release/packaging | `.github/workflows/build.yml` (сборка, GPG-подпись, GitHub Release), `compile-hevtun.sh`; лицензионных шагов, кроме `licenseFdroidReleaseReport`, нет |
| `CR.md` | Политика конфиденциальности upstream (2dust, e-mail upstream) — к форку не относится |
| fastlane | Метаданные v2rayNG (заменены в форке) |

**Copyright notices.** Явных строк copyright в upstream нет; права принадлежат авторам и
контрибьюторам (maintainer — 2dust) согласно истории Git. Поэтому в `NOTICE` не придумывается
конкретный правообладатель.

**Trademark.** В upstream-репозитории нет политики товарных знаков. Регистрационные базы
(WIPO, Роспатент, USPTO) не проверялись — если планируется активное продвижение, это стоит
сделать. Независимо от регистрации, выдавать продукт за официальный v2rayNG нельзя (путаница
потребителей), а указывать происхождение — обязательно.

## GPL Obligations

| Обязательство GPL-3.0 | Где выполнено | Статус |
|---|---|---|
| Сохранить текст лицензии, передать его с программой (§4, §6) | `LICENSE` в репозитории; About → «License: GNU GPL v3» (`assets/legal/license.html`, генерируется из `LICENSE`) | Выполнено |
| Сохранить уведомления об авторстве и отказ от гарантий (§4, §5) | `NOTICE`; About (subtitle «Free software with no warranty…») | Выполнено |
| Заметное уведомление об изменениях и дата (§5a) | `NOTICE` (база, 2026, категории изменений), `CHANGELOG.md`, история Git, About «Based on v2rayNG» | Выполнено |
| Вся производная работа под GPL-3.0 (§5c) | Весь код клиента, включая auto VPN, UI, переводы, в одном GPL-репозитории | Выполнено |
| Appropriate Legal Notices в интерактивном UI (§5d) | About: лицензия, отсутствие гарантий, ссылка на исходники, уведомления | Выполнено |
| Corresponding Source для объектного кода (§6) | Тег + публичный репозиторий + архив исходников с сабмодулями и Go vendor в каждом релизе (`build.yml`) | После B5 |
| Доступ к исходникам «из того же места» при сетевом распространении (§6d) | GitHub Releases: архив рядом с APK; магазины: ссылка в описании и в About | После B5 |
| Не добавлять ограничений (§7, §10, §12) | Нет EULA; `README.md` явно разделяет условия онлайн-сервисов и права по GPL | Выполнено; контролировать ToS сервиса |
| Installation Information (§6, «User Product») | Не применимо: устройства не поставляются; приложение ставится обычным APK | — |
| Коммерческое распространение | Разрешено (преамбула, §4 «charge any price») при выполнении §6 | Разрешено |

Что нельзя включать в пользовательское соглашение для клиента: запрет копирования, модификации,
распространения, ограничение «только для личного использования», запрет обратной разработки.
Условия онлайн-сервиса (аккаунт, подписка, допустимое использование серверов) допустимы только
как условия сервиса и не могут ограничивать права на сам клиент.

## Dependency Audit

Метод: отчёты gradle-license-plugin 0.9.9 (`licensePlaystoreReleaseReport`,
`licenseFdroidReleaseReport`), `:app:dependencies` для `playstore/fdroidReleaseRuntimeClasspath`
и `coreLibraryDesugaring`, POM и файлы лицензий внутри jar/aar из Gradle-кэша. Состав
playstore и fdroid одинаковый: 123 упакованных Maven-артефакта (в отчёте плагина — 146
координат, т. к. KMP-библиотеки записаны по корневым модулям, плюс BOM) + 2 артефакта desugar
+ `libv2ray.aar`.

Сводка по лицензиям Maven: Apache-2.0 — 120; Apache-2.0 AND BSD-3-Clause (camera-core, libyuv) —
1; Apache-2.0 AND MPL-2.0 (OkHttp, Public Suffix List) — 1; BSD-3-Clause AND OpenSSL (MMKV) — 1;
GPL-2.0 WITH Classpath-exception-2.0 (desugar_jdk_libs) — 1; BSD-3-Clause
(desugar_jdk_libs_configuration) — 1. Неизвестных лицензий, «Android SDK License», Play
Services, Firebase, аналитики, crash-reporting и рекламных SDK в release-графе и в `app/src`
нет. Test/debug-зависимости (JUnit EPL-1.0, Mockito MIT, Espresso, ui-tooling) в release не
попадают.

Полный перечень с версиями — `THIRD_PARTY_NOTICES.md`, раздел 5. Существенные компоненты:

| Компонент | Версия / commit | Источник | Лицензия | Совместима с GPL-3.0 | Что требуется при распространении |
|---|---|---|---|---|---|
| v2rayNG (база) | `173f60a` | github.com/2dust/v2rayNG | GPL-3.0 | — (сама GPL) | §4–§6: текст, уведомления, исходники |
| AndroidLibXrayLite (`libv2ray.aar`) | v26.9.9, `d0c6c4ae1b09c912070c8288bd0dbcc2e492ac29` | github.com/2dust/AndroidLibXrayLite | LGPL-3.0 | Да | Текст LGPL+GPL, исходники |
| Xray-core | `52a412d9e2f5c2a5142b1b4e2ab3771dacb8b120` | github.com/XTLS/Xray-core | MPL-2.0 (1 файл MIT) | Да (MPL §3.3, нет Exhibit B) | Текст MPL, указание, где исходники (§3.2) |
| 49 Go-модулей (quic-go, utls, gvisor, grpc, sing, wireguard-go, x/* …) | см. `THIRD_PARTY_NOTICES.md` §1 | proxy.golang.org | MIT, BSD-2/3, Apache-2.0, MPL-2.0, LGPL-3.0+exception, GPL-3.0-or-later | Да | Тексты и copyright (`go-modules.txt`), NOTICE grpc/yaml.v2/freelru, исходники |
| Go toolchain, x/mobile | go1.27.1 | go.dev | BSD-3-Clause | Да | Атрибуция |
| hev-socks5-tunnel, -core, hev-task-system | `64cc609`, 1.6.4, 5.10.3 | github.com/heiher | MIT | Да | Copyright + текст MIT |
| rbtree в hev (из Linux `lib/rbtree.c`) | в тех же коммитах | kernel.org | GPL-2.0-or-later (в hev помечен MIT) | Да | Текст GPL-2.0, авторы; см. Remaining Risks |
| lwIP (форк heiher) | 2.2.1.7 `2a11c14` | github.com/heiher/lwip | BSD-3-Clause | Да | Copyright + текст |
| libyaml (форк heiher) | 0.2.5.2 `efa3611` | github.com/heiher/yaml | MIT | Да | Copyright форка **и** upstream libyaml |
| MMKV `mmkv-static` | 1.3.16 | github.com/Tencent/MMKV | BSD-3-Clause + OpenSSL/SSLeay + zlib/Apache (не встроены) | **LEGAL REVIEW REQUIRED** | Acknowledgement OpenSSL и Eric Young (выполнено); совместимость — B1 |
| OkHttp / Okio | 5.4.0 / 3.17.0 | square | Apache-2.0; PSL — MPL-2.0 | Да | Текст Apache, уведомление MPL для `PublicSuffixDatabase.list` |
| CameraX core | 1.6.1 | androidx | Apache-2.0 + BSD-3 (libyuv) | Да | Copyright LibYuv |
| desugar_jdk_libs | 2.1.5 | github.com/google/desugar_jdk_libs | GPL-2.0-only WITH Classpath-exception-2.0 | Да (через исключение) | Текст GPL-2.0+CE; исходники этой версии — см. Remaining Risks |
| AndroidX, Compose, CameraX, WorkManager, Kotlin, kotlinx, Coil, Gson, ZXing, reorderable, Dagger, JSpecify … | см. §5 notices | Maven Central / Google Maven | Apache-2.0 | Да | Текст Apache-2.0; NOTICE kotlinx.coroutines (в concurrent-futures-ktx) и Jakarta Inject воспроизведены |
| Montserrat Thin | v6.001 | upstream-ресурс | OFL-1.1 | Да | **Удалён**: файл нигде не использовался |

### B1: MMKV и код OpenSSL

Факты (MMKV `LICENSE.TXT` на теге v1.3.16, исходники, символы в бинарнике):

- Лицензия MMKV: «Open Source Software Licensed Under the OpenSSL License: OpenSSL 1.1.0i».
- В `Core/aes/openssl/` — AES (CFB128), MD5, ARM-ассемблер; безусловно включены в
  `Core/CMakeLists.txt`, `MMKV_DISABLE_CRYPT` выключен, `openssl::MD5` вызывается в
  `MMKV::encodeFilePath`.
- В `libmmkv.so` экспортируются `openssl_aes_armv8_*` (arm64-v8a) / `openssl_aes_arm_*`
  (armeabi-v7a); таблицы AES есть во всех четырёх ABI.
- OpenSSL License п. 3 и 6 и SSLeay License содержат требования рекламной атрибуции; FSF
  относит комбинацию OpenSSL/SSLeay к лицензиям, несовместимым с GNU GPL. Системной
  библиотекой (исключение GPL §1) MMKV не является.
- Та же ситуация существует в upstream v2rayNG.

Что сделано: acknowledgement-фразы и полный `MMKV-LICENSE.txt` включены в APK — это выполняет
условия самой OpenSSL License, но не снимает вопрос совместимости с GPL.

Варианты (выбрать после консультации):

1. **Оставить** с юридическим обоснованием. Например: код OpenSSL в MMKV совпадает с кодом,
   который правообладатели OpenSSL перелицензировали под Apache-2.0 в OpenSSL 3.0; или
   правообладатели v2rayNG, объединившие код с MMKV, фактически дали разрешение. Оба довода
   спорные.
2. **Заменить код внутри MMKV**: собрать MMKV 1.3.16 из исходников, заменив
   `Core/aes/openssl/*` на эквивалент под Apache-2.0 (OpenSSL 3.x) или на реализацию MD5 в
   public domain при `MMKV_DISABLE_CRYPT` (приложение не использует `cryptKey`). Формат данных
   не меняется: MD5 детерминирован. Требуются собственная сборка AAR и CI.
3. **Заменить MMKV** на хранилище без такого кода. Это большая миграция `MmkvManager` и
   сохранённых данных, нужна миграция форматов.
4. **Запросить разрешение** у Tencent (MMKV) — маловероятно и не решает вопрос с
   правообладателями OpenSSL.

Рекомендация: вариант 2 как технически ограниченный, либо вариант 1 после письменного
заключения юриста.

## Native Components

Что реально попадает в APK (`lib/<abi>/`, ABI: arm64-v8a, armeabi-v7a, x86, x86_64 — совпадает с
`splits.abi` в `build.gradle.kts`):

| Файл | Происхождение | Воспроизводимость из опубликованных исходников |
|---|---|---|
| `libgojni.so` | `libv2ray.aar`, релизный ассет AndroidLibXrayLite v26.9.9; sha256 AAR `9ecf4c921568d8f4cb8550d3bafe08ff6f1d1984f45a6ad183dcdf52ee9302de` побайтно совпадает с релизом; собран публичным workflow (run 34308662062) | Go-часть: версии модулей зафиксированы `go.sum`, buildinfo совпадает (go1.27.1, `-trimpath`, без `-tags`), но gomobile в upstream-CI берётся `@latest`; побайтная пересборка не проверялась |
| `libhev-socks5-tunnel.so` (JNI), `libhevsockstun.so` (исполняемый для root-режима) | Собираются из сабмодуля `hev-socks5-tunnel` скриптом `compile-hevtun.sh` (NDK 29.0.14206865); в бинарнике `COMMIT_ID 64cc609` | Да, из сабмодулей |
| `libmmkv.so` | Maven `mmkv-static` 1.3.16 | Исходники MMKV на теге v1.3.16 |
| `libimage_processing_util_jni.so`, `libsurface_util_jni.so`, `libandroidx.graphics.path.so` | AndroidX (CameraX, graphics-path) | Исходники AOSP/AndroidX |

Проверки:

- Встроенных закрытых бинарников, без которых сборка невозможна, нет: `libv2ray.aar` и `.so`
  hev не хранятся в Git (`.gitignore`), воспроизводятся шагами `build.yml`/`README.md`.
- `hev-socks5-tunnel-bin` в корневом `libs/` — побочный продукт `Android.mk` (побайтно равен
  `libhevsockstun.so`), в APK не попадает. Корневой `/libs` добавлен в `.gitignore`.
- `hev-socks5-tunnel/third-part/wintun/bin/wintun.dll` — проприетарный Windows-бинарник
  (WireGuard LLC Prebuilt Binaries License). В Android-сборку не входит, из архива исходников
  релиза исключён.
- `AndroidLibXrayLite/assets/*.dat` (старые гео-файлы в сабмодуле) в AAR/APK не попадают; из
  архива исходников исключены.
- Сабмодули объявлены в `.gitmodules`, публичны, клонируются рекурсивно; вложенные сабмодули
  hev (core, hev-task-system, lwip, yaml) публичны.

## Assets

| Ресурс | Происхождение | Лицензия | Статус |
|---|---|---|---|
| `assets/custom_routing_*` | upstream; категории geosite/geoip и IP DNS (факты) | GPL-3.0 | OK |
| `assets/proxy_package_name` | = `2dust/androidpackagenamelist` `proxy.txt` | GPL-3.0 | OK, указано в notices |
| `assets/translators.json` | кредиты переводчиков upstream | GPL-3.0 | OK, сохранить |
| `assets/v2ray_config*.json` | шаблоны upstream | GPL-3.0 | OK |
| `assets/open_source_licenses.html` | отчёт плагина; **был устаревшим** (перечислял Play Services, ML Kit, Firebase, RxJava, которых нет в сборке) | — | Перегенерирован; CI проверяет актуальность |
| `assets/legal/*.html` | генерируются при сборке из `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`, `third_party/licenses/*.txt` | — | Новое |
| `PublicSuffixDatabase.list` (из OkHttp) | publicsuffix.org | MPL-2.0 | Атрибутирован |
| Material Icons / Symbols | Google | Apache-2.0 | Атрибутированы |
| ~20 иконок с viewport 1024 (`ic_about`, `ic_settings`, `ic_qu_*` …) | upstream, набор не установлен | UNKNOWN (часть GPL-репозитория upstream) | Низкий риск |
| GitHub mark (`ic_github_24dp`) | товарный знак GitHub | trademark | OK (ссылка на профиль переводчика) |
| Знак Telegram (`ic_telegram_24dp`), промо-иконка | upstream | trademark | **Удалены** вместе со ссылками |
| Иконки Zapl Lab: `mipmap-*/ic_launcher*`, `drawable-nodpi/zapl_lab_logo.png`, `ic_launcher_monochrome.xml`, `ic_splash_zapl_lab.xml` | форк; содержат «V» v2rayNG (см. комментарии в XML); в PNG нет метаданных | Производная от upstream-лого + неподтверждённый растр | **B3, B4** |
| `ic_stat_*` (уведомления, QS, виджет) | логотип «V» v2rayNG | GPL-3.0 (upstream) | **B3** |
| `mipmap-*/ic_banner*` (Android TV) | «V» + надпись «v2rayNG» | GPL-3.0 (upstream) | **B3** |
| `src/main/ic_launcher-web.png`, `fastlane/…/images/icon.png` | логотип v2rayNG (в APK не входят) | GPL-3.0 (upstream) | **B3** (витрина магазина) |
| Переводы `values-*/strings.xml` | upstream PR + форк | GPL-3.0 | OK. Во вьетнамской локали 9 строк называли продукт «v2rayNG» — исправлено; новые строки форка для `bqi-rIR` пока на английском |
| Шрифт Montserrat Thin | upstream, не используется | OFL-1.1 | Удалён |

## Geo databases

| Файл в APK | Источник | Попадает в APK | Лицензия данных | Требуемая атрибуция | Распространение разрешено |
|---|---|---|---|---|---|
| `geoip.dat` (17 МБ, sha256 `4149e607…c721`) | Loyalsoldier/geoip release 202609040655 | Да, из AAR | CC-BY-SA-4.0 + GPL-3.0 (сборка); MaxMind GeoLite (EULA 2026-02-12, CC-BY-SA-4.0 для охраняемых элементов); GeoNames CC-BY-4.0; china-operator-ip MIT | «This product includes GeoLite Data created by MaxMind…», GeoNames | **LEGAL REVIEW REQUIRED**: EULA §6 запрещает раскрытие третьим лицам «except as explicitly permitted by the Creative Commons License» без согласия MaxMind и требует налагать на получателей те же обязанности |
| `geoip-only-cn-private.dat` (137 КБ) | тот же релиз | Да | CC-BY-SA-4.0 + GPL-3.0; по `config.json` данные MaxMind вероятно не входят (побайтно не проверено) | CC-BY-SA, MIT | Вероятно да |
| `geosite.dat` (11 МБ, sha256 `110be548…e5ea`) | Loyalsoldier/v2ray-rules-dat release 202609082347 | Да | GPL-3.0 (сборка); domain-list-community MIT; dnsmasq-china-list WTFPL; gfwlist LGPL-2.1; EasyList GPL-3.0+/CC-BY-SA-3.0+; AdGuard GPL-3.0; WindowsSpyBlocker MIT; **Dan Pollock hosts — «free to copy and distribute … for non-commercial uses»**; **Peter Lowe — UNKNOWN** | MIT/EasyList-атрибуции | **LEGAL REVIEW REQUIRED** при коммерческом распространении |
| Загрузка по кнопке (Asset files) | Loyalsoldier, runetfreedom, Chocolate4U (GitHub releases) | Нет, скачивает пользователь | см. источники | не требуется от нас | Не распространяем сами |

Атрибуция MaxMind/GeoNames и перечень источников добавлены в `THIRD_PARTY_NOTICES.md` §3.

Варианты для B2:

1. **Не встраивать гео-данные в APK** (исключить `assets/*.dat` из AAR) и скачивать их при
   первом запуске по явному действию пользователя из выбранного источника. Меняет поведение:
   без файлов не работают правила `geosite:`/`geoip:`, нужен UX первого запуска и проверка
   всех трёх режимов (VPN, proxy-only, root).
2. **Заменить источник** на сборку без MaxMind и некоммерческих списков (например, geosite из
   `v2fly/domain-list-community` (MIT) без ad-списков, geoip на данных с открытой
   лицензией) — собственная сборка AAR или подмена ассетов.
3. **Оставить** после письменного заключения по EULA MaxMind и спискам Dan Pollock / Peter Lowe
   (или получения разрешений).

## Branding / Trademark

Сделано:

| Элемент | Было | Стало |
|---|---|---|
| applicationId | `com.v2ray.ang` / `.fdroid` | `store.zapl.lab.vpn` / `store.zapl.lab.vpn.fdroid` |
| Название (main / F-Droid / dev / pre_release) | v2rayNG, «v2rayNG (F-Droid)» … | Zapl Lab, «Zapl Lab (F-Droid)» … |
| Launcher/adaptive/monochrome icon, splash, drawer logo | лого v2rayNG | арт Zapl Lab (см. B3/B4) |
| About | Source code, Feedback → 2dust; Telegram 2dust; Privacy → `CR.md` upstream | Source code, Feedback → `starwares/zapl-lab-vpn`; «Based on v2rayNG — independent modified distribution, not an official v2rayNG release» → ссылка на upstream; License; Notices; Privacy → `PRIVACY.md` форка; версия с commit |
| Навигация | пункт Promotion (реклама upstream `9.234456.xyz`) | удалён |
| Updater | `api.github.com/repos/2dust/v2rayNG/releases` | `api.github.com/repos/starwares/zapl-lab-vpn/releases` |
| Имена APK | `v2rayNG_<ver>_<abi>.apk` | `zapl-lab-vpn_<ver>_<abi>.apk` |
| Файл логов | `v2rayNG_logcat_*` | `zapl-lab_logcat_*` |
| fastlane | title/описание v2rayNG, Telegram 2dust | Zapl Lab, атрибуция, ссылка на исходники |
| README | README upstream (релизы, GPG-отпечаток и Telegram 2dust) | README форка |
| vi-локаль | 9 строк «v2rayNG» | нейтральные формулировки |
| GPG-ключ релиза | `v2rayN-public-key.asc` | `zapl-lab-public-key.asc` |

Оставлено сознательно (техническая совместимость или атрибуция, не брендинг):

- Kotlin namespace/пакеты `com.v2ray.ang` — не видны пользователю; JNI (`-DPKGNAME=com/v2ray/ang/service`) и слияния с upstream не ломаются.
- User-Agent подписок `v2rayNG/<version>` (`HttpUtil.kt`) — панели подписок выбирают формат
  конфигурации по этому UA; замена сломает импорт. Номинативное использование; решение за
  продуктом.
- Deep link `v2rayng://install-config|install-sub` — совместимость со ссылками панелей. Если
  установлены оба приложения, Android покажет выбор.
- Ссылка «Mode help» на wiki upstream — документация общих режимов, с явной атрибуцией.
- Строки «Xray core», `geosite:`/`geoip:` — технические термины.

Осталось сделать — **B3**: заменить «V» в знаке Zapl Lab (launcher, round, monochrome,
splash, drawer), в `ic_stat_*` (уведомления, QS, виджет), TV-баннере `ic_banner*`,
`ic_launcher-web.png` и `fastlane/…/icon.png`. **B4**: зафиксировать автора и права на растр
Zapl Lab; если использовался генератор изображений — условия его сервиса.

### Проверка смены package name (store.zapl.lab.vpn)

| Область | Результат |
|---|---|
| VPN Service, broadcast-actions, процесс `:daemon`/`:bg`, WorkManager | Используют `${applicationId}` / `BuildConfig.APPLICATION_ID` — корректно |
| Native bindings | JNI привязан к Java-пакету `com.v2ray.ang.service`, не к applicationId — не затронут |
| Intents / notification actions | `AppConfig.ANG_PACKAGE = BuildConfig.APPLICATION_ID` — корректно |
| Shortcuts | `res/xml/shortcuts.xml` и `src/fdroid/res/xml/shortcuts.xml` содержали литерал `com.v2ray.ang` — исправлено; на эмуляторе 4 ярлыка зарегистрированы как `store.zapl.lab.vpn/com.v2ray.ang.ui.shortcut.*` |
| Content providers / FileProvider | authorities `${applicationId}.cache`, `${applicationId}.androidx-startup` — корректно |
| Permissions | Нет собственных permission с литералом пакета |
| Deep links | Схема `v2rayng://` не зависит от пакета |
| Updater | Фильтр APK по ABI и `fdroid` в имени — совместим с новыми именами |
| Backup/restore | Экспорт/импорт конфигурации пакет не использует. Данные `com.v2ray.ang` **не переносятся** (другое приложение); пользователи переносят профили через Backup & Restore / QR / подписки |
| Gradle build | `assemblePlaystoreDebug`, `assembleFdroidDebug` — успешно; `aapt2 dump badging` подтверждает ID |
| Сосуществование | На эмуляторе установлены одновременно `com.v2ray.ang` и `store.zapl.lab.vpn` |

## Source Distribution

| Вопрос | Ответ |
|---|---|
| Все исходники присутствуют? | Да: приложение, скрипты сборки, CI, сабмодули (публичные). Закрытых обязательных файлов нет |
| Закрытый prebuilt, без которого сборка невозможна? | Нет. `libv2ray.aar` — открытый LGPL-проект, релизный артефакт его публичного CI; hev собирается из исходников |
| Build scripts доступны? | `build.yml`, `compile-hevtun.sh`, Gradle wrapper, `README.md` |
| Версии toolchain | `build.yml` (SDK 37, build-tools 37.0.0, NDK 29.0.14206865, JDK temurin 21), `libs.versions.toml`, `gradle-wrapper.properties` (Gradle 9.5.1) |
| Native dependencies восстановимы? | Да: hev — из сабмодуля; AAR — релиз по тегу сабмодуля (sha256 фиксируется в `build-info.txt`) или сборка gomobile из сабмодуля |
| Соответствие source ↔ APK | Тег проверяется в CI; commit в About и `build-info.txt`; архив исходников с Go vendor прикладывается к релизу |
| Signing keys / secrets | Не нужны для соответствия GPL; хранятся в GitHub Secrets |

Обязательно до первой публикации (B5): создать публичный репозиторий, выполнить
`git fetch --unshallow upstream` (текущий клон shallow — GitHub отклонит push), запушить ветку и
теги, выпустить релиз через `Build APK` на теге.

## Backend Boundary

Фактическое состояние: в репозитории **нет** кода backend, аккаунтов, аутентификации или
телеметрии. Клиент общается только с серверами из профилей/подписок пользователя, GitHub
(обновления, гео-данные, список пакетов), URL проверки задержки и IP-информации. Закрытых
библиотек, SDK и сетевых ключей в APK нет.

Архитектура «GPL-клиент ↔ HTTPS API ↔ закрытый backend» совместима с GPL-3.0, если backend —
отдельная программа, взаимодействующая через обычный сетевой протокол.

```text
MUST BE OPEN (всё, что входит в распространяемый APK/AAB)
- весь Kotlin/Java-код клиента, включая будущие экраны входа, API-клиент, логику подписок,
  auto VPN, UI, переводы, ресурсы;
- любой код телеметрии внутри приложения и сторонние SDK — только под GPL-совместимыми
  лицензиями (проприетарные SDK вроде Firebase Analytics/Crashlytics недопустимы);
- нативные библиотеки и Corresponding Source к ним (AndroidLibXrayLite, hev, Go-модули);
- скрипты сборки и описание сборки;
- код, который сервер передаёт клиенту для исполнения (dex/jar/so, скрипты, плагины) —
  он становится частью программы; поэтому такой механизм лучше не вводить.

CAN REMAIN PRIVATE (отдельные программы на серверах, не передаются пользователям)
- backend API: аутентификация, подписки, выдача конфигураций, распределение серверов,
  телеметрия, управление аккаунтами;
- серверные Xray/VPN-узлы и их конфигурации (MPL-2.0/GPL-3.0 не требуют раскрытия при
  использовании на сервере; AGPL-компонентов в стеке клиента нет);
- данные, которые сервер выдаёт клиенту (конфигурации, списки серверов) — это данные, не код;
- инфраструктура, credentials, signing keys, секреты CI.
```

Условия и оговорки:

- Не связывать закрытый backend-код с APK как библиотеку (shared/static linking, AAR, dex).
- Любые ключи или токены, встроенные в APK, публичны по определению: секреты держать на
  сервере.
- ToS сервиса может регулировать доступ к серверам, но не права на клиент по GPL (запрет
  модификации или использования с другими серверами как лицензионное условие недопустим).
- Если backend использует модифицированный компонент под AGPL, раскрытие потребуется при
  сетевом взаимодействии — проверять при проектировании backend.

## App Store Considerations

Требования GPL одинаковы для всех каналов; правила магазинов — отдельный слой.

| Канал | Требования лицензии (GPL) | Ссылка на source code | GPL notice | Privacy policy | Ограничения канала для VPN (не GPL) |
|---|---|---|---|---|---|
| GitHub Releases | Архив исходников / тег рядом с APK, текст GPL в APK | Release notes + `README.md` + About | `LICENSE` в репозитории и About | `PRIVACY.md` (рекомендуется) | Нет модерации; подпись APK и GPG-подписи — задача доверия |
| Прямой APK (сайт) | То же; §6d: равноценный доступ к исходникам «из того же места» | Страница загрузки рядом с APK + About | About + ссылка на `LICENSE` | Рекомендуется | Пользователь включает установку из неизвестных источников; updater вне магазина допустим |
| Google Play (AAB) | То же; AAB, переподписанный Google, — объектный код, §6 применяется | Описание в карточке + About | About (внутри приложения) | **Обязательна** (VPN, `QUERY_ALL_PACKAGES`, данные) | [VpnService policy](https://support.google.com/googleplay/android-developer/answer/12564964): VPN — основная функция, декларация в Play Console, видео, шифрование до конечной точки, prominent disclosure с согласием, запрет манипуляции трафиком ради монетизации. [Device and Network Abuse](https://support.google.com/googleplay/android-developer/answer/9888379): приложение не может обновлять себя в обход Google Play → **updater в Play-сборке нужно отключить**. Также декларации `QUERY_ALL_PACKAGES`, foreground service `specialUse`, Usage Access |
| RuStore | То же | Описание + About | About | Требуется при обработке персональных данных (проверить текущие правила) | **LEGAL REVIEW REQUIRED**: российское регулирование VPN/анонимайзеров (149-ФЗ ст. 15.8) и рекламы средств обхода блокировок; модерация RuStore; требования к статусу разработчика и самообновлению — проверить по актуальным правилам |
| Huawei AppGallery | То же | Описание + About | About | Требуется (проверить) | Правила VPN-приложений и документы зависят от региона публикации; самообновление в обход магазина, вероятно, запрещено — проверить по актуальным Review Guidelines (не проверялось) |

Рекомендации: для магазинных сборок — отдельный флейвор или флаг без updater; ссылку на
исходники ставить в описании и в About; `PRIVACY.md` (черновик) — пройти юридическую проверку;
для Google Play добавить экран prominent disclosure до включения VPN и Usage Access.

## Required Changes

Сделано в этой ветке (коммиты описаны в `CHANGELOG.md`):

1. `LICENSE` (GPL-3.0 без изменений), `NOTICE`, `THIRD_PARTY_NOTICES.md`,
   `third_party/licenses/` (Apache-2.0, MPL-2.0, LGPL-3.0, GPL-2.0, GPL-2.0+CE, MMKV, Go-модули,
   hev), `CHANGELOG.md`, `README.md`, `PRIVACY.md` (черновик).
2. Тексты GPL и уведомления встроены в APK (Gradle-задача `generateLegalAssets`), пункты About.
3. Перегенерирован устаревший `open_source_licenses.html`; удалён неиспользуемый шрифт OFL.
4. applicationId `store.zapl.lab.vpn`, ярлыки, названия флейворов, имена APK, vi-строки,
   fastlane, удалены Promotion и Telegram upstream.
5. Updater, Source code, Feedback, Privacy → `github.com/starwares/zapl-lab-vpn`.
6. CI: `License audit` (`scripts/check_licenses.py` + policy + проверка актуальности HTML,
   отчёты как artifact); `Build APK`: проверка тега, архив Corresponding Source (с сабмодулями
   и Go vendor, без wintun и старых `.dat`), `build-info.txt`, лицензионный гейт `--release`.
7. `.gitignore`: корневой `/libs` (артефакты `compile-hevtun.sh`).

Нужно сделать до публикации:

| # | Действие | Владелец |
|---|---|---|
| B1 | Решение по MMKV/OpenSSL (варианты выше), затем `resolved: true` в policy | юрист + разработка |
| B2 | Решение по встроенным гео-данным (варианты выше) | юрист + продукт |
| B3 | Новый знак без «V» v2rayNG во всех перечисленных ресурсах | дизайн |
| B4 | Подтверждение прав на арт Zapl Lab | владелец продукта |
| B5 | Создать публичный `starwares/zapl-lab-vpn`, unshallow, push, первый релиз через workflow | владелец репозитория |
| R1 | Свой keystore и GPG-ключ в GitHub Secrets (`APP_KEYSTORE_*`, `GPG_PRIVATE_KEY`); опубликовать отпечаток GPG в README | владелец |
| R2 | Юридическая проверка `PRIVACY.md`, отдельные ToS сервиса | юрист |
| R3 | Для магазинов: сборка без updater, prominent disclosure (Google Play) | разработка |

## Remaining Risks

| Риск | Уровень | Комментарий |
|---|---|---|
| desugar_jdk_libs 2.1.5: нет тега и sources.jar; ближайший коммит исходников `73170c345e` («Prepare for jdk-11 based desugared library version 2.1.5») | Средний | GPL-2.0 требует исходники распространяемого объектного кода. Варианты: приложить исходники этого коммита к релизу; уточнить у Google; отказаться от `coreLibraryDesugaring`, если он не нужен при minSdk 24 (требует анализа кода) |
| rbtree в hev помечен MIT, но происходит из Linux (GPL-2.0-or-later) | Низкий | Совместимо с GPL-3.0; атрибуция добавлена. LEGAL REVIEW REQUIRED только по корректности маркировки |
| Upstream-CI AndroidLibXrayLite: gomobile `@latest`, runner не закреплён, гео-данные `latest` | Средний | Воспроизводимость AAR не доказана; для полной независимости собирать AAR в своём CI из сабмодуля |
| Corresponding Source частично у третьих сторон (сабмодули на GitHub, модули на proxy.golang.org) | Низкий | Снимается архивом исходников в каждом релизе |
| Выход новых версий upstream / AAR | Средний | CI падает при смене gitlink сабмодулей или desugar; Maven-зависимости сверяются с notices |
| User-Agent `v2rayNG/<ver>` и схема `v2rayng://` | Низкий | Техническая совместимость; возможна путаница, если приложения установлены вместе |
| `versionName` 2.3.8 совпадает с upstream | Низкий | Не нарушение; отдельная нумерация упростит поддержку |
| История Git | Низкий | Просканированы рабочая копия (tracked + untracked), 50 доступных коммитов upstream и 2 коммита форка: ключей, keystore, токенов, `.env`, `google-services.json` нет (только тестовые фикстуры). Полная история upstream не сканировалась (shallow) — она уже публична. Ротация не требуется. Перед push рекомендуется `gitleaks detect` после unshallow |
| `CR.md`, `.github/ISSUE_TEMPLATE/*` от upstream остаются в репозитории | Низкий | Не упоминаются в приложении; при желании заменить шаблоны issues |
| Новые строки для `bqi-rIR` на английском | Низкий | Требование AGENTS.md о ключах выполнено, перевод отсутствует |
| Проверка на устройстве ограничена эмулятором | Средний | См. ниже |

### Проверки

На HEAD ветки после всех коммитов:

- `./gradlew licensePlaystoreReleaseReport --rerun licenseFdroidReleaseReport --rerun
  :app:compilePlaystoreDebugKotlin :app:testPlaystoreDebugUnitTest :app:assemblePlaystoreDebug
  :app:assembleFdroidDebug :app:assemblePlaystoreRelease :app:assembleFdroidRelease` —
  BUILD SUCCESSFUL, 128 unit-тестов, 0 падений; `lintVital` обоих release-вариантов прошёл;
  `open_source_licenses.html` после перегенерации не изменился. Первый прогон release выявил
  оставшиеся `drawable-night` варианты удалённых иконок Telegram/Promotion (`MissingDefaultResource`) —
  исправлено в соответствующих коммитах.
- Промежуточные коммиты с разнесёнными изменениями (attribution, branding) отдельно
  компилируются: `:app:compilePlaystoreDebugKotlin :app:compileFdroidDebugKotlin` — exit 0.
- `scripts/check_licenses.py` — 0 ошибок, 2 предупреждения (B1, B2); `--release` — exit 1;
  негативные сценарии (нет лицензии, «Android SDK License», неописанная версия) — exit 1.
- `BuildConfig.SOURCE_COMMIT` равен `git rev-parse --short=12 HEAD`.

На рабочем дереве перед коммитами (код идентичен HEAD, кроме двух night-иконок):

- `aapt2 dump badging`: `store.zapl.lab.vpn` / `store.zapl.lab.vpn.fdroid`, label «Zapl Lab» /
  «Zapl Lab (F-Droid)»; `shortcuts.xml` targetPackage корректен; в APK есть
  `assets/legal/license.html`, `assets/legal/notices.html`; шрифта нет.
- Эмулятор Pixel 9, API 37, x86_64: установка рядом с `com.v2ray.ang`, запуск, 4 статических
  ярлыка с новым package, в drawer нет Promotion, About показывает атрибуцию и commit, диалоги
  License и Notices открывают встроенные тексты (касание).

Not run:

- GitHub Actions `License audit` и `Build APK` (репозиторий не создан); шаги архива исходников
  и `build-info` проверены локально вручную без `go mod vendor` (Go не установлен).
- Подписанная release-сборка (нет keystore); unsigned `assemble*Release` выполнен.
- Запуск VPN с профилем под новым applicationId, updater против реального релиза (репозитория
  нет), F-Droid APK на устройстве, физические устройства.
- About: проверка TalkBack, клавиатуры и D-pad для новых пунктов (требование UI-гайда).
- `:app:lintPlaystoreDebug` — не требуется (новых API guard/opt-in/suppress нет).

## Maintaining the audit

При изменении зависимостей, сабмодулей или гео-данных:

1. `cd V2rayNG && ./gradlew licensePlaystoreReleaseReport --rerun licenseFdroidReleaseReport --rerun`
   (перезаписывает `open_source_licenses.html`).
2. `python3 scripts/check_licenses.py V2rayNG/app/build/reports/licenses/*.json`; для новых
   артефактов проверить POM и файлы лицензий; обновить таблицу:
   `scripts/check_licenses.py --markdown <reports>` → `THIRD_PARTY_NOTICES.md` §5.
3. Новый `libv2ray.aar`: сверить sha256 с релизом; список Go-модулей — из buildinfo
   (`strings -n 3 jni/arm64-v8a/libgojni.so | grep -P '^(dep|=>)\t'`); лицензии — из
   `https://proxy.golang.org/<module>/@v/<version>.zip`; обновить `go-modules.txt`, таблицу §1
   и `gitlink` в `third_party/license-policy.json`.
4. Новый hev: проверить LICENSE сабмодулей и вложенных сабмодулей, обновить
   `hev-socks5-tunnel.txt` и `gitlink`.
5. Гео-данные: сверить источники и лицензии списков, обновить §3 и policy.

## Release Checklist

```text
[ ] APK соответствует конкретному commit/tag
[ ] исходный код этой версии опубликован
[ ] GPL-3.0 присутствует
[ ] NOTICE актуален
[ ] THIRD_PARTY_NOTICES актуален
[ ] новые зависимости проверены
[ ] proprietary GPL-incompatible SDK отсутствуют
[ ] брендинг не выдаёт проект за официальный v2rayNG
[ ] updater указывает на наши releases
[ ] signing key не опубликован
[ ] secrets отсутствуют
[ ] source URL доступен пользователю
[ ] CHANGELOG содержит изменения
[ ] versionCode/versionName согласованы
[ ] license CI green
```
