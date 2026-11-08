package com.reactgram;

import android.content.Context;
import android.content.SharedPreferences;

import com.reactgram.liquidglass.GlassTuning;

import org.telegram.messenger.ApplicationLoader;

/** Persists ReactGram's own settings (currently the liquid glass tuning) and pushes them into {@link GlassTuning}. */
public final class ReactGramConfig {
    private static final String PREFS = "reactgramconfig";
    private static boolean loaded;
    private static boolean ignoreBatterySaver = true;

    private ReactGramConfig() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        final SharedPreferences p = prefs();
        GlassTuning.tint = p.getFloat("glassTint", GlassTuning.DEFAULT_TINT);
        GlassTuning.refraction = p.getFloat("glassRefraction", GlassTuning.DEFAULT_REFRACTION);
        GlassTuning.thickness = p.getFloat("glassThickness", GlassTuning.DEFAULT_THICKNESS);
        GlassTuning.rim = p.getFloat("glassRim", GlassTuning.DEFAULT_RIM);
        GlassTuning.saturation = p.getFloat("glassSaturation", GlassTuning.DEFAULT_SATURATION);
        GlassTuning.bubbleOpacity = p.getFloat("glassBubble", GlassTuning.DEFAULT_BUBBLE);
        ignoreBatterySaver = p.getBoolean("glassIgnoreBatterySaver", true);
        loaded = true;
    }

    /** True when the glass must stay on even if Telegram's automatic power saving (low battery) would turn it off. */
    public static boolean isGlassIgnoringBatterySaver() {
        load();
        return ignoreBatterySaver;
    }

    public static void setGlassIgnoringBatterySaver(boolean value) {
        load();
        ignoreBatterySaver = value;
        prefs().edit().putBoolean("glassIgnoreBatterySaver", value).apply();
        GlassTuning.touch();
    }

    public static void saveGlass() {
        GlassTuning.touch();
        prefs().edit()
            .putFloat("glassTint", GlassTuning.tint)
            .putFloat("glassRefraction", GlassTuning.refraction)
            .putFloat("glassThickness", GlassTuning.thickness)
            .putFloat("glassRim", GlassTuning.rim)
            .putFloat("glassSaturation", GlassTuning.saturation)
            .putFloat("glassBubble", GlassTuning.bubbleOpacity)
            .apply();
    }

    public static void resetGlass() {
        GlassTuning.reset();
        saveGlass();
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
