/*
 * ReactGram: replying to a message that its author has deleted.
 *
 * The server no longer has the message, so a normal reply would be rejected. The reply is sent as a plain message that
 * quotes the deleted one (author with a profile link, then the quoted text), as AyuGram for Android does,
 * Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE).
 */

package com.reactgram.features;

import android.text.TextUtils;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public final class DeletedReply {
    private DeletedReply() {}

    private static final int MAX_QUOTE = 300;

    /** Rewrites the parameters of a send that replies to a deleted message. Does nothing otherwise. */
    public static void apply(int account, SendMessagesHelper.SendMessageParams params) {
        final MessageObject reply = params.replyToMsg;
        if (reply == null || reply.messageOwner == null
                || !SavedStore.get().isDeleted(account, reply.getDialogId(), reply.getId())) {
            return;
        }

        // the server cannot reply to a message that does not exist any more
        params.replyToMsg = null;
        params.replyQuote = null;

        final boolean inCaption = params.message == null && params.caption != null;
        final String userText = inCaption ? params.caption : params.message;
        if (userText == null) {
            return; // stickers, voice messages and the like: no text to carry a quote
        }

        final String author = authorName(account, reply.messageOwner);
        final String url = authorUrl(reply.messageOwner);
        String quoted = reply.messageOwner.message;
        if (TextUtils.isEmpty(quoted)) {
            quoted = "…";
        } else if (quoted.length() > MAX_QUOTE) {
            quoted = quoted.substring(0, MAX_QUOTE) + "…";
        }

        final String prefix = author + "\n" + quoted + "\n";
        final ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();

        // one quote holds both the bold, linked nick and the quoted message
        final TLRPC.TL_messageEntityBlockquote quote = new TLRPC.TL_messageEntityBlockquote();
        quote.offset = 0;
        quote.length = author.length() + 1 + quoted.length();
        entities.add(quote);

        final TLRPC.TL_messageEntityBold bold = new TLRPC.TL_messageEntityBold();
        bold.offset = 0;
        bold.length = author.length();
        entities.add(bold);

        if (url != null) {
            final TLRPC.TL_messageEntityTextUrl link = new TLRPC.TL_messageEntityTextUrl();
            link.offset = 0;
            link.length = author.length();
            link.url = url;
            entities.add(link);
        }

        if (params.entities != null) {
            for (TLRPC.MessageEntity entity : params.entities) {
                entity.offset += prefix.length();
                entities.add(entity);
            }
        }

        params.entities = entities;
        if (inCaption) {
            params.caption = prefix + userText;
        } else {
            params.message = prefix + userText;
        }
    }

    private static String authorName(int account, TLRPC.Message message) {
        final long id = DialogObject.getPeerDialogId(message.from_id != null ? message.from_id : message.peer_id);
        final MessagesController controller = MessagesController.getInstance(account);
        if (id > 0) {
            final TLRPC.User user = controller.getUser(id);
            if (user != null) {
                return UserObject.getUserName(user);
            }
        } else if (id < 0) {
            final TLRPC.Chat chat = controller.getChat(-id);
            if (chat != null && chat.title != null) {
                return chat.title;
            }
        }
        return "?";
    }

    private static String authorUrl(TLRPC.Message message) {
        final long id = DialogObject.getPeerDialogId(message.from_id != null ? message.from_id : message.peer_id);
        return id > 0 ? "tg://user?id=" + id : null;
    }
}
