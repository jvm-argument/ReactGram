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

    /**
     * Called with the deletions that arrived from the server. When saving is on, the messages are flagged and nothing is
     * deleted locally (returns null); otherwise the deletions are returned unchanged.
     */
    public static LongSparseArray<ArrayList<Integer>> keep(int account, LongSparseArray<ArrayList<Integer>> deleted) {
        if (deleted == null || deleted.size() == 0 || !FeatureConfig.saveDeleted()) {
            return deleted;
        }
        final SavedStore store = SavedStore.get();
        for (int i = 0, n = deleted.size(); i < n; i++) {
            store.markDeleted(account, deleted.keyAt(i), deleted.valueAt(i));
        }
        return null;
    }
}
