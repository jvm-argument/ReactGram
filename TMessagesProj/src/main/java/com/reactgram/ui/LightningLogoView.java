package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/**
 * ReactGram logo for the welcome screen: the lightning bolt appears slowly. It is revealed from top to bottom through a
 * soft edge while fading and scaling in, and a glow settles around it. Covers the original Telegram logo on page 0 and
 * fades out as the user swipes to the next page.
 */
public class LightningLogoView extends View {

    private static final int DURATION_MS = 2600;
    private static final int START_DELAY_MS = 350;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tile = new RectF();
    private final Bitmap bolt;

    private ValueAnimator animator;
    private float progress;
    private float pageAlpha = 1f;
    private boolean played;

    public LightningLogoView(Context context) {
        super(context);
        bolt = BitmapFactory.decodeResource(context.getResources(), R.mipmap.icon_foreground);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Called from the pager: only page 0 shows the logo, it fades out while scrolling away. */
    public void setPageOffset(int position, float positionOffset) {
        final float a = position == 0 ? 1f - positionOffset : 0f;
        if (a != pageAlpha) {
            pageAlpha = a;
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (played) {
            return;
        }
        played = true;
        progress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setStartDelay(START_DELAY_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.4f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
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
    protected void onDraw(@NonNull Canvas canvas) {
        final int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || bolt == null) {
            return;
        }
        // Cover the original logo with the page background so only our bolt is visible.
        paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        paint.setAlpha(Math.round(255 * pageAlpha));
        canvas.drawRect(0, 0, w, h, paint);
        if (pageAlpha <= 0f) {
            return;
        }

        final float size = Math.min(dp(120), Math.min(w, h) - dp(8));
        final float cx = w / 2f, cy = h / 2f;
        final float scale = 0.82f + 0.18f * progress;
        final float half = size * scale / 2f;
        tile.set(cx - half, cy - half, cx + half, cy + half);

        final int alpha = Math.round(255 * Math.min(1f, progress * 1.8f) * pageAlpha);

        // Glow that settles in the later part of the animation.
        final float glow = Math.max(0f, (progress - 0.35f) / 0.65f);
        if (glow > 0f) {
            glowPaint.setShader(new RadialGradient(cx, cy, size * 0.95f,
                new int[]{0x44FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            glowPaint.setAlpha(Math.round(255 * glow * pageAlpha));
            canvas.drawCircle(cx, cy, size * 0.95f, glowPaint);
        }

        // Bolt on its black tile, revealed from the top with a soft edge.
        final int layer = canvas.saveLayer(tile, null);
        paint.setAlpha(alpha);
        paint.setShader(null);
        canvas.save();
        final android.graphics.Path round = new android.graphics.Path();
        round.addRoundRect(tile, half * 0.44f, half * 0.44f, android.graphics.Path.Direction.CW);
        canvas.clipPath(round);
        canvas.drawBitmap(bolt, null, tile, paint);
        canvas.restore();

        final float edge = tile.height() * 0.28f;
        final float reveal = tile.top + (tile.height() + edge) * progress;
        maskPaint.setShader(new LinearGradient(0, reveal - edge, 0, reveal,
            0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(tile, maskPaint);
        maskPaint.setShader(null);
        canvas.restoreToCount(layer);
    }
}
