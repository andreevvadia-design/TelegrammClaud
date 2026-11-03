from contextvars import ContextVar
from contextlib import contextmanager
_current = ContextVar("claud_plugin_account", default=None)
def get_hook_account(): return _current.get()
@contextmanager
def account_scope(account):
    token = _current.set(None if account is None else int(account))
    try: yield
    finally: _current.reset(token)
