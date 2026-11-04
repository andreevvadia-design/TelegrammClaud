package org.telegram.messenger.music;

import java.net.URI;
import java.net.URLEncoder;
import java.util.*;

/** Canonical links only; never accepts user HTML, credentials or arbitrary embed URLs. */
public final class MusicLink {
 public final String source,original,embed;
 private MusicLink(String source,String original,String embed){this.source=source;this.original=original;this.embed=embed;}
 public static MusicLink parse(String input){try{
  URI uri=new URI(input.trim());if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getPort()!=-1)return null;
  String host=uri.getHost().toLowerCase(Locale.ROOT),path=uri.getPath();if(path==null)return null;
  if(host.equals("open.spotify.com")){
   String p=path.replaceFirst("^/intl-[a-zA-Z-]+/","/");if(!p.matches("/(track|album|playlist|artist)/[A-Za-z0-9]{22}/?"))return null;p=p.replaceFirst("/$","");return new MusicLink("Spotify","https://open.spotify.com"+p,"https://open.spotify.com/embed"+p);
  }
  if(host.equals("soundcloud.com")||host.equals("www.soundcloud.com")){
   if(!path.matches("/[A-Za-z0-9_-]+/(sets/)?[A-Za-z0-9_-]+/?"))return null;String url="https://soundcloud.com"+path;return new MusicLink("SoundCloud",url,"https://w.soundcloud.com/player/?url="+URLEncoder.encode(url,"UTF-8")+"&auto_play=false");
  }
  if(host.equals("youtu.be")||host.equals("youtube.com")||host.equals("www.youtube.com")||host.equals("music.youtube.com")){
   Map<String,String> query=new HashMap<>();if(uri.getRawQuery()!=null)for(String part:uri.getRawQuery().split("&")){String[] kv=part.split("=",2);if(kv.length==2)query.put(kv[0],kv[1]);}
   String id=host.equals("youtu.be")?path.substring(1):query.get("v");String playlist=query.get("list");
   if(id!=null&&id.matches("[A-Za-z0-9_-]{11}"))return new MusicLink("YouTube","https://www.youtube.com/watch?v="+id,"https://www.youtube.com/embed/"+id);
   if(playlist!=null&&playlist.matches("[A-Za-z0-9_-]{10,100}"))return new MusicLink("YouTube","https://www.youtube.com/playlist?list="+playlist,"https://www.youtube.com/embed/videoseries?list="+playlist);
   return null;
  }
  if(host.equals("music.yandex.ru")||host.equals("music.yandex.com")){
   if(path.matches("/album/[0-9]+/track/[0-9]+/?")){String[] p=path.split("/");return new MusicLink("Яндекс Музыка","https://music.yandex.ru/album/"+p[2]+"/track/"+p[4],"https://music.yandex.ru/iframe/#track/"+p[4]+"/"+p[2]);}
   if(path.matches("/album/[0-9]+/?")){String id=path.split("/")[2];return new MusicLink("Яндекс Музыка","https://music.yandex.ru/album/"+id,"https://music.yandex.ru/iframe/#album/"+id);}
  }
 }catch(Exception ignored){}return null;}
}
