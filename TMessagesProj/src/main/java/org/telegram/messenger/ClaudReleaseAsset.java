package org.telegram.messenger;
import org.json.*;
/** Release metadata selection, independent of Android for compatibility checks. */
public final class ClaudReleaseAsset {
 public final int version;public final String url;public final long size;
 private ClaudReleaseAsset(int v,JSONObject asset)throws JSONException{version=v;url=asset.getString("browser_download_url");size=asset.getLong("size");}
 public static ClaudReleaseAsset select(JSONObject release)throws JSONException{
  JSONArray assets=release.getJSONArray("assets");String tag=release.optString("tag_name");int stable=tag.matches("claud-[0-9]{1,9}")?Integer.parseInt(tag.substring(6)):0;ClaudReleaseAsset legacy=null;
  for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);String name=a.optString("name");if(name.equals("claud-mode.apk")&&stable>0)return new ClaudReleaseAsset(stable,a);if(name.matches("claud-mode-arm64-[0-9]{1,9}\\.apk"))legacy=new ClaudReleaseAsset(Integer.parseInt(name.substring(17,name.length()-4)),a);}
  return legacy;
 }
}
