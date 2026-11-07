/*
 * ReactGram Ghost Mode settings screen.
 *
 * The set of options mirrors AyuGram's "Ghost essentials" (AyuGramPreferencesActivity),
 * Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A, GPL-2.0-or-later (see LICENSE).
 * The screen itself is written for ReactGram on top of Telegram's own cells.
 */

package com.reactgram.ghost;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;

public class GhostSettingsActivity extends BaseFragment {

    private interface Getter { boolean get(); }
    private interface Setter { void set(boolean value); }

    private LinearLayout list;
    private TextCheckCell masterCell;
    private final java.util.ArrayList<Runnable> refreshers = new java.util.ArrayList<>();

    @Override
    public View createView(Context context) {
        GhostConfig.load();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.GhostMode));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        masterCell = addSwitch(context, LocaleController.getString(R.string.GhostModeToggle),
                GhostConfig::isActive, v -> GhostConfig.setActive(v), true);

        addHeader(context, LocaleController.getString(R.string.GhostEssentialsHeader));
        // "hide X" switches are the inverse of the "send X" flags
        addSwitch(context, LocaleController.getString(R.string.GhostHideRead),
                () -> !GhostConfig.sendReadPackets, v -> GhostConfig.sendReadPackets = !v, true);
        addSwitch(context, LocaleController.getString(R.string.GhostHideOnline),
                () -> !GhostConfig.sendOnlinePackets, v -> GhostConfig.sendOnlinePackets = !v, true);
        addSwitch(context, LocaleController.getString(R.string.GhostHideTyping),
                () -> !GhostConfig.sendTypingPackets, v -> GhostConfig.sendTypingPackets = !v, true);
        addSwitch(context, LocaleController.getString(R.string.GhostHideStories),
                () -> !GhostConfig.sendStoryViews, v -> GhostConfig.sendStoryViews = !v, false);
        addInfo(context, LocaleController.getString(R.string.GhostHideInfo));

        addHeader(context, LocaleController.getString(R.string.GhostExtrasHeader));
        addSwitch(context, LocaleController.getString(R.string.GhostOfflineAfterOnline),
                () -> GhostConfig.sendOfflineAfterOnline, v -> GhostConfig.sendOfflineAfterOnline = v, true);
        addSwitch(context, LocaleController.getString(R.string.GhostMarkReadAfterSend),
                () -> GhostConfig.markReadAfterSend, v -> GhostConfig.markReadAfterSend = v, false);
        addInfo(context, LocaleController.getString(R.string.GhostExtrasInfo));

        return fragmentView;
    }

    private TextCheckCell addSwitch(Context context, String title, Getter getter, Setter setter, boolean divider) {
        TextCheckCell cell = new TextCheckCell(context);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setTextAndCheck(title, getter.get(), divider);
        cell.setOnClickListener(v -> {
            setter.set(!getter.get());
            GhostConfig.save();
            refreshAll();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.mainUserInfoChanged);
        });
        refreshers.add(() -> cell.setChecked(getter.get()));
        list.addView(cell, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return cell;
    }

    private void addHeader(Context context, String title) {
        HeaderCell header = new HeaderCell(context);
        header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        header.setText(title);
        list.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void addInfo(Context context, String text) {
        TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(text);
        list.addView(info, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void refreshAll() {
        for (Runnable r : refreshers) {
            r.run();
        }
    }
}
