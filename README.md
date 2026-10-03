# CalculatorVault

A calculator-style Android privacy vault.

## Features

- Normal calculator UI
- Three-dot calculator menu
- Ordinary calculator settings
- Secret PIN entry opens the private space
- Separate three-dot private-space menu
- Separate private-space settings
- Change private PIN from inside private settings
- AES-256-GCM encryption for vault files
- Android Keystore protected vault encryption key
- Explicit "Encrypt a file" action in private settings
- Multiple-file encryption through the system document picker
- App-private internal storage for encrypted vault data
- Private Apps shortcut page
- Screenshot protection with Android FLAG_SECURE

## Encryption

Vault files are encrypted with **AES-256-GCM** before being written into the app's private internal storage. The AES key is generated and kept in Android Keystore.

The app does not use ECB or other unauthenticated encryption modes for vault storage.

Android's app-specific internal storage is sandboxed from other apps, making it appropriate for sensitive app-only data. The encrypted vault adds another layer on top of that storage boundary.

## Settings layout

### Calculator

The public calculator settings contain normal calculator options such as button vibration, button sounds, appearance information, and app information.

### Private space

Private settings are only reachable after the PIN unlocks the private space. They contain:

- Private PIN change
- AES encryption status
- Android Keystore key status
- Encrypt-a-file action
- Encrypted-file count
- Internal-storage status
- Screenshot protection status

## Android app hiding

The project does **not** use hidden/private Android APIs to secretly remove other apps from a launcher. The Apps page currently provides private shortcuts.

True launcher-level hiding is controlled by Android's supported profile/private-space mechanisms or by a launcher application with the appropriate role. That can be added later without pretending a normal app has capabilities Android does not grant it.

## Build

Open the repository in Android Studio and let Gradle sync.

Command-line build:

```bash
gradle --no-daemon :app:assembleDebug
```

The project targets SDK 36 and has a minimum SDK of 26.

Package:

`com.leon.calculatorvault`
