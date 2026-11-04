package org.telegram.messenger;
import org.json.*;
public class ClaudReleaseAssetTest{
 static JSONObject asset(String name){JSONObject a=new JSONObject();try{return a.put("name",name).put("size",1234).put("browser_download_url","https://github.com/example/"+name);}catch(Exception e){throw new RuntimeException(e);}}
 static JSONObject release(String tag,String...names)throws Exception{JSONArray a=new JSONArray();for(String n:names)a.put(asset(n));return new JSONObject().put("tag_name",tag).put("assets",a);}
 public static void main(String[] args)throws Exception{
 ClaudReleaseAsset a=ClaudReleaseAsset.select(release("claud-72409","claud-mode.apk"));if(a==null||a.version!=72409||!a.url.endsWith("claud-mode.apk"))throw new AssertionError("stable");
 a=ClaudReleaseAsset.select(release("old","claud-mode-arm64-72389.apk"));if(a==null||a.version!=72389)throw new AssertionError("legacy");
 a=ClaudReleaseAsset.select(release("claud-72409","claud-mode-arm64-72409.apk","claud-mode.apk"));if(!a.url.endsWith("claud-mode.apk"))throw new AssertionError("stable preferred");
 if(ClaudReleaseAsset.select(release("invalid","claud-mode.apk"))!=null)throw new AssertionError("unversioned stable");
 if(ClaudReleaseAsset.select(release("claud-72409","other.apk"))!=null)throw new AssertionError("foreign name");
 System.out.println("Release asset selection: stable, legacy, preference and invalid metadata passed.");}
}