package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives {@link BottomSheet} the liquid glass background. The sheet's original 9-patch background stays in place until a
 * snapshot of the screen behind it is ready, and keeps providing the padding so the layout does not change.
 */
public final class SheetGlass {
    private SheetGlass() {}

    public static void attach(@NonNull BottomSheet sheet, @Nullable ViewGroup container, @Nullable Drawable original) {
        if (container == null || original == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS) || container.getBackground() != original) {
            return;
        }

        final BlurredBackgroundSourceBitmap source = new BlurredBackgroundSourceBitmap().withLiquidGlass();
        final Rect pad = new Rect();
        original.getPadding(pad);
        final BlurredBackgroundDrawable glass = new BlurredBackgroundDrawableViewFactory(source)
            .create(container, true)
            .setColorProvider(provider())
            .setPadding(Math.max(pad.left, 0))
            .setHasPadding(true)
            .setRadius(dp(16), dp(16), 0, 0);
        final SheetBackground background = new SheetBackground(original, glass);
        container.setBackgroundDrawable(background);

        final List<View> exclude = new ArrayList<>();
        exclude.add(container.getRootView());
        AndroidUtilities.makeGlobalBlurBitmap(bitmap -> {
            if (bitmap == null || !sheet.isShowing()) {
                return;
            }
            source.setBitmap(bitmap);
            final Matrix m = new Matrix();
            m.setScale(
                (float) AndroidUtilities.displaySize.x / bitmap.getWidth(),
                (float) (AndroidUtilities.displaySize.y + AndroidUtilities.statusBarHeight + AndroidUtilities.navigationBarHeight) / bitmap.getHeight());
            source.setMatrix(m);

            final int[] loc = new int[2];
            container.getLocationOnScreen(loc);
            glass.setSourceOffset(loc[0], loc[1]);
            background.ready = true;
            container.invalidate();

            // The sheet slides in and can be dragged: keep the snapshot aligned with the screen every frame.
            container.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (!sheet.isShowing()) {
                        final ViewTreeObserver vto = container.getViewTreeObserver();
                        if (vto.isAlive()) {
                            vto.removeOnPreDrawListener(this);
                        }
                        return true;
                    }
                    container.getLocationOnScreen(loc);
                    glass.setSourceOffset(loc[0], loc[1]);
                    return true;
                }
            });
        }, 2f, 1, null, exclude);
    }

    private static BlurredBackgroundProvider provider() {
        return new BlurredBackgroundProviderBuilder(null)
            .setBackgroundColor((r, isDark) -> Theme.multAlpha(Theme.getColor(Theme.key_dialogBackground), isDark ? 0.85f : 0.825f))
            .setStrokeColorTop(0x44FFFFFF, 0)
            .setStrokeColorBottom(0x22FFFFFF, 0)
            .setShadowColor(0x26000000, 0)
            .setShadowLayer(AndroidUtilities.dpf2(4f), 0, 0)
            .setStrokeWidth(AndroidUtilities.dpf2(2 / 3f), AndroidUtilities.dpf2(2 / 3f))
            .build();
    }

    private static final class SheetBackground extends Drawable {
        private final Drawable original;
        private final BlurredBackgroundDrawable glass;
        boolean ready;

        SheetBackground(Drawable original, BlurredBackgroundDrawable glass) {
            this.original = original;
            this.glass = glass;
        }

        @Override
        public boolean getPadding(@NonNull Rect padding) {
            return original.getPadding(padding);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (ready) {
                glass.setBounds(getBounds());
                glass.draw(canvas);
            } else {
                original.setBounds(getBounds());
                original.draw(canvas);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            original.setAlpha(alpha);
            glass.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            original.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
