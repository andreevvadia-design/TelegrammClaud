import org.telegram.messenger.music.SoundCloudPublic;
public class SoundCloudPaginationProbe {
 public static void main(String[] args)throws Exception{
  SoundCloudPublic.SearchPage one=SoundCloudPublic.searchPage("lofi",null);
  if(one.next==null)throw new AssertionError("No next page");
  SoundCloudPublic.SearchPage two=SoundCloudPublic.searchPage("lofi",one.next);
  java.util.HashSet<String> ids=new java.util.HashSet<>();for(SoundCloudPublic.Track t:one.tracks)ids.add(t.id);int before=ids.size();for(SoundCloudPublic.Track t:two.tracks)ids.add(t.id);
  if(ids.size()<=before||ids.size()<=24)throw new AssertionError("Pagination did not extend results");
  System.out.println("Live pagination passed: page1="+one.tracks.size()+", page2="+two.tracks.size()+", unique="+ids.size());
 }
}
