package org.telegram.messenger.plugins;

import java.lang.reflect.Member;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Java/Python callback adapter; native hooks are provided by AliuHook. */
public final class JavaHookBridge {
    public interface Callback {
        void before(XC_MethodHook.MethodHookParam param);
        void after(XC_MethodHook.MethodHookParam param);
    }
    private static volatile boolean verified;
    private static int probe(int value) { return value + 1; }
    private static synchronized void verify() {
        if (verified) return;
        XC_MethodHook.Unhook test = null;
        try {
            java.lang.reflect.Method method = JavaHookBridge.class.getDeclaredMethod("probe", int.class);
            method.setAccessible(true);
            test = XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) { param.setResult(42); }
            });
            if (((Number) method.invoke(null,0)).intValue() != 42) throw new IllegalStateException("Hook verification failed");
            test.unhook(); test = null;
            if (((Number) method.invoke(null,0)).intValue() != 1) throw new IllegalStateException("Unhook verification failed");
            verified = true;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Native hook engine unavailable on this Android build", error);
        } finally { if (test != null) test.unhook(); }
    }
    public static XC_MethodHook.Unhook hook(Member member, final Callback callback, int priority) {
        verify();
        if (member == null || callback == null) throw new IllegalArgumentException("Hook target and callback required");
        return XposedBridge.hookMethod(member, new XC_MethodHook(priority) {
            @Override protected void beforeHookedMethod(MethodHookParam param) { callback.before(param); }
            @Override protected void afterHookedMethod(MethodHookParam param) { callback.after(param); }
        });
    }
}
