# OLYMPUS – GODS OF THUNDER

Android slot-style game prototype inspired by Greek mythology. It uses virtual credits only; there are no real-money bets or payouts.

## Features in this first build
- Portrait Android interface
- 6 × 5 gemstone board
- Smooth vertical reel animation with staggered stopping
- Virtual credits and adjustable bet
- Initial win detection and reward feedback
- Jetpack Compose rendering

## Build
Open the project in Android Studio, or run:

`gradle assembleDebug`

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Every push to `main` builds the debug APK and uploads it as an artifact.
