/*
 * ReactGram message filters screen.
 *
 * Feature from AyuGram for Android (message filters), Copyright @Radolyn, 2023, https://github.com/AyuGram/AyuGram4A,
 * GPL-2.0-or-later (see LICENSE). Screen written for ReactGram.
 */

package com.reactgram.features;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

public class RegexFiltersActivity extends BaseFragment {

    private LinearLayout list;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.RegexFilters));
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

        rebuild(context);
        return fragmentView;
    }

    private void rebuild(Context context) {
        list.removeAllViews();

        final TextCell add = new TextCell(context);
        add.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        add.setTextAndIcon(LocaleController.getString(R.string.RegexFiltersAdd), R.drawable.msg_add, false);
        add.setColors(Theme.key_windowBackgroundWhiteBlueText4, Theme.key_windowBackgroundWhiteBlueText4);
        add.setOnClickListener(v -> showEditor(context, -1));
        list.addView(add, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final List<String> filters = FeatureConfig.getFilters();
        if (!filters.isEmpty()) {
            final HeaderCell header = new HeaderCell(context);
            header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            header.setText(LocaleController.getString(R.string.RegexFiltersHeader));
            list.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 0));
        }
        for (int i = 0; i < filters.size(); i++) {
            final int index = i;
            final TextCell cell = new TextCell(context);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cell.setText(filters.get(i), i != filters.size() - 1);
            cell.setOnClickListener(v -> showEditor(context, index));
            list.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        final TextInfoPrivacyCell info = new TextInfoPrivacyCell(context);
        info.setText(LocaleController.getString(R.string.RegexFiltersAddDescription));
        list.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    /** Adds a filter ({@code index < 0}) or edits/deletes the one at {@code index}. */
    private void showEditor(Context context, int index) {
        final List<String> filters = new ArrayList<>(FeatureConfig.getFilters());
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setSingleLine(false);
        input.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        input.setHint("regex");
        input.setPadding(dp(4), dp(8), dp(4), dp(8));
        if (index >= 0) {
            input.setText(filters.get(index));
            input.setSelection(input.getText().length());
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(index >= 0 ? R.string.RegexFiltersEdit : R.string.RegexFiltersAdd));
        builder.setView(input);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), null);
        builder.setNegativeButton(LocaleController.getString(index >= 0 ? R.string.Delete : R.string.Cancel), (d, w) -> {
            if (index >= 0) {
                filters.remove(index);
                FeatureConfig.setFilters(filters);
                rebuild(context);
            }
        });
        final AlertDialog dialog = builder.create();
        showDialog(dialog);

        // keep the dialog open while the expression is invalid
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                input.setError(LocaleController.getString(R.string.RegexFiltersAddError));
                return;
            }
            final String error = MessageFilter.validate(text);
            if (error != null) {
                input.setError(LocaleController.getString(R.string.RegexFiltersAddError) + ": " + error);
                return;
            }
            if (index >= 0) {
                filters.set(index, text);
            } else {
                filters.add(text);
            }
            FeatureConfig.setFilters(filters);
            rebuild(context);
            dialog.dismiss();
        });
    }
}
