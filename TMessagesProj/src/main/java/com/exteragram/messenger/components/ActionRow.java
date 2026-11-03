package com.exteragram.messenger.components;

import android.content.Context;
import android.widget.LinearLayout;

/** Row type exposed for plugins that inspect grouped action layouts. */
public class ActionRow extends LinearLayout {
    public ActionRow(Context context) { super(context); setOrientation(HORIZONTAL); }
}
