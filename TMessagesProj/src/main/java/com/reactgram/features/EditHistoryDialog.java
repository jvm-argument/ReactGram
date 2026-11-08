/*
 * ReactGram edit history viewer.
 *
 * Feature from AyuGram for Android ("edits history"), Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A,
 * GPL-2.0-or-later (see LICENSE). Dialog written for ReactGram.
 */

package com.reactgram.features;

import android.content.Context;
import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class EditHistoryDialog {
    private EditHistoryDialog() {}

    public static AlertDialog build(Context context, int account, MessageObject message) {
        final List<SavedStore.Edit> edits = SavedStore.get().getEdits(account, message.getDialogId(), message.getId());
        final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        final StringBuilder text = new StringBuilder();
        for (SavedStore.Edit edit : edits) {
            text.append(format.format(new Date(edit.editDate * 1000L))).append('\n')
                .append(edit.text).append("\n\n");
        }
        text.append(LocaleController.getString(R.string.EditHistoryCurrent)).append('\n');
        final CharSequence current = message.messageOwner != null ? message.messageOwner.message : "";
        text.append(TextUtils.isEmpty(current) ? "" : current);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.EditHistoryTitle));
        builder.setMessage(text.toString());
        builder.setPositiveButton(LocaleController.getString(R.string.Close), null);
        return builder.create();
    }
}
