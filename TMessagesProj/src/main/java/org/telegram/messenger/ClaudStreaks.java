package org.telegram.messenger;
import android.content.SharedPreferences;
import java.util.*;
import org.json.JSONObject;
/** Local daily activity only, no message text is stored or sent. Day boundary is UTC. */
public final class ClaudStreaks {
 private static SharedPreferences prefs(int account){return ApplicationLoader.applicationContext.getSharedPreferences("claud_streaks_"+account,0);}
 public static synchronized void record(int account,long dialog,int date,boolean outgoing){
  if(dialog<=0||dialog==UserConfig.getInstance(account).getClientUserId())return;
  org.telegram.tgnet.TLRPC.User user=MessagesController.getInstance(account).getUser(dialog);if(user==null||user.bot)return;
  long day=((long)date)/86400;long today=org.telegram.tgnet.ConnectionsManager.getInstance(account).getCurrentTime()/86400L;
  if(day<today-90||day>today)return;
  try{SharedPreferences p=prefs(account);JSONObject days=new JSONObject(p.getString("d"+dialog,"{}"));String key=""+day;int mask=days.optInt(key);int updated=mask|(outgoing?1:2);if(updated==mask)return;days.put(key,updated);
   java.util.Iterator<String> it=days.keys();java.util.ArrayList<String> old=new java.util.ArrayList<>();while(it.hasNext()){String k=it.next();if(Long.parseLong(k)<today-90)old.add(k);}for(String k:old)days.remove(k);p.edit().putString("d"+dialog,days.toString()).apply();
  }catch(Exception e){FileLog.e(e);}
 }
 public static int count(int account,long dialog){try{JSONObject days=new JSONObject(prefs(account).getString("d"+dialog,"{}"));long day=org.telegram.tgnet.ConnectionsManager.getInstance(account).getCurrentTime()/86400L;if(days.optInt(""+day)!=3)day--;int count=0;while(days.optInt(""+day)==3){count++;day--;}return count;}catch(Exception e){return 0;}}
 public static void event(int account,int id,Object[] args){
  try{
   if(id==NotificationCenter.didReceiveNewMessages && args.length>=3 && !Boolean.TRUE.equals(args[2]) && args[1] instanceof java.util.ArrayList){
    for(Object item:(java.util.ArrayList<?>)args[1])if(item instanceof MessageObject){MessageObject m=(MessageObject)item;if(m.getId()>0&&m.messageOwner.action==null)record(account,m.getDialogId(),m.messageOwner.date,m.isOutOwner());}
   }else if(id==NotificationCenter.messageReceivedByServer && args.length>=7 && !Boolean.TRUE.equals(args[6]) && args[3] instanceof Long){record(account,(Long)args[3],org.telegram.tgnet.ConnectionsManager.getInstance(account).getCurrentTime(),true);}
  }catch(Exception e){FileLog.e(e);}
 }
}
