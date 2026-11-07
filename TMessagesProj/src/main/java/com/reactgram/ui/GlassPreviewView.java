package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

import com.reactgram.liquidglass.GlassTuning;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.LiquidGlassEffect;

/**
 * Shows how the glass will look over a chat: a scrolling bubble scene with a glass top bar, input field and
 * round button drawn by the same {@link LiquidGlassEffect} the app uses, so the settings sliders act live.
 */
public class GlassPreviewView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();

    private final RenderNode sceneNode = Build.VERSION.SDK_INT >= 29 ? new RenderNode("GlassPreviewScene") : null;
    // Kept clear on purpose: the glass refracts the sharp scene, which is the look the preview was approved with.
    private final RenderNode blurNode = null;
    private boolean blurEffectSet;
    private final Piece topBar = new Piece();
    private final Piece input = new Piece();
    private final Piece button = new Piece();

    private ValueAnimator animator;
    private float scroll;

    private static final int[][] BUBBLES = {
        // {outgoing (1) / incoming (0), width in dp, lines}
        {0, 190, 2}, {1, 150, 1}, {0, 230, 3}, {1, 210, 2}, {0, 140, 1}, {1, 240, 3}, {0, 200, 2},
    };

    public GlassPreviewView(Context context) {
        super(context);
        textPaint.setTextSize(dp(15));
        shadowPaint.setColor(0);
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            scroll = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(300), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        final int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        final boolean dark = Theme.isCurrentThemeDark();
        final int tint = dark ? 0x59000000 : 0x66FFFFFF;

        clip.reset();
        rect.set(0, 0, w, h);
        clip.addRoundRect(rect, dp(18), dp(18), Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);

        if (sceneNode != null && canvas.isHardwareAccelerated()) {
            sceneNode.setPosition(0, 0, w, h);
            final Canvas sc = sceneNode.beginRecording();
            drawScene(sc, w, h, dark);
            sceneNode.endRecording();
            canvas.drawRenderNode(sceneNode);
            if (blurNode != null) {
                blurNode.setPosition(0, 0, w, h);
                if (!blurEffectSet) {
                    blurNode.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(dp(4), dp(4), Shader.TileMode.CLAMP));
                    blurEffectSet = true;
                }
                final Canvas bc = blurNode.beginRecording();
                bc.drawRenderNode(sceneNode);
                blurNode.endRecording();
            }
        } else {
            drawScene(canvas, w, h, dark);
        }

        topBar.set(dp(12), dp(12), w - dp(12), dp(12) + dp(52), dp(26));
        input.set(dp(12), h - dp(12) - dp(52), w - dp(12) - dp(60), h - dp(12), dp(26));
        button.set(w - dp(12) - dp(52), h - dp(12) - dp(52), w - dp(12), h - dp(12), dp(26));

        drawPiece(canvas, topBar, tint);
        drawPiece(canvas, input, tint);
        drawPiece(canvas, button, tint);

        final int textColor = dark ? Color.WHITE : 0xFF1B1B1B;
        textPaint.setColor(textColor);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("ReactGram", topBar.l + dp(20), topBar.t + topBar.height() / 2f + dp(5), textPaint);
        textPaint.setFakeBoldText(false);
        textPaint.setColor((textColor & 0x00FFFFFF) | 0xA0000000);
        canvas.drawText("Message", input.l + dp(20), input.t + input.height() / 2f + dp(5), textPaint);

        // paper plane-ish glyph on the round button
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        final float cx = button.l + button.width() / 2f, cy = button.t + button.height() / 2f;
        clip.reset();
        clip.moveTo(cx - dp(9), cy - dp(8));
        clip.lineTo(cx + dp(10), cy);
        clip.lineTo(cx - dp(9), cy + dp(8));
        clip.lineTo(cx - dp(5), cy);
        clip.close();
        canvas.drawPath(clip, paint);

        canvas.restore();
    }

    private void drawScene(Canvas c, int w, int h, boolean dark) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, w, h,
            dark ? new int[]{0xFF1E3A4C, 0xFF3B2A52, 0xFF14202E} : new int[]{0xFF7BC4D6, 0xFFB79BE0, 0xFFE5C99A},
            null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // soft doodle circles, like a chat wallpaper
        paint.setColor(0x22FFFFFF);
        for (int i = 0; i < 9; i++) {
            c.drawCircle((i * 97 % 330) / 330f * w, ((i * 61 + 40) % 300) / 300f * h, dp(18 + (i % 3) * 10), paint);
        }

        // bubbles scroll upward in a loop
        final float lineH = dp(20);
        float total = 0;
        for (int[] b : BUBBLES) {
            total += dp(16) + b[2] * lineH + dp(8);
        }
        float y = h - (scroll * total) + dp(10);
        for (int rep = 0; rep < 2; rep++) {
            for (int[] b : BUBBLES) {
                final float bw = Math.min(dp(b[1]), w - dp(60));
                final float bh = dp(16) + b[2] * lineH;
                final boolean out = b[0] == 1;
                final float l = out ? w - dp(12) - bw : dp(12);
                rect.set(l, y, l + bw, y + bh);
                paint.setColor(out ? (dark ? 0xFF3C7A57 : 0xFFEFFDDE) : (dark ? 0xFF2B2F36 : 0xFFFFFFFF));
                c.drawRoundRect(rect, dp(16), dp(16), paint);
                paint.setColor(out ? (dark ? 0xCCFFFFFF : 0xAA2E7D32) : (dark ? 0x88FFFFFF : 0x66000000));
                for (int i = 0; i < b[2]; i++) {
                    final float lw = (i == b[2] - 1 && b[2] > 1) ? bw * 0.5f : bw - dp(28);
                    rect.set(l + dp(14), y + dp(12) + i * lineH, l + dp(14) + lw, y + dp(12) + i * lineH + dp(8));
                    c.drawRoundRect(rect, dp(4), dp(4), paint);
                }
                y += bh + dp(8);
            }
        }
    }

    private void drawPiece(Canvas canvas, Piece p, int tint) {
        // shadow under the glass
        shadowPaint.setShadowLayer(dp(10), 0, dp(3), 0x40000000);
        rect.set(p.l, p.t, p.r, p.b);
        canvas.drawRoundRect(rect, p.radius, p.radius, shadowPaint);

        canvas.save();
        clip.reset();
        clip.addRoundRect(rect, p.radius, p.radius, Path.Direction.CW);
        canvas.clipPath(clip);

        if (Build.VERSION.SDK_INT >= 33 && canvas.isHardwareAccelerated() && sceneNode != null) {
            final int pw = Math.round(p.width()), ph = Math.round(p.height());
            if (p.node == null) {
                p.node = new RenderNode("GlassPreviewPiece");
                p.effect = new LiquidGlassEffect(p.node);
            }
            p.node.setPosition(0, 0, pw, ph);
            final Canvas rc = p.node.beginRecording();
            rc.translate(-p.l, -p.t);
            rc.drawRenderNode(blurNode != null ? blurNode : sceneNode);
            p.node.endRecording();
            final float thickness = Math.max(Math.min(dp(11), Math.min(pw, ph) / 5f), 1);
            p.effect.update(0, 0, pw, ph, p.radius, p.radius, p.radius, p.radius, thickness, 0.75f, 1.5f, tint);
            canvas.translate(p.l, p.t);
            canvas.drawRenderNode(p.node);
        } else {
            // No shader on this device: translucent tint only.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(GlassTuning.scaleAlpha(tint, Math.max(GlassTuning.tint, 0.4f)));
            canvas.drawRoundRect(rect, p.radius, p.radius, paint);
        }
        canvas.restore();
    }

    private static final class Piece {
        float l, t, r, b, radius;
        RenderNode node;
        LiquidGlassEffect effect;

        void set(float l, float t, float r, float b, float radius) {
            this.l = l; this.t = t; this.r = r; this.b = b; this.radius = radius;
        }

        float width() { return r - l; }
        float height() { return b - t; }
    }
}
