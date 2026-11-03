package com.exteragram.messenger.pillstack.ui.pills;
import android.content.Context;
import android.os.SystemClock;
import android.widget.FrameLayout;
import org.telegram.ui.ActionBar.Theme;
/** Click and refresh lifecycle shared by plugin-provided widgets. */
public abstract class BasePill extends FrameLayout {
    protected final Theme.ResourcesProvider resourcesProvider;
    private long lastUpdated;
    private boolean attached;
    private final Runnable refresh=new Runnable() { public void run() { if(!attached || !isShown()) return; onUpdateData(false); schedule(); } };
    public BasePill(Context context,Theme.ResourcesProvider resourcesProvider) {
        super(context);this.resourcesProvider=resourcesProvider;setClickable(true);setFocusable(true);
        setOnClickListener(v -> onPillClicked()); setOnLongClickListener(v -> onPillLongClicked());
    }
    public abstract int getPillId();
    public abstract void onPillClicked();
    public abstract boolean onPillLongClicked();
    public abstract void onPillSelected();
    public abstract void onUpdateData(boolean force);
    public abstract void updateColors();
    public abstract long getRefreshInterval();
    public final void markDataUpdated() { lastUpdated=SystemClock.uptimeMillis(); invalidate(); }
    public final long getLastDataUpdate() { return lastUpdated; }
    private void schedule() { removeCallbacks(refresh);long interval=getRefreshInterval();if(attached && isShown() && interval>0) postDelayed(refresh,Math.max(1000,interval)); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow();attached=true;onPillSelected();schedule(); }
    @Override protected void onDetachedFromWindow() { attached=false;removeCallbacks(refresh);super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility);if(attached) { if(visibility==VISIBLE) schedule();else removeCallbacks(refresh); } }
}
