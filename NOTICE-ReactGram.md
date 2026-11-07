# ReactGram — third-party notices

ReactGram is a modified build of **Telegram for Android** (https://github.com/DrKLO/Telegram),
Copyright Nikolai Kudashov and contributors, licensed under GPL-2.0-or-later (see `LICENSE`).

## Ghost Mode — derived from AyuGram for Android
`TMessagesProj/src/main/java/com/reactgram/ghost/*` is derived from the Ghost Mode logic of
**AyuGram for Android** (https://github.com/AyuGram/AyuGram4A): `AyuConfig`, `AyuState`/`AyuStateVariable`,
`AyuGhostUtils` and the request hook in `ConnectionsManager`.
Copyright @Radolyn, 2023, licensed under GPL-2.0-or-later. Only the Ghost Mode behaviour was taken
(read-receipt, online-status, typing/upload and story-view suppression, read-after-send);
none of AyuGram's UI, sync or message-saving code is included.

## Liquid Glass
The `liquidglass` module re-implements, in AGSL, the visual principles of the Liquid Glass components of
**Telegram iOS** (https://github.com/TelegramMessenger/Telegram-iOS, GPL-2.0-or-later). No Metal code is copied.
