# ReactGram — Liquid Glass & Ghost Mode architecture

Based on Telegram for Android 13.1.1 (7237). Research sources: Telegram-iOS
(`submodules/TelegramUI/Components/LiquidGlassShapes`, `GlassBackgroundComponent`) and AyuGram4A.

## 1. Finding: Telegram Android already ships Liquid Glass

| Piece | Where (Android) |
|---|---|
| Backdrop capture + blur | `ui/Components/blur3/source/*`, `ViewGroupPartRenderer`, `DownscaleScrollableNoiseSuppressor` |
| Glass surface drawable | `blur3/drawable/BlurredBackgroundDrawableRenderNode` (RenderNode fill + outline clip + top/bottom stroke + shadow) |
| Refraction shader (AGSL, API 33+) | previously `res/raw/liquid_glass_shader.agsl` via `blur3/LiquidGlassEffect` |
| Factory / wiring | `BlurredBackgroundDrawableViewFactory`, `utils/glass/GlassEngine`, `LiteMode.FLAG_LIQUID_GLASS` |
| Glass tabs | `ui/Components/glass/GlassTabView`, `GlassTabsView`, `MainTabsActivity`, `MainTabsLayout` |
| Current users | main tab bar, chat input/top bars (`ChatActivity`), attach sheet (`ChatAttachAlert`), `EmojiView`, `CallLogActivity`, `ChannelAdminLogActivity` |

So the work is **not** building glass from scratch but closing the gap to iOS and extracting it into a module.

## 2. iOS vs Android, effect by effect

iOS (`LiquidGlassShapes.metal`): a signed distance field per shape; content near the edge is pulled inward by a
displacement map (`shape.inwardNormal * glassShift * falloff²`, falloff over `glassBand`), the backdrop is blurred and masked
to the shape union, then tint gradient, white matte, saturation multiply, a constant-width rim line (`rimWidth`,
dimmed by `rimDarkAppearanceScale` in dark mode) and a soft shadow are composed. Crest/radial kernels (call bar waves,
recording blob) animate non-rectangular shapes.

| Effect | iOS | Android before | Android now (`:liquidglass`) |
|---|---|---|---|
| Edge refraction | displacement map from SDF normal, quadratic falloff | rounded-rect SDF + Snell refraction on a bevel (physically based) | unchanged (kept; already equivalent in purpose) |
| Backdrop blur | `CALayer` blur filter, masked | blur3 downscale blur in RenderNode | reused |
| Tint | gradient colour fill | `foreground_color_premultiplied` src-over | reused |
| Edge highlight | rim line of constant width, per layer | two stroked paths (top/bottom) | **added in shader:** SDF-based rim, light-direction dependent, dimmer in dark theme |
| Saturation | multiply layer | none | **added:** `saturation` (default 0.25) |
| White matte | matte layer under tint | none | **added:** `matte` + inner bevel lift |
| Shadow | blurred coverage of outer shape | `Paint.setShadowLayer` | reused |
| Crest / radial animated shapes | Metal compute kernels | not present | **not ported** |
| Touch/press deformation (`TouchEffect`) | spring-driven | `GlassTabView` animations | not changed |

## 3. Module layout

`liquidglass/` (Gradle library, `com.reactgram.liquidglass`, no dependency on Telegram code):

- `GlassShaders` — AGSL source.
- `GlassParams` — mutable value object, change detection (no allocation per frame).
- `GlassShaderEffect` — owns the `RuntimeShader`; recreates the `RenderEffect` only when params change.
- `GlassCapabilities` — API gate (`RuntimeShader` needs API 33; below that callers keep blur + tint).

`TMessagesProj` depends on it; `blur3/LiquidGlassEffect` is now a thin adapter that fills `GlassParams`.

## 3a. Context menus and previews (bitmap sources)

Menus that blur a snapshot of the screen (`ItemOptions`, `ScrimOptions`, `PollItemMenu`, `TodoItemMenu`,
`MessageSendPreview`, `ContentPreviewViewer`, `AvatarPreviewer`, the message and profile scrims) used
`BlurredBackgroundSourceBitmap`, whose drawable was a plain `BlurredBackgroundDrawableSource` without the shader.
`BlurredBackgroundSourceBitmap.withLiquidGlass()` now makes it create a `BlurredBackgroundDrawableRenderNode` with the
glass effect (API 33+ and `LiteMode.FLAG_LIQUID_GLASS`), and invalidates those drawables when the bitmap or matrix changes.
The opt-in is a one-line change at each construction site; other bitmap users (wallpaper, passcode) are untouched.

Already on glass before this work and left as is: `DialogsActivity`, `MainTabsActivity`, `ChatActivity` bars and input,
`ChatAttachAlert`, `ShareAlert`, `EmojiView`, `ProfileActivity`, `TopicsFragment`. Not on glass: the generic
`BottomSheet`/`AlertDialog` and `ActionBarPopupWindow` menus opened from the action bar (3-dot menus).

## 4. Ghost Mode (from AyuGram4A)

AyuGram intercepts requests in `ConnectionsManager.sendRequestInternal`. ReactGram does the same through one call,
`com.reactgram.ghost.GhostRequestFilter.intercept/wrap`:

- read receipts (`messages.readHistory`, `channels.readHistory`, `messages.readDiscussion`, `readMessageContents`,
  `readEncryptedHistory`) are answered locally with a fake `affectedMessages` (pts = -1) so the UI marks chats read;
- `messages.setTyping` / `setEncryptedTyping` dropped;
- `account.updateStatus` forced to `offline = true`;
- story views (`stories.readStories`, `incrementStoryViews`) dropped — an addition over AyuGram;
- read-after-send: after a send request completes, one real `readHistory` is let through (`GhostState`);
- settings: `GhostConfig` (SharedPreferences `ghostconfig`) and `GhostSettingsActivity` (Settings → Ghost Mode).

Not taken from AyuGram: scheduled-send trick, message history saving, sync, UI.

## 5. Verification status

Built with JDK 17, SDK 36, NDK 27.2 (arm64). Installed and run on an Android 13 device: tab bar renders, Ghost Mode
screen and switches work, no shader errors in logcat. **Not verified:** frame rate under scroll, refraction quality on
other GPUs, Ghost Mode effect against a second account (no controlled second client was available).
