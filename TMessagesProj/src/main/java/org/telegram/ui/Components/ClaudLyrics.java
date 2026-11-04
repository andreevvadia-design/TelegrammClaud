package org.telegram.ui.Components;
import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.*;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.AlertDialog;
/** Lyrics stay on the device and are keyed by account and audio document. */
public final class ClaudLyrics {
 public static void show(Activity a,MessageObject m){
  if(a==null||m==null||m.getDocument()==null)return;
  String key=m.currentAccount+":"+(m.getDocument().id!=0?"tg"+m.getDocument().id:"file"+m.messageOwner.attachPath);
  SharedPreferences prefs=a.getSharedPreferences("claud_lyrics",0);
  String text=prefs.getString(key,"");
  ScrollView scroll=new ScrollView(a);TextView words=new TextView(a);words.setTextSize(20);words.setTextColor(0xFFF2E6DC);words.setPadding(32,24,32,24);words.setTextIsSelectable(true);words.setText(text.isEmpty()?"Текст для этого трека ещё не добавлен. Можно вставить его вручную — он сохранится только на этом устройстве.":text);scroll.addView(words);scroll.setBackgroundColor(0xFF211F1D);
  new AlertDialog.Builder(a).setTitle("Текст песни").setView(scroll).setPositiveButton("Закрыть",null).setNeutralButton("Изменить текст",(d,w)->{
   EditText input=new EditText(a);input.setText(prefs.getString(key,""));input.setMinLines(8);input.setMaxLines(16);input.setGravity(android.view.Gravity.TOP);input.setHint("Вставь слова песни");input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(50000)});
   new AlertDialog.Builder(a).setTitle("Текст песни").setView(input).setPositiveButton("Сохранить",(dd,ww)->{prefs.edit().putString(key,input.getText().toString()).apply();show(a,m);}).setNegativeButton("Отмена",null).show();
  }).show();
 }
}
