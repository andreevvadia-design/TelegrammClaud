import ast
import inspect
import hashlib
import json
import os
import re
import sys
import types
import threading
from functools import wraps
from pathlib import Path
from base_plugin import BasePlugin, AppEvent

def serialized(method):
    @wraps(method)
    def call(self, *args, **kwargs):
        with self._lock:
            return method(self, *args, **kwargs)
    return call

class Engine:
    def __init__(self, root):
        self._lock = threading.RLock()
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.loaded = {}
        self.state = self._read_state()
        self.errors = {}
        self.recovery = False
        reset = self.root / "disable_all.flag"
        if reset.exists():
            self.recovery = True
            for state in self.state.values(): state["enabled"] = False
            self._save()
            reset.unlink()
        marker = self.root / "executing.json"
        try:
            interrupted = json.loads(marker.read_text("utf-8"))
            self.recovery = True
            pid = self._id(interrupted["id"])
            self.state.setdefault(pid, {})["enabled"] = False
            self.errors[pid] = "Previous plugin execution did not finish; disabled for recovery"
            self._save()
            marker.unlink()
        except (OSError, ValueError, KeyError, TypeError): pass

    def _read_state(self):
        try:
            state = json.loads((self.root / "state.json").read_text("utf-8"))
            if not isinstance(state, dict): return {}
            return {pid: value for pid, value in state.items()
                    if isinstance(value, dict) and re.fullmatch(r"[A-Za-z][A-Za-z0-9_-]{0,63}", pid)}
        except (OSError, ValueError): return {}

    def _save(self):
        temp = self.root / "state.tmp"
        temp.write_text(json.dumps(self.state), "utf-8")
        os.replace(temp, self.root / "state.json")

    def _id(self, value):
        if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z][A-Za-z0-9_-]{0,63}", value):
            raise ValueError("Invalid plugin ID")
        return value

    def metadata(self, source):
        if len(source.encode("utf-8")) > 4194304: raise ValueError("Plugin exceeds 4 MB")
        tree = ast.parse(source)
        meta = {"__version__": "1.0"}
        for node in tree.body:
            if isinstance(node, ast.Assign):
                for target in node.targets:
                    if isinstance(target, ast.Name) and target.id in ("__id__", "__name__", "__version__", "__description__", "__author__", "__icon__", "__requirements__", "__app_version__", "__sdk_version__", "__min_version__"):
                        meta[target.id] = ast.literal_eval(node.value)
        self._id(meta.get("__id__"))
        for key in ("__name__", "__version__"):
            if not isinstance(meta.get(key), str) or not 1 <= len(meta[key]) <= 100:
                raise ValueError("Missing or invalid " + key)
        return meta

    def _read_plugin_file(self, path):
        with open(path, "rb") as stream:
            raw = stream.read(4194305)
        if len(raw) > 4194304: raise ValueError("Plugin exceeds 4 MB")
        return raw, raw.decode("utf-8-sig")

    @serialized
    def preview_file(self, path):
        raw, source = self._read_plugin_file(path)
        meta = self.metadata(source)
        pid = meta["__id__"]
        meta["sha256"] = hashlib.sha256(raw).hexdigest()
        meta["bytes"] = len(raw)
        installed = self.root / (pid + ".py")
        meta["installed_version"] = self.metadata(installed.read_text("utf-8"))["__version__"] if installed.exists() else ""
        return json.dumps(meta, ensure_ascii=False)

    @serialized
    def install_file(self, path, expected_sha256, enabled=False):
        raw, source = self._read_plugin_file(path)
        if hashlib.sha256(raw).hexdigest() != expected_sha256:
            raise ValueError("The file changed after preview. Open it again.")
        pid = self.install(source)
        if enabled: self.enable(pid)
        return pid

    @serialized
    def install(self, source):
        source = source.removeprefix("\ufeff")
        meta = self.metadata(source)
        pid = meta["__id__"]
        existing = self.root / (pid + ".py")
        if existing.exists():
            old_id = self.metadata(existing.read_text("utf-8"))["__id__"]
            if old_id != pid and old_id.casefold() == pid.casefold():
                raise ValueError("Plugin ID conflicts with an installed ID differing only in case")
        self.disable(pid)
        temp = self.root / (pid + ".tmp")
        temp.write_text(source, "utf-8")
        os.replace(temp, self.root / (pid + ".py"))
        return pid

    @serialized
    def install_bundled(self, source, enabled=True):
        """One-time opt-in authorized by the client owner; respect safe mode and later changes."""
        pid = self.metadata(source)["__id__"]
        receipt = self.root / (pid + ".bundled")
        if receipt.exists(): return pid
        # Record before executing, so a failed first load is never retried in a loop.
        activate = enabled or self.state.get(pid, {}).get("enabled", False)
        self.install(source)
        receipt.write_text("1", "utf-8")
        if activate and not self.recovery:
            try: self.enable(pid)
            except BaseException as error: self.errors[pid] = str(error)
        return pid

    @serialized
    def enable(self, pid):
        pid = self._id(pid)
        if pid in self.loaded: return
        source = (self.root / (pid + ".py")).read_text("utf-8")
        if self.metadata(source)["__id__"] != pid: raise ValueError("ID mismatch")
        # Mark loading before running user code. An interrupted load stays disabled.
        self.state.setdefault(pid, {})["enabled"] = False
        self._save()
        module_name = "claud_plugin_" + pid
        module = types.ModuleType(module_name)
        module.__file__ = str(self.root / (pid + ".py"))
        sys.modules[module_name] = module
        instance = None
        try:
            tree = ast.parse(source, filename=module.__file__)
            # Display-name metadata must not replace Python's module identity.
            for index, node in enumerate(tree.body):
                if isinstance(node, ast.Assign):
                    node.targets = [target for target in node.targets
                                    if not (isinstance(target, ast.Name) and target.id == "__name__")]
                    if not node.targets: tree.body[index] = ast.copy_location(ast.Pass(), node)
            ast.fix_missing_locations(tree)
            exec(compile(tree, module.__file__, "exec"), module.__dict__)
            classes = [v for v in module.__dict__.values() if inspect.isclass(v) and v is not BasePlugin and issubclass(v, BasePlugin) and v.__module__ == module_name]
            if len(classes) != 1: raise ValueError("Expected one BasePlugin subclass")
            instance = classes[0].__new__(classes[0])
            instance._engine, instance._id = self, pid
            classes[0].__init__(instance)
            instance.on_plugin_load()
            self.loaded[pid] = instance
            self.state[pid]["enabled"] = True
            self._save()
            self.errors.pop(pid, None)
        except BaseException as error:
            if instance:
                try: instance.on_plugin_unload()
                except BaseException: pass
                finally: instance._dispose()
            self.loaded.pop(pid, None)
            sys.modules.pop(module_name, None)
            self.state[pid]["enabled"] = False
            self._save()
            self.errors[pid] = str(error)
            raise

    @serialized
    def disable(self, pid):
        pid = self._id(pid)
        self.state.setdefault(pid, {})["enabled"] = False
        self._save()
        instance = self.loaded.pop(pid, None)
        try:
            if instance: instance.on_plugin_unload()
        except BaseException as error:
            self.errors[pid] = str(error)
        finally:
            if instance: instance._dispose()
            sys.modules.pop("claud_plugin_" + pid, None)

    @serialized
    def remove(self, pid):
        self.disable(pid)
        (self.root / (pid + ".py")).unlink(missing_ok=True)
        self.state.pop(pid, None)
        self._save()

    @serialized
    def restore(self):
        for pid, state in list(self.state.items()):
            if state.get("enabled"):
                try: self.enable(pid)
                except BaseException as error: self.errors[pid] = str(error)

    @serialized
    def setting(self, pid, key, default=None):
        return self.state.get(pid, {}).get("settings", {}).get(key, default)

    @serialized
    def set_setting(self, pid, key, value):
        json.dumps(value)
        self.state.setdefault(pid, {}).setdefault("settings", {})[key] = value
        self._save()

    @serialized
    def settings_json(self, pid):
        return json.dumps(self.state.get(self._id(pid), {}).get("settings", {}), ensure_ascii=False)

    @serialized
    def save_settings_json(self, pid, text):
        value = json.loads(text)
        if not isinstance(value, dict): raise ValueError("Settings must be a JSON object")
        self.state.setdefault(self._id(pid), {})["settings"] = value
        self._save()

    def _enter(self, pid):
        temp = self.root / "executing.tmp"
        temp.write_text(json.dumps({"id": pid}), "utf-8")
        os.replace(temp, self.root / "executing.json")

    def _leave(self):
        (self.root / "executing.json").unlink(missing_ok=True)

    def _settings_rows(self, pid, rows):
        from ui.settings import Header, Switch, Input, Selector, Text, Divider, EditText, Custom
        if not hasattr(self, "_ui_rows"): self._ui_rows = {}
        store = self._ui_rows.setdefault(pid, {})
        result = []
        for row in rows:
            index = len(store)
            store[index] = row
            info = {"index": index, "text": str(getattr(row,"text",getattr(row,"hint","")))}
            if isinstance(row, (Header,Divider)): info["type"] = "header"
            elif isinstance(row, Custom): info["type"] = "custom"
            elif isinstance(row, Text):
                info.update(type="text", subtext=str(row.subtext))
            elif isinstance(row,(Switch,Input,Selector,EditText)):
                info.update(type="switch" if isinstance(row,Switch) else "selector" if isinstance(row,Selector) else "input",
                            key=row.key, value=self.setting(pid,row.key,row.default))
                if isinstance(row,Selector): info["items"] = list(row.items)
            else: raise ValueError("Unsupported settings row: " + type(row).__name__)
            result.append(info)
        return json.dumps(result,ensure_ascii=False)

    def _open_custom_profile(self, instance, workshop=False):
        from java import jboolean
        from java import jarray, jclass
        def args(*values):
            result = jarray(jclass("java.lang.Object"))(len(values))
            for index, value in enumerate(values): result[index] = value
            return result
        core = instance._core
        native = core._methods["get"].invoke(None, args())
        if workshop:
            for method in core._cls.getDeclaredMethods():
                if method.getName() == "workshop" and len(method.getParameterTypes()) == 0:
                    target = method.invoke(native, args())
                    for entry in target.getClass().getMethods():
                        if entry.getName() == "open" and len(entry.getParameterTypes()) == 0:
                            entry.invoke(target, args())
                            return
        else:
            for method in core._cls.getDeclaredMethods():
                if method.getName() == "openEditorAt" and len(method.getParameterTypes()) == 2:
                    method.invoke(native, args("banner", jboolean(True)))
                    return
        raise ValueError("Custom Profile entry point unavailable in this plugin version")

    @serialized
    def settings_schema(self,pid):
        instance = self.loaded.get(self._id(pid))
        if instance is None: raise ValueError("Enable the plugin to open settings")
        if not hasattr(self,"_ui_rows"): self._ui_rows = {}
        self._ui_rows[pid] = {}
        rows = list(instance.create_settings())
        # Keep the dedicated workspace usable after a user installs the original
        # plugin or updates its launcher, without relying on our bundled wrapper.
        if pid == "custom_profile" and getattr(instance, "_core", None) is not None:
            from ui.settings import Text
            launcher_titles = {"Оформить мой профиль", "Мастерская оформления"}
            rows = [row for row in rows if not isinstance(row, Text) or row.text not in launcher_titles]
            rows = [Text("Оформить мой профиль", on_click=lambda _view: self._open_custom_profile(instance)),
                    Text("Мастерская оформления", on_click=lambda _view: self._open_custom_profile(instance, True))] + rows
        return self._settings_rows(pid,rows)

    @serialized
    def setting_click(self,pid,index,view=None):
        row = self._ui_rows[pid][int(index)]
        child = getattr(row,"create_sub_fragment",None)
        if child is not None: return self._settings_rows(pid,child())
        click = getattr(row,"on_click",None)
        if click is not None: click(view)
        return ""

    @serialized
    def change_setting(self,pid,index,value):
        row = self._ui_rows[pid][int(index)]
        self.set_setting(pid,row.key,value)
        callback = getattr(row,"on_change",None)
        if callback is not None: callback(value)

    @serialized
    def custom_view(self,pid,index,context):
        row = self._ui_rows[pid][int(index)]
        if row.view is not None: return row.view
        if row.item is not None: return row.item.view
        factory = row.factory
        if factory is not None:
            from client_utils import get_selected_account
            view = factory.create_view(context,None,get_selected_account(),0,None)
            if factory.bind_view is not None: factory.bind_view(view,None,False,None,None)
            return view
        raise ValueError("Custom setting has no view or factory")

    @serialized
    def menu_actions(self,pid):
        instance = self.loaded.get(pid)
        if instance is None: raise ValueError("Enable the plugin first")
        menu = getattr(instance,"_claud_menu",{})
        seen = set()
        result = []
        for key,data in sorted(menu.items(),key=lambda pair:-pair[1].priority):
            if data.text in seen: continue
            seen.add(data.text)
            result.append({"id":key,"text":data.text})
        return json.dumps(result,ensure_ascii=False)

    @serialized
    def menu_click(self,pid,key):
        from client_utils import get_last_fragment, get_selected_account
        instance = self.loaded.get(pid)
        if instance is None: raise ValueError("Plugin disabled")
        entry = instance._claud_menu[str(key)]
        fragment = get_last_fragment()
        entry.on_click({"account":get_selected_account(),"fragment":fragment,
                        "context":fragment.getParentActivity() if fragment else None})

    @serialized
    def dispatch(self, name, payload):
        for pid, instance in list(self.loaded.items()):
            try:
                self._enter(pid)
                if name in {event.value for event in AppEvent}:
                    instance.on_app_event(AppEvent(name))
                result = instance.on_event(name, payload)
                if result is not None: payload = result
            except BaseException as error:
                self.errors[pid] = str(error)
                try: self.disable(pid)
                except BaseException: pass
            finally: self._leave()
        return payload

    @serialized
    def listing(self):
        result = []
        for file in sorted(self.root.glob("*.py")):
            try:
                meta = self.metadata(file.read_text("utf-8"))
                meta["enabled"] = meta["__id__"] in self.loaded
                meta["error"] = self.errors.get(meta["__id__"], "")
                result.append(meta)
            except BaseException: pass
        return json.dumps(result, ensure_ascii=False)

_engine = None
def initialize(root):
    global _engine
    if _engine is None:
        _engine = Engine(root)
        _engine.restore()
    return _engine
