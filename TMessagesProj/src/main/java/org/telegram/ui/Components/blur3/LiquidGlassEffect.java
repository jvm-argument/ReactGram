package org.telegram.ui.Components.blur3;

import android.graphics.RenderNode;

import androidx.annotation.RequiresApi;

import com.reactgram.liquidglass.GlassParams;
import com.reactgram.liquidglass.GlassShaderEffect;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * Thin adapter between the blur3 drawables and the {@code :liquidglass} module.
 * Keeps the original call signature; the shader itself lives in {@link GlassShaderEffect}.
 */
@RequiresApi(api = 33)
public class LiquidGlassEffect {

    private final GlassShaderEffect effect;
    private final GlassParams params = new GlassParams();
    private final RenderNode node;

    public LiquidGlassEffect(RenderNode node) {
        this.node = node;
        this.effect = new GlassShaderEffect(node);
    }

    public void update(
        float left, float top, float right, float bottom,
        float radiusLeftTop, float radiusRightTop, float radiusRightBottom, float radiusLeftBottom,

        float thickness,
        float intensity,
        float index,
        int foregroundColor
    ) {
        final float width = right - left, height = bottom - top;

        if (radiusLeftTop + radiusLeftBottom > height) {
            float a = radiusLeftTop / (radiusLeftTop + radiusLeftBottom);
            radiusLeftTop = height * a;
            radiusLeftBottom = height * (1.0f - a);
        }
        if (radiusRightTop + radiusRightBottom > height) {
            float a = radiusRightTop / (radiusRightTop + radiusRightBottom);
            radiusRightTop = height * a;
            radiusRightBottom = height * (1.0f - a);
        }

        final GlassParams p = params;
        p.resolutionX = node.getWidth();
        p.resolutionY = node.getHeight();
        p.centerX = (left + right) / 2;
        p.centerY = (top + bottom) / 2;
        p.halfWidth = width / 2;
        p.halfHeight = height / 2;
        p.radiusRightBottom = radiusRightBottom;
        p.radiusRightTop = radiusRightTop;
        p.radiusLeftBottom = radiusLeftBottom;
        p.radiusLeftTop = radiusLeftTop;
        p.thickness = thickness;
        p.refractIntensity = intensity;
        p.refractIndex = index;
        p.tintColor = foregroundColor;

        // iOS-style edge light: a thin highlight, dimmer on dark themes (LiquidGlassAppearance.rimDarkAppearanceScale).
        p.rimWidth = Math.max(AndroidUtilities.dpf2(1.25f), 1f);
        p.rimStrength = Theme.isCurrentThemeDark() ? 0.35f : 0.55f;

        effect.update(p);
    }
}
