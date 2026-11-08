/*
 * ReactGram message filters.
 *
 * Behaviour follows AyuGram for Android (AyuFilter: regular-expression message filters, optional in chats, case
 * insensitivity), Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE).
 */

package com.reactgram.features;

import android.text.TextUtils;
import android.util.LongSparseArray;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MessageFilter {
    private MessageFilter() {}

    private static ArrayList<Pattern> patterns;
    private static LongSparseArray<HashMap<Integer, Boolean>> cache = new LongSparseArray<>();

    /** Call when the filter list or its options change. */
    public static synchronized void invalidate() {
        patterns = null;
        cache = new LongSparseArray<>();
    }

    /** Returns null when the expression is valid, otherwise the syntax error message. */
    public static String validate(String regex) {
        try {
            Pattern.compile(regex);
            return null;
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        }
    }

    private static void build() {
        final int flags = Pattern.MULTILINE | (FeatureConfig.filtersCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
        final ArrayList<Pattern> list = new ArrayList<>();
        for (String filter : FeatureConfig.getFilters()) {
            try {
                list.add(Pattern.compile(filter, flags));
            } catch (PatternSyntaxException ignore) {
                // an invalid saved expression is skipped
            }
        }
        patterns = list;
    }

    public static synchronized boolean isFiltered(MessageObject message) {
        if (message == null || message.messageOwner == null || !FeatureConfig.filtersEnabled()) {
            return false;
        }
        if (patterns == null) {
            build();
        }
        final long dialogId = message.getDialogId();
        HashMap<Integer, Boolean> perDialog = cache.get(dialogId);
        if (perDialog == null) {
            perDialog = new HashMap<>();
            cache.put(dialogId, perDialog);
        }
        final Boolean cached = perDialog.get(message.getId());
        if (cached != null) {
            return cached;
        }
        final boolean result = matches(message.messageOwner.message);
        perDialog.put(message.getId(), result);
        return result;
    }

    private static boolean matches(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /** Whether the message is hidden from a chat list: incoming messages only, in channels (and in chats if enabled). */
    public static boolean hidesInChat(MessageObject message, boolean isChannel) {
        if (!FeatureConfig.filtersEnabled() || message == null || message.isOutOwner()) {
            return false;
        }
        if (!isChannel && !FeatureConfig.filtersInChats()) {
            return false;
        }
        return isFiltered(message);
    }

    /** Marks the text and media of a message as spoiler so that it is blurred wherever it is shown. */
    public static void blurify(MessageObject message) {
        final TLRPC.Message owner = message == null ? null : message.messageOwner;
        if (owner == null) {
            return;
        }
        if (!TextUtils.isEmpty(owner.message)) {
            boolean already = false;
            for (TLRPC.MessageEntity entity : owner.entities) {
                if (entity instanceof TLRPC.TL_messageEntitySpoiler && entity.offset == 0 && entity.length == owner.message.length()) {
                    already = true;
                    break;
                }
            }
            if (!already) {
                final TLRPC.TL_messageEntitySpoiler spoiler = new TLRPC.TL_messageEntitySpoiler();
                spoiler.offset = 0;
                spoiler.length = owner.message.length();
                owner.entities.add(spoiler);
            }
        }
        if (owner.media != null) {
            owner.media.spoiler = true;
        }
    }

    public static List<String> list() {
        return FeatureConfig.getFilters();
    }
}
