package org.telegram.messenger.music;
import org.json.*;
public class SoundCloudCatalogTest{public static void main(String[] args)throws Exception{
 String path="/search/playlists_without_albums";
 SoundCloudPublic.CatalogPage p=SoundCloudPublic.parseCatalog(new JSONObject("{\"collection\":[{\"kind\":\"playlist\",\"id\":1,\"title\":\"Mix\",\"tracks\":[null]},{\"kind\":\"user\",\"id\":2,\"username\":\"A\"},{\"kind\":\"track\",\"id\":3,\"streamable\":false}]}"),path);
 if(p.playlists.size()!=1||p.artists.size()!=1||!p.tracks.isEmpty()||p.next!=null)throw new AssertionError("Catalog parsing/filtering");
 for(String bad:new String[]{"http://api-v2.soundcloud.com"+path,"https://evil.example"+path,"https://api-v2.soundcloud.com/search/users","https://user@api-v2.soundcloud.com"+path,"https://api-v2.soundcloud.com:444"+path,"https://api-v2.soundcloud.com"+path+"#x"}){boolean rejected=false;try{SoundCloudPublic.catalogCursor(bad,path);}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("Unsafe cursor accepted");}
 if(SoundCloudPublic.catalogCursor("https://api-v2.soundcloud.com"+path+"?offset=50",path)==null)throw new AssertionError("Valid cursor rejected");
 System.out.println("Catalog parser and cursor boundary tests passed.");}}