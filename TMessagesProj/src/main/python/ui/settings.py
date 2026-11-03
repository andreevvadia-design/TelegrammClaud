from dataclasses import dataclass
from typing import Any
@dataclass
class Header:
    text: str
@dataclass
class Divider:
    text: str = ""
@dataclass
class Switch:
    key: str
    text: str
    default: bool = False
    subtext: str = ""
    icon: str = ""
    on_change: Any = None
    on_long_click: Any = None
    link_alias: str = ""
@dataclass
class Input:
    key: str
    text: str
    default: str = ""
    subtext: str = ""
    icon: str = ""
    on_change: Any = None
    on_long_click: Any = None
    link_alias: str = ""
@dataclass
class Selector:
    key: str
    text: str
    default: int
    items: list
    icon: str = ""
    on_change: Any = None
    on_long_click: Any = None
    link_alias: str = ""
@dataclass
class Text:
    text: str
    subtext: str = ""
    icon: str = ""
    accent: bool = False
    red: bool = False
    on_click: Any = None
    create_sub_fragment: Any = None
    on_long_click: Any = None
    link_alias: str = ""
@dataclass
class EditText:
    key: str
    hint: str
    default: str = ""
    multiline: bool = False
    max_length: int = 0
    mask: str = ""
    on_change: Any = None
@dataclass
class Custom:
    item: Any = None
    view: Any = None
    factory: Any = None
    factory_args: Any = None
    on_click: Any = None
    on_long_click: Any = None
    create_sub_fragment: Any = None
    link_alias: str = ""
class SimpleSettingFactory:
    def __init__(self,create_view,bind_view,**kwargs):
        self.create_view,self.bind_view = create_view,bind_view
        self.instance = self
        self.java = self
    def __call__(self,*args,**kwargs): return Custom(factory=self,factory_args=args,**kwargs)
    def release(self): self.create_view,self.bind_view = None,None
