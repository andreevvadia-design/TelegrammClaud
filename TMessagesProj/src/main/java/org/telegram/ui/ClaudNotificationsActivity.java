package org.telegram.ui;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.telegram.messenger.*;
import org.telegram.ui.ActionBar.*;

/** Separates Android notification permission/channel failures from push registration. */
public final class ClaudNotificationsActivity extends BaseFragment {
    private static final String TEST_CHANNEL="claud_notification_check";
    private TextView status;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private final Runnable poll=new Runnable(){public void run(){if(!active)return;refresh();handler.postDelayed(this,2000);}};
    private int dp(float n){return AndroidUtilities.dp(n);}
    private void button(LinearLayout box,String title,Runnable action){TextView v=new TextView(box.getContext());v.setText(title);v.setTextColor(0xFFF2E6DC);v.setTextSize(16);v.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable bg=new GradientDrawable();bg.setColor(0xFF302923);bg.setCornerRadius(dp(18));v.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);box.addView(v,lp);v.setOnClickListener(w -> action.run());}
    @Override public View createView(Context c){
        actionBar.setTitle("Проверка уведомлений");actionBar.setBackgroundColor(0xFF211F1D);actionBar.setTitleColor(0xFFF2E6DC);actionBar.setItemsColor(0xFFE9C3AE,false);actionBar.setBackButtonImage(R.drawable.ic_ab_back);actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){@Override public void onItemClick(int id){if(id==-1)finishFragment();}});
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setBackgroundColor(0xFF211F1D);LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(20),dp(20),dp(30));scroll.addView(box);
        status=new TextView(c);status.setTextColor(0xFFF2E6DC);status.setTextSize(16);status.setPadding(0,0,0,dp(20));box.addView(status);
        android.widget.Switch background=new android.widget.Switch(c);
        background.setText("Постоянная работа в фоне");background.setTextColor(0xFFF2E6DC);background.setPadding(0,dp(16),0,dp(20));background.setChecked(ClaudBackgroundService.enabled());box.addView(background);
        background.setOnCheckedChangeListener((v,on) -> {
            c.getSharedPreferences("claud_background",0).edit().putBoolean("enabled",on).apply();
            try{if(on)ClaudBackgroundService.start(c);else {c.stopService(new Intent(c,ClaudBackgroundService.class));for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++)if(UserConfig.getInstance(a).isClientActivated()){org.telegram.tgnet.ConnectionsManager cm=org.telegram.tgnet.ConnectionsManager.getInstance(a);cm.setPushConnectionEnabled(cm.isPushConnectionEnabled());}}}
            catch(Exception e){c.getSharedPreferences("claud_background",0).edit().putBoolean("enabled",false).apply();background.setChecked(false);Toast.makeText(c,"Android не разрешил запуск фоновой службы",Toast.LENGTH_LONG).show();}
        });
        button(box,"Настройки расхода батареи",() -> {try{c.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+c.getPackageName())));}catch(Exception e){Toast.makeText(c,"Открой настройки приложения в Android",Toast.LENGTH_LONG).show();}});
        button(box,"Разрешить уведомления",this::requestPermission);
        button(box,"Отправить тестовое уведомление",this::test);
        button(box,"Повторить регистрацию доставки",() -> {try{ApplicationLoader.getPushProvider().onRequestPushToken();Toast.makeText(c,"Повторная регистрация запрошена. Статус обновится ниже.",Toast.LENGTH_LONG).show();refresh();}catch(Exception e){Toast.makeText(c,"Регистрация не запустилась: "+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();}});
        button(box,"Системные настройки уведомлений",this::openSettings);
        button(box,"Настройки уведомлений Telegram",() -> presentFragment(new NotificationsSettingsActivity()));
        TextView hint=new TextView(c);hint.setTextColor(0xFFB7A99E);hint.setTextSize(14);hint.setText("Если тест появляется, но новые сообщения не дают уведомлений, проверь настройки чатов и статус регистрации доставки. Запрет Android нельзя снять из клиента без твоего выбора.");box.addView(hint);
        fragmentView=scroll;refresh();return scroll;
    }
    private void refresh(){
        if(status==null || getParentActivity()==null)return;Context c=getParentActivity();
        boolean permission=Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        boolean allowed=NotificationManagerCompat.from(c).areNotificationsEnabled();
        int blocked=0;if(Build.VERSION.SDK_INT>=26){NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);for(NotificationChannel channel:manager.getNotificationChannels())if(channel.getImportance()==NotificationManager.IMPORTANCE_NONE && !TEST_CHANNEL.equals(channel.getId()))blocked++;}
        String token=SharedConfig.pushString;String push=token==null||token.isEmpty()?"не получен":"получен";
        if("__FIREBASE_FAILED__".equals(SharedConfig.pushStringStatus) && (token==null||token.isEmpty()))push="ошибка получения";
        String services=ApplicationLoader.getPushProvider().hasServices()?"доступен":"недоступен";
        status.setText("Разрешение Android: "+(permission?"выдано":"не выдано")+"\nУведомления приложения: "+(allowed?"разрешены":"запрещены")+"\nОтключённых категорий: "+blocked+"\nСервис доставки: "+services+"\nКлюч доставки: "+push+"\nРегистрация аккаунта: "+(getUserConfig().registeredForPush?"подтверждена":"не подтверждена"));
    }
    private void requestPermission(){if(getParentActivity()==null)return;if(Build.VERSION.SDK_INT>=33 && getParentActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)getParentActivity().requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7403);else openSettings();}
    private void openSettings(){if(getParentActivity()==null)return;Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getParentActivity().getPackageName());try{getParentActivity().startActivity(i);}catch(Exception e){Intent fallback=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getParentActivity().getPackageName()));getParentActivity().startActivity(fallback);}}
    private void test(){
        if(getParentActivity()==null)return;Context c=getParentActivity();
        if((Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)||!NotificationManagerCompat.from(c).areNotificationsEnabled()){Toast.makeText(c,"Сначала разреши уведомления для клиента.",Toast.LENGTH_LONG).show();requestPermission();return;}
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){manager.createNotificationChannel(new NotificationChannel(TEST_CHANNEL,"Проверка уведомлений",NotificationManager.IMPORTANCE_DEFAULT));if(manager.getNotificationChannel(TEST_CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE){Toast.makeText(c,"Тестовая категория отключена в Android.",Toast.LENGTH_LONG).show();openSettings();return;}}
        PendingIntent intent=PendingIntent.getActivity(c,7403,new Intent(c,LaunchActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{manager.notify(7403,new NotificationCompat.Builder(c,TEST_CHANNEL).setSmallIcon(R.drawable.notification).setContentTitle("claud mode · проверка").setContentText("Android разрешает показывать уведомления клиента.").setContentIntent(intent).setAutoCancel(true).build());Toast.makeText(c,"Тест передан Android. Проверь шторку уведомлений.",Toast.LENGTH_LONG).show();}catch(SecurityException e){Toast.makeText(c,"Android запретил уведомление. Проверь системные настройки.",Toast.LENGTH_LONG).show();}
    }
    @Override public void onResume(){super.onResume();active=true;handler.removeCallbacksAndMessages(null);handler.post(poll);}
    @Override public void onPause(){active=false;handler.removeCallbacksAndMessages(null);super.onPause();}
    @Override public void onFragmentDestroy(){active=false;handler.removeCallbacksAndMessages(null);status=null;super.onFragmentDestroy();}
}
