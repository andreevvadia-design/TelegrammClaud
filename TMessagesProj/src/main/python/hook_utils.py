"""Reflection helpers, compatible with common exteraGram SDK calls."""
from java import jclass

def find_class(name):
    context = jclass("org.telegram.messenger.ApplicationLoader").applicationContext
    return jclass("java.lang.Class").forName(str(name), False, context.getClassLoader())

def _field(obj, name):
    cls = obj.getClass()
    while cls is not None:
        try:
            field = cls.getDeclaredField(name)
            field.setAccessible(True)
            return field
        except Exception:
            cls = cls.getSuperclass()
    raise AttributeError(name)

def get_private_field(obj, name): return _field(obj, name).get(obj)
def set_private_field(obj, name, value): _field(obj, name).set(obj, value)
