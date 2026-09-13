# Third-party notices

Zapl Lab (a modified version of [v2rayNG](https://github.com/2dust/v2rayNG), GPL-3.0) bundles the
third-party components listed below. This file is part of every APK: About → Notices and
third-party components shows it together with the full license texts from
[third_party/licenses/](third_party/licenses/). The GPL-3.0 text of the application is in
[LICENSE](LICENSE).

Versions correspond to this source revision. How the inventory was produced and how to update it
is described in [docs/licensing-audit.md](docs/licensing-audit.md); CI checks the Maven part with
`scripts/check_licenses.py`.

## 1. Native components

### Xray core library: libv2ray.aar (lib/*/libgojni.so)

| | |
|---|---|
| Project | AndroidLibXrayLite, <https://github.com/2dust/AndroidLibXrayLite> |
| Version | tag `v26.9.9`, commit `d0c6c4ae1b09c912070c8288bd0dbcc2e492ac29` (submodule `AndroidLibXrayLite`) |
| Binary | release asset `libv2ray.aar`, sha256 `9ecf4c921568d8f4cb8550d3bafe08ff6f1d1984f45a6ad183dcdf52ee9302de`, built by the project's public GitHub Actions workflow |
| License | LGPL-3.0 (full text: `LGPL-3.0.txt`, which supplements the GPL-3.0 in `LICENSE`) |
| Built with | Go go1.27.1 (BSD-3-Clause), golang.org/x/mobile (BSD-3-Clause), Android NDK r29 |

The library links **Xray-core** <https://github.com/XTLS/Xray-core>, commit
`52a412d9e2f5c2a5142b1b4e2ab3771dacb8b120` (v26.9.9), licensed under the **Mozilla Public License
2.0** (full text: `MPL-2.0.txt`). As required by MPL-2.0 §3.2, the Source Code Form of Xray-core,
and of the MPL-2.0 module `github.com/xtls/reality`, is available at the commits named here and in
the source archive of each Zapl Lab release. The file `proxy/wireguard/netstack.go` of Xray-core is
licensed under the MIT License, Copyright (C) WireGuard LLC.

It also links 49 Go modules under MIT, BSD-2-Clause, BSD-3-Clause, Apache-2.0, MPL-2.0,
LGPL-3.0 with linking exception (`github.com/juju/ratelimit`) and GPL-3.0-or-later
(`github.com/sagernet/sing`, `github.com/sagernet/sing-shadowsocks`). The module list, versions,
copyright notices, license texts and Apache NOTICE files (`google.golang.org/grpc`,
`gopkg.in/yaml.v2`, `github.com/sagernet/sing/contrab/freelru`) are reproduced in
`go-modules.txt`.

| Module | Version | License |
|---|---|---|
| github.com/andybalholm/brotli | v1.2.0 | MIT |
| github.com/apernet/quic-go | v0.61.1-0.20260806010916-184d081eef3e | MIT |
| github.com/cloudflare/circl | v1.6.5 | BSD-3-Clause |
| github.com/ghodss/yaml | v1.0.1-0.20220118164431-d8423dcdf344 | MIT AND BSD-3-Clause |
| github.com/google/btree | v1.1.3 | Apache-2.0 |
| github.com/google/gopacket | v1.1.19 | BSD-3-Clause |
| github.com/google/uuid | v1.6.0 | BSD-3-Clause |
| github.com/gorilla/websocket | v1.5.3 | BSD-2-Clause |
| github.com/huin/goupnp | v1.2.0 | BSD-2-Clause |
| github.com/jackpal/go-nat-pmp | v1.0.2 | Apache-2.0 |
| github.com/juju/ratelimit | v1.0.2 | LGPL-3.0-only WITH LGPL-3.0-linking-exception |
| github.com/klauspost/compress | v1.18.2 | BSD-3-Clause AND Apache-2.0 AND MIT |
| github.com/klauspost/cpuid/v2 | v2.4.0 | MIT |
| github.com/koron/go-ssdp | v0.0.4 | MIT |
| github.com/libp2p/go-nat | v1.0.1-0.20250821073202-01afc089f138 | Apache-2.0 |
| github.com/libp2p/go-netroute | v0.2.1 | BSD-3-Clause |
| github.com/miekg/dns | v1.1.73 | BSD-3-Clause |
| github.com/pelletier/go-toml | v1.9.5 | MIT AND Apache-2.0 |
| github.com/pion/dtls/v3 | v3.1.5 | MIT |
| github.com/pion/logging | v0.2.4 | MIT |
| github.com/pion/stun/v3 | v3.1.7 | MIT AND BSD-3-Clause |
| github.com/pion/transport/v4 | v4.1.0 | MIT AND BSD-3-Clause |
| github.com/pires/go-proxyproto | v0.15.0 | Apache-2.0 |
| github.com/quic-go/qpack | v0.6.0 | MIT |
| github.com/refraction-networking/utls | v1.8.3-0.20260301010127-aa6edf4b11af | BSD-3-Clause |
| github.com/robfig/cron/v3 | v3.0.1 | MIT |
| github.com/sagernet/sing | v0.7.13 | GPL-3.0-or-later AND Apache-2.0 |
| github.com/sagernet/sing-shadowsocks | v0.2.9 | GPL-3.0-or-later |
| github.com/vishvananda/netlink | v1.3.1 | Apache-2.0 |
| github.com/vishvananda/netns | v0.0.5 | Apache-2.0 |
| github.com/wlynxg/anet | v0.0.5 | BSD-3-Clause |
| github.com/xtls/reality | v0.0.0-20260908062103-8cdf7bf9c7f0 | MPL-2.0 AND BSD-3-Clause |
| github.com/xtls/xray-core | v1.260327.1-0.20260908222543-52a412d9e2f5 | MPL-2.0 |
| go4.org/netipx | v0.0.0-20231129151722-fdeea329fbba | BSD-3-Clause |
| golang.org/x/crypto | v0.57.0 | BSD-3-Clause |
| golang.org/x/exp | v0.0.0-20240506185415-9bf2ced13842 | BSD-3-Clause |
| golang.org/x/mobile | v0.0.0-20260908204917-8b95e45f8d3e | BSD-3-Clause |
| golang.org/x/net | v0.59.0 | BSD-3-Clause |
| golang.org/x/sync | v0.23.0 | BSD-3-Clause |
| golang.org/x/sys | v0.48.0 | BSD-3-Clause |
| golang.org/x/text | v0.42.0 | BSD-3-Clause |
| golang.org/x/time | v0.14.0 | BSD-3-Clause |
| golang.zx2c4.com/wireguard | v0.0.0-20250521234502-f333402bd9cb | MIT |
| google.golang.org/genproto/googleapis/rpc | v0.0.0-20260526163538-3dc84a4a5aaa | Apache-2.0 |
| google.golang.org/grpc | v1.83.2 | Apache-2.0 |
| google.golang.org/protobuf | v1.36.12 | BSD-3-Clause |
| gopkg.in/yaml.v2 | v2.4.0 | Apache-2.0 AND MIT |
| gvisor.dev/gvisor | v0.0.0-20260122175437-89a5d21be8f0 | Apache-2.0 AND MIT AND BSD-3-Clause |
| lukechampine.com/blake3 | v1.4.1 | MIT |

### Tunnel: hev-socks5-tunnel (lib/*/libhev-socks5-tunnel.so, lib/*/libhevsockstun.so)

Built from source by `compile-hevtun.sh`. Full texts: `hev-socks5-tunnel.txt`, `GPL-2.0.txt`.

| Component | Version | License | Copyright |
|---|---|---|---|
| hev-socks5-tunnel <https://github.com/heiher/hev-socks5-tunnel> | commit `64cc609f945253b0e9ebc56317d544268f3c68c1` | MIT | Copyright (c) 2022 hev |
| hev-socks5-core <https://github.com/heiher/hev-socks5-core> | 1.6.4, `162dd996299fc2d2bff2dd63728f8a2cd71ed31a` | MIT | Copyright (c) 2022 hev |
| hev-task-system <https://github.com/heiher/hev-task-system> | 5.10.3, `328f35d903221b51811b3d02b277d665dfbdc75f` | MIT | Copyright (c) 2022 hev |
| Red-black tree code in the two projects above, derived from Linux `lib/rbtree.c` | same | GPL-2.0-or-later | (C) 1999 Andrea Arcangeli, (C) 2002 David Woodhouse, (C) 2012 Michel Lespinasse |
| lwIP, fork <https://github.com/heiher/lwip> of <https://savannah.nongnu.org/projects/lwip/> | 2.2.1.7, `2a11c14c7a32887af25a034e82ef18b0b12076ac` | BSD-3-Clause | Copyright (c) 2001, 2002 Swedish Institute of Computer Science, and the authors named in the source files |
| libyaml, fork <https://github.com/heiher/yaml> of <https://github.com/yaml/libyaml> | 0.2.5.2, `efa36117a8646d26d12b58e05bac472d7854a70d` | MIT | Copyright (c) 2022 hev; Copyright (c) 2017-2020 Ingy döt Net; Copyright (c) 2006-2016 Kirill Simonov |

## 2. Maven dependencies with additional notices

- **MMKV** (`com.tencent:mmkv-static` 1.3.16, <https://github.com/Tencent/MMKV>): BSD-3-Clause,
  Copyright (C) 2018 THL A29 Limited, a Tencent company. `libmmkv.so` contains code from OpenSSL
  1.1.0i under the OpenSSL License and the original SSLeay License (full text: `MMKV-LICENSE.txt`):
  - This product includes software developed by the OpenSSL Project for use in the OpenSSL
    Toolkit (http://www.openssl.org/).
  - This product includes cryptographic software written by Eric Young (eay@cryptsoft.com). This
    product includes software written by Tim Hudson (tjh@cryptsoft.com).

  Compatibility of these terms with the GPL is under review; see docs/licensing-audit.md.
- **OkHttp** (`com.squareup.okhttp3:okhttp` 5.4.0): Apache-2.0. Its `PublicSuffixDatabase.list` is
  compiled from the Public Suffix List, <https://publicsuffix.org/list/public_suffix_list.dat>,
  and is subject to the terms of the Mozilla Public License, v. 2.0 (`MPL-2.0.txt`).
- **CameraX core** (`androidx.camera:camera-core` 1.6.1): Apache-2.0; its native library includes
  code from libyuv, <https://chromium.googlesource.com/libyuv/libyuv/>, BSD-3-Clause, Copyright
  2011 The LibYuv Project Authors.
- **Desugared JDK library** (`com.android.tools:desugar_jdk_libs` 2.1.5, packaged by the Android
  build): GNU General Public License version 2 with the Classpath Exception (full text:
  `GPL-2.0-with-Classpath-exception.txt`); source: <https://github.com/google/desugar_jdk_libs>.
  `desugar_jdk_libs_configuration` 2.1.5: BSD-3-Clause, Copyright (c) 2016, the R8 project authors.
- **kotlinx.coroutines** (NOTICE carried by `androidx.concurrent:concurrent-futures-ktx`):
  kotlinx.coroutines library. Copyright 2016-2024 JetBrains s.r.o and contributors.
- **Jakarta Dependency Injection** (`jakarta.inject:jakarta.inject-api` 2.0.1): Jakarta Dependency
  Injection is a trademark of the Eclipse Foundation. Source:
  <https://github.com/eclipse-ee4j/injection-api>.

All other Maven dependencies below are licensed under the Apache License 2.0 (`Apache-2.0.txt`).
Copyright belongs to their respective authors, as stated in each project; the in-app page
"Open-source licenses" lists each library with its project URL.

## 3. Routing data bundled in libv2ray.aar (assets)

| File | sha256 | Source | License |
|---|---|---|---|
| `geoip.dat` | `4149e607530f91da697bad4696f8c59f0a475af38e69405e4124438c9886c721` | Loyalsoldier/geoip release 202609040655, <https://github.com/Loyalsoldier/geoip> | CC-BY-SA-4.0 and GPL-3.0; contains MaxMind GeoLite data |
| `geoip-only-cn-private.dat` | `60fe632fc24e762f3847b77aef56fb40b7a90533b48d668395c78d4e3cb6647b` | same | same |
| `geosite.dat` | `110be548fd2c2d84249310842667f996e01ab5c753d526717a0f434a5a08e5ea` | Loyalsoldier/v2ray-rules-dat release 202609082347, <https://github.com/Loyalsoldier/v2ray-rules-dat> | GPL-3.0; compiled from the lists below |

- This product includes GeoLite Data created by MaxMind, available from <https://www.maxmind.com>.
  Use of GeoLite Data is governed by the GeoLite End User License Agreement,
  <https://www.maxmind.com/en/geolite/eula>. The data incorporates GeoNames geographical data
  (<https://www.geonames.org>), available under the Creative Commons Attribution 4.0 License.
- IP lists of mainland China: gaoyifan/china-operator-ip <https://github.com/gaoyifan/china-operator-ip>, MIT.
- `geosite.dat` includes: v2fly/domain-list-community (MIT, Copyright (c) 2018-2019 V2Ray) via
  Loyalsoldier/domain-list-custom (MIT); felixonmars/dnsmasq-china-list (WTFPL); GFWList (LGPL-2.1)
  via cokebar/gfwlist2dnsmasq (GPL-3.0); EasyList and EasyList China (The EasyList authors,
  GPL-3.0-or-later or CC-BY-SA-3.0-or-later); AdGuard DNS filter (GPL-3.0); crazy-max/
  WindowsSpyBlocker (MIT); Peter Lowe's ad server list <https://pgl.yoyo.org/adservers>; Dan
  Pollock's hosts file <https://someonewhocares.org/hosts>.

The licensing of the bundled routing data is under review; see docs/licensing-audit.md.

## 4. Application resources

- Material Design icons and Material Symbols, <https://github.com/google/material-design-icons>,
  Apache-2.0, Google LLC.
- The GitHub mark links translator profiles; it is a trademark of GitHub, Inc.
- `assets/proxy_package_name` is the list from <https://github.com/2dust/androidpackagenamelist>,
  GPL-3.0.
- Other icons, artwork, translations and routing presets originate from v2rayNG (GPL-3.0) or were
  created for Zapl Lab.

## 5. Maven dependency inventory

Generated from the gradle-license-plugin report of the release variants
(`scripts/check_licenses.py --markdown`). Kotlin Multiplatform libraries are listed under their
root coordinates.

| Artifact | Version | License |
|---|---|---|
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 |
| `androidx.activity:activity-ktx` | 1.13.0 | Apache-2.0 |
| `androidx.activity:activity` | 1.13.0 | Apache-2.0 |
| `androidx.annotation:annotation-experimental` | 1.4.1 | Apache-2.0 |
| `androidx.annotation:annotation` | 1.10.0 | Apache-2.0 |
| `androidx.appcompat:appcompat-resources` | 1.7.1 | Apache-2.0 |
| `androidx.appcompat:appcompat` | 1.7.1 | Apache-2.0 |
| `androidx.arch.core:core-common` | 2.2.0 | Apache-2.0 |
| `androidx.arch.core:core-runtime` | 2.2.0 | Apache-2.0 |
| `androidx.autofill:autofill` | 1.0.0 | Apache-2.0 |
| `androidx.camera.featurecombinationquery:featurecombinationquery` | 1.6.1 | Apache-2.0 |
| `androidx.camera.viewfinder:viewfinder-compose` | 1.6.1 | Apache-2.0 |
| `androidx.camera.viewfinder:viewfinder-core` | 1.6.1 | Apache-2.0 |
| `androidx.camera:camera-camera2-pipe` | 1.6.1 | Apache-2.0 |
| `androidx.camera:camera-camera2` | 1.6.1 | Apache-2.0 |
| `androidx.camera:camera-compose` | 1.6.1 | Apache-2.0 |
| `androidx.camera:camera-core` | 1.6.1 | Apache-2.0 AND BSD-3-Clause |
| `androidx.camera:camera-lifecycle` | 1.6.1 | Apache-2.0 |
| `androidx.collection:collection-ktx` | 1.5.0 | Apache-2.0 |
| `androidx.collection:collection` | 1.5.0 | Apache-2.0 |
| `androidx.compose.animation:animation-core` | 1.11.4 | Apache-2.0 |
| `androidx.compose.animation:animation` | 1.11.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation-layout` | 1.11.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation` | 1.11.4 | Apache-2.0 |
| `androidx.compose.material3:material3` | 1.4.0 | Apache-2.0 |
| `androidx.compose.material:material-ripple` | 1.11.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-annotation` | 1.11.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-retain` | 1.11.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-saveable` | 1.11.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-geometry` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-graphics` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-text` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-tooling-preview` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-unit` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui-util` | 1.11.4 | Apache-2.0 |
| `androidx.compose.ui:ui` | 1.11.4 | Apache-2.0 |
| `androidx.compose:compose-bom` | 2026.06.01 | Apache-2.0 |
| `androidx.concurrent:concurrent-futures-ktx` | 1.2.0 | Apache-2.0 |
| `androidx.concurrent:concurrent-futures` | 1.2.0 | Apache-2.0 |
| `androidx.core:core-backported-fixes` | 1.0.0 | Apache-2.0 |
| `androidx.core:core-ktx` | 1.19.0 | Apache-2.0 |
| `androidx.core:core-viewtree` | 1.0.0 | Apache-2.0 |
| `androidx.core:core` | 1.19.0 | Apache-2.0 |
| `androidx.cursoradapter:cursoradapter` | 1.0.0 | Apache-2.0 |
| `androidx.customview:customview-poolingcontainer` | 1.0.0 | Apache-2.0 |
| `androidx.customview:customview` | 1.0.0 | Apache-2.0 |
| `androidx.documentfile:documentfile` | 1.0.0 | Apache-2.0 |
| `androidx.drawerlayout:drawerlayout` | 1.0.0 | Apache-2.0 |
| `androidx.dynamicanimation:dynamicanimation` | 1.0.0 | Apache-2.0 |
| `androidx.emoji2:emoji2-views-helper` | 1.4.0 | Apache-2.0 |
| `androidx.emoji2:emoji2` | 1.4.0 | Apache-2.0 |
| `androidx.exifinterface:exifinterface` | 1.4.2 | Apache-2.0 |
| `androidx.fragment:fragment` | 1.5.4 | Apache-2.0 |
| `androidx.graphics:graphics-path` | 1.0.1 | Apache-2.0 |
| `androidx.interpolator:interpolator` | 1.0.0 | Apache-2.0 |
| `androidx.legacy:legacy-support-core-utils` | 1.0.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common-java8` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core-ktx` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-process` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-service` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.11.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.11.0 | Apache-2.0 |
| `androidx.loader:loader` | 1.0.0 | Apache-2.0 |
| `androidx.localbroadcastmanager:localbroadcastmanager` | 1.0.0 | Apache-2.0 |
| `androidx.navigationevent:navigationevent-compose` | 1.0.0 | Apache-2.0 |
| `androidx.navigationevent:navigationevent` | 1.0.0 | Apache-2.0 |
| `androidx.print:print` | 1.0.0 | Apache-2.0 |
| `androidx.profileinstaller:profileinstaller` | 1.4.0 | Apache-2.0 |
| `androidx.resourceinspection:resourceinspection-annotation` | 1.0.1 | Apache-2.0 |
| `androidx.room:room-common` | 2.7.0 | Apache-2.0 |
| `androidx.room:room-runtime` | 2.7.0 | Apache-2.0 |
| `androidx.savedstate:savedstate-compose` | 1.4.0 | Apache-2.0 |
| `androidx.savedstate:savedstate-ktx` | 1.4.0 | Apache-2.0 |
| `androidx.savedstate:savedstate` | 1.4.0 | Apache-2.0 |
| `androidx.sqlite:sqlite-framework` | 2.5.0 | Apache-2.0 |
| `androidx.sqlite:sqlite` | 2.5.0 | Apache-2.0 |
| `androidx.startup:startup-runtime` | 1.2.0 | Apache-2.0 |
| `androidx.tracing:tracing-ktx` | 1.3.0 | Apache-2.0 |
| `androidx.tracing:tracing` | 1.3.0 | Apache-2.0 |
| `androidx.transition:transition` | 1.6.0 | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable-animated` | 1.1.0 | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable` | 1.1.0 | Apache-2.0 |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 | Apache-2.0 |
| `androidx.viewpager:viewpager` | 1.0.0 | Apache-2.0 |
| `androidx.window:window-core` | 1.5.0 | Apache-2.0 |
| `androidx.window:window` | 1.5.0 | Apache-2.0 |
| `androidx.work:work-multiprocess` | 2.11.2 | Apache-2.0 |
| `androidx.work:work-runtime-ktx` | 2.11.2 | Apache-2.0 |
| `androidx.work:work-runtime` | 2.11.2 | Apache-2.0 |
| `com.google.accompanist:accompanist-drawablepainter` | 0.32.0 | Apache-2.0 |
| `com.google.auto.value:auto-value-annotations` | 1.6.3 | Apache-2.0 |
| `com.google.code.gson:gson` | 2.14.0 | Apache-2.0 |
| `com.google.dagger:dagger` | 2.59 | Apache-2.0 |
| `com.google.errorprone:error_prone_annotations` | 2.48.0 | Apache-2.0 |
| `com.google.guava:listenablefuture` | 1.0 | Apache-2.0 |
| `com.google.zxing:core` | 3.5.4 | Apache-2.0 |
| `com.squareup.okhttp3:okhttp` | 5.4.0 | Apache-2.0 |
| `com.squareup.okio:okio` | 3.17.0 | Apache-2.0 |
| `com.tencent:mmkv-static` | 1.3.16 | BSD-3-Clause |
| `io.coil-kt:coil-base` | 2.7.0 | Apache-2.0 |
| `io.coil-kt:coil-compose-base` | 2.7.0 | Apache-2.0 |
| `io.coil-kt:coil-compose` | 2.7.0 | Apache-2.0 |
| `io.coil-kt:coil` | 2.7.0 | Apache-2.0 |
| `jakarta.inject:jakarta.inject-api` | 2.0.1 | Apache-2.0 |
| `javax.inject:javax.inject` | 1 | Apache-2.0 |
| `org.jetbrains.androidx.lifecycle:lifecycle-common` | 2.9.5 | Apache-2.0 |
| `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose` | 2.9.5 | Apache-2.0 |
| `org.jetbrains.androidx.lifecycle:lifecycle-runtime` | 2.9.5 | Apache-2.0 |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` | 2.9.5 | Apache-2.0 |
| `org.jetbrains.androidx.savedstate:savedstate-compose` | 1.3.5 | Apache-2.0 |
| `org.jetbrains.androidx.savedstate:savedstate` | 1.3.5 | Apache-2.0 |
| `org.jetbrains.compose.animation:animation-core` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.animation:animation` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.annotation-internal:annotation` | 1.8.0 | Apache-2.0 |
| `org.jetbrains.compose.collection-internal:collection` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.foundation:foundation-layout` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.foundation:foundation` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.runtime:runtime-saveable` | 1.9.2 | Apache-2.0 |
| `org.jetbrains.compose.runtime:runtime` | 1.9.2 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui-geometry` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui-graphics` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui-text` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui-unit` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui-util` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.compose.ui:ui` | 1.7.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | 2.4.10 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | 1.9.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 1.9.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.4.10 | Apache-2.0 |
| `org.jetbrains.kotlinx:atomicfu` | 0.28.0 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom` | 1.11.0 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.11.0 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom` | 1.7.3 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.7.3 | Apache-2.0 |
| `org.jetbrains:annotations` | 23.0.0 | Apache-2.0 |
| `org.jspecify:jspecify` | 1.0.0 | Apache-2.0 |
| `sh.calvin.reorderable:reorderable` | 3.1.0 | Apache-2.0 |
