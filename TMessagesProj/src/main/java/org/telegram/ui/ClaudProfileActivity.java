package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import org.json.*;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.plugins.PythonPlugins;
import org.telegram.ui.ActionBar.*;
import java.util.ArrayDeque;

/** Dedicated Custom Profile workspace. Never renders the plugin inventory. */
public final class ClaudProfileActivity extends BaseFragment {
    private final String ID;
    private final String screenTitle;
    public ClaudProfileActivity(){this("custom_profile","Оформление профиля");}
    public ClaudProfileActivity(String pluginId,String title){ID=pluginId;screenTitle=title;}
    private LinearLayout rows;
    private int generation;
    private final ArrayDeque<JSONArray> history=new ArrayDeque<>();
    private JSONArray current;
    private int dp(float n) { return AndroidUtilities.dp(n); }
    private TextView text(String title,int size,int color) { TextView t=new TextView(rows.getContext());t.setText(title);t.setTextSize(size);t.setTextColor(color);return t; }
    private TextView action(String title,Runnable run) {
        TextView t=text(title,16,0xFFF2E6DC);t.setPadding(dp(16),dp(17),dp(16),dp(17));
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF302923);bg.setCornerRadius(dp(18));t.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);rows.addView(t,lp);t.setOnClickListener(v -> run.run());t.setFocusable(true);return t;
    }
    private void error(String message) { if(rows==null)return;rows.removeAllViews();TextView info=text(message==null?"Не удалось открыть Custom Profile":message,16,0xFFE9C3AE);info.setPadding(0,dp(12),0,dp(20));rows.addView(info);action("Повторить",this::load);action("Включить «"+screenTitle+"»",() -> PythonPlugins.call(rows.getContext(),"enable",(r,e) -> { if(rows!=null) { if(e!=null) error(e);else load(); } },ID)); }
    @Override public View createView(Context c) {
        actionBar.setTitle(screenTitle);actionBar.setBackgroundColor(0xFF211F1D);actionBar.setTitleColor(0xFFF2E6DC);actionBar.setItemsColor(0xFFE9C3AE,false);actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1 && onBackPressed(true)) finishFragment();}});
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFF211F1D);rows=new LinearLayout(c);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(20),dp(20),dp(20),dp(30));scroll.addView(rows);fragmentView=scroll;load();return scroll;
    }
    private void load() {
        if(rows==null)return;int request=++generation;history.clear();rows.removeAllViews();rows.addView(text("Открываю «"+screenTitle+"»…",16,0xFFE9C3AE));
        PythonPlugins.call(rows.getContext(),"listing",(listing,loadError) -> {
            if(rows==null || request!=generation)return;
            if(loadError!=null){error(loadError);return;}
            try {
                JSONArray list=new JSONArray(listing);JSONObject plugin=null;
                for(int i=0;i<list.length();i++) if(ID.equals(list.getJSONObject(i).optString("__id__"))) plugin=list.getJSONObject(i);
                if(plugin==null){error("Встроенная функция не найдена.");return;}
                if(!plugin.optBoolean("enabled")){error(plugin.optString("error").isEmpty()?"Функция выключена. Включи её кнопкой ниже.":plugin.optString("error"));return;}
            }catch(Exception e){error(e.getMessage());return;}
            PythonPlugins.uiCall(rows.getContext(),"settings_schema",(schema,e) -> {if(rows==null || request!=generation)return;if(e!=null)error(e);else try{render(new JSONArray(schema));}catch(Exception failure){error(failure.getMessage());}},ID);
        });
    }
    private void save(JSONObject row,Object value) {
        if(rows==null)return;
        PythonPlugins.uiCall(rows.getContext(),"change_setting",(r,e) -> {if(rows==null)return;if(e!=null)Toast.makeText(rows.getContext(),e,Toast.LENGTH_LONG).show();else try{row.put("value",value);}catch(Exception ignored){}},ID,row.optInt("index"),value);
    }
    private void render(JSONArray schema) throws Exception {
        if(rows==null)return;current=schema;rows.removeAllViews();TextView title=text(history.isEmpty()?screenTitle:"Настройки",25,0xFFF2E6DC);title.setTypeface(AndroidUtilities.bold());title.setPadding(0,0,0,dp(18));rows.addView(title);
        for(int i=0;i<schema.length();i++) {
            JSONObject row=schema.getJSONObject(i);String type=row.getString("type");int index=row.getInt("index");
            if(type.equals("header")){TextView h=text(row.optString("text"),17,0xFFDA9775);h.setPadding(0,dp(15),0,dp(12));rows.addView(h);}
            else if(type.equals("text")) {
                action(row.optString("text")+"  ›",() -> PythonPlugins.uiCall(rows.getContext(),"setting_click",(child,e) -> {
                    if(rows==null)return;if(e!=null){Toast.makeText(rows.getContext(),e,Toast.LENGTH_LONG).show();return;}
                    if(!child.isEmpty())try{JSONArray next=new JSONArray(child);history.push(current);render(next);}catch(Exception ex){error(ex.getMessage());}
                },ID,index,null));
                if(!row.optString("subtext").isEmpty()){TextView hint=text(row.optString("subtext"),13,0xFFB7A99E);hint.setPadding(dp(4),0,0,dp(12));rows.addView(hint);}
            } else if(type.equals("switch")) {
                android.widget.Switch toggle=new android.widget.Switch(rows.getContext());toggle.setText(row.optString("text"));toggle.setTextColor(0xFFF2E6DC);toggle.setPadding(0,dp(12),0,dp(12));toggle.setChecked(row.optBoolean("value"));rows.addView(toggle);toggle.setOnCheckedChangeListener((v,on) -> save(row,on));
                if(!row.optString("subtext").isEmpty())rows.addView(text(row.optString("subtext"),13,0xFFB7A99E));
            } else if(type.equals("selector")) {
                JSONArray options=row.getJSONArray("items");String[] names=new String[options.length()];for(int j=0;j<names.length;j++)names[j]=options.getString(j);
                int selected=Math.max(0,Math.min(names.length-1,row.optInt("value")));
                action(row.optString("text")+" · "+(names.length>0?names[selected]:""),() -> {
                    new AlertDialog.Builder(getParentActivity()).setTitle(row.optString("text")).setItems(names,(d,which) -> {save(row,which);try{row.put("value",which);render(current);}catch(Exception e){error(e.getMessage());}}).setNegativeButton("Отмена",null).show();
                });
            } else if(type.equals("custom")) {
                View custom=PythonPlugins.customView(ID,index,rows.getContext());if(custom.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)custom.getParent()).removeView(custom);rows.addView(custom);
            } else {
                rows.addView(text(row.optString("text"),14,0xFFE9C3AE));EditText input=new EditText(rows.getContext());input.setTextColor(0xFFF2E6DC);input.setText(row.optString("value"));rows.addView(input);action("Сохранить",() -> save(row,input.getText().toString()));
            }
        }
        if(history.isEmpty() && !ID.equals("custom_profile")) {
            if(schema.length()==0){TextView info=text("Функция включена. Отдельных параметров нет — изменения работают в соответствующих экранах клиента.",15,0xFFB7A99E);info.setPadding(0,0,0,dp(16));rows.addView(info);}
            action("Выключить функцию",() -> PythonPlugins.call(rows.getContext(),"disable",(r,e) -> {if(rows!=null)error(e==null?"Функция выключена.":e);},ID));
        }
        if(history.isEmpty() && ID.equals("custom_profile"))rows.addView(new org.telegram.ui.Components.ClaudPillStackView(rows.getContext()));
    }
    @Override public boolean onBackPressed(boolean invoked) {if(!super.onBackPressed(invoked))return false;if(!history.isEmpty()){if(invoked)try{render(history.pop());}catch(Exception e){error(e.getMessage());}return false;}return true;}
    @Override public void onFragmentDestroy(){generation++;rows=null;history.clear();super.onFragmentDestroy();}
}
