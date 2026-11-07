# ReactGram

A modified Telegram for Android client (based on [DrKLO/Telegram](https://github.com/DrKLO/Telegram)) with:

- **Ghost Mode** — hide read receipts, online status, typing/upload status and story views
  (derived from [AyuGram4A](https://github.com/AyuGram/AyuGram4A); see `NOTICE-ReactGram.md`).
- ReactGram branding and icon.
- Liquid Glass work in progress (see `docs/`).

## Building
JDK 17, Android SDK 36 + NDK 27.2.12479018, then:

```
git submodule update --init --depth 1
./gradlew :TMessagesProj_App:assembleAfatDebug
```

Put your own `api_id`/`api_hash` (https://my.telegram.org) in `~/.gradle/gradle.properties`:

```
TG_APP_ID=...
TG_APP_HASH=...
```

Licensed under the GNU GPL, as the upstream projects (see `LICENSE`).
