package org.telegram.messenger.plugins;

import android.content.Context;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import org.telegram.messenger.AndroidUtilities;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public final class PythonPlugins {
    private static final ExecutorService queue = Executors.newSingleThreadExecutor();
    private static volatile PyObject engine;
    public interface Result { void done(String result, String error); }
    private static PyObject engine(Context context) {
        if (engine == null) {
            if (!Python.isStarted()) Python.start(new AndroidPlatform(context.getApplicationContext()));
            engine = Python.getInstance().getModule("claud_engine").callAttr("initialize",
                new File(context.getFilesDir(), "python_plugins").getAbsolutePath());
            try {
                for (String file : context.getAssets().list("claud_plugins")) {
                    if (!file.endsWith(".plugin")) continue;
                    try (InputStream input = context.getAssets().open("claud_plugins/"+file)) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
                        while ((count=input.read(buffer))!=-1) bytes.write(buffer,0,count);
                        engine.callAttr("install_bundled",bytes.toString("UTF-8"),"custom_profile.plugin".equals(file));
                    } catch (Exception error) { org.telegram.messenger.FileLog.e(error); }
                }
            } catch (Exception error) { org.telegram.messenger.FileLog.e(error); }

        }
        return engine;
    }
    public static void call(Context c, String method, Result callback, Object... args) {
        Context app = c.getApplicationContext();
        queue.execute(() -> {
            String value = "", error = null;
            try { PyObject result = engine(app).callAttr(method, args); value = result == null ? "" : result.toString(); }
            catch (Exception e) { error = e.getMessage(); }
            final String answer = value, failure = error;
            if (callback != null) AndroidUtilities.runOnUIThread(() -> callback.done(answer, failure));
        });
    }
    public static void uiCall(Context c, String method, Result callback, Object... args) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (engine == null) throw new IllegalStateException("Движок ещё запускается. Повторите попытку.");
                PyObject answer = engine.callAttr(method, args);
                callback.done(answer == null ? "" : answer.toString(), null);
            } catch (Exception e) { callback.done("", e.getMessage()); }
        });
    }
    public static android.view.View customView(String pid, int index, Context c) {
        if (engine == null) throw new IllegalStateException("Plugin engine unavailable");
        return engine.callAttr("custom_view", pid, index, c).toJava(android.view.View.class);
    }
    public static void importFile(Context c, android.net.Uri uri, Result callback) {
        Context app = c.getApplicationContext();
        queue.execute(() -> {
            String value = "", error = null;
            try (InputStream input = app.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new IOException("File unavailable");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (out.size() + count > 4194304) throw new IOException("Maximum size is 4 MB");
                    out.write(buffer, 0, count);
                }
                value = engine(app).callAttr("install", out.toString("UTF-8")).toString();
            } catch (Exception e) { error = e.getMessage(); }
            final String answer = value, failure = error;
            AndroidUtilities.runOnUIThread(() -> callback.done(answer, failure));
        });
    }
}
