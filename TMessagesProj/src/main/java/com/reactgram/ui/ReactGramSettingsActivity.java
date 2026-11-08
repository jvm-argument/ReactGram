package com.reactgram.ui;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.reactgram.ReactGramConfig;
import com.reactgram.ghost.GhostConfig;
import com.reactgram.ghost.GhostSettingsActivity;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

/** Top-level ReactGram settings. Currently: the Ghost Mode entry; more features are added here. */
public class ReactGramSettingsActivity extends BaseFragment {

    private LinearLayout list;
    private TextCell ghostCell;

    @Override
    public View createView(Context context) {
        ReactGramConfig.load();
        GhostConfig.load();

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

        ghostCell = new TextCell(context);
        ghostCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        ghostCell.setOnClickListener(v -> presentFragment(new GhostSettingsActivity()));
        list.addView(ghostCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(LocaleController.getString(R.string.ReactGramGhostInfo));
        list.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        updateGhostCell();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ghostCell != null) {
            updateGhostCell();
        }
    }

    private void updateGhostCell() {
        final String state = LocaleController.getString(GhostConfig.isActive() ? R.string.ReactGramOn : R.string.ReactGramOff);
        ghostCell.setTextAndValue(LocaleController.getString(R.string.GhostMode), state, false);
    }
}
