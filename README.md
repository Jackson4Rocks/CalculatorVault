# CalculatorVault

A calculator-style Android privacy vault.

## Current features

- Normal calculator UI
- PIN setup and verification
- Secret PIN entry through the calculator opens the vault
- App-private vault storage
- AES-256-GCM encryption with an Android Keystore key
- Import multiple files with Android's system document picker
- Delete protected vault items
- Private Apps page for launching installed apps from inside the vault
- Screenshot protection with Android's `FLAG_SECURE`

## Android app hiding

The project does **not** use hidden/private Android APIs to secretly remove other apps from a launcher. The Apps page currently provides private shortcuts.

True launcher-level hiding is controlled by Android's supported profile/private-space mechanisms or by a launcher application with the appropriate role. That can be added later without pretending the normal app has capabilities Android does not grant it.

## Build

Open the repository in Android Studio and let Gradle sync.

Command-line build:

```bash
gradle --no-daemon :app:assembleDebug
```

The project currently targets SDK 36, has a minimum SDK of 26, and uses Jetpack Compose Material 3.

## Package

`com.leon.calculatorvault`
