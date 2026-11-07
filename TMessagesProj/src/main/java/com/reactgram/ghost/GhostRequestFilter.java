/*
 * ReactGram Ghost Mode.
 *
 * Derived from AyuGram for Android (the "AyuGram request hook" in ConnectionsManager and AyuGhostUtils),
 * Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A, licensed under GPL-2.0-or-later (see LICENSE).
 * Telegram for Android, Copyright Nikolai Kudashov, GPL-2.0-or-later.
 *
 * Modified and restructured for ReactGram.
 */

package com.reactgram.ghost;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

/**
 * Single hook point for ConnectionsManager. Decides, per outgoing request, whether it is sent as is,
 * rewritten, answered locally, or dropped.
 */
public final class GhostRequestFilter {
    private GhostRequestFilter() {}

    /**
     * Called before a request is serialized. Returns true when the request must NOT be sent
     * (it was either answered locally or silently dropped).
     */
    public static boolean intercept(int account, TLObject req, RequestDelegate onComplete) {
        GhostConfig.load();

        if (!GhostConfig.sendTypingPackets
                && (req instanceof TLRPC.TL_messages_setTyping || req instanceof TLRPC.TL_messages_setEncryptedTyping)) {
            return true;
        }

        if (!GhostConfig.sendStoryViews
                && (req instanceof TL_stories.TL_stories_readStories || req instanceof TL_stories.TL_stories_incrementStoryViews)) {
            return true;
        }

        if (req instanceof TL_account.updateStatus && !GhostConfig.sendOnlinePackets) {
            ((TL_account.updateStatus) req).offline = true;
            return false;
        }

        if (!GhostConfig.sendReadPackets && isReadRequest(req) && !GhostState.consumeReadPermission()) {
            if (onComplete != null) {
                TLRPC.TL_messages_affectedMessages fake = new TLRPC.TL_messages_affectedMessages();
                fake.pts = -1;
                fake.pts_count = 0;
                try {
                    onComplete.run(fake, null);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return true;
        }
        return false;
    }

    /** Wraps the completion callback for requests that need a follow-up (read-after-send, offline-after-online). */
    public static RequestDelegate wrap(int account, TLObject req, RequestDelegate onComplete) {
        GhostConfig.load();

        if (GhostConfig.markReadAfterSend && !GhostConfig.sendReadPackets) {
            final TLRPC.InputPeer peer = getSendPeer(req);
            if (peer != null) {
                final long dialogId = getDialogId(peer);
                return (response, error) -> {
                    if (onComplete != null) {
                        onComplete.run(response, error);
                    }
                    MessagesStorage.getInstance(account).getDialogMaxMessageId(dialogId, maxId -> {
                        TLRPC.TL_messages_readHistory read = new TLRPC.TL_messages_readHistory();
                        read.peer = peer;
                        read.max_id = maxId;
                        GhostState.allowNextReadPackets(1);
                        ConnectionsManager.getInstance(account).sendRequest(read, (a, b) -> {});
                    });
                };
            }
        }

        if (GhostConfig.sendOfflineAfterOnline && GhostConfig.sendOnlinePackets
                && req instanceof TL_account.updateStatus && !((TL_account.updateStatus) req).offline) {
            return (response, error) -> {
                if (onComplete != null) {
                    onComplete.run(response, error);
                }
                TL_account.updateStatus off = new TL_account.updateStatus();
                off.offline = true;
                ConnectionsManager.getInstance(account).sendRequest(off, (a, b) -> {});
            };
        }
        return onComplete;
    }

    /** Sends a real read receipt up to {@code messageId}; for an explicit "mark as read" action. */
    public static void markReadOnServer(int account, int messageId, TLRPC.InputPeer peer) {
        TLObject req;
        if (peer instanceof TLRPC.TL_inputPeerChannel) {
            TLRPC.TL_channels_readHistory r = new TLRPC.TL_channels_readHistory();
            r.channel = MessagesController.getInputChannel(peer);
            r.max_id = messageId;
            req = r;
        } else {
            TLRPC.TL_messages_readHistory r = new TLRPC.TL_messages_readHistory();
            r.peer = peer;
            r.max_id = messageId;
            req = r;
        }
        GhostState.allowNextReadPackets(1);
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error == null && response instanceof TLRPC.TL_messages_affectedMessages) {
                TLRPC.TL_messages_affectedMessages res = (TLRPC.TL_messages_affectedMessages) response;
                MessagesController.getInstance(account).processNewDifferenceParams(-1, res.pts, -1, res.pts_count);
            }
        });
    }

    private static boolean isReadRequest(TLObject req) {
        return req instanceof TLRPC.TL_messages_readHistory
                || req instanceof TLRPC.TL_messages_readEncryptedHistory
                || req instanceof TLRPC.TL_messages_readDiscussion
                || req instanceof TLRPC.TL_messages_readMessageContents
                || req instanceof TLRPC.TL_channels_readHistory
                || req instanceof TLRPC.TL_channels_readMessageContents;
    }

    private static TLRPC.InputPeer getSendPeer(TLObject req) {
        if (req instanceof TLRPC.TL_messages_sendMessage) {
            return ((TLRPC.TL_messages_sendMessage) req).peer;
        } else if (req instanceof TLRPC.TL_messages_sendMedia) {
            return ((TLRPC.TL_messages_sendMedia) req).peer;
        } else if (req instanceof TLRPC.TL_messages_sendMultiMedia) {
            return ((TLRPC.TL_messages_sendMultiMedia) req).peer;
        }
        return null;
    }

    public static long getDialogId(TLRPC.InputPeer peer) {
        if (peer.chat_id != 0) {
            return -peer.chat_id;
        } else if (peer.channel_id != 0) {
            return -peer.channel_id;
        }
        return peer.user_id;
    }
}
