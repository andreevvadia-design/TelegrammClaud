"""Native hook lifecycle adapter. Imports native bridge only on first use."""
class Handle:
    def __init__(self, native, callback):
        self.native, self.callback = native, callback
    def unhook(self):
        if self.native is not None:
            self.native.unhook()
            self.native = None
            self.callback = None

def install(member, hook, priority=50):
    from java import jclass, dynamic_proxy
    bridge = jclass("org.telegram.messenger.plugins.JavaHookBridge")
    interface = jclass("org.telegram.messenger.plugins.JavaHookBridge$Callback")
    class Callback(dynamic_proxy(interface)):
        def before(self, param): hook.before_hooked_method(param)
        def after(self, param): hook.after_hooked_method(param)
    callback = Callback()
    return Handle(bridge.hook(member, callback, int(priority)), callback)
