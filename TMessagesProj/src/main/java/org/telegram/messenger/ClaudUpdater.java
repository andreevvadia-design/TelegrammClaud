package org.telegram.messenger;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.telegram.ui.ActionBar.AlertDialog;

/** Checks only this project's public releases; APK signer must match the installed app. */
public final class ClaudUpdater {
    private static final AtomicBoolean busy=new AtomicBoolean();
    private static long lastCheck;
    private static final String API="https://api.github.com/repos/andreevvadia-design/TelegrammClaud/releases/latest";
    public static void check(Activity a,boolean manual){
        if(a==null||a.isFinishing()||(!manual&&System.currentTimeMillis()-lastCheck<21600000)||!busy.compareAndSet(false,true))return;
        lastCheck=System.currentTimeMillis();
        new Thread(() -> {try{
            JSONObject release=new JSONObject(new String(fetch(API,1048576),java.nio.charset.StandardCharsets.UTF_8));
            ClaudReleaseAsset asset=ClaudReleaseAsset.select(release);
            String url=asset==null?null:asset.url;long size=asset==null?0:asset.size;int version=asset==null?0:asset.version;
            long installed=Build.VERSION.SDK_INT>=28?a.getPackageManager().getPackageInfo(a.getPackageName(),0).getLongVersionCode():a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionCode;
            if(url==null||version<=installed){if(manual)message(a,"Обновлений пока нет");return;}
            final String download=url;final long expected=size;
            AndroidUtilities.runOnUIThread(() -> {if(!a.isFinishing())new AlertDialog.Builder(a).setTitle("Обновление claud mode").setMessage("Доступна новая версия. Скачать APK ("+(expected/1048576)+" МБ)?").setPositiveButton("Скачать",(d,w)->download(a,download,expected)).setNegativeButton("Позже",null).show();});
        }catch(Exception e){if(manual)message(a,"Не удалось проверить обновления. Проверь интернет и наличие Releases.");}finally{busy.set(false);}},"claud-update-check").start();
    }
    private static byte[] fetch(String url,int limit)throws Exception{
        try(InputStream in=open(url);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Too large");out.write(b,0,n);}return out.toByteArray();}
    }
    private static InputStream open(String url)throws Exception{
        for(int redirects=0;redirects<6;redirects++){
            URL u=new URL(url);if(!"https".equals(u.getProtocol()))throw new IOException("HTTPS required");
            HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","claud-mode-updater");
            int status=c.getResponseCode();if(status>=300&&status<400){String next=c.getHeaderField("Location");c.disconnect();url=new URL(u,next).toString();continue;}
            if(status!=200){c.disconnect();throw new IOException("HTTP "+status);}return new FilterInputStream(c.getInputStream()){@Override public void close()throws IOException{try{super.close();}finally{c.disconnect();}}};
        }throw new IOException("Redirect limit");
    }
    private static void download(Activity a,String url,long size){
        if(size<=0||size>300L*1048576||!busy.compareAndSet(false,true))return;
        message(a,"Скачиваю обновление. Дождись сообщения о готовности.");
        new Thread(() -> {File file=new File(a.getFilesDir(),"cache/claud-update.apk");try{
            file.getParentFile().mkdirs();long total=0;
            try(InputStream in=open(url);OutputStream out=new FileOutputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){total+=n;if(total>size)throw new IOException("Size mismatch");out.write(b,0,n);}}
            if(total!=size)throw new IOException("Incomplete APK");
            PackageManager pm=a.getPackageManager();PackageInfo current=pm.getPackageInfo(a.getPackageName(),PackageManager.GET_SIGNATURES);PackageInfo next=pm.getPackageArchiveInfo(file.getPath(),PackageManager.GET_SIGNATURES);
            if(next==null||!a.getPackageName().equals(next.packageName)||!Arrays.equals(current.signatures,next.signatures)||next.versionCode<=current.versionCode)throw new IOException("APK identity/version mismatch");
            AndroidUtilities.runOnUIThread(() -> {if(!a.isFinishing())new AlertDialog.Builder(a).setTitle("Обновление скачано").setMessage("Подпись приложения проверена. Установить обновление?").setPositiveButton("Установить",(d,w)->install(a,file)).setNegativeButton("Позже",null).show();});
        }catch(Exception e){file.delete();message(a,"Обновление не установлено: ошибка загрузки или проверки APK. Попробуй снова.");}finally{busy.set(false);}},"claud-update-download").start();
    }
    private static void install(Activity a,File file){try{
        if(Build.VERSION.SDK_INT>=26&&!a.getPackageManager().canRequestPackageInstalls()){a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));message(a,"Разреши установку обновлений, затем повтори проверку.");return;}
        a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(a,a.getPackageName()+".provider",file),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }catch(Exception e){message(a,"Не удалось открыть установщик Android");}}
    private static void message(Activity a,String text){AndroidUtilities.runOnUIThread(() -> {if(!a.isFinishing())Toast.makeText(a,text,Toast.LENGTH_LONG).show();});}
}
