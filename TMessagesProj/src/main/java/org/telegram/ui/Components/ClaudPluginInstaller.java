package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.plugins.PythonPlugins;
import org.telegram.ui.ActionBar.BottomSheet;

/** Metadata-only preview of a downloaded .plugin; installation is a separate explicit action. */
public final class ClaudPluginInstaller {
    private static int dp(float n) { return AndroidUtilities.dp(n); }
    private static TextView text(Context c,String value,int size,int color) { TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t; }
    private static void gap(LinearLayout box,int n) { box.addView(new android.view.View(box.getContext()),new LinearLayout.LayoutParams(1,dp(n))); }
    private static GradientDrawable background(int color,int radius) {GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
    private static TextView card(LinearLayout box,String value,int color) {TextView v=text(box.getContext(),value,15,color);v.setPadding(dp(16),dp(16),dp(16),dp(16));v.setBackground(background(0xFF302923,18));box.addView(v,new LinearLayout.LayoutParams(-1,-2));gap(box,12);return v;}
    private static TextView button(LinearLayout box,String value,boolean primary) {TextView t=text(box.getContext(),value,16,primary?0xFF211F1D:0xFFE9C3AE);t.setGravity(Gravity.CENTER);t.setTypeface(AndroidUtilities.bold());t.setPadding(dp(14),dp(17),dp(14),dp(17));t.setBackground(background(primary?0xFFDA9775:0xFF302923,20));box.addView(t,new LinearLayout.LayoutParams(-1,-2));gap(box,10);t.setFocusable(true);return t;}
    public static void show(Activity activity,File file,String filename) {
        if(activity==null || activity.isFinishing())return;
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(false);
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(16),dp(20),dp(20));scroll.addView(box);
        TextView loading=text(activity,"Читаю "+filename+"…",17,0xFFF2E6DC);box.addView(loading);
        BottomSheet sheet=new BottomSheet.Builder(activity,false,0xFF211F1D).setCustomView(scroll).create();sheet.show();
        PythonPlugins.call(activity,"preview_file",(result,error) -> {
            if(activity.isFinishing() || !sheet.isShowing())return;
            box.removeAllViews();
            if(error!=null){card(box,"Не удалось прочитать плагин\n"+error,0xFFE9C3AE);button(box,"Закрыть",false).setOnClickListener(v -> sheet.dismiss());return;}
            try {
                JSONObject meta=new JSONObject(result);
                TextView icon=text(activity,"✦",44,0xFFDA9775);icon.setGravity(Gravity.CENTER);box.addView(icon,new LinearLayout.LayoutParams(-1,dp(74)));
                TextView title=text(activity,meta.getString("__name__"),25,0xFFF2E6DC);title.setTypeface(AndroidUtilities.bold());title.setGravity(Gravity.CENTER);box.addView(title);
                TextView author=text(activity,"Версия "+meta.getString("__version__")+" · "+meta.optString("__author__","Автор не указан"),14,0xFFB7A99E);author.setGravity(Gravity.CENTER);box.addView(author);gap(box,18);
                String old=meta.optString("installed_version");
                if(!old.isEmpty())card(box,"Установлена версия "+old+". При обновлении настройки сохранятся.",0xFFE9C3AE);
                String description=meta.optString("__description__","Описание не указано");if(description.length()>1600)description=description.substring(0,1600)+"…";
                card(box,description,0xFFF2E6DC);
                card(box,"Доступ после включения\n\n• Сообщения и обновления\n• Действия от имени аккаунта\n• Сеть и данные приложения\n\nПлагин выполняется с правами клиента. Отдельные ограничения доступа здесь не задаются.",0xFFE9C3AE);
                TextView status=card(box,"Метаданные прочитаны · код файла не запускался\nБезопасность плагина не проверена.",0xFFB7A99E);
                TextView install=button(box,old.isEmpty()?"Установить и включить":"Обновить и включить",true);
                TextView disabled=button(box,"Установить выключенным",false);
                final String fileHash=meta.getString("sha256");
                android.view.View.OnClickListener installClick=v -> {
                    boolean enable=v==install;install.setEnabled(false);disabled.setEnabled(false);install.setAlpha(.5f);disabled.setAlpha(.5f);status.setText("Устанавливаю плагин…");
                    PythonPlugins.call(activity,"install_file",(r,e) -> {
                        if(activity.isFinishing())return;
                        if(e==null){if(sheet.isShowing())sheet.dismiss();Toast.makeText(activity,enable?"Плагин установлен и включён":"Плагин установлен выключенным",Toast.LENGTH_LONG).show();}
                        else if(sheet.isShowing()){status.setText("Не удалось установить или включить плагин\n"+e);install.setEnabled(true);disabled.setEnabled(true);install.setAlpha(1);disabled.setAlpha(1);}
                    },file.getAbsolutePath(),fileHash,enable);
                };
                install.setOnClickListener(installClick);disabled.setOnClickListener(installClick);
            } catch(Exception e){card(box,"Не удалось открыть сведения: "+e.getMessage(),0xFFE9C3AE);}
        },file.getAbsolutePath());
    }
}
