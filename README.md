# OLYMPUS — GODS OF THUNDER

Android slot-style game prototype inspired by Greek mythology, built with Kotlin and Jetpack Compose. This is a fictional game using virtual credits only: no deposits, real-money bets, or cash payouts.

## Enhanced edition

The enhanced implementation is on the `feature/olympus-enhanced` branch.

- Portrait-first interface with a dark temple palette and gold accents.
- 6 × 5 gemstone board with animated vertical reel presentation.
- Adjustable virtual bet (5–100 credits).
- Virtual credit balance, persisted across activity recreation.
- Session spin counter and best-win record.
- Tiered reward multipliers based on matching-symbol count.
- In-game paytable explaining the reward tiers.
- Credit reset control for continued testing.
- Android APK build through GitHub Actions.

## Reward table

| Matching symbols | Reward |
|---:|---:|
| 8 | 2× bet |
| 9 | 3× bet |
| 10 | 5× bet |
| 11 | 8× bet |
| 12–14 | 12× bet |
| 15 or more | 30× bet |

The board is randomly generated for entertainment. Rewards are virtual and have no monetary value.

## Build

Open the project in Android Studio with Android SDK 35 and JDK 17, or run:

```bash
gradle assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

Pushes to `main` and pull requests targeting `main` build the debug APK. The workflow uploads it as the `OLYMPUS-debug-apk` artifact.

## Original version

The original prototype remains unchanged on the `main` branch.
