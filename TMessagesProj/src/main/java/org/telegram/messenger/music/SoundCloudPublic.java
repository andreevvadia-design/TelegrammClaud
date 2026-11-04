package org.telegram.messenger.music;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Anonymous public web catalog adapter. No user cookies, private audio or paid-track unlocking. */
public final class SoundCloudPublic {
 public static final class Track {
  public final String id,title,artist,artwork,permalink;public final long duration;
  final JSONObject raw;
  Track(JSONObject o){raw=o;id=o.optString("id");title=o.optString("title");JSONObject user=o.optJSONObject("user");artist=o.optString("metadata_artist",user==null?"":user.optString("username"));artwork=o.optString("artwork_url");permalink=o.optString("permalink_url");duration=o.optLong("duration");}
 }
 public static final class Stream {public final String url,type;Stream(String url,String type){this.url=url;this.type=type;}}
 private static String client;private static long expires;
 private static String get(String value,int max)throws Exception{
  URL url=new URL(value);String host=url.getHost();if(!"https".equals(url.getProtocol())||!(host.equals("soundcloud.com")||host.equals("a-v2.sndcdn.com")||host.equals("api-v2.soundcloud.com")))throw new IOException("Unexpected provider host");
  HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0");
  try{int status=c.getResponseCode();if(status!=200)throw new IOException("SoundCloud HTTP "+status);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Response too large");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}finally{c.disconnect();}
 }
 private static synchronized String client()throws Exception{
  if(client!=null&&System.currentTimeMillis()<expires)return client;
  String html=get("https://soundcloud.com",4*1024*1024);Matcher scripts=Pattern.compile("src=\"(https://a-v2\\.sndcdn\\.com/assets/[^\"]+\\.js)\"").matcher(html);ArrayList<String> assets=new ArrayList<>();while(scripts.find())assets.add(scripts.group(1));Collections.reverse(assets);
  int count=0;for(String asset:assets){if(++count>24)break;Matcher id=Pattern.compile("client_id:\"([A-Za-z0-9]{20,64})\"").matcher(get(asset,12*1024*1024));if(id.find()){client=id.group(1);expires=System.currentTimeMillis()+3600000;return client;}}
  throw new IOException("SoundCloud web catalog changed; update the connector");
 }
 private static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 public static final class SearchPage {
  public final ArrayList<Track> tracks;public final String next;
  SearchPage(ArrayList<Track> tracks,String next){this.tracks=tracks;this.next=next;}
 }
 static String searchCursor(String value)throws Exception{
  if(value==null||value.isEmpty()||value.equals("null"))return null;
  URI uri=new URI(value);if(!"https".equalsIgnoreCase(uri.getScheme())||!"api-v2.soundcloud.com".equalsIgnoreCase(uri.getHost())||!"/search/tracks".equals(uri.getPath())||uri.getUserInfo()!=null||uri.getFragment()!=null||(uri.getPort()!=-1&&uri.getPort()!=443))throw new IOException("Unexpected search cursor");return value;
 }
 static SearchPage parseSearchPage(JSONObject data)throws Exception{
  JSONArray items=data.optJSONArray("collection");ArrayList<Track> result=new ArrayList<>();if(items!=null)for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);if(item.optBoolean("streamable")&&!item.optString("policy").equals("BLOCK"))result.add(new Track(item));}return new SearchPage(result,searchCursor(data.optString("next_href",null)));
 }
 public static SearchPage searchPage(String query,String cursor)throws Exception{
  query=query.trim();if(query.isEmpty()||query.length()>200)throw new IllegalArgumentException("Введите название до 200 символов");
  String url=cursor==null?"https://api-v2.soundcloud.com/search/tracks?q="+enc(query)+"&client_id="+enc(client())+"&limit=50&linked_partitioning=1":searchCursor(cursor);
  if(cursor!=null){URI u=new URI(url);StringBuilder clean=new StringBuilder("https://api-v2.soundcloud.com/search/tracks?");String raw=u.getRawQuery();if(raw!=null)for(String part:raw.split("&"))if(!part.isEmpty()&&!part.split("=",2)[0].equals("client_id"))clean.append(part).append('&');url=clean.append("client_id=").append(enc(client())).toString();}
  return parseSearchPage(new JSONObject(get(url,4*1024*1024)));
 }
 public static ArrayList<Track> search(String query)throws Exception{return searchPage(query,null).tracks;}
 public static Stream resolve(Track track)throws Exception{
  track=new Track(new JSONObject(get("https://api-v2.soundcloud.com/tracks/"+numeric(track.id)+"?client_id="+enc(client()),4*1024*1024)));
  JSONObject media=track.raw.optJSONObject("media");JSONArray choices=media==null?null:media.optJSONArray("transcodings");if(choices==null)throw new IOException("Аудио недоступно");
  for(int i=0;i<choices.length();i++){JSONObject item=choices.getJSONObject(i),format=item.optJSONObject("format");if(item.optBoolean("snipped")||format==null||!"hls".equals(format.optString("protocol")))continue;String endpoint=item.getString("url");URL parsed=new URL(endpoint);if(!parsed.getProtocol().equals("https")||!parsed.getHost().equals("api-v2.soundcloud.com"))throw new IOException("Unexpected stream host");String separator=endpoint.contains("?")?"&":"?";JSONObject stream=new JSONObject(get(endpoint+separator+"client_id="+enc(client())+"&track_authorization="+enc(track.raw.optString("track_authorization")),1024*1024));String url=stream.getString("url");URL audio=new URL(url);String host=audio.getHost();if(!audio.getProtocol().equals("https")||!(host.equals("sndcdn.com")||host.endsWith(".sndcdn.com")||host.equals("playback.media-streaming.soundcloud.cloud")))throw new IOException("Unexpected audio CDN");return new Stream(url,"hls");}
  throw new IOException("Полное воспроизведение этого трека недоступно");
 }

 public static final class Artist {
  public final String id,name,avatar,description;public final int followers;final JSONObject raw;
  Artist(JSONObject o){raw=o;id=o.optString("id");name=o.optString("username");avatar=o.optString("avatar_url");description=o.optString("description");followers=o.optInt("followers_count");}
 }
 public static final class Playlist {
  public final String id,title,owner,artwork;public final int count;final JSONObject raw;
  Playlist(JSONObject o){raw=o;id=o.optString("id");title=o.optString("title");JSONObject u=o.optJSONObject("user");owner=u==null?"":u.optString("username");count=o.optInt("track_count");String art=o.optString("artwork_url");JSONArray a=o.optJSONArray("tracks");if((art.isEmpty()||art.equals("null"))&&a!=null&&a.length()>0)art=(a.optJSONObject(0)==null?"":a.optJSONObject(0).optString("artwork_url"));artwork=art;}
 }
 public static Artist artistFor(Track t){JSONObject u=t.raw.optJSONObject("user");return u==null?null:new Artist(u);}
 public static final class CatalogPage {
  public final ArrayList<Object> items=new ArrayList<>();public final ArrayList<Track> tracks=new ArrayList<>();public final ArrayList<Playlist> playlists=new ArrayList<>();public final ArrayList<Artist> artists=new ArrayList<>();public String next;
 }
 private static String numeric(String id){if(id==null||!id.matches("[0-9]{1,20}"))throw new IllegalArgumentException("Invalid SoundCloud id");return id;}
 static String catalogCursor(String value,String path)throws Exception{
  if(value==null||value.isEmpty()||value.equals("null"))return null;URI u=new URI(value);
  if(!"https".equalsIgnoreCase(u.getScheme())||!"api-v2.soundcloud.com".equalsIgnoreCase(u.getHost())||!path.equals(u.getPath())||u.getUserInfo()!=null||u.getFragment()!=null||(u.getPort()!=-1&&u.getPort()!=443))throw new IOException("Unexpected catalog cursor");return value;
 }
 public static CatalogPage catalog(String path,String query,String cursor)throws Exception{
  if(!path.matches("/search|/search/(playlists_without_albums|users)|/users/[0-9]{1,20}/(tracks|playlists_without_albums)"))throw new IllegalArgumentException("Unsupported catalog");
  String url;if(cursor==null){url="https://api-v2.soundcloud.com"+path+"?limit=50&linked_partitioning=1";if(path.equals("/search")||path.startsWith("/search/")){String q=query==null?"":query.trim();if(q.isEmpty()||q.length()>200)throw new IllegalArgumentException("Invalid query");url+="&q="+enc(q);}}else url=catalogCursor(cursor,path);
  URI u=new URI(url);StringBuilder clean=new StringBuilder("https://api-v2.soundcloud.com"+path+"?");if(u.getRawQuery()!=null)for(String part:u.getRawQuery().split("&"))if(!part.isEmpty()&&!part.split("=",2)[0].equals("client_id"))clean.append(part).append('&');url=clean.append("client_id=").append(enc(client())).toString();
  return parseCatalog(new JSONObject(get(url,4*1024*1024)),path);
 }
 static CatalogPage parseCatalog(JSONObject data,String path)throws Exception{
  CatalogPage page=new CatalogPage();page.next=catalogCursor(data.optString("next_href",null),path);JSONArray a=data.optJSONArray("collection");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String kind=o.optString("kind");if(kind.equals("user")){Artist artist=new Artist(o);page.artists.add(artist);page.items.add(artist);}else if(kind.equals("playlist")){Playlist playlist=new Playlist(o);page.playlists.add(playlist);page.items.add(playlist);}else if(o.optBoolean("streamable")&&!o.optString("policy").equals("BLOCK")){Track track=new Track(o);page.tracks.add(track);page.items.add(track);}}return page;
 }
 public static Artist artist(String id)throws Exception{return new Artist(new JSONObject(get("https://api-v2.soundcloud.com/users/"+numeric(id)+"?client_id="+enc(client()),4*1024*1024)));}
 public static Playlist playlist(String id)throws Exception{return new Playlist(new JSONObject(get("https://api-v2.soundcloud.com/playlists/"+numeric(id)+"?client_id="+enc(client()),4*1024*1024)));}
 public static int playlistSize(Playlist p){JSONArray a=p.raw.optJSONArray("tracks");return a==null?0:a.length();}
 public static ArrayList<Track> playlistTracks(Playlist playlist,int offset)throws Exception{
  JSONArray a=playlist.raw.optJSONArray("tracks");ArrayList<Track> result=new ArrayList<>();if(a==null)return result;ArrayList<String> ids=new ArrayList<>();for(int i=Math.max(0,offset);i<Math.min(a.length(),offset+50);i++)ids.add(numeric(a.getJSONObject(i).optString("id")));if(ids.isEmpty())return result;
  JSONArray resolved=new JSONArray(get("https://api-v2.soundcloud.com/tracks?ids="+enc(String.join(",",ids))+"&client_id="+enc(client()),4*1024*1024));HashMap<String,JSONObject> map=new HashMap<>();for(int i=0;i<resolved.length();i++){JSONObject o=resolved.optJSONObject(i);if(o!=null)map.put(o.optString("id"),o);}for(String id:ids){JSONObject o=map.get(id);if(o!=null&&o.optBoolean("streamable")&&!o.optString("policy").equals("BLOCK"))result.add(new Track(o));}return result;
 }

}