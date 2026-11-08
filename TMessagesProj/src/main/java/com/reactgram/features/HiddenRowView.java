package com.reactgram.features;

import android.content.Context;
import android.view.View;

/** Placeholder row for a message hidden by a filter. One pixel high so the layout manager still sees the item. */
public class HiddenRowView extends View {
    public HiddenRowView(Context context) {
        super(context);
        setMinimumHeight(1);
    }
}
