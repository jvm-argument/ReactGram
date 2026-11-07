package com.reactgram.liquidglass;

/**
 * Everything the glass shader needs. All lengths are in pixels.
 * One instance is reused per glass surface, so updating it allocates nothing.
 */
public final class GlassParams {
    // Geometry: rounded rectangle in the coordinate space of the node the effect is applied to.
    public float resolutionX, resolutionY;
    public float centerX, centerY;
    public float halfWidth, halfHeight;
    /** Corner radii: right-bottom, right-top, left-bottom, left-top (the order the shader expects). */
    public float radiusRightBottom, radiusRightTop, radiusLeftBottom, radiusLeftTop;

    // Refraction (Snell's law on a rounded bevel).
    public float thickness;
    public float refractIntensity = 0.75f;
    public float refractIndex = 1.5f;

    /** Premultiplied ARGB tint laid over the refracted backdrop. */
    public int tintColor;

    // Look taken from the iOS LiquidGlassShapes layer style (rim / saturation / matte).
    /** Width of the edge highlight, in pixels. */
    public float rimWidth;
    /** Opacity of the edge highlight, 0..1. */
    public float rimStrength = 0.5f;
    /** Direction the light comes from, in screen space (does not need to be normalized). */
    public float lightX = -0.6f, lightY = -0.8f;
    /** Extra saturation of the refracted backdrop, 0 = unchanged. */
    public float saturation = 0.25f;
    /** White lift under the tint (iOS "matte"), 0..1. */
    public float matte = 0.04f;

    /** Copies all fields; returns true if anything differs enough to need new uniforms. */
    public boolean copyFrom(GlassParams o) {
        boolean changed =
            differs(resolutionX, o.resolutionX) || differs(resolutionY, o.resolutionY)
            || differs(centerX, o.centerX) || differs(centerY, o.centerY)
            || differs(halfWidth, o.halfWidth) || differs(halfHeight, o.halfHeight)
            || differs(radiusRightBottom, o.radiusRightBottom) || differs(radiusRightTop, o.radiusRightTop)
            || differs(radiusLeftBottom, o.radiusLeftBottom) || differs(radiusLeftTop, o.radiusLeftTop)
            || differs(thickness, o.thickness) || differs(refractIntensity, o.refractIntensity)
            || differs(refractIndex, o.refractIndex) || tintColor != o.tintColor
            || differs(rimWidth, o.rimWidth) || differs(rimStrength, o.rimStrength)
            || differs(lightX, o.lightX) || differs(lightY, o.lightY)
            || differs(saturation, o.saturation) || differs(matte, o.matte);
        if (changed) {
            resolutionX = o.resolutionX; resolutionY = o.resolutionY;
            centerX = o.centerX; centerY = o.centerY;
            halfWidth = o.halfWidth; halfHeight = o.halfHeight;
            radiusRightBottom = o.radiusRightBottom; radiusRightTop = o.radiusRightTop;
            radiusLeftBottom = o.radiusLeftBottom; radiusLeftTop = o.radiusLeftTop;
            thickness = o.thickness; refractIntensity = o.refractIntensity; refractIndex = o.refractIndex;
            tintColor = o.tintColor;
            rimWidth = o.rimWidth; rimStrength = o.rimStrength;
            lightX = o.lightX; lightY = o.lightY;
            saturation = o.saturation; matte = o.matte;
        }
        return changed;
    }

    private static boolean differs(float a, float b) {
        return Math.abs(a - b) > 0.01f;
    }
}
