package org.telegram.messenger.music;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.widget.TextView;
import org.telegram.messenger.*;
import java.io.File;

/** Reads embedded artwork off the UI thread; never downloads from arbitrary URLs. */
public final class MusicArtwork {
 private static final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
 private static final android.util.LruCache<String,Bitmap> cache=new android.util.LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String key,Bitmap value){return value.getByteCount();}};
 public static void bind(TextView view,MessageObject message){
  String path=message.messageOwner.attachPath;File file=path!=null&&!path.isEmpty()?new File(path):FileLoader.getInstance(message.currentAccount).getPathToMessage(message.messageOwner);if(file==null||!file.isFile())return;
  String key=file.getPath()+":"+file.lastModified();view.setTag(key);Bitmap existing=cache.get(key);if(existing!=null){apply(view,key,existing);return;}
  java.lang.ref.WeakReference<TextView> ref=new java.lang.ref.WeakReference<>(view);
  worker.execute(()->{MediaMetadataRetriever metadata=new MediaMetadataRetriever();try{metadata.setDataSource(file.getPath());byte[] image=metadata.getEmbeddedPicture();if(image==null||image.length>16*1024*1024)return;BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(image,0,image.length,options);if(options.outWidth<=0||options.outHeight<=0)return;options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>640)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(image,0,image.length,options);if(bitmap==null)return;cache.put(key,bitmap);AndroidUtilities.runOnUIThread(()->{TextView target=ref.get();if(target!=null)apply(target,key,bitmap);});}catch(Exception ignored){}finally{try{metadata.release();}catch(Exception ignored){}}});
 }
 private static void apply(TextView view,String key,Bitmap bitmap){if(!key.equals(view.getTag()))return;view.setText("");BitmapDrawable drawable=new BitmapDrawable(view.getResources(),bitmap);drawable.setGravity(android.view.Gravity.FILL);view.setBackground(drawable);}
}
