# Zapl Lab privacy policy (Android client)

> **Draft — legal review required before store submission.** This document describes what
> the Zapl Lab Android client does, as verified against its source code. It does not cover
> online services (accounts, subscriptions, server allocation) that may be offered
> separately; such services must publish their own privacy policy and terms.

Last updated: 2026-09-13. Applies to the source revision published at
<https://github.com/starwares/zapl-lab-vpn>.

## Summary

- The client contains no analytics, advertising, crash-reporting or tracking SDKs.
- The client does not send your data to the Zapl Lab developers.
- Network traffic is sent to the proxy servers **you** configure, or that the subscriptions
  you add provide.

## Data processed on the device

| Data | Why | Leaves the device? |
|---|---|---|
| Server profiles, subscriptions, routing rules, settings | Core function | Only if you export, share or back them up (clipboard, QR code, file, WebDAV server you configure) |
| List of installed applications (`QUERY_ALL_PACKAGES`) | Per-app proxy selection and Automatic VPN control policies | No |
| Foreground application events (Usage access, only when you enable Automatic VPN control and grant the permission) | Deciding whether the VPN should be on for the app on screen | No |
| Camera image (only while scanning a QR code) | Importing a profile | No |
| Application logs | Troubleshooting | Only if you share them |

## Network connections made by the client

| Destination | When | Data sent |
|---|---|---|
| Proxy servers from your profiles or subscriptions | While the VPN or proxy is running, and during connection tests | Your network traffic, as with any VPN |
| Subscription URLs you add | When subscriptions are updated (manually or on a schedule you enable) | Standard HTTP request, including the client's User-Agent |
| `api.github.com`, `github.com` (repository `starwares/zapl-lab-vpn`) | When you check for updates | Standard HTTP request |
| `github.com`, `raw.githubusercontent.com` (geo data and app-list sources you select) | When you download routing data or the per-app proxy list | Standard HTTP request |
| Connection test URL (default `https://www.gstatic.com/generate_204`) | When you test server latency | Standard HTTP request through the proxy |
| IP information URL (default `https://api.ip.sb/geoip`) | When you check the outbound IP | Standard HTTP request through the proxy |
| DNS servers configured in the settings | While the VPN is running | DNS queries |

Every default URL above can be changed in the settings.

## Third parties

App stores that distribute the client (for example Google Play, RuStore, Huawei AppGallery)
may collect data under their own privacy policies.

## Contact

Open an issue at <https://github.com/starwares/zapl-lab-vpn/issues>.
