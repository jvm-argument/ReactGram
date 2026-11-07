/*
 * ReactGram Ghost Mode.
 *
 * Derived from AyuGram for Android (AyuConfig / AyuState / AyuGhostUtils and the
 * request hook in ConnectionsManager), Copyright @Radolyn, 2023,
 * https://github.com/AyuGram/AyuGram4A, licensed under GPL-2.0-or-later (see LICENSE).
 * Telegram for Android, Copyright Nikolai Kudashov, GPL-2.0-or-later.
 *
 * Modified and restructured for ReactGram.
 */

package com.reactgram.ghost;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/** Persistent Ghost Mode switches. Each flag is {@code true} when the corresponding packet IS allowed to be sent. */
public final class GhostConfig {
    private static final String PREFS = "ghostconfig";

    public static volatile boolean sendReadPackets = true;
    public static volatile boolean sendOnlinePackets = true;
    public static volatile boolean sendTypingPackets = true;
    public static volatile boolean sendStoryViews = true;
    public static volatile boolean sendOfflineAfterOnline = false;
    public static volatile boolean markReadAfterSend = true;
    public static volatile boolean showToggleInDrawer = true;

    private static SharedPreferences prefs;
    private static boolean loaded;

    private GhostConfig() {}

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sendReadPackets = prefs.getBoolean("sendReadPackets", true);
        sendOnlinePackets = prefs.getBoolean("sendOnlinePackets", true);
        sendTypingPackets = prefs.getBoolean("sendTypingPackets", true);
        sendStoryViews = prefs.getBoolean("sendStoryViews", true);
        sendOfflineAfterOnline = prefs.getBoolean("sendOfflineAfterOnline", false);
        markReadAfterSend = prefs.getBoolean("markReadAfterSend", true);
        showToggleInDrawer = prefs.getBoolean("showToggleInDrawer", true);
        loaded = true;
    }

    public static void save() {
        load();
        prefs.edit()
                .putBoolean("sendReadPackets", sendReadPackets)
                .putBoolean("sendOnlinePackets", sendOnlinePackets)
                .putBoolean("sendTypingPackets", sendTypingPackets)
                .putBoolean("sendStoryViews", sendStoryViews)
                .putBoolean("sendOfflineAfterOnline", sendOfflineAfterOnline)
                .putBoolean("markReadAfterSend", markReadAfterSend)
                .putBoolean("showToggleInDrawer", showToggleInDrawer)
                .apply();
    }

    /** Master state: every presence packet is suppressed. */
    public static boolean isActive() {
        load();
        return !sendReadPackets && !sendOnlinePackets && !sendTypingPackets && !sendStoryViews;
    }

    public static void setActive(boolean enabled) {
        load();
        sendReadPackets = !enabled;
        sendOnlinePackets = !enabled;
        sendTypingPackets = !enabled;
        sendStoryViews = !enabled;
        sendOfflineAfterOnline = enabled;
        save();
    }

    public static void toggle() {
        setActive(!isActive());
    }
}
