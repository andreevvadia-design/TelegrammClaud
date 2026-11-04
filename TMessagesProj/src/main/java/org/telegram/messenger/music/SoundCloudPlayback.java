package org.telegram.messenger.music;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.core.app.NotificationCompat;
import org.telegram.messenger.*;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;
import java.util.*;

/** Explicit user-started streaming service. Signed stream URLs stay only in memory. */
public final class SoundCloudPlayback extends Service {
 public static SoundCloudPublic.Track current;public static String error;private static ExoPlayer player;
 private static final ArrayList<SoundCloudPublic.Track> queue=new ArrayList<>();private static int index;
 private static long generation;private static boolean loading;private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable update=new Runnable(){public void run(){if(player==null)return;notice();handler.postDelayed(this,3000);}};
 public static void play(Context c,ArrayList<SoundCloudPublic.Track> tracks,SoundCloudPublic.Track track){queue.clear();queue.addAll(tracks);index=queue.indexOf(track);if(index<0){queue.add(track);index=queue.size()-1;}current=track;error=null;Intent i=new Intent(c,SoundCloudPlayback.class).setAction("load");if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
 public static SoundCloudPublic.Track nextTrack(){return index+1<queue.size()?queue.get(index+1):null;}
 public static ArrayList<SoundCloudPublic.Track> queueSnapshot(){return new ArrayList<>(queue);}
 public static void stop(Context c){current=null;queue.clear();c.stopService(new Intent(c,SoundCloudPlayback.class));}
 public static boolean paused(){return player==null||!player.getPlayWhenReady();}public static boolean loading(){return loading;}public static long position(){return player==null?0:Math.max(0,player.getCurrentPosition());}public static long duration(){return current==null?0:current.duration;}
 public static void toggle(){if(player!=null){if(player.getPlayWhenReady())player.pause();else player.play();}}
 public static void seek(float value){if(player!=null&&current!=null)player.seekTo((long)(Math.max(0,Math.min(1,value))*current.duration));}
 public static void next(Context c,int delta){int next=index+delta;if(next>=0&&next<queue.size())play(c,new ArrayList<>(queue),queue.get(next));}
 @Override public void onCreate(){super.onCreate();NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel("claud_soundcloud","claud music · SoundCloud",NotificationManager.IMPORTANCE_LOW));startForeground(8406,notification());player=new ExoPlayer.Builder(this).setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true).setHandleAudioBecomingNoisy(true).build();player.addListener(new Player.Listener(){public void onPlayerError(PlaybackException e){error="Не удалось воспроизвести аудио. Проверь VPN и соединение.";loading=false;notice();}public void onPlaybackStateChanged(int state){loading=state==Player.STATE_BUFFERING;if(state==Player.STATE_ENDED)next(SoundCloudPlayback.this,1);notice();}});handler.post(update);}
 @Override public int onStartCommand(Intent intent,int flags,int id){if(intent==null||current==null){stopSelf();return START_NOT_STICKY;}if("toggle".equals(intent.getAction())){toggle();notice();return START_NOT_STICKY;}final long request=++generation;final SoundCloudPublic.Track selected=current;loading=true;error=null;player.stop();org.telegram.messenger.MessageObject local=MediaController.getInstance().getPlayingMessageObject();if(local!=null)MediaController.getInstance().pauseMessage(local);notice();new Thread(()->{try{SoundCloudPublic.Stream stream=SoundCloudPublic.resolve(selected);handler.post(()->{if(request!=generation||player==null)return;player.setMediaItem(new MediaItem.Builder().setUri(stream.url).setMimeType(MimeTypes.APPLICATION_M3U8).build());player.prepare();player.play();notice();});}catch(Exception e){handler.post(()->{if(request!=generation)return;loading=false;error=e instanceof java.net.SocketTimeoutException?"SoundCloud не ответил. Проверь VPN.":"SoundCloud не предоставил полное аудио этого трека. Попробуй другой.";notice();});}},"claud-sc-stream").start();return START_NOT_STICKY;}
 private Notification notification(){PendingIntent open=PendingIntent.getActivity(this,8406,new Intent(this,LaunchActivity.class).setAction("app.claud.OPEN_SOUNDCLOUD_PLAYER").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent toggle=PendingIntent.getService(this,8407,new Intent(this,SoundCloudPlayback.class).setAction("toggle"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);return new NotificationCompat.Builder(this,"claud_soundcloud").setSmallIcon(R.drawable.notification).setContentTitle(current==null?"claud music":current.title).setContentText(error!=null?error:loading?"Подключение к SoundCloud…":current==null?"SoundCloud":current.artist+" · SoundCloud").setContentIntent(open).setOngoing(!paused()||loading).setOnlyAlertOnce(true).addAction(R.drawable.notification,paused()?"Продолжить":"Пауза",toggle).build();}
 private void notice(){if(player!=null)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(8406,notification());}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){++generation;handler.removeCallbacksAndMessages(null);if(player!=null){player.release();player=null;}loading=false;super.onDestroy();}
}
