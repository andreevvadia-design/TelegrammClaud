import org.telegram.messenger.music.SoundCloudPublic;
public class SoundCloudNativeProbe {
 public static void main(String[] args)throws Exception{
  java.util.ArrayList<SoundCloudPublic.Track> tracks=SoundCloudPublic.search("lofi");
  if(tracks.isEmpty())throw new AssertionError("No tracks");
  SoundCloudPublic.Stream stream=SoundCloudPublic.resolve(tracks.get(0));
  java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL(stream.url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
  try(java.io.InputStream in=connection.getInputStream()){byte[] header=in.readNBytes(128);if(!new String(header,java.nio.charset.StandardCharsets.UTF_8).startsWith("#EXTM3U"))throw new AssertionError("Not HLS");}
  finally{connection.disconnect();}
  System.out.println("Native adapter: "+tracks.size()+" public results; signed HLS manifest readable; no login or personal API key");
 }
}
