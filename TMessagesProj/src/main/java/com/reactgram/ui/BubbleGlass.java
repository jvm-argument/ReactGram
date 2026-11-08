package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.reactgram.ReactGramConfig;
import com.reactgram.liquidglass.GlassTuning;

import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;

import java.util.WeakHashMap;

/**
 * Liquid glass for chat message bubbles, built from the same pieces as the other glass elements
 * ({@link BlurredBackgroundDrawableRenderNode} + the {@code :liquidglass} shader), backed by the chat wallpaper.
 * Every bubble cell owns one glass drawable; it is drawn clipped to the bubble path (tail included).
 * When the real glass is not available, the older optical layers ({@link #drawGlass}) are used.
 */
public final class BubbleGlass {
    private BubbleGlass() {}

    // ---- chat context (set by ChatActivity while a chat is on screen) ----
    private static BlurredBackgroundSource chatSource;
    private static ViewGroup chatParent;

    public static void setChat(BlurredBackgroundSource source, ViewGroup parent) {
        chatSource = source;
        chatParent = parent;
    }

    public static void clearChat(ViewGroup parent) {
        if (chatParent == parent) {
            chatSource = null;
            chatParent = null;
        }
    }

    // ---- per-cell glass ----
    private static final class Entry {
        BlurredBackgroundDrawableRenderNode glass;
        int tint;
        int radius = -1;
        boolean invalid;
    }

    private static final WeakHashMap<View, Entry> entries = new WeakHashMap<>();

    /** Opacity multiplier for bubble fills, 1 = unchanged. */
    public static float opacity() {
        ReactGramConfig.load();
        return GlassTuning.bubbleOpacity;
    }

    /** True when bubbles are drawn as glass instead of the opaque original. */
    public static boolean active() {
        return opacity() < 1f;
    }

    /**
     * Draws the real glass for the bubble of {@code cell} instead of its fill.
     * Returns false when the caller must draw the bubble the usual way.
     */
    public static boolean draw(View cell, Canvas canvas, MessageDrawable drawable, MessageDrawable.PathDrawParams params,
                               int fillColor, int alpha) {
        final BlurredBackgroundSource source = chatSource;
        final ViewGroup parent = chatParent;
        if (source == null || parent == null || !active() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !canvas.isHardwareAccelerated() || !LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS)) {
            return false;
        }
        final Rect bounds = drawable.getBounds();
        if (bounds.isEmpty()) {
            return false;
        }

        Entry e = entries.get(cell);
        if (e == null) {
            e = new Entry();
            e.invalid = !isDescendant(cell, parent);
            if (!e.invalid) {
                final Entry entry = e;
                e.glass = new BlurredBackgroundDrawableRenderNode(source);
                e.glass.setLiquidGlassEffectAllowed();
                e.glass.setColorProvider(new BlurredBackgroundProviderBuilder(null)
                    .setBackgroundColor((r, isDark) -> entry.tint)
                    .setStrokeColorTop(0x66FFFFFF, 0x55FFFFFF)
                    .setStrokeColorBottom(0x30FFFFFF, 0x26FFFFFF)
                    .setShadowColor(0, 0)
                    .setStrokeWidth(dpf2(2 / 3f), dpf2(2 / 3f))
                    .build());
            }
            entries.put(cell, e);
        }
        if (e.invalid) {
            return false;
        }

        // The tint the glass shader will apply is scaled by the user's tint setting; compensate so that the bubble
        // opacity slider alone decides how solid the bubble is.
        final float tintScale = Math.max(GlassTuning.tint, 0.05f);
        final int tintAlpha = Math.round(Math.min(1f, opacity() / tintScale) * (fillColor >>> 24));
        final int tint = (fillColor & 0x00FFFFFF) | (tintAlpha << 24);
        if (tint != e.tint) {
            e.tint = tint;
            e.glass.updateColors();
        }
        final int radius = dp(SharedConfig.bubbleRadius);
        if (radius != e.radius) {
            e.radius = radius;
            e.glass.setRadius(radius);
        }

        final Path path = drawable.makePath(params);
        final float[] off = offsetInParent(cell, parent);
        e.glass.setSourceOffset(off[0], off[1]);
        e.glass.setAlpha(alpha);
        e.glass.setBounds(bounds);

        canvas.save();
        canvas.clipPath(path);
        e.glass.draw(canvas);
        canvas.restore();
        // Over the smooth blurred wallpaper there is little to refract, so add the glass highlights on top.
        drawGlass(canvas, path, bounds, alpha);
        return true;
    }

    private static final float[] offsetTmp = new float[2];

    /** Position of {@code view}'s top-left corner in the coordinates of {@code parent}, scrolling and translation included. */
    private static float[] offsetInParent(View view, ViewGroup parent) {
        float x = 0, y = 0;
        View v = view;
        while (v != null && v != parent) {
            x += v.getX();
            y += v.getY();
            final ViewParent p = v.getParent();
            if (!(p instanceof View) || p == parent) {
                break;
            }
            x -= ((View) p).getScrollX();
            y -= ((View) p).getScrollY();
            v = (View) p;
        }
        offsetTmp[0] = x;
        offsetTmp[1] = y;
        return offsetTmp;
    }

    private static boolean isDescendant(View view, ViewGroup parent) {
        ViewParent p = view.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    // ---- fallback: optical layers when the real glass cannot be used ----
    private static final Matrix matrix = new Matrix();
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

    /** Draws sheen, inner glow and rim over a bubble already filled; {@code alpha} is the bubble's own 0..255 alpha. */
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
        glowPaint.setStrokeWidth(dpf2(5f));
        glowPaint.setAlpha(Math.round(70 * Math.min(1.5f, k)));
        canvas.drawPath(path, glowPaint);
        canvas.restore();

        rimPaint.setStrokeWidth(dpf2(1f));
        rimPaint.setAlpha(Math.round(150 * Math.min(1.5f, k)));
        canvas.drawPath(path, rimPaint);
    }
}
