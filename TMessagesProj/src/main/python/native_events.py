from base_plugin import MethodHook, HookStrategy
from hook_utils import find_class, get_private_field
from account_context import account_scope

def _method(cls, name, count):
    return next(m for m in find_class(cls).getDeclaredMethods() if str(m.getName())==name and len(m.getParameterTypes())==count)

def install_send(plugin, priority):
    class Send(MethodHook):
        def before_hooked_method(self,param):
            account = int(get_private_field(param.thisObject,"currentAccount"))
            with account_scope(account):
                result = plugin.on_send_message_hook(account,param.args[0])
            if result is None: return
            if result.strategy == HookStrategy.CANCEL: param.setResult(None)
            elif result.params is not None: param.args[0] = result.params
    return plugin.hook_method(_method("org.telegram.messenger.SendMessagesHelper","sendMessage",1),Send(),priority)

def install_request(plugin, name, substring, priority):
    class Request(MethodHook):
        def before_hooked_method(self,param):
            request = param.args[0]
            actual = str(request.getClass().getSimpleName())
            if not (name in actual if substring else actual==name): return
            account = int(get_private_field(param.thisObject,"currentAccount"))
            with account_scope(account):
                result = plugin.pre_request_hook(actual,account,request)
            if result is not None:
                if result.strategy == HookStrategy.CANCEL: param.setResult(None); return
                if result.request is not None: param.args[0] = result.request
            original = param.args[1]
            if original is not None:
                from client_utils import RequestCallback
                def completed(response,error):
                    result = plugin.post_request_hook(actual,account,response,error)
                    if result is not None:
                        if result.strategy == HookStrategy.CANCEL: return
                        if result.response is not None: response = result.response
                        if result.error is not None: error = result.error
                    original.run(response,error)
                param.args[1] = RequestCallback(completed,account)
    return plugin.hook_method(_method("org.telegram.tgnet.ConnectionsManager","sendRequestInternal",10),Request(),priority)
