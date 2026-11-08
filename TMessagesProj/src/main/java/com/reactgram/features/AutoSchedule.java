/*
 * ReactGram scheduled sending.
 *
 * Idea from AyuGram for Android ("Schedule messages"), Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A,
 * GPL-2.0-or-later (see LICENSE): a message sent with a schedule date at least 10 seconds ahead does not need the sender to
 * stay online, and the server delivers it when the time comes.
 */

package com.reactgram.features;

import android.os.SystemClock;

import org.telegram.messenger.DialogObject;
import org.telegram.tgnet.ConnectionsManager;

public final class AutoSchedule {
    private AutoSchedule() {}

    /** The server sends a message at once if the date is less than 10 seconds ahead, so aim a little past that. */
    private static final int DELAY_SECONDS = 11;
    private static final long WINDOW_MS = 6000;
    private static volatile long lastAppliedAt;

    /** Returns the schedule date to use: the given one, or "now + 11s" when automatic scheduling applies. */
    public static int apply(int account, long peer, int scheduleDate) {
        if (scheduleDate != 0 || !FeatureConfig.scheduledSend() || DialogObject.isEncryptedDialog(peer)) {
            return scheduleDate;
        }
        lastAppliedAt = SystemClock.elapsedRealtime();
        return ConnectionsManager.getInstance(account).getCurrentTime() + DELAY_SECONDS;
    }

    /** True shortly after a message was scheduled automatically: the chat must not jump to the scheduled list. */
    public static boolean justApplied() {
        return lastAppliedAt != 0 && SystemClock.elapsedRealtime() - lastAppliedAt < WINDOW_MS;
    }
}
