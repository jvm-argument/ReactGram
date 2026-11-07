package com.reactgram.liquidglass;

import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.RuntimeShader;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Applies the liquid glass shader to a {@link RenderNode} that records the (blurred) backdrop.
 * The shader and the {@link RenderEffect} are created once; uniforms are rewritten only when {@link GlassParams} change.
 */
@RequiresApi(api = 33)
public final class GlassShaderEffect {
    private final RenderNode node;
    private final RuntimeShader shader = new RuntimeShader(GlassShaders.LIQUID_GLASS);
    private final GlassParams applied = new GlassParams();
    private boolean hasApplied;

    public GlassShaderEffect(@NonNull RenderNode node) {
        this.node = node;
        node.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "img"));
    }

    /** Pushes {@code params} to the shader if they changed since the last call. */
    public void update(@NonNull GlassParams params) {
        if (!applied.copyFrom(params) && hasApplied) {
            return;
        }
        hasApplied = true;

        final float a = Color.alpha(params.tintColor) / 255f;
        final float r = Color.red(params.tintColor) / 255f * a;
        final float g = Color.green(params.tintColor) / 255f * a;
        final float b = Color.blue(params.tintColor) / 255f * a;

        shader.setFloatUniform("resolution", params.resolutionX, params.resolutionY);
        shader.setFloatUniform("center", params.centerX, params.centerY);
        shader.setFloatUniform("size", params.halfWidth, params.halfHeight);
        shader.setFloatUniform("radius", params.radiusRightBottom, params.radiusRightTop, params.radiusLeftBottom, params.radiusLeftTop);
        shader.setFloatUniform("thickness", Math.max(params.thickness, 1f));
        shader.setFloatUniform("refract_intensity", params.refractIntensity);
        shader.setFloatUniform("refract_index", params.refractIndex);
        shader.setFloatUniform("foreground_color_premultiplied", r, g, b, a);
        shader.setFloatUniform("rim_width", params.rimWidth);
        shader.setFloatUniform("rim_strength", params.rimStrength);
        shader.setFloatUniform("light_dir", params.lightX, params.lightY);
        shader.setFloatUniform("saturation", params.saturation);
        shader.setFloatUniform("matte", params.matte);
        // A new effect object is required for the node to pick up changed uniforms.
        node.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "img"));
    }
}
