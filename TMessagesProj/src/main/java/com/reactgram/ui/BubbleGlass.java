package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.reactgram.ReactGramConfig;
import com.reactgram.liquidglass.GlassTuning;

/** Glass look of chat message bubbles: translucent fill (chat background shows through) and a thin edge highlight. */
public final class BubbleGlass {
    private BubbleGlass() {}

    private static final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static {
        rimPaint.setStyle(Paint.Style.STROKE);
    }

    /** Opacity multiplier for bubble fills, 1 = unchanged. */
    public static float opacity() {
        ReactGramConfig.load();
        return GlassTuning.bubbleOpacity;
    }

    /** Draws the edge highlight along a bubble path; {@code alpha} is the bubble's own 0..255 alpha (crossfades). */
    public static void drawRim(Canvas canvas, Path path, int alpha) {
        final int a = Math.round(0x55 * GlassTuning.rim * Math.min(1f, alpha / 255f));
        if (a <= 0) {
            return;
        }
        rimPaint.setStrokeWidth(dpf2(1f));
        rimPaint.setColor((a << 24) | 0x00FFFFFF);
        canvas.drawPath(path, rimPaint);
    }
}
