package org.telegram.ui.Components;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.Switch;
import org.telegram.messenger.AndroidUtilities;

/** In-app companion; never creates a system overlay or requests overlay permission. */
public final class ClaudPetOverlay {
    private final Activity activity;
    private ClaudPetView pet;
    private TextView bubble;
    private boolean resumed;
    private int lastPhrase=-1;
    private final Handler chatter=new Handler(Looper.getMainLooper());
    private final String[] phrases={"Я рядом. Клешни наготове!","Тут уютно. Останусь с тобой.","Пора немного потянуть клешни.","Тыкни на меня — поиграем?","У тебя всё получится.","Я сегодня в хорошем настроении.","Клод на связи!","Маленький краб — большие планы.","Мой шарф передаёт привет.","Плывём дальше?"};
    private final Runnable hideBubble=() -> { if(bubble!=null)bubble.setVisibility(View.GONE); };
    private final Runnable chatTick=new Runnable(){public void run(){
        if(!resumed)return;
        if(pet!=null && pet.isShown() && pet.hasWindowFocus() && prefs(activity).getBoolean("phrases",true)
                && org.telegram.ui.LaunchActivity.getLastFragmentIncludeMainTabs() instanceof org.telegram.ui.ChatActivity) {
            android.view.accessibility.AccessibilityManager access=(android.view.accessibility.AccessibilityManager)activity.getSystemService(Activity.ACCESSIBILITY_SERVICE);
            if(access==null || !access.isTouchExplorationEnabled())speak();
        }
        scheduleChatter();
    }};
    private void scheduleChatter(){chatter.removeCallbacks(chatTick);if(resumed)chatter.postDelayed(chatTick,40000+java.util.concurrent.ThreadLocalRandom.current().nextInt(50001));}
    private void speak(){
        if(pet==null)return;
        ViewGroup decor=(ViewGroup)activity.getWindow().getDecorView();
        if(bubble==null){bubble=new TextView(activity);bubble.setTextColor(0xFFF2E6DC);bubble.setTextSize(14);bubble.setPadding(dp(14),dp(10),dp(14),dp(10));bubble.setMaxWidth(dp(220));GradientDrawable bg=new GradientDrawable();bg.setColor(0xF5302923);bg.setCornerRadius(dp(20));bubble.setBackground(bg);bubble.setClickable(false);bubble.setFocusable(false);decor.addView(bubble,new ViewGroup.LayoutParams(-2,-2));}
        int next=java.util.concurrent.ThreadLocalRandom.current().nextInt(phrases.length-1);if(next>=lastPhrase && lastPhrase>=0)next++;lastPhrase=next;
        bubble.setText(phrases[next]);bubble.setVisibility(View.VISIBLE);bubble.bringToFront();pet.react();
        bubble.post(() -> {if(bubble!=null && pet!=null){bubble.setX(Math.max(dp(8),Math.min(decor.getWidth()-bubble.getWidth()-dp(8),pet.getX()-bubble.getWidth()/2f)));bubble.setY(Math.max(dp(32),pet.getY()-bubble.getHeight()));}});
        chatter.removeCallbacks(hideBubble);chatter.postDelayed(hideBubble,4500);
    }
    private float downX, downY, startX, startY;
    private boolean moved;
    public ClaudPetOverlay(Activity activity) { this.activity=activity; }
    private static SharedPreferences prefs(Activity a) { return a.getSharedPreferences("claud_pet",0); }
    private static int dp(float n) { return AndroidUtilities.dp(n); }
    public void resume() {
        resumed=true; scheduleChatter();
        if (!prefs(activity).getBoolean("visible",true)) return;
        ViewGroup decor=(ViewGroup)activity.getWindow().getDecorView();
        if(pet==null) {
            pet=new ClaudPetView(activity);
            pet.setContentDescription("Питомец Клод: нажми для игры и настроек, перетащи для перемещения");
            decor.addView(pet,new ViewGroup.LayoutParams(dp(76),dp(84)));
            pet.setOnClickListener(v -> showHome(activity));
            pet.setOnTouchListener((v,e) -> {
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN) {
                    hideBubble.run(); downX=e.getRawX(); downY=e.getRawY(); startX=v.getX(); startY=v.getY(); moved=false; return true;
                }
                if(e.getActionMasked()==MotionEvent.ACTION_MOVE) {
                    float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                    if(Math.abs(dx)+Math.abs(dy)>dp(8)) moved=true;
                    if(moved) { v.setX(Math.max(0,Math.min(decor.getWidth()-v.getWidth(),startX+dx))); v.setY(Math.max(dp(24),Math.min(decor.getHeight()-v.getHeight()-dp(32),startY+dy))); }
                    return true;
                }
                if(e.getActionMasked()==MotionEvent.ACTION_UP) {
                    if(!moved) v.performClick();
                    else prefs(activity).edit().putFloat("x",v.getX()/Math.max(1,decor.getWidth()-v.getWidth())).putFloat("y",v.getY()/Math.max(1,decor.getHeight()-v.getHeight())).apply();
                    return true;
                }
                return e.getActionMasked()==MotionEvent.ACTION_CANCEL;
            });
        }
        pet.setVisibility(View.VISIBLE); pet.setActive(true); pet.bringToFront();
        pet.post(() -> { if(pet!=null) { pet.setX(Math.max(0,decor.getWidth()-pet.getWidth())*prefs(activity).getFloat("x",.88f)); pet.setY(Math.max(0,decor.getHeight()-pet.getHeight())*prefs(activity).getFloat("y",.35f)); } });
    }
    public void pause() { resumed=false;chatter.removeCallbacksAndMessages(null);hideBubble.run();if(pet!=null) pet.setActive(false); }
    public void destroy() { pause();if(bubble!=null){((ViewGroup)bubble.getParent()).removeView(bubble);bubble=null;}if(pet!=null) { pet.setActive(false); ((ViewGroup)pet.getParent()).removeView(pet); pet=null; } }
    private static TextView button(Activity a, LinearLayout box,String title,Runnable action) {
        TextView v=new TextView(a); v.setText(title); v.setTextColor(0xFFF2E6DC); v.setTextSize(16); v.setGravity(Gravity.CENTER); v.setPadding(dp(12),dp(14),dp(12),dp(14));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xFF41332D); bg.setCornerRadius(dp(16)); v.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); box.addView(v,lp); v.setOnClickListener(w -> action.run()); return v;
    }
    private static void styleDialog(AlertDialog dialog) {
        if(dialog.getWindow()!=null) { GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF211F1D);bg.setCornerRadius(dp(22));dialog.getWindow().setBackgroundDrawable(bg); }
        for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}) if(dialog.getButton(id)!=null) dialog.getButton(id).setTextColor(0xFFE9B595);
    }
    public static void showHome(Activity a) {
        if(a==null || a.isFinishing()) return;
        SharedPreferences p=prefs(a);
        LinearLayout box=new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(12),dp(20),dp(20)); box.setBackgroundColor(0xFF211F1D);
        ClaudPetView preview=new ClaudPetView(a); box.addView(preview,new LinearLayout.LayoutParams(-1,dp(150)));
        TextView hello=new TextView(a); hello.setTextColor(0xFFF2E6DC); hello.setTextSize(18); hello.setGravity(Gravity.CENTER); hello.setText(p.getString("name","Клод")+": поиграем?"); box.addView(hello);
        preview.setOnClickListener(v -> { preview.react(); hello.setText("Рад тебя видеть! Давай ещё разок."); });
        button(a,box,"Изменить имя",() -> {
            EditText input=new EditText(new android.view.ContextThemeWrapper(a, android.R.style.Theme_Material_Dialog_Alert)); input.setTextColor(0xFFF2E6DC); input.setSingleLine(true); input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)}); input.setText(p.getString("name","Клод"));
            new AlertDialog.Builder(new android.view.ContextThemeWrapper(a, android.R.style.Theme_Material_Dialog_Alert)).setTitle("Имя питомца").setView(input).setNegativeButton("Отмена",null).setPositiveButton("Сохранить",(d,w) -> { String name=input.getText().toString().trim(); if(name.isEmpty()) name="Клод"; p.edit().putString("name",name).apply(); hello.setText(name+": это я!"); preview.react(); }).show();
        });
        button(a,box,"Гардероб",() -> {
            String[] ids={"none","scarf","hat","glasses"}; String current=p.getString("outfit","none"); int selected=0; for(int i=0;i<ids.length;i++) if(ids[i].equals(current)) selected=i;
            new AlertDialog.Builder(new android.view.ContextThemeWrapper(a, android.R.style.Theme_Material_Dialog_Alert)).setTitle("Наряд Клода").setSingleChoiceItems(new String[]{"Без аксессуаров","Розовый шарф","Фиолетовая шляпа","Очки"},selected,(d,w) -> { p.edit().putString("outfit",ids[w]).apply(); preview.react(); ((ViewGroup)a.getWindow().getDecorView()).invalidate(); d.dismiss(); }).setNegativeButton("Закрыть",null).show();
        });
        button(a,box,"Играть · поймай Клода",() -> play(a));
        Switch visible=new Switch(a); visible.setText("Питомец поверх вкладок и чатов"); visible.setTextColor(Color.WHITE); visible.setPadding(0,dp(20),0,0); visible.setChecked(p.getBoolean("visible",true)); box.addView(visible);
        visible.setOnCheckedChangeListener((v,on) -> { p.edit().putBoolean("visible",on).apply(); if(a instanceof org.telegram.ui.LaunchActivity) ((org.telegram.ui.LaunchActivity)a).refreshClaudPet(); });
        Switch phrases=new Switch(a);phrases.setText("Случайные фразы в чатах");phrases.setTextColor(0xFFF2E6DC);phrases.setPadding(0,dp(12),0,0);phrases.setChecked(p.getBoolean("phrases",true));box.addView(phrases);phrases.setOnCheckedChangeListener((v,on) -> p.edit().putBoolean("phrases",on).apply());
        AlertDialog dialog=new AlertDialog.Builder(new android.view.ContextThemeWrapper(a, android.R.style.Theme_Material_Dialog_Alert)).setView(box).setPositiveButton("Готово",null).create(); dialog.setOnDismissListener(d -> { preview.setActive(false); if(a instanceof org.telegram.ui.LaunchActivity) ((org.telegram.ui.LaunchActivity)a).refreshClaudPet(); }); dialog.show(); styleDialog(dialog);
    }
    private static void play(Activity a) {
        LinearLayout box=new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16),dp(16),dp(16),dp(16)); box.setBackgroundColor(0xFF211F1D);
        TextView score=new TextView(a); score.setTextColor(Color.WHITE); score.setTextSize(18); box.addView(score);
        FrameLayout arena=new FrameLayout(a); box.addView(arena,new LinearLayout.LayoutParams(-1,dp(300)));
        ClaudPetView target=new ClaudPetView(a); arena.addView(target,new FrameLayout.LayoutParams(dp(76),dp(84)));
        Handler handler=new Handler(Looper.getMainLooper()); int[] points={0}; long deadline=SystemClock.uptimeMillis()+15000; boolean[] running={true};
        Runnable jump=() -> { target.setX((float)Math.random()*Math.max(0,arena.getWidth()-target.getWidth())); target.setY((float)Math.random()*Math.max(0,arena.getHeight()-target.getHeight())); };
        target.setOnClickListener(v -> { if(running[0] && SystemClock.uptimeMillis()<deadline) { points[0]++; target.react(); jump.run(); } });
        Runnable tick=new Runnable() { public void run() { if(!running[0]) return; long left=Math.max(0,deadline-SystemClock.uptimeMillis()); score.setText("Поймано: "+points[0]+" · "+((left+999)/1000)+" с"); if(left>0) handler.postDelayed(this,150); else { running[0]=false; target.setActive(false); int best=Math.max(points[0],prefs(a).getInt("best",0)); prefs(a).edit().putInt("best",best).apply(); score.setText("Поймано: "+points[0]+" · Рекорд: "+best); } } };
        AlertDialog dialog=new AlertDialog.Builder(new android.view.ContextThemeWrapper(a, android.R.style.Theme_Material_Dialog_Alert)).setTitle("Поймай Клода за 15 секунд").setView(box).setPositiveButton("Закрыть",null).create();
        dialog.setOnDismissListener(d -> { running[0]=false; handler.removeCallbacksAndMessages(null); target.setActive(false); }); dialog.show(); styleDialog(dialog); arena.post(jump); handler.post(tick);
    }
    public void refresh() { hideBubble.run(); if(pet!=null) { boolean visible=prefs(activity).getBoolean("visible",true); pet.setVisibility(visible?View.VISIBLE:View.GONE); pet.setActive(visible); pet.react(); } resume(); }
}
