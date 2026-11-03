from java import jclass, dynamic_proxy
class AlertDialogBuilder:
    def __init__(self, context, *args, **kwargs):
        self.builder = jclass("org.telegram.ui.ActionBar.AlertDialog$Builder")(context)
        self.listeners = []
    def set_title(self, text): self.builder.setTitle(str(text)); return self
    def set_message(self, text): self.builder.setMessage(str(text)); return self
    def _button(self, method, text, fn):
        listener = None
        if fn is not None:
            class Click(dynamic_proxy(jclass("android.content.DialogInterface$OnClickListener"))):
                def onClick(self, dialog, which): fn(dialog,which)
            listener = Click(); self.listeners.append(listener)
        getattr(self.builder,method)(str(text),listener); return self
    def set_positive_button(self,text,fn=None): return self._button("setPositiveButton",text,fn)
    def set_negative_button(self,text,fn=None): return self._button("setNegativeButton",text,fn)
    def set_neutral_button(self,text,fn=None): return self._button("setNeutralButton",text,fn)
    def set_view(self,view): self.builder.setView(view); return self
    def create(self): return self.builder.create()
    def show(self): return self.builder.show()
