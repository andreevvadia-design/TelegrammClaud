package org.telegram.messenger.music;
import org.json.*;
public class SoundCloudPaginationTest {
 public static void main(String[] args)throws Exception{
  String next="https://api-v2.soundcloud.com/search/tracks?offset=50&q=lofi";
  JSONObject page=new JSONObject().put("collection",new JSONArray().put(new JSONObject().put("id",1).put("title","ok").put("streamable",true)).put(new JSONObject().put("id",2).put("streamable",false))).put("next_href",next);
  SoundCloudPublic.SearchPage parsed=SoundCloudPublic.parseSearchPage(page);
  if(parsed.tracks.size()!=1||!next.equals(parsed.next))throw new AssertionError("Cursor or filtering lost");
  if(SoundCloudPublic.parseSearchPage(new JSONObject().put("collection",new JSONArray())).next!=null)throw new AssertionError("Missing cursor");
  if(SoundCloudPublic.parseSearchPage(new JSONObject().put("next_href",JSONObject.NULL)).next!=null)throw new AssertionError("Null cursor");
  for(String bad:new String[]{"http://api-v2.soundcloud.com/search/tracks","https://evil.example/search/tracks","https://api-v2.soundcloud.com/users","https://user@api-v2.soundcloud.com/search/tracks","https://api-v2.soundcloud.com:444/search/tracks","https://api-v2.soundcloud.com/search/tracks#fragment"}){try{SoundCloudPublic.searchCursor(bad);throw new AssertionError(bad);}catch(java.io.IOException expected){}}
  System.out.println("Pagination parsing and cursor validation checks passed");
 }
}
