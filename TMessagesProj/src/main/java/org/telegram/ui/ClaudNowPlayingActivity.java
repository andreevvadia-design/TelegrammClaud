package org.telegram.ui;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.telegram.messenger.*;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.ClaudLyrics;
import java.util.*;

/** Native claud music player on top of Telegram's existing playback service. */
public final class ClaudNowPlayingActivity extends BaseFragment {
 private String artworkKey="";
 private TextView title,artist,time,play,heart,art;private SeekBar seek;private boolean dragging,active;
 private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable update=new Runnable(){public void run(){if(!active)return;refresh();handler.postDelayed(this,500);}};
 private int dp(float n){return AndroidUtilities.dp(n);}
 private MessageObject song(){MessageObject m=MediaController.getInstance().getPlayingMessageObject();return m!=null&&m.currentAccount==currentAccount&&m.getDocument()!=null&&m.isMusic()?m:null;}
 private TextView text(Context c,int size){TextView v=new TextView(c);v.setTextColor(0xFFF4E8DD);v.setTextSize(size);return v;}
 private String key(MessageObject m){return m.getDocument().id!=0?"tg"+m.getDocument().id:"file"+m.messageOwner.attachPath;}
 private android.content.SharedPreferences prefs(){return ApplicationLoader.applicationContext.getSharedPreferences("claud_music_"+currentAccount,0);}
 private TextView control(LinearLayout row,String name,Runnable action){TextView v=text(row.getContext(),25);String type=name.equals("|◀")?"previous":name.equals("▶|")?"next":name.equals("▶")?"play":null;
 if(type==null){v.setText(name);v.setTextSize(name.length()>2?16:25);}else{v.setText("");org.telegram.ui.Components.ClaudMusicIcon icon=new org.telegram.ui.Components.ClaudMusicIcon(type,0xFFF4E8DD);icon.setBounds(0,0,dp(27),dp(27));v.setCompoundDrawables(null,icon,null,null);v.setPadding(0,dp(20),0,0);v.setContentDescription(type.equals("previous")?"Предыдущий трек":type.equals("next")?"Следующий трек":"Воспроизвести");}v.setGravity(Gravity.CENTER);row.addView(v,new LinearLayout.LayoutParams(0,dp(68),1));v.setOnClickListener(w->action.run());return v;}
 @Override public View createView(Context c){
  actionBar.setTitle("СЕЙЧАС ИГРАЕТ");actionBar.setBackButtonImage(R.drawable.ic_ab_back);actionBar.setBackgroundColor(0xFF68403B);actionBar.setTitleColor(0xFFF4E8DD);actionBar.setItemsColor(0xFFE9C3AE,false);actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick(){public void onItemClick(int id){if(id==-1)finishFragment();}});
  ScrollView scroll=new ScrollView(c);LinearLayout box=new LinearLayout(c);box.setOrientation(1);box.setPadding(dp(28),dp(24),dp(28),dp(32));GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFF85544D,0xFF493035,0xFF211B1C});scroll.setBackground(bg);scroll.addView(box);
  TextView caption=text(c,12);caption.setText("claud music");caption.setLetterSpacing(.14f);caption.setGravity(Gravity.CENTER);caption.setPadding(0,0,0,dp(18));box.addView(caption);
  art=text(c,80);art.setText("♪");art.setGravity(Gravity.CENTER);GradientDrawable cover=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFFB16C4D,0xFF513942,0xFF322824});cover.setCornerRadius(dp(30));art.setBackground(cover);art.setClipToOutline(true);art.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(20));}});int width=c.getResources().getDisplayMetrics().widthPixels-dp(56);box.addView(art,new LinearLayout.LayoutParams(-1,Math.min(width,dp(380))));
  title=text(c,27);title.setTypeface(AndroidUtilities.bold());title.setMaxLines(2);title.setPadding(0,dp(28),0,dp(7));box.addView(title);artist=text(c,17);artist.setTextColor(0xFFC5AFA1);box.addView(artist);
  seek=new SeekBar(c);seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFEAA37D));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFF2DDCA));LinearLayout.LayoutParams seekLp=new LinearLayout.LayoutParams(-1,dp(40));seekLp.topMargin=dp(22);box.addView(seek,seekLp);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar v){dragging=true;}public void onProgressChanged(SeekBar v,int p,boolean user){}public void onStopTrackingTouch(SeekBar v){MessageObject m=song();if(m!=null)MediaController.getInstance().seekToProgress(m,v.getProgress()/1000f);dragging=false;refresh();}});
  time=text(c,13);time.setTextColor(0xFFC5AFA1);box.addView(time);
  LinearLayout transport=new LinearLayout(c);box.addView(transport);control(transport,"|◀",()->{if(song()!=null)MediaController.getInstance().playPreviousMessage();refresh();});play=control(transport,"▶",()->{MessageObject m=song();if(m!=null){if(MediaController.getInstance().isMessagePaused())MediaController.getInstance().playMessage(m);else MediaController.getInstance().pauseMessage(m);refresh();}});GradientDrawable circle=new GradientDrawable();circle.setColor(0xFFEFAB82);circle.setCornerRadius(dp(36));play.setBackground(circle);control(transport,"▶|",()->{if(song()!=null)MediaController.getInstance().playNextMessage();refresh();});
  LinearLayout actions=new LinearLayout(c);box.addView(actions);heart=control(actions,"♡",()->{MessageObject m=song();if(m==null)return;Set<String> set=new HashSet<>(prefs().getStringSet("set:Избранное",Collections.emptySet()));if(!set.add(key(m)))set.remove(key(m));prefs().edit().putStringSet("set:Избранное",set).apply();refresh();});control(actions,"Текст",()->{MessageObject m=song();if(m!=null)ClaudLyrics.show(getParentActivity(),m);});control(actions,"Очередь",this::queue);
  fragmentView=scroll;refresh();return scroll;
 }
 private void queue(){ArrayList<MessageObject> songs=new ArrayList<>();for(MessageObject m:MediaController.getInstance().getPlaylist())if(m.currentAccount==currentAccount&&m.getDocument()!=null&&m.isMusic())songs.add(m);String[] names=new String[songs.size()];for(int i=0;i<names.length;i++)names[i]=songs.get(i).getMusicTitle()+" · "+songs.get(i).getMusicAuthor();new AlertDialog.Builder(getParentActivity()).setTitle("Очередь").setItems(names,(d,w)->{MediaController.getInstance().setPlaylist(songs,songs.get(w),0,false,null);refresh();}).setNegativeButton("Закрыть",null).show();}
 private String clock(int seconds){return String.format(java.util.Locale.ROOT,"%d:%02d",Math.max(0,seconds)/60,Math.max(0,seconds)%60);}
 private void refresh(){if(title==null)return;MessageObject m=song();if(m==null){title.setText("Выбери трек");artist.setText("Вернись в коллекцию claud music");seek.setEnabled(false);play.setText("▶");time.setText("");return;}seek.setEnabled(true);if(!key(m).equals(artworkKey)){artworkKey=key(m);art.setTag(null);art.setText("♪");GradientDrawable placeholder=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFFB16C4D,0xFF322824});placeholder.setCornerRadius(dp(30));art.setBackground(placeholder);org.telegram.messenger.music.MusicArtwork.bind(art,m);}title.setText(m.getMusicTitle());artist.setText(m.getMusicAuthor());play.setText("");org.telegram.ui.Components.ClaudMusicIcon stateIcon=new org.telegram.ui.Components.ClaudMusicIcon(MediaController.getInstance().isMessagePaused()?"play":"pause",0xFF211713);stateIcon.setBounds(0,0,dp(28),dp(28));play.setCompoundDrawables(null,stateIcon,null,null);play.setContentDescription(MediaController.getInstance().isMessagePaused()?"Продолжить":"Пауза");if(!dragging)seek.setProgress((int)(Math.max(0,Math.min(1,m.audioProgress))*1000));time.setText(clock(m.audioProgressSec)+"                         "+clock((int)m.getDuration()));heart.setText(prefs().getStringSet("set:Избранное",Collections.emptySet()).contains(key(m))?"♥":"♡");}
 @Override public void onResume(){super.onResume();active=true;handler.removeCallbacks(update);handler.post(update);}
 @Override public void onPause(){active=false;handler.removeCallbacks(update);super.onPause();}
 @Override public void onFragmentDestroy(){active=false;handler.removeCallbacks(update);title=null;super.onFragmentDestroy();}
}
