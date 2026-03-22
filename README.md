# Walkie LAN

Open-source hybrid LAN walkie-talkie for small teams.

Android hosts the room and offers the best operator experience. Web clients join by short code, which makes it possible to test with iPhone users through the browser while keeping the control flow simple.

## Current status

This repository is an MVP that already supports:

- Android host creates a room with a short code
- Web client joins by room code
- Presence, channel state, and event log
- Push-to-talk control flow with a single speaker per channel
- WebRTC signaling between Android and web
- Local debugging with an Android emulator, a real Android phone, and browser clients

This repository does not yet provide:

- End-to-end encryption
- Offline iPhone microphone support without HTTPS
- A production deployment bundle with a hosted signaling backend out of the box
- Play Store / App Store packaging

## Repository layout

```text
apps/
  android-host/       Android app in Kotlin + Jetpack Compose
  signaling-server/   REST + WebSocket bootstrap/signaling server
  web-client/         React + Vite web app / PWA
packages/
  protocol/           Shared protocol schemas and types
```

## Requirements

- Node.js 20+
- npm 10+
- JDK 17+ for Android builds
- Android Studio + Android SDK for APK builds
- Windows, macOS, or Linux for server/web development

## Quick start

### 1. Install dependencies

```bash
npm install
```

The shared `@walkie/protocol` package is built automatically after install so the server and web client can start from a clean clone.

### 2. Start the signaling server

```bash
npm run dev:server
```

Default server URL:

```text
http://localhost:8787
```

Health endpoint:

```text
http://localhost:8787/health
```

### 3. Start the web client

In another terminal:

```bash
npm run dev:web
```

Default web URL:

```text
http://localhost:5173
```

### 4. Build the Android debug APK

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

APK output:

```text
apps/android-host/app/build/outputs/apk/debug/app-debug.apk
```

### 5. Test locally

Use the same Wi-Fi network for every real device involved.

For the Android app:

- On the Android emulator, use `http://10.0.2.2:8787`
- On a real Android phone, use your computer IP, for example `http://192.168.1.29:8787`

For the web client:

- Open `http://YOUR_PC_IP:5173` from another device on the same network

## Real-device testing notes

### Android phone

1. Install the APK
2. Start the signaling server on your computer
3. Open the Android app
4. Fill the server URL with your computer IP and port `8787`
5. Create a room

### Browser client

1. Open the web client from another device
2. Point it to the signaling server URL
3. Join with the room code shown by the host

### iPhone / Safari

For real microphone capture in Safari/WebKit, the web page should be served over HTTPS. Local HTTP can be enough for flow validation, but production-like iPhone audio tests should use a secure origin.

## Environment variables

The signaling server supports:

- `PORT`
- `PUBLIC_HTTP_BASE_URL`
- `PUBLIC_WS_BASE_URL`

See [.env.example](.env.example).

## Useful commands

```bash
npm run build
npm test
```

Android:

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

Install with `adb`:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r apps\android-host\app\build\outputs\apk\debug\app-debug.apk
```

## Troubleshooting

### The Android emulator cannot reach the server

Use:

```text
http://10.0.2.2:8787
```

### A real phone cannot reach the server

- Make sure the phone and computer are on the same Wi-Fi
- Use your computer LAN IP, not `localhost`
- Make sure the server is running
- Check Windows Firewall for port `8787`

### The web app seems outdated after a fix

- Stop old `npm run dev:server` and `npm run dev:web` processes
- Start both again
- Hard refresh the browser
- If installed as a PWA, remove it and open it again

### The Android app crashes while testing

Capture logs:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat
```

## Open source

This project is released under the MIT license. See [LICENSE](LICENSE).

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## CI

GitHub Actions is included in [.github/workflows/ci.yml](.github/workflows/ci.yml) to run build and test checks on pushes and pull requests.
