package com.reactgram.liquidglass;

/**
 * User-adjustable look of the glass. Scales are relative to the built-in defaults (1.0 = unchanged),
 * so every surface keeps its own base values and only the user's preference is applied on top.
 */
public final class GlassTuning {
    private GlassTuning() {}

    /** Telegram's own tint is ~85% opaque; ReactGram defaults to a much clearer glass (matches the settings preview). */
    public static final float DEFAULT_TINT = 0.35f;
    public static final float DEFAULT_REFRACTION = 1f;
    public static final float DEFAULT_THICKNESS = 1f;
    public static final float DEFAULT_RIM = 1f;
    /** Opacity of message bubbles, 1 = Telegram's own (opaque). */
    public static final float DEFAULT_BUBBLE = 0.6f;
    public static final float DEFAULT_SATURATION = 0.25f;

    /** Multiplier of the tint (colour overlay) opacity: 0 = fully clear glass. */
    public static volatile float tint = DEFAULT_TINT;
    /** Multiplier of how strongly the backdrop is bent at the edge. */
    public static volatile float refraction = DEFAULT_REFRACTION;
    /** Multiplier of the bevel width. */
    public static volatile float thickness = DEFAULT_THICKNESS;
    /** Multiplier of the edge highlight opacity. */
    public static volatile float rim = DEFAULT_RIM;
    /** Opacity multiplier of chat message bubbles so the chat background shows through. */
    public static volatile float bubbleOpacity = DEFAULT_BUBBLE;
    /** Extra saturation of the backdrop, absolute. */
    public static volatile float saturation = DEFAULT_SATURATION;

    /** Bumped on every change so that already-recorded glass surfaces know they must redraw. */
    public static volatile int version;

    public static void touch() {
        version++;
    }

    public static void reset() {
        tint = DEFAULT_TINT;
        refraction = DEFAULT_REFRACTION;
        thickness = DEFAULT_THICKNESS;
        rim = DEFAULT_RIM;
        saturation = DEFAULT_SATURATION;
        bubbleOpacity = DEFAULT_BUBBLE;
        touch();
    }

    /** Scales the alpha channel of an ARGB colour. */
    public static int scaleAlpha(int color, float scale) {
        final int a = Math.min(255, Math.max(0, Math.round((color >>> 24) * scale)));
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
