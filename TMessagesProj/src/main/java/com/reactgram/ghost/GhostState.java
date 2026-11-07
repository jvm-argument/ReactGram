/*
 * ReactGram Ghost Mode.
 *
 * Derived from AyuGram for Android (AyuState / AyuStateVariable), Copyright @Radolyn, 2023,
 * https://github.com/AyuGram/AyuGram4A, licensed under GPL-2.0-or-later (see LICENSE).
 */

package com.reactgram.ghost;

/** One-shot permission flag, so our own legitimate read packets are not dropped by {@link GhostRequestFilter}. */
public final class GhostState {
    private static final Object sync = new Object();
    private static int allowedReadPackets;

    private GhostState() {}

    /** Lets the next {@code count} read packets through. */
    public static void allowNextReadPackets(int count) {
        synchronized (sync) {
            allowedReadPackets = count;
        }
    }

    /** Consumes one permission; true when a read packet may go out right now. */
    public static boolean consumeReadPermission() {
        synchronized (sync) {
            if (allowedReadPackets <= 0) {
                return false;
            }
            allowedReadPackets--;
            return true;
        }
    }
}
