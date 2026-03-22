# Contributing

Thanks for your interest in improving Walkie LAN.

## Development setup

1. Install Node.js and npm
2. Run `npm install`
3. Start the local services with `npm run dev:server` and `npm run dev:web`
4. For Android changes, open `apps/android-host` in Android Studio

## Before opening a pull request

- Keep changes focused
- Run `npm run build`
- Run `npm test`
- If you changed the Android app, build the debug APK with `.\gradlew.bat :app:assembleDebug`
- Update docs when behavior changes

## Pull requests

- Use a short descriptive title
- Explain what changed and how it was tested
- Mention limitations or follow-up work if something is still incomplete

## Issues

When reporting a bug, include:

- device and OS
- browser, if relevant
- exact steps to reproduce
- logs or screenshots when possible
