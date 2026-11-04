package org.telegram.messenger.music;
import android.content.SharedPreferences;
import org.json.*;
import org.telegram.messenger.ApplicationLoader;
import java.util.*;
/** Account-scoped collections. These are claud subscriptions, not SoundCloud account follows. */
public final class SoundCloudLibrary {
 private static SharedPreferences prefs(int account){return ApplicationLoader.applicationContext.getSharedPreferences("claud_soundcloud_"+account,0);}
 private static JSONObject read(int account,String key){try{return new JSONObject(prefs(account).getString(key,"{}"));}catch(Exception e){return new JSONObject();}}
 private static void save(int account,String key,JSONObject value){prefs(account).edit().putString(key,value.toString()).apply();}
 public static String create(int account,String name){name=name.trim();if(name.isEmpty()||name.length()>100)throw new IllegalArgumentException("Название: от 1 до 100 символов");String id=UUID.randomUUID().toString();JSONObject all=read(account,"playlists");try{all.put(id,new JSONObject().put("name",name).put("tracks",new JSONArray()));}catch(Exception e){throw new IllegalStateException(e);}save(account,"playlists",all);return id;}
 public static LinkedHashMap<String,String> playlists(int account){LinkedHashMap<String,String> result=new LinkedHashMap<>();result.put("favorites","Любимые треки");JSONObject all=read(account,"playlists");Iterator<String> keys=all.keys();while(keys.hasNext()){String id=keys.next();JSONObject item=all.optJSONObject(id);if(item!=null)result.put(id,item.optString("name"));}return result;}
 public static ArrayList<SoundCloudPublic.Track> tracks(int account,String id){JSONArray a;if(id.equals("favorites"))a=read(account,"favorites").optJSONArray("tracks");else{JSONObject p=read(account,"playlists").optJSONObject(id);a=p==null?null:p.optJSONArray("tracks");}ArrayList<SoundCloudPublic.Track> result=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)result.add(new SoundCloudPublic.Track(o));}return result;}
 public static boolean contains(int account,String playlist,String track){for(SoundCloudPublic.Track t:tracks(account,playlist))if(t.id.equals(track))return true;return false;}
 public static void add(int account,String id,SoundCloudPublic.Track track){ArrayList<SoundCloudPublic.Track> songs=tracks(account,id);for(SoundCloudPublic.Track t:songs)if(t.id.equals(track.id))return;songs.add(track);writeTracks(account,id,songs);}
 public static void remove(int account,String id,String track){ArrayList<SoundCloudPublic.Track> songs=tracks(account,id);songs.removeIf(t->t.id.equals(track));writeTracks(account,id,songs);}
 private static void writeTracks(int account,String id,ArrayList<SoundCloudPublic.Track> songs){JSONArray a=new JSONArray();for(SoundCloudPublic.Track t:songs)a.put(t.raw);try{if(id.equals("favorites")){save(account,"favorites",new JSONObject().put("tracks",a));return;}JSONObject all=read(account,"playlists"),p=all.optJSONObject(id);if(p==null)return;p.put("tracks",a);save(account,"playlists",all);}catch(Exception e){throw new IllegalStateException(e);}}
 public static String importPlaylist(int account,String name,ArrayList<SoundCloudPublic.Track> tracks){if(name==null||name.trim().isEmpty())name="SoundCloud";String id=create(account,name.length()>100?name.substring(0,100):name);writeTracks(account,id,tracks);return id;}
 public static void delete(int account,String id){JSONObject all=read(account,"playlists");all.remove(id);save(account,"playlists",all);}
 public static void rename(int account,String id,String name){name=name.trim();if(name.isEmpty()||name.length()>100)throw new IllegalArgumentException("Название: от 1 до 100 символов");JSONObject all=read(account,"playlists"),p=all.optJSONObject(id);if(p==null)return;try{p.put("name",name);}catch(Exception ignored){}save(account,"playlists",all);}
 public static boolean following(int account,String id){return read(account,"following").has(id);}
 public static void follow(int account,SoundCloudPublic.Artist artist){JSONObject all=read(account,"following");if(all.has(artist.id))all.remove(artist.id);else try{all.put(artist.id,artist.raw);}catch(Exception ignored){}save(account,"following",all);}
 public static ArrayList<SoundCloudPublic.Artist> following(int account){JSONObject all=read(account,"following");ArrayList<SoundCloudPublic.Artist> result=new ArrayList<>();Iterator<String> ids=all.keys();while(ids.hasNext()){JSONObject o=all.optJSONObject(ids.next());if(o!=null)result.add(new SoundCloudPublic.Artist(o));}return result;}
}
