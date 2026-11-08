package com.reactgram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.reactgram.features.FeatureConfig;
import com.reactgram.features.RegexFiltersActivity;
import com.reactgram.ghost.GhostConfig;
import com.reactgram.ghost.GhostSettingsActivity;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

/** Top-level ReactGram settings: Ghost Mode, saved messages, filters and the other features. */
public class ReactGramSettingsActivity extends BaseFragment {

    private interface Getter { boolean get(); }
    private interface Setter { void set(boolean value); }

    private LinearLayout list;
    private TextCheckCell ghostSwitch;
    private TextCell filtersListCell;

    @Override
    public View createView(Context context) {
        GhostConfig.load();
        FeatureConfig.load();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.ReactGramSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        final ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        fragmentView = scroll;

        // Ghost Mode
        ghostSwitch = addCheck(context, R.string.GhostModeToggle, GhostConfig::isActive, v -> GhostConfig.setActive(v), true, null);
        final TextCell ghostSettings = addCell(context, LocaleController.getString(R.string.ReactGramGhostSettings), false);
        ghostSettings.setOnClickListener(v -> presentFragment(new GhostSettingsActivity()));
        addInfo(context, R.string.ReactGramGhostInfo);

        // Saved messages
        addHeader(context, R.string.ReactGramMessagesHeader);
        addCheck(context, R.string.ReactGramSaveDeleted, FeatureConfig::saveDeleted, FeatureConfig::setSaveDeleted, true, null);
        addCheck(context, R.string.ReactGramSaveEdits, FeatureConfig::saveEdits, FeatureConfig::setSaveEdits, false, null);
        addInfo(context, R.string.ReactGramMessagesInfo);

        // Filters
        addHeader(context, R.string.RegexFilters);
        addCheck(context, R.string.ReactGramFiltersEnable, FeatureConfig::filtersEnabled, FeatureConfig::setFiltersEnabled, true, null);
        addCheck(context, R.string.ReactGramFiltersInChats, FeatureConfig::filtersInChats, FeatureConfig::setFiltersInChats, true, null);
        addCheck(context, R.string.ReactGramFiltersCase, FeatureConfig::filtersCaseInsensitive, FeatureConfig::setFiltersCaseInsensitive, true, null);
        filtersListCell = addCell(context, LocaleController.getString(R.string.RegexFilters), false);
        filtersListCell.setOnClickListener(v -> presentFragment(new RegexFiltersActivity()));
        addInfo(context, R.string.ReactGramFiltersInfo);

        // Other features
        addHeader(context, R.string.ReactGramOtherHeader);
        addCheck(context, R.string.ReactGramDisableAds, FeatureConfig::disableAds, FeatureConfig::setDisableAds, true, null);
        addCheck(context, R.string.ReactGramLocalPremium, FeatureConfig::localPremium, FeatureConfig::setLocalPremium, true, this::onLocalPremiumChanged);
        addCheck(context, R.string.ReactGramScheduled, FeatureConfig::scheduledSend, FeatureConfig::setScheduledSend, true, null);
        addCheck(context, R.string.ReactGramKeepAlive, FeatureConfig::keepAlive, FeatureConfig::setKeepAlive, false, this::onKeepAliveChanged);
        addInfo(context, R.string.ReactGramOtherInfo);

        final TextCell kill = addCell(context, LocaleController.getString(R.string.ReactGramKill), false);
        kill.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
        kill.setOnClickListener(v -> {
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        updateValues();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ghostSwitch != null) {
            ghostSwitch.setChecked(GhostConfig.isActive());
            updateValues();
        }
    }

    private void updateValues() {
        filtersListCell.setTextAndValue(LocaleController.getString(R.string.RegexFilters),
            FeatureConfig.getFilters().size() + " " + LocaleController.getString(R.string.RegexFiltersAmount), false);
    }

    private void onLocalPremiumChanged() {
        final int account = UserConfig.selectedAccount;
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.premiumStatusChangedGlobal);
        getMediaDataController().loadPremiumPromo(false);
        getMediaDataController().loadReactions(false, null);
    }

    private void onKeepAliveChanged() {
        // restart the push service so that it (re)enters or leaves the foreground
        ApplicationLoader.applicationContext.stopService(new android.content.Intent(ApplicationLoader.applicationContext, org.telegram.messenger.NotificationsService.class));
        ApplicationLoader.startPushService();
    }

    private TextCheckCell addCheck(Context context, int titleRes, Getter getter, Setter setter, boolean divider, Runnable onChanged) {
        final TextCheckCell cell = new TextCheckCell(context);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setTextAndCheck(LocaleController.getString(titleRes), getter.get(), divider);
        cell.setOnClickListener(v -> {
            setter.set(!getter.get());
            cell.setChecked(getter.get());
            if (onChanged != null) {
                onChanged.run();
            }
        });
        list.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
    }

    private TextCell addCell(Context context, String title, boolean divider) {
        final TextCell cell = new TextCell(context);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cell.setText(title, divider);
        list.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return cell;
    }

    private void addHeader(Context context, int titleRes) {
        final HeaderCell header = new HeaderCell(context);
        header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        header.setText(LocaleController.getString(titleRes));
        list.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addInfo(Context context, int textRes) {
        final TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(LocaleController.getString(textRes));
        list.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
}
