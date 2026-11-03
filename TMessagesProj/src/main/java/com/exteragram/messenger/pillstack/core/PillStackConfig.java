package com.exteragram.messenger.pillstack.core;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import org.json.JSONArray;
import org.telegram.messenger.ApplicationLoader;
/** Persistent order and visibility for plugin-provided pill widgets. */
public final class PillStackConfig {
    public static final ArrayList<Integer> activePills=new ArrayList<>();
    public static final ArrayList<Integer> hiddenPills=new ArrayList<>();
    private static boolean loaded;
    public static synchronized void load() {
        if(loaded || ApplicationLoader.applicationContext==null) return;
        loaded=true;
        android.content.SharedPreferences p=ApplicationLoader.applicationContext.getSharedPreferences("claud_pill_stack",0);
        read(p.getString("active","[]"),activePills); read(p.getString("hidden","[]"),hiddenPills); hiddenPills.removeAll(activePills);
    }
    private static void read(String json,ArrayList<Integer> target) {
        try { JSONArray a=new JSONArray(json); LinkedHashSet<Integer> unique=new LinkedHashSet<>(); for(int i=0;i<a.length();i++) unique.add(a.getInt(i)); target.addAll(unique); } catch(Exception e) { org.telegram.messenger.FileLog.e(e); }
    }
    public static synchronized void savePillsLayout() {
        if(ApplicationLoader.applicationContext==null) return;
        ApplicationLoader.applicationContext.getSharedPreferences("claud_pill_stack",0).edit().putString("active",new JSONArray(activePills).toString()).putString("hidden",new JSONArray(hiddenPills).toString()).apply();
        PillRegistry.changed();
    }
}
