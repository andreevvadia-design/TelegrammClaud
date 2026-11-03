package org.telegram.messenger;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import org.telegram.ui.LaunchActivity;

/** User-controlled persistent message connection, with a visible ongoing notification. */
public final class ClaudBackgroundService extends Service {
    public static boolean enabled(){return ApplicationLoader.applicationContext.getSharedPreferences("claud_background",0).getBoolean("enabled",false);}
    public static void start(Context c){Intent i=new Intent(c,ClaudBackgroundService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("claud_background","Фоновая доставка",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,8401,new Intent(this,LaunchActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new NotificationCompat.Builder(this,"claud_background").setSmallIcon(R.drawable.notification).setContentTitle("claud mode работает в фоне").setContentText("Поддерживается соединение для сообщений. Управление: claud mode → Уведомления.").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(8401,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(8401,n);
        ApplicationLoader.postInitApplication();
        for(int a=0;a<UserConfig.MAX_ACCOUNT_COUNT;a++)if(UserConfig.getInstance(a).isClientActivated())org.telegram.tgnet.ConnectionsManager.getInstance(a).setPushConnectionEnabled(true);
    }
    @Override public int onStartCommand(Intent i,int flags,int id){if(!enabled()){stopSelf();return START_NOT_STICKY;}return START_STICKY;}
    @Override public IBinder onBind(Intent i){return null;}
}
