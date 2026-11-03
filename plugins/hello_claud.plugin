from base_plugin import BasePlugin
from ui.settings import Header, Switch, Input
__id__ = "hello_claud"
__name__ = "Hello Claud"
__version__ = "1.1.0"
__description__ = "Changes Settings title; configurable from the plugin screen."
class HelloClaud(BasePlugin):
    def on_plugin_load(self):
        self.set_setting("loads", self.get_setting("loads", 0) + 1)
    def create_settings(self):
        return [Header("Appearance"), Switch("custom_title", "Change Settings title", True),
                Input("title", "Title", "claud code · Settings")]
    def on_event(self, name, payload):
        if name == "settings_title" and self.get_setting("custom_title", True):
            return self.get_setting("title", "claud code · Settings")
        return payload
