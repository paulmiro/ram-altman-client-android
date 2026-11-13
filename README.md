# RAM Altman (Android)

A tiny Android app that periodically reports your phone's memory usage to the
[RAM dashboard](https://y7v.de) (`PUT /api/v1/report/{username}/{hostname}`).

## Installation with Obtainium

1. Install [Obtainium](https://github.com/ImranR98/Obtainium).
2. In Obtainium, tap **Add App**.
3. Paste `https://github.com/paulmiro/ram-altman-client-android` as the source URL.
4. Tap **Add**, then **Install**.

Obtainium will notify you about new releases and update the app.

Alternatively, download the APK from the
[releases page](https://github.com/paulmiro/ram-altman-client-android/releases) and install it manually.

## Usage

1. Open the app and fill in:
   - **Server URL**: defaults to `https://y7v.de`
   - **API token**: the shared token of the server
   - **Username**: your name on the dashboard
   - **Hostname**: name of this device (letters, digits, `.`, `_`, `-`)
   - **Color** (optional): your dashboard color, e.g. `#3d8bff`
   - **Interval**: minutes between reports (default 1)
   - **Timeout**: minutes the dashboard keeps this device after a report (1-60, default 5).
     Keep it above the interval, or the device will disappear between reports.
2. Allow notifications and the battery exemption when asked, then turn on **Report memory usage**.
   The first report is sent right away. While reporting, a persistent notification is shown;
   Android requires it to keep the app running in the background.

Everything is saved as you type. The line under the switch shows the result of the last report.
Turn the switch off to stop reporting.

## Building

Requires JDK 21 and the Android SDK.

```sh
./gradlew assembleDebug
```

## Releasing

Bump `appVersion` in `app/build.gradle.kts` and push to `main`.
The [release workflow](.github/workflows/release.yml) builds a signed APK and creates a GitHub release `v<version>`.

It needs these repository secrets:

| Secret              | Content                         |
|---------------------|---------------------------------|
| `KEYSTORE_BASE64`   | `base64 -w0 release.jks`        |
| `KEYSTORE_PASSWORD` | keystore password               |
| `KEY_ALIAS`         | key alias                       |
| `KEY_PASSWORD`      | key password                    |
