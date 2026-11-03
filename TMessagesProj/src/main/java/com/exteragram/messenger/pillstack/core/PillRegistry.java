package com.exteragram.messenger.pillstack.core;
import android.content.Context;
import java.util.*;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import com.exteragram.messenger.pillstack.ui.pills.BasePill;
/** Host registry for the exact API referenced by Custom Profile 1.9. */
public final class PillRegistry {
    public interface PillCreator { BasePill create(Context context,Theme.ResourcesProvider resourcesProvider); }
    public static final class PillInfo {
        public final int id,icon,colorTop,colorBottom;
        public final CharSequence title;
        public final PillCreator creator;
        public PillInfo(int id,CharSequence title,int icon,int colorTop,int colorBottom,PillCreator creator) {
            if(creator==null) throw new IllegalArgumentException("Missing pill creator");
            this.id=id;this.title=title;this.icon=icon;this.colorTop=colorTop;this.colorBottom=colorBottom;this.creator=creator;
        }
    }
    private static final LinkedHashMap<Integer,PillInfo> entries=new LinkedHashMap<>();
    private static final Set<Runnable> listeners=Collections.newSetFromMap(new WeakHashMap<>());
    private static int transactions;
    private static boolean dirty;
    public static synchronized void beginTransaction() { PillStackConfig.load(); transactions++; }
    public static synchronized void endTransaction() { if(transactions==0) throw new IllegalStateException("Unbalanced pill transaction"); if(--transactions==0 && dirty) { dirty=false;changed(); } }
    public static synchronized void register(PillInfo info) { PillStackConfig.load(); entries.put(info.id,info); if(!PillStackConfig.activePills.contains(info.id) && !PillStackConfig.hiddenPills.contains(info.id)) PillStackConfig.hiddenPills.add(info.id); changed(); }
    public static synchronized void unregister(int id) { entries.remove(id); changed(); }
    public static synchronized void activatePill(int id) { if(!entries.containsKey(id)) throw new IllegalArgumentException("Unknown pill "+id); PillStackConfig.hiddenPills.remove(Integer.valueOf(id)); if(!PillStackConfig.activePills.contains(id)) PillStackConfig.activePills.add(id); PillStackConfig.savePillsLayout(); }
    public static synchronized void hidePill(int id) { PillStackConfig.activePills.remove(Integer.valueOf(id)); if(!PillStackConfig.hiddenPills.contains(id)) PillStackConfig.hiddenPills.add(id); PillStackConfig.savePillsLayout(); }
    public static synchronized List<PillInfo> all() { return new ArrayList<>(entries.values()); }
    public static synchronized List<PillInfo> active() { List<PillInfo> result=new ArrayList<>(); for(int id:PillStackConfig.activePills) { PillInfo info=entries.get(id);if(info!=null) result.add(info); } return result; }
    public static synchronized void observe(Runnable listener) { listeners.add(listener); }
    public static synchronized void forget(Runnable listener) { listeners.remove(listener); }
    public static synchronized void changed() { if(transactions>0) { dirty=true;return; } for(Runnable listener:new ArrayList<>(listeners)) AndroidUtilities.runOnUIThread(listener); }
}
