package com.reactgram.liquidglass;

import android.os.Build;

/** What the device can do; callers fall back to plain blur + tint when {@link #supportsRefraction()} is false. */
public final class GlassCapabilities {
    private GlassCapabilities() {}

    /** AGSL {@code RuntimeShader} / {@code RenderEffect.createRuntimeShaderEffect} need API 33. */
    public static boolean supportsRefraction() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
