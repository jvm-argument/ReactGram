package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;

import com.reactgram.ReactGramConfig;
import com.reactgram.liquidglass.GlassTuning;

/**
 * Glass look of chat message bubbles. Bubbles are drawn straight into the chat list, so there is no per-bubble backdrop
 * to refract; instead the fill is translucent (the chat background shows through) and three optical layers are added:
 * a soft top-down sheen, an inner glow hugging the edge and a thin bright rim that is brightest at the top.
 */
public final class BubbleGlass {
    private BubbleGlass() {}

    private static final Matrix matrix = new Matrix();
    // Unit-height gradients; scaled to each bubble through the shader's local matrix, so nothing is allocated per draw.
    private static final LinearGradient sheenShader = new LinearGradient(0, 0, 0, 1,
        new int[]{0x40FFFFFF, 0x0CFFFFFF, 0x00FFFFFF}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
    private static final LinearGradient rimShader = new LinearGradient(0, 0, 0, 1,
        new int[]{0xFFFFFFFF, 0x66FFFFFF, 0xAAFFFFFF}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
    private static final Paint sheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        sheenPaint.setShader(sheenShader);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setShader(rimShader);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setShader(rimShader);
    }

    /** Opacity multiplier for bubble fills, 1 = unchanged. */
    public static float opacity() {
        ReactGramConfig.load();
        return GlassTuning.bubbleOpacity;
    }

    /** True when bubbles are drawn as glass (the path route is then used instead of the opaque 9-patch). */
    public static boolean active() {
        return opacity() < 1f;
    }

    /** Draws the glass layers over a bubble already filled; {@code alpha} is the bubble's own 0..255 alpha. */
    public static void drawGlass(Canvas canvas, Path path, Rect bounds, int alpha) {
        final float k = GlassTuning.rim * Math.min(1f, alpha / 255f);
        if (k <= 0f || bounds.height() <= 0) {
            return;
        }
        matrix.setScale(1f, bounds.height());
        matrix.postTranslate(0f, bounds.top);
        sheenShader.setLocalMatrix(matrix);
        rimShader.setLocalMatrix(matrix);

        canvas.save();
        canvas.clipPath(path);
        sheenPaint.setAlpha(Math.round(255 * Math.min(1f, k)));
        canvas.drawRect(bounds, sheenPaint);

        // inner glow: a wide stroke, only its inner half is visible because of the clip
        glowPaint.setStrokeWidth(dpf2(5f));
        glowPaint.setAlpha(Math.round(70 * Math.min(1.5f, k)));
        canvas.drawPath(path, glowPaint);
        canvas.restore();

        rimPaint.setStrokeWidth(dpf2(1f));
        rimPaint.setAlpha(Math.round(150 * Math.min(1.5f, k)));
        canvas.drawPath(path, rimPaint);
    }
}
