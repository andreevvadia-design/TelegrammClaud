package org.telegram.messenger.music;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.widget.TextView;
import java.net.*;
import java.io.*;
import org.telegram.messenger.AndroidUtilities;
public final class SoundCloudArtwork {
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newFixedThreadPool(2);
 private static final android.util.LruCache<String,Bitmap> cache=new android.util.LruCache<String,Bitmap>(6*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getByteCount();}};
 public static void bind(TextView view,String url){if(url==null||url.isEmpty()||url.equals("null"))return;try{URL u=new URL(url);if(!u.getProtocol().equals("https")||!u.getHost().endsWith(".sndcdn.com"))return;}catch(Exception e){return;}view.setTag(url);Bitmap hit=cache.get(url);if(hit!=null){apply(view,url,hit);return;}java.lang.ref.WeakReference<TextView> ref=new java.lang.ref.WeakReference<>(view);worker.execute(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);if(c.getResponseCode()!=200)return;byte[] bytes;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>4*1024*1024)return;out.write(b,0,n);}bytes=out.toByteArray();}BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>640)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)return;cache.put(url,bitmap);AndroidUtilities.runOnUIThread(()->{TextView v=ref.get();if(v!=null)apply(v,url,bitmap);});}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}});}
 private static void apply(TextView view,String url,Bitmap b){if(!url.equals(view.getTag()))return;view.setText("");BitmapDrawable d=new BitmapDrawable(view.getResources(),b);d.setGravity(android.view.Gravity.FILL);view.setBackground(d);}
}
