# Releasing Zapl Lab

Every published APK/AAB must be traceable to one Git tag, one commit and a published source
archive:

```text
APK/AAB 2.3.8 (versionCode 748)
        ↕  build-info.txt, About screen shows the commit
Git tag v2.3.8  →  commit SHA
        ↕
zapl-lab-vpn-v2.3.8-source.tar.gz (repository + submodules + Go module sources)
```

## Identity of a release

| Item | Where it is defined | Rule |
|---|---|---|
| Tag | Git | `v<versionName>`, created on the exact commit that is built |
| Commit SHA | Git | recorded in `build-info.txt`; `BuildConfig.SOURCE_COMMIT` (About screen) |
| `versionName`, `versionCode` | `V2rayNG/app/build.gradle.kts` | bump both before tagging; the update check compares the tag with `versionName` |
| APK version codes | `applicationVariants` block in `build.gradle.kts` | Play Store: `4000000 + versionCode`; F-Droid: `100 × versionCode + ABI digit + 5000000` |
| Xray core | `AndroidLibXrayLite` gitlink; `libv2ray.aar` release asset of the same tag | sha256 recorded in `build-info.txt` and in `THIRD_PARTY_NOTICES.md` |
| hev-socks5-tunnel | `hev-socks5-tunnel` gitlink, built by `compile-hevtun.sh` | commit recorded in `build-info.txt` |
| Gradle | `V2rayNG/gradle/wrapper/gradle-wrapper.properties` | |
| AGP, Kotlin, libraries | `V2rayNG/gradle/libs.versions.toml` | |
| Android SDK, NDK, cmdline-tools | `.github/workflows/build.yml` | compileSdk/targetSdk/minSdk in `build.gradle.kts` |
| JDK | `.github/workflows/build.yml` (`temurin 21`) | |
| Go (source archive only) | `AndroidLibXrayLite/go.mod` | |

Versions of the current revision, for reference: Gradle 9.5.1, AGP 9.3.2, Kotlin 2.4.10,
compileSdk/targetSdk 37, minSdk 24, build-tools 37.0.0, NDK 29.0.14206865, JDK 21,
AndroidLibXrayLite v26.9.9 (`d0c6c4ae1b09c912070c8288bd0dbcc2e492ac29`, Xray-core
`52a412d9e2f5c2a5142b1b4e2ab3771dacb8b120`, Go 1.27.1), hev-socks5-tunnel
`64cc609f945253b0e9ebc56317d544268f3c68c1`.

## Procedure

1. Update `CHANGELOG.md` (move *Unreleased* under the new version; name the upstream base commit
   and native component versions) and `NOTICE` if the list of major modifications changed.
2. If dependencies, submodules or bundled data changed, re-run the audit steps in
   [licensing-audit.md](licensing-audit.md#maintaining-the-audit) and update
   `THIRD_PARTY_NOTICES.md`, `third_party/licenses/` and `third_party/license-policy.json`.
3. Bump `versionCode` and `versionName`; commit.
4. Confirm the **License audit** workflow is green for that commit.
5. Tag and push: `git tag -a v<versionName> -m "Zapl Lab <versionName>" && git push origin v<versionName>`.
6. Run the **Build APK** workflow on the tag (`workflow_dispatch`, ref = the tag,
   `release_tag` = the tag). The workflow:
   - fails unless the checked-out commit is the tagged commit;
   - creates `zapl-lab-vpn-<tag>-source.tar.gz` (tracked files of the repository and submodules,
     plus `go mod vendor` of `AndroidLibXrayLite`) before any build step touches the tree;
   - runs `scripts/check_licenses.py --release`, which fails while any component is marked
     unresolved in `third_party/license-policy.json`;
   - builds signed APKs and writes `build-info.txt`;
   - uploads APKs, `build-info.txt`, the source archive and their GPG signatures to the release.
7. Publish the release notes with a link to the source archive and to the tag.
8. Keep the release and its source archive available for as long as the APK is distributed
   (GPL-3.0 §6).

Signing keys, keystore passwords and the GPG private key are GitHub Actions secrets
(`APP_KEYSTORE_BASE64`, `APP_KEYSTORE_PASSWORD`, `APP_KEYSTORE_ALIAS`, `APP_KEY_PASSWORD`,
`GPG_PRIVATE_KEY`). They are never committed; GPL-3.0 does not require publishing them.

## Store builds (AAB)

The workflow builds APKs only. An AAB for a store must be built from the same tag with
`./gradlew bundlePlaystoreRelease`, and its `build-info.txt` data recorded in the store release
notes or attached to the GitHub release.

## Verifying a published APK

```sh
aapt2 dump badging zapl-lab-vpn_<version>_universal.apk | head -1   # package, versionCode, versionName
unzip -p zapl-lab-vpn_<version>_universal.apk assets/legal/notices.html | head
```

About → the version block shows `<applicationId> · <commit>`; the commit must equal the tag's
commit in `build-info.txt`. Android APK signing and the store's re-signing make byte-for-byte
comparison with a local build impractical; the tag, the commit and the recorded native component
checksums are the reference.

## Release checklist

```text
[ ] APK corresponds to a specific commit/tag (build-info.txt, About screen)
[ ] source code of this version is published (tag + source archive)
[ ] GPL-3.0 text present (LICENSE, About → License)
[ ] NOTICE is current
[ ] THIRD_PARTY_NOTICES.md is current
[ ] new dependencies were reviewed (License audit workflow)
[ ] no proprietary or GPL-incompatible SDK
[ ] branding does not present the app as official v2rayNG
[ ] updater disabled (UPDATE_CHECK_ENABLED = false) or pointing to github.com/starwares/zapl-lab-vpn releases; always disabled in store builds
[ ] signing key not published
[ ] no secrets in the repository or its history
[ ] source URL reachable by users (repository is public)
[ ] CHANGELOG.md lists the changes
[ ] versionCode/versionName consistent with the tag
[ ] License audit CI green; check_licenses.py --release passes
```
