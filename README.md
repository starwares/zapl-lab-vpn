# Zapl Lab

Zapl Lab is an Android client for the [Xray core](https://github.com/XTLS/Xray-core) that can
turn the VPN on and off automatically depending on the application on screen.

> Zapl Lab is an independent modified distribution of the open-source
> [v2rayNG](https://github.com/2dust/v2rayNG) project by 2dust and the v2rayNG contributors.
> It is **not** an official v2rayNG release and is not endorsed by the v2rayNG authors.
> What was changed is listed in [NOTICE](NOTICE) and [CHANGELOG.md](CHANGELOG.md).

## Download

Releases, their signed APKs and the matching source archives are published at
<https://github.com/starwares/zapl-lab-vpn/releases>. Every release tag identifies the exact
source revision of its APKs; see [docs/releasing.md](docs/releasing.md).

## License

Zapl Lab is free software, distributed under the
[GNU General Public License version 3](LICENSE), the license of v2rayNG. You may use, study,
share and modify it, including commercially, under the terms of that license. There is no
warranty.

- [NOTICE](NOTICE): origin, modifications and attribution.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md): bundled third-party components, their
  licenses and required attributions. The same texts are shown in the app under
  About → Notices and third-party components.
- [PRIVACY.md](PRIVACY.md): what the Android client does with data.
- [docs/licensing-audit.md](docs/licensing-audit.md): licensing audit of this repository.

Online services that may be offered for use with the client are separate programs with their
own terms; those terms do not restrict your rights under the GPL for this client.

## Building from source

The build reproduces [.github/workflows/build.yml](.github/workflows/build.yml). Tool versions
are pinned there and in [V2rayNG/gradle/libs.versions.toml](V2rayNG/gradle/libs.versions.toml).

Requirements: JDK 21, Android SDK platform and build-tools 37, NDK `29.0.14206865`, Git.

```sh
git clone --recurse-submodules https://github.com/starwares/zapl-lab-vpn.git
cd zapl-lab-vpn

# 1. Native tunnel (hev-socks5-tunnel) from the submodule
export NDK_HOME="$ANDROID_HOME/ndk/29.0.14206865"
bash compile-hevtun.sh
cp -r libs V2rayNG/app

# 2. Xray core library: the libv2ray.aar release matching the AndroidLibXrayLite submodule
TAG=$(git -C AndroidLibXrayLite describe --tags --abbrev=0)
curl -L -o V2rayNG/app/libs/libv2ray.aar \
  "https://github.com/2dust/AndroidLibXrayLite/releases/download/$TAG/libv2ray.aar"

# 3. APKs
cd V2rayNG
./gradlew assemblePlaystoreDebug
```

`libv2ray.aar` can also be built from the `AndroidLibXrayLite` submodule with gomobile; see
that project's README and [docs/releasing.md](docs/releasing.md).

## Upstream

v2rayNG documentation also applies to the features Zapl Lab inherits:
<https://github.com/2dust/v2rayNG/wiki>. Please report issues of this distribution at
<https://github.com/starwares/zapl-lab-vpn/issues>, not to the v2rayNG project.
