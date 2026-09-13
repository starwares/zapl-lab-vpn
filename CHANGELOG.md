# Changelog

All notable changes of Zapl Lab relative to its upstream, v2rayNG, are listed here.
Upstream changes are not repeated; see <https://github.com/2dust/v2rayNG/commits/master>.

Each release entry names the Git tag, the upstream base commit and the bundled native
component versions, so that the published APK can be matched to its source revision.
The release procedure is described in [docs/releasing.md](docs/releasing.md).

## [Unreleased]

Upstream base: v2rayNG commit `173f60a1dabe57e822a56381931be6354f7c7f61` (versionName 2.3.8).

### Added

- Automatic VPN control by the application on screen: per-application policies
  (through VPN, without VPN, always turn VPN off), foreground detection through
  usage access, split-screen handling, configurable turn-off delay, option to keep
  the VPN while the screen is off, a monitor service restored after boot and updates.
- About screen: attribution to the upstream project, the GNU GPL v3 text, and the
  notices of bundled third-party components; the build shows its source commit.
- `NOTICE`, `THIRD_PARTY_NOTICES.md` with full third-party license texts, `PRIVACY.md`,
  licensing audit and release documentation.
- CI: dependency license check; release builds verify the tag and publish a corresponding
  source archive and `build-info.txt` next to the APKs.

### Changed

- Product identity: application name Zapl Lab, application ID `store.zapl.lab.vpn`
  (F-Droid flavor: `store.zapl.lab.vpn.fdroid`), launcher icons, splash screen, drawer
  logo, APK file names; the main screen marks test builds.
- Source code, issue tracker and privacy policy links point to
  <https://github.com/starwares/zapl-lab-vpn>.
- The in-app update check reads releases of this distribution instead of upstream
  v2rayNG releases, and is disabled (`UPDATE_CHECK_ENABLED = false`) until those releases
  are published.
- "Minimal Trust" visual design: navy and slate palette with green reserved for the
  connected state, updated typography and shapes; dynamic color is off by default.
- Main screen: a connection card with the status (tap to test the connection), a start/stop
  button and the selected profile replaces the bottom status bar and floating button;
  profiles are shown as cards, with share, edit and delete in the overflow menu.
- Vietnamese strings no longer name the product "v2rayNG".
- The bundled open-source licenses page is regenerated from the current dependencies.
- Core service teardown unregisters its receiver exactly once, so stopping the VPN
  service from `onDestroy` is idempotent.

### Removed

- Upstream promotion link in the navigation drawer.
- Link to the upstream Telegram channel.
- Unused Montserrat Thin font.
- "Check for update" entry in the navigation drawer while the update check is disabled.
