/*
 * ReactGram features.
 *
 * The set of features and their defaults follow AyuGram for Android (AyuConfig), Copyright @Radolyn, 2023,
 * https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE). Code written for ReactGram.
 */

package com.reactgram.features;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

/** Persistent switches of the ReactGram features. */
public final class FeatureConfig {
    private static final String PREFS = "reactgram_features";
    private static final String FILTER_SEPARATOR = "\u0000";

    private static SharedPreferences prefs;

    private static volatile boolean saveDeleted = true;
    private static volatile boolean saveEdits = true;
    private static volatile boolean disableAds = true;
    private static volatile boolean localPremium;
    private static volatile boolean scheduledSend;
    private static volatile boolean keepAlive;
    private static volatile boolean filtersEnabled;
    private static volatile boolean filtersInChats;
    private static volatile boolean filtersCaseInsensitive = true;
    private static final ArrayList<String> filters = new ArrayList<>();
    private static boolean loaded;

    private FeatureConfig() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        saveDeleted = prefs.getBoolean("saveDeleted", true);
        saveEdits = prefs.getBoolean("saveEdits", true);
        disableAds = prefs.getBoolean("disableAds", true);
        localPremium = prefs.getBoolean("localPremium", false);
        scheduledSend = prefs.getBoolean("scheduledSend", false);
        keepAlive = prefs.getBoolean("keepAlive", false);
        filtersEnabled = prefs.getBoolean("filtersEnabled", false);
        filtersInChats = prefs.getBoolean("filtersInChats", false);
        filtersCaseInsensitive = prefs.getBoolean("filtersCaseInsensitive", true);
        filters.clear();
        final String raw = prefs.getString("filters", "");
        if (!raw.isEmpty()) {
            for (String s : raw.split(FILTER_SEPARATOR)) {
                if (!s.isEmpty()) {
                    filters.add(s);
                }
            }
        }
        loaded = true;
    }

    public static boolean saveDeleted() { load(); return saveDeleted; }
    public static boolean saveEdits() { load(); return saveEdits; }
    public static boolean disableAds() { load(); return disableAds; }
    public static boolean localPremium() { load(); return localPremium; }
    public static boolean scheduledSend() { load(); return scheduledSend; }
    public static boolean keepAlive() { load(); return keepAlive; }
    public static boolean filtersEnabled() { load(); return filtersEnabled; }
    public static boolean filtersInChats() { load(); return filtersInChats; }
    public static boolean filtersCaseInsensitive() { load(); return filtersCaseInsensitive; }

    public static void setSaveDeleted(boolean v) { load(); saveDeleted = v; put("saveDeleted", v); }
    public static void setSaveEdits(boolean v) { load(); saveEdits = v; put("saveEdits", v); }
    public static void setDisableAds(boolean v) { load(); disableAds = v; put("disableAds", v); }
    public static void setLocalPremium(boolean v) { load(); localPremium = v; put("localPremium", v); }
    public static void setScheduledSend(boolean v) { load(); scheduledSend = v; put("scheduledSend", v); }
    public static void setKeepAlive(boolean v) { load(); keepAlive = v; put("keepAlive", v); }
    public static void setFiltersEnabled(boolean v) { load(); filtersEnabled = v; put("filtersEnabled", v); MessageFilter.invalidate(); }
    public static void setFiltersInChats(boolean v) { load(); filtersInChats = v; put("filtersInChats", v); }
    public static void setFiltersCaseInsensitive(boolean v) { load(); filtersCaseInsensitive = v; put("filtersCaseInsensitive", v); MessageFilter.invalidate(); }

    public static synchronized List<String> getFilters() {
        load();
        return new ArrayList<>(filters);
    }

    public static synchronized void setFilters(List<String> list) {
        load();
        filters.clear();
        filters.addAll(list);
        prefs.edit().putString("filters", String.join(FILTER_SEPARATOR, filters)).apply();
        MessageFilter.invalidate();
    }

    private static void put(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }
}
