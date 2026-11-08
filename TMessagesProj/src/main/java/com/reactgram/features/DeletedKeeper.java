/*
 * ReactGram: keep messages deleted by the other side.
 *
 * Feature from AyuGram for Android ("Save deleted messages"), Copyright @Radolyn, 2023,
 * https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE).
 */

package com.reactgram.features;

import androidx.collection.LongSparseArray;

import java.util.ArrayList;

public final class DeletedKeeper {
    private DeletedKeeper() {}

    // ids of messages the user is deleting right now; their server echo must really delete them
    private static final java.util.HashSet<String> own = new java.util.HashSet<>();

    public static synchronized void markOwn(int account, java.util.List<Integer> ids) {
        if (ids == null) {
            return;
        }
        for (int id : ids) {
            own.add(account + ":" + id);
        }
    }

    private static synchronized boolean takeOwn(int account, int id) {
        return own.remove(account + ":" + id);
    }

    /**
     * Called with the deletions that arrived from the server. When saving is on, the messages are flagged and nothing is
     * deleted locally (returns null); otherwise the deletions are returned unchanged.
     */
    public static LongSparseArray<ArrayList<Integer>> keep(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null || deleted.size() == 0 || !FeatureConfig.saveDeleted()) {
            return deleted;
        }
        final SavedStore store = SavedStore.get();
        LongSparseArray<ArrayList<Integer>> remove = null;
        for (int i = 0, n = deleted.size(); i < n; i++) {
            final ArrayList<Integer> keep = new ArrayList<>();
            final ArrayList<Integer> real = new ArrayList<>();
            for (int id : deleted.valueAt(i)) {
                (takeOwn(account, id) ? real : keep).add(id);
            }
            if (!keep.isEmpty()) {
                store.markDeleted(account, deleted.keyAt(i), keep);
            }
            if (!real.isEmpty()) {
                if (remove == null) {
                    remove = new LongSparseArray<>();
                }
                remove.put(deleted.keyAt(i), real);
            }
        }
        return remove;
    }
}
