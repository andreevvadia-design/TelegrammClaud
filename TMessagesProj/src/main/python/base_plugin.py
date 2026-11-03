"""Claud plugin API. Java interop is not sandboxed."""
import json
import logging
from enum import Enum
from dataclasses import dataclass
from typing import Any

class AppEvent(Enum):
    START = "app_started"
    PAUSE = "app_paused"
    RESUME = "app_resumed"
    STOP = "app_stopped"

class HookStrategy(Enum):
    DEFAULT = 0
    CANCEL = 1
    MODIFY = 2
    MODIFY_FINAL = 3

@dataclass
class HookResult:
    strategy: HookStrategy = HookStrategy.DEFAULT
    request: Any = None
    response: Any = None
    error: Any = None
    update: Any = None
    updates: Any = None
    params: Any = None

class MenuItemType(Enum):
    MESSAGE_CONTEXT_MENU = "message"
    DRAWER_MENU = "drawer"
    MAIN_MENU = "main"
    CHAT_ACTION_MENU = "chat"
    PROFILE_ACTION_MENU = "profile"

@dataclass
class MenuItemData:
    menu_type: MenuItemType
    text: str
    on_click: Any
    item_id: Any = None
    icon: Any = None
    subtext: Any = None
    condition: Any = None
    priority: int = 0

class MethodHook:
    def before_hooked_method(self, param): pass
    def after_hooked_method(self, param): pass

class MethodReplacement(MethodHook):
    def replace_hooked_method(self, param): raise NotImplementedError()
    def before_hooked_method(self, param): param.setResult(self.replace_hooked_method(param))

class BaseHook(MethodHook):
    def __init__(self, plugin=None, *, before=None, after=None, **kwargs):
        self.before, self.after = before, after
    def before_hooked_method(self, param):
        if self.before: self.before(param)
    def after_hooked_method(self, param):
        if self.after: self.after(param)

class BasePlugin:
    @property
    def id(self): return self._id
    def get_current_account(self):
        from client_utils import get_selected_account, get_hook_account
        account = get_hook_account()
        return get_selected_account() if account is None else account
    def add_menu_item(self, data):
        if not hasattr(self,"_claud_menu"): self._claud_menu = {}
        key = data.item_id or (self._id + ":" + str(len(self._claud_menu)))
        self._claud_menu[str(key)] = data
        return str(key)
    def remove_menu_item(self,key):
        return getattr(self,"_claud_menu",{}).pop(str(key),None) is not None
    def register_cleanup(self, cleanup):
        if not hasattr(self, "_claud_cleanups"): self._claud_cleanups = []
        self._claud_cleanups.append(cleanup)
        return cleanup
    def _dispose(self):
        for cleanup in reversed(getattr(self, "_claud_cleanups", [])):
            try: cleanup()
            except BaseException: self.logger.exception("Plugin cleanup failed")
        self._claud_cleanups = []
        self._claud_menu = {}
    def hook_method(self, member, xposed_hook=None, priority=None, *, before=None, after=None, **kwargs):
        if kwargs.get("before_filters") or kwargs.get("after_filters"):
            raise NotImplementedError("Hook filters are not supported by this SDK adapter yet")
        from java_hooks import install
        hook = xposed_hook if xposed_hook is not None else BaseHook(self, before=before, after=after)
        handle = install(member, hook, 50 if priority is None else priority)
        self.register_cleanup(handle.unhook)
        return handle
    def hook_all_methods(self, cls, name, xposed_hook=None, priority=None, **kwargs):
        if isinstance(cls, str):
            from hook_utils import find_class
            cls = find_class(cls)
        return [self.hook_method(method, xposed_hook, priority, **kwargs)
                for method in cls.getDeclaredMethods() if str(method.getName()) == name]
    def hook_all_constructors(self, cls, xposed_hook=None, priority=None, **kwargs):
        return [self.hook_method(method, xposed_hook, priority, **kwargs) for method in cls.getDeclaredConstructors()]
    def unhook_method(self, handle):
        if handle is not None: handle.unhook()

    def add_on_send_message_hook(self, priority=0):
        from native_events import install_send
        return install_send(self, priority)
    def add_hook(self, name, match_substring=False, priority=0):
        from native_events import install_request
        return install_request(self,name,match_substring,priority)
    def pre_request_hook(self, name, account, request): return HookResult()
    def post_request_hook(self, name, account, response, error): return HookResult()
    def on_send_message_hook(self, account, params): return HookResult()
    def on_plugin_load(self): pass
    def on_plugin_unload(self): pass
    def on_app_event(self, event_type): pass
    def on_event(self, name, payload): return payload
    def create_settings(self): return []
    def get_setting(self, key, default=None):
        return self._engine.setting(self._id, key, default)
    def set_setting(self, key, value, reload_settings=False):
        self._engine.set_setting(self._id, key, value)
    def export_settings(self):
        return json.loads(self._engine.settings_json(self._id))
    def import_settings(self, settings, reload_settings=True):
        self._engine.save_settings_json(self._id, json.dumps(settings))
    @property
    def logger(self): return logging.getLogger("plugin." + self._id)
    def log(self, message): self.logger.info(message)
