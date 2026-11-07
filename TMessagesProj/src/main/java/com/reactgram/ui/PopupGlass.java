package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.Components.ScrimOptions;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;

/**
 * Gives every {@link ActionBarPopupWindow} menu that is not already glass (3-dot menus and the like) the liquid glass
 * background: a blurred snapshot of the screen behind it, refracted at the edges by the glass shader.
 * Menus built by ItemOptions already carry a blurred background and are left alone.
 */
public final class PopupGlass {
    private PopupGlass() {}

    public static void attach(@Nullable ActionBarPopupWindow window) {
        if (window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || !LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS)) {
            return;
        }
        final ActionBarPopupWindow.ActionBarPopupWindowLayout layout = findLayout(window.getContentView(), 0);
        if (layout == null || layout.getBackgroundDrawable() instanceof BlurredBackgroundDrawable) {
            return;
        }

        final BlurredBackgroundSourceBitmap source = new BlurredBackgroundSourceBitmap().withLiquidGlass();
        final BlurredBackgroundDrawable bg = new BlurredBackgroundDrawableViewFactory(source)
            .create(layout, true)
            .setColorProvider(BlurredBackgroundProviderImpl.scrimMenuBackground(null))
            .setPadding(dp(8))
            .setHasPadding(true)
            .setRadius(dp(12));

        // Half-resolution, almost unblurred snapshot of everything on screen except this popup itself:
        // clear glass refracts recognisable content instead of a smear.
        final java.util.List<View> exclude = new java.util.ArrayList<>();
        exclude.add(layout.getRootView());
        AndroidUtilities.makeGlobalBlurBitmap(bitmapOptions -> {
            if (!window.isShowing() || bitmapOptions == null) {
                return;
            }
            source.setBitmap(bitmapOptions);
            final Matrix m = new Matrix();
            m.setScale(
                (float) AndroidUtilities.displaySize.x / bitmapOptions.getWidth(),
                (float) (AndroidUtilities.displaySize.y + AndroidUtilities.statusBarHeight + AndroidUtilities.navigationBarHeight) / bitmapOptions.getHeight());
            source.setMatrix(m);

            final int[] loc = new int[2];
            layout.getLocationOnScreen(loc);
            bg.setSourceOffset(loc[0], loc[1]);
            layout.setBackground(bg);
            layout.invalidate();

            // The popup moves and animates after it is shown; keep the snapshot aligned with the screen every frame.
            layout.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (!window.isShowing()) {
                        final android.view.ViewTreeObserver vto = layout.getViewTreeObserver();
                        if (vto.isAlive()) {
                            vto.removeOnPreDrawListener(this);
                        }
                        return true;
                    }
                    layout.getLocationOnScreen(loc);
                    bg.setSourceOffset(loc[0], loc[1]);
                    return true;
                }
            });
        }, 2f, 1, null, exclude);
    }

    @Nullable
    private static ActionBarPopupWindow.ActionBarPopupWindowLayout findLayout(@Nullable View v, int depth) {
        if (v instanceof ActionBarPopupWindow.ActionBarPopupWindowLayout) {
            return (ActionBarPopupWindow.ActionBarPopupWindowLayout) v;
        }
        if (depth < 3 && v instanceof ViewGroup) {
            final ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                final ActionBarPopupWindow.ActionBarPopupWindowLayout r = findLayout(g.getChildAt(i), depth + 1);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }
}
