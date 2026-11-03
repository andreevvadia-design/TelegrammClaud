from java import jclass, dynamic_proxy

def run_on_ui_thread(func, delay=0):
    from account_context import get_hook_account, account_scope
    captured_account = get_hook_account()
    class Task(dynamic_proxy(jclass("java.lang.Runnable"))):
        def run(self):
            try:
                with account_scope(captured_account): func()
            except Exception as error: log(error)
    jclass("org.telegram.messenger.AndroidUtilities").runOnUIThread(Task(), int(delay))

def log(message): jclass("android.util.Log").i("claud.plugin", str(message))

class OnClickListener(dynamic_proxy(jclass("android.view.View$OnClickListener"))):
    def __init__(self, fn): super().__init__(); self.fn = fn
    def onClick(self, view): self.fn(view)

class OnLongClickListener(dynamic_proxy(jclass("android.view.View$OnLongClickListener"))):
    def __init__(self, fn): super().__init__(); self.fn = fn
    def onLongClick(self, view): return bool(self.fn(view))

def copy_to_clipboard(text):
    context = jclass("org.telegram.messenger.ApplicationLoader").applicationContext
    clipboard = context.getSystemService("clipboard")
    clipboard.setPrimaryClip(jclass("android.content.ClipData").newPlainText("", str(text)))
