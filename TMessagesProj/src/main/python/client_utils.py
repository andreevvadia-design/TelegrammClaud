"""Account-aware access to Telegram controllers. Imported only on Android."""
from java import jclass
from account_context import get_hook_account, account_scope

def get_selected_account():
    return int(jclass("org.telegram.messenger.UserConfig").selectedAccount)

def _instance(class_name, account):
    if account is None: account = get_hook_account()
    if account is None: account = get_selected_account()
    limit = jclass("org.telegram.messenger.UserConfig").MAX_ACCOUNT_COUNT
    if not 0 <= int(account) < limit: raise ValueError("Invalid account")
    return jclass(class_name).getInstance(int(account))

def get_messages_controller(account=None):
    return _instance("org.telegram.messenger.MessagesController", account)

def get_user_config(account=None):
    return _instance("org.telegram.messenger.UserConfig", account)

def get_connections_manager(account=None):
    return _instance("org.telegram.tgnet.ConnectionsManager", account)

def get_send_messages_helper(account=None):
    return _instance("org.telegram.messenger.SendMessagesHelper", account)


def get_last_fragment():
    return jclass("org.telegram.ui.LaunchActivity").getLastFragmentIncludeMainTabs()

def get_media_controller(): return jclass("org.telegram.messenger.MediaController").getInstance()
def get_media_data_controller(account=None): return _instance("org.telegram.messenger.MediaDataController", account)
def get_account_instance(account=None): return _instance("org.telegram.messenger.AccountInstance", account)
def get_contacts_controller(account=None): return _instance("org.telegram.messenger.ContactsController", account)

from java import dynamic_proxy
class RequestCallback(dynamic_proxy(jclass("org.telegram.tgnet.RequestDelegate"))):
    def __init__(self, fn, account=None): super().__init__(); self.fn, self.account = fn, account
    def run(self, response, error):
        with account_scope(self.account): self.fn(response, error)

def send_request(request, fn, *, account=None):
    if account is None: account = get_hook_account()
    if account is None: account = get_selected_account()
    callback = RequestCallback(fn, account) if callable(fn) else fn
    return get_connections_manager(account).sendRequest(request, callback)

PLUGINS_QUEUE = "pluginsQueue"
GLOBAL_QUEUE = "globalQueue"
STAGE_QUEUE = "stageQueue"
def run_on_queue(fn, queue_name=PLUGINS_QUEUE, delay=0):
    from android_utils import log
    import threading
    captured_account = get_hook_account()
    def work():
        try:
            with account_scope(captured_account): fn()
        except Exception as error: log(error)
    timer = threading.Timer(max(0,delay)/1000,work); timer.daemon = True; timer.start(); return timer
