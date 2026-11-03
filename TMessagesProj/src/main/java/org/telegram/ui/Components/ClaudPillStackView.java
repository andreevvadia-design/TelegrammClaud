package org.telegram.ui.Components;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.exteragram.messenger.pillstack.core.PillRegistry;
import com.exteragram.messenger.pillstack.core.PillStackConfig;
import com.exteragram.messenger.pillstack.ui.pills.BasePill;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
/** Visible host for plugin widgets and their persistent selection. */
public final class ClaudPillStackView extends LinearLayout {
    private final Runnable rebuild=this::rebuild;
    public ClaudPillStackView(Context c) { super(c);setOrientation(VERTICAL); }
    private void rebuild() {
        removeAllViews(); List<PillRegistry.PillInfo> all=PillRegistry.all(); if(all.isEmpty()) return;
        TextView edit=new TextView(getContext());edit.setText("Виджеты Custom Profile  ›");edit.setTextColor(0xFFE9C3AE);edit.setTextSize(16);edit.setPadding(0,AndroidUtilities.dp(12),0,AndroidUtilities.dp(12));addView(edit);
        edit.setOnClickListener(v -> {
            List<PillRegistry.PillInfo> entries=PillRegistry.all();CharSequence[] titles=new CharSequence[entries.size()];boolean[] enabled=new boolean[entries.size()];
            for(int i=0;i<entries.size();i++) { titles[i]=entries.get(i).title;enabled[i]=PillStackConfig.activePills.contains(entries.get(i).id); }
            new AlertDialog.Builder(getContext()).setTitle("Виджеты").setMultiChoiceItems(titles,enabled,(d,index,on) -> { int id=entries.get(index).id;if(on) PillRegistry.activatePill(id);else PillRegistry.hidePill(id); }).setPositiveButton("Готово",null).show();
        });
        for(PillRegistry.PillInfo info:PillRegistry.active()) {
            try { BasePill pill=info.creator.create(getContext(),null);addView(pill,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(42))); }
            catch(Throwable error) { FileLog.e(error);TextView failed=new TextView(getContext());failed.setText(info.title+": не удалось загрузить виджет");failed.setTextColor(0xFFE9C3AE);addView(failed); }
        }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow();PillRegistry.observe(rebuild);rebuild(); }
    @Override protected void onDetachedFromWindow() { PillRegistry.forget(rebuild);super.onDetachedFromWindow(); }
}
