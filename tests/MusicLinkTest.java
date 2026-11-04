import org.telegram.messenger.music.MusicLink;
public class MusicLinkTest {
 public static void main(String[] args){
 String[] good={"https://open.spotify.com/track/0Lr4kGOYn9l83EjuK6cZFQ?si=abc","https://open.spotify.com/intl-ru/playlist/37i9dQZF1DXcBWIGoYBM5M","https://soundcloud.com/artist/song","https://soundcloud.com/artist/sets/collection","https://youtu.be/dQw4w9WgXcQ","https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ","https://music.yandex.ru/album/419460/track/71263","https://music.yandex.ru/album/419460"};
 for(String s:good)if(MusicLink.parse(s)==null)throw new AssertionError(s);
 String[] bad={"javascript:alert(1)","http://open.spotify.com/track/0Lr4kGOYn9l83EjuK6cZFQ","https://open.spotify.com.evil.test/track/0Lr4kGOYn9l83EjuK6cZFQ","https://user:pass@open.spotify.com/track/0Lr4kGOYn9l83EjuK6cZFQ","https://open.spotify.com:444/track/0Lr4kGOYn9l83EjuK6cZFQ","https://youtube.com/watch?v=%22%3E","https://soundcloud.com/a/<script>","https://music.yandex.ru/album/abc"};
 for(String s:bad)if(MusicLink.parse(s)!=null)throw new AssertionError(s);
 if(!MusicLink.parse(good[6]).embed.endsWith("#track/71263/419460"))throw new AssertionError("Yandex ordering");
 System.out.println("17 music link checks passed");
 }
}
