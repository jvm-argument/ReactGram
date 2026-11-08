package com.reactgram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.reactgram.ReactGramConfig;
import com.reactgram.ghost.GhostConfig;
import com.reactgram.ghost.GhostSettingsActivity;
import com.reactgram.liquidglass.GlassTuning;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;
import java.util.Locale;

/** Top-level ReactGram settings: Ghost Mode entry, Liquid Glass tuning (5 sliders) and a live preview. */
public class ReactGramSettingsActivity extends BaseFragment {

    private interface FloatGetter { float get(); }
    private interface FloatSetter { void set(float value); }

    private LinearLayout list;
    private GlassPreviewView preview;
    private TextCell ghostCell;
    private final ArrayList<SliderCell> sliders = new ArrayList<>();

    @Override
    public View createView(Context context) {
        ReactGramConfig.load();
        GhostConfig.load();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.ReactGramSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        final ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        // Ghost Mode
        ghostCell = new TextCell(context);
        ghostCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        ghostCell.setOnClickListener(v -> presentFragment(new GhostSettingsActivity()));
        list.addView(ghostCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        addInfo(context, LocaleController.getString(R.string.ReactGramGhostInfo));

        // Liquid Glass
        addHeader(context, LocaleController.getString(R.string.GlassPreviewHeader));
        preview = new GlassPreviewView(context);
        final FrameLayout previewHolder = new FrameLayout(context);
        previewHolder.setPadding(dp(16), dp(8), dp(16), dp(16));
        previewHolder.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        previewHolder.addView(preview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        list.addView(previewHolder, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addHeader(context, LocaleController.getString(R.string.GlassHeader));
        addSlider(context, R.string.GlassTint, 0f, 2f, () -> GlassTuning.tint, v -> GlassTuning.tint = v, true);
        addSlider(context, R.string.GlassRefraction, 0f, 2f, () -> GlassTuning.refraction, v -> GlassTuning.refraction = v, true);
        addSlider(context, R.string.GlassThickness, 0.3f, 2f, () -> GlassTuning.thickness, v -> GlassTuning.thickness = v, true);
        addSlider(context, R.string.GlassRim, 0f, 2f, () -> GlassTuning.rim, v -> GlassTuning.rim = v, true);
        addSlider(context, R.string.GlassSaturation, 0f, 1f, () -> GlassTuning.saturation, v -> GlassTuning.saturation = v, true);
        addSlider(context, R.string.GlassBubble, 0.2f, 1f, () -> GlassTuning.bubbleOpacity, v -> GlassTuning.bubbleOpacity = v, false);

        final TextCheckCell batteryCell = new TextCheckCell(context);
        batteryCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        batteryCell.setTextAndCheck(LocaleController.getString(R.string.GlassIgnoreBatterySaver), ReactGramConfig.isGlassIgnoringBatterySaver(), false);
        batteryCell.setOnClickListener(v -> {
            final boolean value = !ReactGramConfig.isGlassIgnoringBatterySaver();
            ReactGramConfig.setGlassIgnoringBatterySaver(value);
            batteryCell.setChecked(value);
        });
        list.addView(batteryCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));

        final TextCell reset = new TextCell(context);
        reset.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        reset.setTextAndIcon(LocaleController.getString(R.string.GlassReset), R.drawable.msg_reset, false);
        reset.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
        reset.setOnClickListener(v -> {
            ReactGramConfig.resetGlass();
            for (SliderCell s : sliders) {
                s.refresh();
            }
            preview.invalidate();
        });
        list.addView(reset, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));
        addInfo(context, LocaleController.getString(R.string.GlassInfo));

        updateGhostCell();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ghostCell != null) {
            updateGhostCell();
        }
    }

    private void updateGhostCell() {
        final String state = LocaleController.getString(GhostConfig.isActive() ? R.string.ReactGramOn : R.string.ReactGramOff);
        ghostCell.setTextAndValue(LocaleController.getString(R.string.GhostMode), state, false);
    }

    private void addHeader(Context context, String title) {
        final HeaderCell header = new HeaderCell(context);
        header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        header.setText(title);
        list.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfo(Context context, String text) {
        final TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(text);
        list.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addSlider(Context context, int titleRes, float min, float max, FloatGetter getter, FloatSetter setter, boolean divider) {
        final SliderCell cell = new SliderCell(context, LocaleController.getString(titleRes), min, max, getter, setter);
        sliders.add(cell);
        list.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
    }

    private final class SliderCell extends FrameLayout {
        private final TextView valueView;
        private final SeekBarView seekBar;
        private final float min, max;
        private final FloatGetter getter;

        SliderCell(Context context, String title, float min, float max, FloatGetter getter, FloatSetter setter) {
            super(context);
            this.min = min;
            this.max = max;
            this.getter = getter;
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            final TextView titleView = new TextView(context);
            titleView.setTextSize(16);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setText(title);
            titleView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 24, Gravity.LEFT | Gravity.TOP, 21, 8, 21, 0));

            valueView = new TextView(context);
            valueView.setTextSize(16);
            valueView.setTypeface(Typeface.DEFAULT_BOLD);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            addView(valueView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 24, Gravity.RIGHT | Gravity.TOP, 21, 8, 21, 0));

            seekBar = new SeekBarView(context);
            seekBar.setReportChanges(true);
            seekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    setter.set(SliderCell.this.min + progress * (SliderCell.this.max - SliderCell.this.min));
                    GlassTuning.touch();
                    updateValue();
                    preview.invalidate();
                    if (stop) {
                        ReactGramConfig.saveGlass();
                    }
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {
                    if (!pressed) {
                        ReactGramConfig.saveGlass();
                    }
                }
            });
            addView(seekBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.BOTTOM, 6, 0, 6, 2));
            refresh();
        }

        void refresh() {
            seekBar.setProgress((getter.get() - min) / (max - min));
            updateValue();
        }

        private void updateValue() {
            valueView.setText(String.format(Locale.US, "%d%%", Math.round(getter.get() * 100)));
        }
    }
}
